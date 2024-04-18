package com.genersoft.iot.vmp.conf.redis;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.service.redisMsg.control.RedisRpcController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@Component
public class RedisRpcConfig implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisRpcConfig.class);

    public final static String REDIS_REQUEST_CHANNEL_KEY = "WVP_REDIS_REQUEST_CHANNEL_KEY";

    private final Random random = new Random();

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisRpcController redisRpcController;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {
                        RedisRpcMessage redisRpcMessage = JSON.parseObject(new String(msg.getBody()), RedisRpcMessage.class);
                        if (redisRpcMessage.getRequest() != null) {
                            handlerRequest(redisRpcMessage.getRequest());
                        } else if (redisRpcMessage.getResponse() != null){
                            handlerResponse(redisRpcMessage.getResponse());
                        } else {
                            logger.error("[redis rpc 解析失败] {}", JSON.toJSONString(redisRpcMessage));
                        }
                    } catch (Exception e) {
                        logger.error("[redis rpc 解析异常] ", e);
                    }
                }
            });
        }
    }

    private void handlerResponse(RedisRpcResponse response) {
        if (userSetting.getServerId().equals(response.getToId())) {
            return;
        }
        logger.info("[redis-rpc] << {}", response);
        response(response);
    }

    private void handlerRequest(RedisRpcRequest request) {
        try {
            if (userSetting.getServerId().equals(request.getFromId())) {
                return;
            }
            logger.info("[redis-rpc] << {}", request);
            Method method = getMethod(request.getUri());
            // 没有携带目标ID的可以理解为哪个wvp有结果就哪个回复，携带目标ID，但是如果是不存在的uri则直接回复404
            if (userSetting.getServerId().equals(request.getToId())) {
                if (method == null) {
                    // 回复404结果
                    RedisRpcResponse response = request.getResponse();
                    response.setStatusCode(404);
                    sendResponse(response);
                    return;
                }
                RedisRpcResponse response = (RedisRpcResponse)method.invoke(redisRpcController, request);
                if(response != null) {
                    sendResponse(response);
                }
            }else {
                if (method == null) {
                    return;
                }
                RedisRpcResponse response = (RedisRpcResponse)method.invoke(redisRpcController, request);
                if (response != null) {
                    sendResponse(response);
                }
            }
        }catch (InvocationTargetException | IllegalAccessException e) {
            logger.error("[redis rpc ] 处理请求失败 ", e);
        }

    }

    private Method getMethod(String name) {
        // 启动后扫描所有的路径注解
        Method[] methods = redisRpcController.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    private void sendResponse(RedisRpcResponse response){
        logger.info("[redis-rpc] >> {}", response);
        response.setToId(userSetting.getServerId());
        RedisRpcMessage message = new RedisRpcMessage();
        message.setResponse(response);
        redisTemplate.convertAndSend(REDIS_REQUEST_CHANNEL_KEY, message);
    }

    private void sendRequest(RedisRpcRequest request){
        logger.info("[redis-rpc] >> {}", request);
        RedisRpcMessage message = new RedisRpcMessage();
        message.setRequest(request);
        redisTemplate.convertAndSend(REDIS_REQUEST_CHANNEL_KEY, message);
    }


    private final Map<Long, SynchronousQueue<RedisRpcResponse>> topicSubscribers = new ConcurrentHashMap<>();
    private final Map<Long, CommonCallback<RedisRpcResponse>> callbacks = new ConcurrentHashMap<>();

    public RedisRpcResponse request(RedisRpcRequest request, int timeOut) {
        request.setSn((long) random.nextInt(1000) + 1);
        SynchronousQueue<RedisRpcResponse> subscribe = subscribe(request.getSn());
        try {
            sendRequest(request);
            return subscribe.poll(timeOut, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("[redis rpc timeout] uri: {}, sn: {}", request.getUri(), request.getSn(), e);
        } finally {
            this.unsubscribe(request.getSn());
        }
        return null;
    }

    public void request(RedisRpcRequest request, CommonCallback<RedisRpcResponse> callback) {
        request.setSn((long) random.nextInt(1000) + 1);
        setCallback(request.getSn(), callback);
        sendRequest(request);
    }

    public Boolean response(RedisRpcResponse response) {
        SynchronousQueue<RedisRpcResponse> queue = topicSubscribers.get(response.getSn());
        CommonCallback<RedisRpcResponse> callback = callbacks.get(response.getSn());
        if (queue != null) {
            try {
                return queue.offer(response, 2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("{}", e.getMessage(), e);
            }
        }else if (callback != null) {
            callback.run(response);
            callbacks.remove(response.getSn());
        }
        return false;
    }

    private void unsubscribe(long key) {
        topicSubscribers.remove(key);
    }


    private SynchronousQueue<RedisRpcResponse> subscribe(long key) {
        SynchronousQueue<RedisRpcResponse> queue = null;
        if (!topicSubscribers.containsKey(key))
            topicSubscribers.put(key, queue = new SynchronousQueue<>());
        return queue;
    }

    private void setCallback(long key, CommonCallback<RedisRpcResponse> callback)  {
        // TODO 如果多个上级点播同一个通道会有问题
        callbacks.put(key, callback);
    }

    public void removeCallback(long key)  {
        callbacks.remove(key);
    }


    public int getCallbackCount(){
        return callbacks.size();
    }
}
