package com.genersoft.iot.vmp.conf.redis;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcClassHandler;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisRpcConfig implements MessageListener {

    public final static String REDIS_REQUEST_CHANNEL_KEY = "WVP_REDIS_REQUEST_CHANNEL_KEY";

    private final Random random = new Random();

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    private final static Map<String, RedisRpcClassHandler> protocolHash = new HashMap<>();

    public void addHandler(String path, RedisRpcClassHandler handler) {
        protocolHash.put(path, handler);
    }

//    @Override
//    public void run(String... args) throws Exception {
//        List<Class<?>> classList = ClassUtil.getClassList("com.genersoft.iot.vmp.service.redisMsg.control", RedisRpcController.class);
//        for (Class<?> handlerClass : classList) {
//            String controllerPath = handlerClass.getAnnotation(RedisRpcController.class).value();
//            Object bean = ClassUtil.getBean(controllerPath, handlerClass);
//            // 扫描其下的方法
//            Method[] methods = handlerClass.getDeclaredMethods();
//            for (Method method : methods) {
//                RedisRpcMapping annotation = method.getAnnotation(RedisRpcMapping.class);
//                if (annotation != null) {
//                    String methodPath =  annotation.value();
//                    if (methodPath != null) {
//                        protocolHash.put(controllerPath + "/" + methodPath, new RedisRpcClassHandler(bean, method));
//                    }
//                }
//
//            }
//
//        }
//        for (String s : protocolHash.keySet()) {
//            System.out.println(s);
//        }
//        if (log.isDebugEnabled()) {
//            log.debug("消息ID缓存表 protocolHash:{}", protocolHash);
//        }
//    }

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
                            log.error("[redis-rpc]解析失败 {}", JSON.toJSONString(redisRpcMessage));
                        }
                    } catch (Exception e) {
                        log.error("[redis-rpc]解析异常 {}",new String(msg.getBody()), e);
                    }
                }
            });
        }
    }

    private void handlerResponse(RedisRpcResponse response) {
        if (userSetting.getServerId().equals(response.getToId())) {
            return;
        }
        log.info("[redis-rpc] << {}", response);
        response(response);
    }

    private void handlerRequest(RedisRpcRequest request) {
        try {
            if (userSetting.getServerId().equals(request.getFromId())) {
                return;
            }
            log.info("[redis-rpc] << {}", request);
            RedisRpcClassHandler redisRpcClassHandler = protocolHash.get(request.getUri());
            if (redisRpcClassHandler == null) {
                log.error("[redis-rpc] 路径: {}不存在", request.getUri());
                return;
            }
            RpcController controller = redisRpcClassHandler.getController();
            Method method = redisRpcClassHandler.getMethod();
            // 没有携带目标ID的可以理解为哪个wvp有结果就哪个回复，携带目标ID，但是如果是不存在的uri则直接回复404
            if (userSetting.getServerId().equals(request.getToId())) {
                if (method == null) {
                    // 回复404结果
                    RedisRpcResponse response = request.getResponse();
                    response.setStatusCode(ErrorCode.ERROR404.getCode());
                    sendResponse(response);
                    return;
                }
                RedisRpcResponse response = (RedisRpcResponse)method.invoke(controller, request);
                if(response != null) {
                    sendResponse(response);
                }
            }else {
                if (method == null) {
                    return;
                }
                RedisRpcResponse response = (RedisRpcResponse)method.invoke(controller, request);
                if (response != null) {
                    sendResponse(response);
                }
            }
        }catch (InvocationTargetException | IllegalAccessException e) {
            log.error("[redis-rpc ] 处理请求失败 ", e);
        }
    }

    private void sendResponse(RedisRpcResponse response){
        log.info("[redis-rpc] >> {}", response);
        response.setToId(userSetting.getServerId());
        RedisRpcMessage message = new RedisRpcMessage();
        message.setResponse(response);
        redisTemplate.convertAndSend(REDIS_REQUEST_CHANNEL_KEY, message);
    }

    private void sendRequest(RedisRpcRequest request){
        log.info("[redis-rpc] >> {}", request);
        RedisRpcMessage message = new RedisRpcMessage();
        message.setRequest(request);
        redisTemplate.convertAndSend(REDIS_REQUEST_CHANNEL_KEY, message);
    }

    private final Map<Long, SynchronousQueue<RedisRpcResponse>> topicSubscribers = new ConcurrentHashMap<>();
    private final Map<Long, CommonCallback<RedisRpcResponse>> callbacks = new ConcurrentHashMap<>();

    public RedisRpcResponse request(RedisRpcRequest request, long timeOut) {
        return request(request, timeOut, TimeUnit.SECONDS);
    }

    public RedisRpcResponse request(RedisRpcRequest request, long timeOut, TimeUnit timeUnit) {
        request.setSn((long) random.nextInt(1000) + 1);
        SynchronousQueue<RedisRpcResponse> subscribe = subscribe(request.getSn());

        try {
            sendRequest(request);
            return subscribe.poll(timeOut, timeUnit);
        } catch (InterruptedException e) {
            log.warn("[redis rpc timeout] uri: {}, sn: {}", request.getUri(), request.getSn(), e);
            RedisRpcResponse redisRpcResponse = new RedisRpcResponse();
            redisRpcResponse.setStatusCode(ErrorCode.ERROR486.getCode());
            return redisRpcResponse;
        } finally {
            this.unsubscribe(request.getSn());
        }
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
                log.error("{}", e.getMessage(), e);
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




//    @Scheduled(fixedRate = 1000)   //每1秒执行一次
//    public void execute(){
//        logger.info("callbacks的长度: " + callbacks.size());
//        logger.info("队列的长度: " + topicSubscribers.size());
//        logger.info("HOOK监听的长度: " + hookSubscribe.size());
//        logger.info("");
//    }
}
