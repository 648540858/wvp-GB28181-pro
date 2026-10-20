package com.genersoft.iot.vmp.conf;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 动态定时任务
 * @author lin
 */
@Slf4j
@Component
public class DynamicTask {

    @Autowired
    private TaskScheduler taskScheduler;

    private final Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final Map<String, Runnable> runnableMap = new ConcurrentHashMap<>();


    /**
     * 循环执行的任务
     * @param key 任务ID
     * @param task 任务
     * @param cycleForCatalog 间隔 毫秒
     * @return
     */
    public void startCron(String key, Runnable task, int cycleForCatalog) {
        if(ObjectUtils.isEmpty(key)) {
            return;
        }
        ScheduledFuture<?> future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                log.debug("任务【{}】已存在但是关闭状态！！！", key);
            } else {
                log.debug("任务【{}】已存在且已启动！！！", key);
                return;
            }
        }
        // scheduleWithFixedDelay 必须等待上一个任务结束才开始计时period， cycleForCatalog表示执行的间隔

        future = taskScheduler.scheduleAtFixedRate(task, new Date(System.currentTimeMillis() + cycleForCatalog), cycleForCatalog);
        if (future != null){
            futureMap.put(key, future);
            runnableMap.put(key, task);
            log.debug("任务【{}】启动成功！！！", key);
        }else {
            log.debug("任务【{}】启动失败！！！", key);
        }
    }

    /**
     * 延时任务
     * @param key 任务ID
     * @param task 任务
     * @param delay 延时 /毫秒
     * @return
     */
    public void startDelay(String key, Runnable task, int delay) {
        if(ObjectUtils.isEmpty(key)) {
            return;
        }
        stop(key);

        // 获取执行的时刻
        Instant startInstant = Instant.now().plusMillis(TimeUnit.MILLISECONDS.toMillis(delay));

        ScheduledFuture future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                log.debug("任务【{}】已存在但是关闭状态！！！", key);
            } else {
                log.debug("任务【{}】已存在且已启动！！！", key);
                return;
            }
        }
        // scheduleWithFixedDelay 必须等待上一个任务结束才开始计时period， cycleForCatalog表示执行的间隔
        future = taskScheduler.schedule(task, startInstant);
        if (future != null){
            futureMap.put(key, future);
            runnableMap.put(key, task);
            log.debug("任务【{}】启动成功！！！", key);
        }else {
            log.debug("任务【{}】启动失败！！！", key);
        }
    }

    /**
     * 停止并移除指定 key 的任务。
     *
     * <p>这里的核心约定是：<b>futureMap / runnableMap 中不允许残留“已结束但未被移除”的任务</b>。
     * 因为 startDelay()/startCron() 判断“任务是否已存在”时只看 futureMap 中是否有值、
     * 以及该 future 是否 isCancelled()。一个已经执行完成（isDone() == true）的 future
     * 如果继续留在 map 里，会被误判为“任务仍然存在且处于启动状态”，从而拒绝后续同 key 任务的注册。
     * 典型场景：收流超时看门狗执行完成后，future 不会被自动移除；同一通道再次点播时
     * startDelay() 发现旧 future 仍在 map 中，新的看门狗注册失败，最终导致点播回调永久残留。</p>
     *
     * <p>并发边界说明：本方法不是原子的。可能有其他线程在 get() 与 remove() 之间
     * 用同一个 key 注册了新任务（startDelay 内部就是先 stop 再 schedule）。
     * 因此这里使用 {@code remove(key, future)} 这种“比较后删除”的方式：
     * 只有 map 中当前值仍然是本次读到的那个 future 时才删除。
     * 如果期间已经被别的线程替换成新 future，则保留新任务，避免误删。
     * runnableMap 同理，先取出当前 runnable 再做条件删除。</p>
     *
     * @param key 任务ID
     * @return 是否成功取消了本次读取到的、仍在运行中的任务。
     *         任务已完成/已取消/不存在时返回 false，但这不代表失败：
     *         已完成或已取消的任务同样会从 map 中清理掉。
     */
    public boolean stop(String key) {
        if(ObjectUtils.isEmpty(key)) {
            return false;
        }
        boolean result = false;
        ScheduledFuture<?> future = futureMap.get(key);
        if (future == null) {
            // 没有任务，或已经被其他线程清理。runnableMap 理论上不会单独存在，
            // 但这里不做额外处理，避免误删其他线程刚注册的新任务。
            log.debug("任务【{}】不存在或已被清理，无需停止", key);
            return false;
        }
        if (!future.isCancelled() && !future.isDone()) {
            // cancel(false)：不中断正在执行的任务，只阻止尚未开始的任务继续执行。
            result = future.cancel(false);
            if (result) {
                log.debug("任务【{}】已取消", key);
            } else {
                // 通常表示任务已经进入执行阶段，cancel 不会中断它。
                // 此时仍然要清理 map，让后续同 key 任务可以重新注册。
                log.debug("任务【{}】取消失败（任务可能已经开始执行），继续清理任务记录", key);
            }
        } else {
            // 已取消/已执行完成的任务不再需要取消，但必须从 map 中移除，
            // 否则残留的 completed future 会拦截后续同key任务的注册(startDelay)，
            // 例如收流超时看门狗执行完成后，同通道再次点播将无法注册新的看门狗。
            log.debug("任务【{}】已结束（cancelled={}, done={}），从任务表中移除",
                    key, future.isCancelled(), future.isDone());
        }

        // 比较后删除：仅当 map 中仍是本次读到的 future 时才删除，
        // 防止并发场景下误删其他线程刚注册的新任务。
        if (futureMap.remove(key, future)) {
            Runnable runnable = runnableMap.get(key);
            if (runnable != null) {
                runnableMap.remove(key, runnable);
            }
            log.debug("任务【{}】已从任务表移除", key);
        } else {
            // 走到这里说明在本次 stop 期间，该 key 已经被其他线程移除或替换成了新任务。
            // 新任务不属于本次 stop 的处理范围，保留即可，否则会误删其他线程刚注册的任务。
            log.debug("任务【{}】在停止过程中已被移除或替换，保留当前任务", key);
        }
        return result;
    }

    public boolean contains(String key) {
        if(ObjectUtils.isEmpty(key)) {
            return false;
        }
        return futureMap.get(key) != null;
    }

    public Set<String> getAllKeys() {
        return futureMap.keySet();
    }

    public Runnable get(String key) {
        if(ObjectUtils.isEmpty(key)) {
            return null;
        }
        return runnableMap.get(key);
    }

    /**
     * 每五分钟检查失效的任务，并移除。
     *
     * <p>并发边界说明：futureMap 是 ConcurrentHashMap，keySet() 的迭代是“弱一致”的，
     * 迭代过程中其他线程（stop / startDelay）可能已经把某个 key 删除或替换。
     * 因此 {@code futureMap.get(key)} 可能返回 null，必须先判空再调用 isDone()，
     * 否则会在定时清理线程里抛出 NullPointerException。
     * 删除时同样使用 {@code remove(key, future)}：只删除本次检查到的那个 future，
     * 避免把其他线程刚注册的新任务误删。</p>
     */
    @Scheduled(cron="0 0/5 * * * ?")
    public void execute(){
        if (futureMap.isEmpty()) {
            return;
        }
        for (String key : futureMap.keySet()) {
            ScheduledFuture<?> future = futureMap.get(key);
            if (future == null) {
                // 迭代期间被其他线程删除，属于正常并发情况，跳过即可。
                continue;
            }
            if (future.isDone() || future.isCancelled()) {
                if (futureMap.remove(key, future)) {
                    Runnable runnable = runnableMap.get(key);
                    if (runnable != null) {
                        runnableMap.remove(key, runnable);
                    }
                    log.debug("定时清理：移除已结束的任务【{}】", key);
                }
            }
        }
    }

    public boolean isAlive(String key) {
        return futureMap.get(key) != null && !futureMap.get(key).isDone() && !futureMap.get(key).isCancelled();
    }
}
