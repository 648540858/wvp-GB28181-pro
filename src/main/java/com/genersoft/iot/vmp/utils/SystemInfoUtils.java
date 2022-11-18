package com.genersoft.iot.vmp.utils;

import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 实现参考自xiaozhangnomoney原创文章，
 * 版权声明：本文为xiaozhangnomoney原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接和本声明
 * 原文出处链接：https://blog.csdn.net/xiaozhangnomoney/article/details/107769147
 */
public class SystemInfoUtils {

    private final static Logger logger = LoggerFactory.getLogger(SystemInfoUtils.class);

    /**
     * 获取cpu信息
     * @return
     * @throws InterruptedException
     */
    public static double getCpuInfo() throws InterruptedException {
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // 睡眠1s
        TimeUnit.SECONDS.sleep(1);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
        return 1.0-(idle * 1.0 / totalCpu);
    }

    /**
     * 获取内存使用率
     * @return
     */
    public static double getMemInfo(){
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        //总内存
        long totalByte = memory.getTotal();
        //剩余
        long acaliableByte = memory.getAvailable();
        return (totalByte-acaliableByte)*1.0/totalByte;
    }

    /**
     * 获取网络上传和下载
     * @return
     */
    public static Map<String,Double> getNetworkInterfaces() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        List<NetworkIF> beforeRecvNetworkIFs = hal.getNetworkIFs();
        NetworkIF beforeBet= beforeRecvNetworkIFs.get(beforeRecvNetworkIFs.size() - 1);
        long beforeRecv = beforeBet.getBytesRecv();
        long beforeSend = beforeBet.getBytesSent();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("[线程休眠失败] : {}", e.getMessage());
        }
        List<NetworkIF> afterNetworkIFs = hal.getNetworkIFs();
        NetworkIF afterNet = afterNetworkIFs.get(afterNetworkIFs.size() - 1);

        HashMap<String, Double> map = new HashMap<>();
        // 速度单位: Mbps
        map.put("in",formatUnits(afterNet.getBytesRecv()-beforeRecv, 1048576L));
        map.put("out",formatUnits(afterNet.getBytesSent()-beforeSend, 1048576L));
        return map;
    }

    /**
     * 获取带宽总值
     * @return
     */
    public static long getNetworkTotal() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        List<NetworkIF> recvNetworkIFs = hal.getNetworkIFs();
        NetworkIF networkIF= recvNetworkIFs.get(recvNetworkIFs.size() - 1);

        return networkIF.getSpeed()/1048576L/8L;
    }

    public static double formatUnits(long value, long prefix) {
        return (double)value / (double)prefix;
    }

    /**
     * 获取进程数
     * @return
     */
    public static int getProcessesCount(){
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();

        int processCount = os.getProcessCount();
        return processCount;
    }

    public static List<Map<String, Object>> getDiskInfo() {
        List<Map<String, Object>> result = new ArrayList<>();

        String osName = System.getProperty("os.name");
        List<String> pathArray = new ArrayList<>();
        if (osName.startsWith("Mac OS")) {
            // 苹果
            pathArray.add("/");
        } else if (osName.startsWith("Windows")) {
            // windows
            pathArray.add("C:");
        } else {
            pathArray.add("/");
            pathArray.add("/home");
        }
        for (String path : pathArray) {
            Map<String, Object> infoMap = new HashMap<>();
            infoMap.put("path", path);
            File partitionFile = new File(path);
            // 单位： GB
            infoMap.put("use", (partitionFile.getTotalSpace() - partitionFile.getFreeSpace())/1024/1024/1024D);
            infoMap.put("free", partitionFile.getFreeSpace()/1024/1024/1024D);
            result.add(infoMap);
        }
        return result;
    }
}
