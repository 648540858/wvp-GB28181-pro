package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IRecordPlanService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.service.bean.RecordPlanItem;
import com.genersoft.iot.vmp.storager.dao.RecordPlanMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RecordPlanServiceImpl implements IRecordPlanService {

    @Autowired
    private RecordPlanMapper recordPlanMapper;

    @Autowired
    private CommonGBChannelMapper channelMapper;

    @Autowired
    private IGbChannelPlayService channelPlayService;

    @Autowired
    private IMediaServerService mediaServerService;



    /**
     * 流离开的处理
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        // 流断开，检查是否还处于录像状态， 如果是则继续录像
        Integer channelId = recording(event.getApp(), event.getStream());
        if(channelId == null) {
            // 内存中没有匹配记录，按 stream 反查通道，若当前时间段需要录像则继续走下面的拉起逻辑
            channelId = queryRecordChannelIdByStream(event.getApp(), event.getStream(), queryCurrentChannelRecord());
            if (channelId == null) {
                return;
            }
        }
        // 重新拉起
        CommonGBChannel channel = channelMapper.queryById(channelId);
        if (channel == null) {
            log.warn("[录制计划] 流离开时拉起需要录像的流时, 发现通道不存在, id: {}", channelId);
            return;
        }
        final Integer recordChannelId = channelId;
        // 开启点播,
        channelPlayService.play(channel, null, true, ((code, msg, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode() && streamInfo != null) {
                log.info("[录像] 流离开时拉起需要录像的流, 开启成功, 通道ID: {}", channel.getGbId());
                recordStreamMap.put(channel.getGbId(), streamInfo);
            } else {
                recordStreamMap.remove(recordChannelId);
                log.info("[录像] 流离开时拉起需要录像的流, 开启失败, 1分钟后重试,  通道ID: {}", channel.getGbId());
            }
        }));
    }

    // 定时任务、流事件监听、流媒体hook 多个线程都会读写, 必须用并发容器
    Map<Integer, StreamInfo> recordStreamMap = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void execution() {
        // 对账: 核实内存中"正在录像"的流在流媒体中真实存在
        reconcileRecordStreams();

        // 查询现在需要录像的通道Id
        List<Integer> startChannelIdList = queryCurrentChannelRecord();

        if (startChannelIdList == null || startChannelIdList.isEmpty()) {
            // 当前没有录像任务, 如果存在旧的正在录像的就移除
            if(!recordStreamMap.isEmpty()) {
                Set<Integer> recordStreamSet = new HashSet<>(recordStreamMap.keySet());
                stopStreams(recordStreamSet, recordStreamMap);
                recordStreamMap.clear();
            }
        }else {
            // 当前存在录像任务, 获取正在录像中存在但是当前录制列表不存在的内容,进行停止; 获取正在录像中没有但是当前需录制的列表中存在的进行开启.
            Set<Integer> recordStreamSet = new HashSet<>(recordStreamMap.keySet());
            startChannelIdList.forEach(recordStreamSet::remove);
            if (!recordStreamSet.isEmpty()) {
                // 正在录像中存在但是当前录制列表不存在的内容,进行停止;
                stopStreams(recordStreamSet, recordStreamMap);
            }

            // 移除startChannelIdList中已经在录像的部分, 剩下的都是需要新添加的(正在录像中没有但是当前需录制的列表中存在的进行开启)
            recordStreamMap.keySet().forEach(startChannelIdList::remove);
            if (!startChannelIdList.isEmpty()) {
                // 获取所有的关联的通道
                List<CommonGBChannel> channelList = channelMapper.queryByIds(startChannelIdList);
                if (!channelList.isEmpty()) {
                    // 查找是否已经开启录像, 如果没有则开启录像
                    for (CommonGBChannel channel : channelList) {
                        // 开启点播,
                        channelPlayService.play(channel, null, true, ((code, msg, streamInfo) -> {
                            if (code == InviteErrorCode.SUCCESS.getCode() && streamInfo != null) {
                                log.info("[录像] 开启成功, 通道ID: {}", channel.getGbId());
                                recordStreamMap.put(channel.getGbId(), streamInfo);
                            } else {
                                log.info("[录像] 开启失败, 1分钟后重试,  通道ID: {}", channel.getGbId());
                            }
                        }));
                    }
                } else {
                    log.error("[录制计划] 数据异常, 这些关联的通道已经不存在了: {}", Joiner.on(",").join(startChannelIdList));
                }
            }
        }
    }

    /**
     * 对账: 核实内存中"正在录像"的流在流媒体中真实存在, 不存在则移除记录。
     * 兜住流注销事件丢失(ZLM重启/hook失败/事件匹配不上)导致的僵尸记录，否则内存认为在录而实际没录, 通道会永久停录。
     * 按媒体节点分组批量拉取流列表, 每个节点每分钟只一次HTTP请求, 通道数多时不会线性放大。
     */
    private void reconcileRecordStreams() {
        if (recordStreamMap.isEmpty()) {
            return;
        }
        // 按节点ID分组
        Map<String, List<Integer>> channelIdsByServerId = new HashMap<>();
        Map<String, MediaServer> serverById = new HashMap<>();
        for (Integer channelId : new HashSet<>(recordStreamMap.keySet())) {
            StreamInfo streamInfo = recordStreamMap.get(channelId);
            if (streamInfo == null || streamInfo.getMediaServer() == null) {
                recordStreamMap.remove(channelId);
                continue;
            }
            String serverId = streamInfo.getMediaServer().getId();
            channelIdsByServerId.computeIfAbsent(serverId, k -> new ArrayList<>()).add(channelId);
            serverById.putIfAbsent(serverId, streamInfo.getMediaServer());
        }
        for (Map.Entry<String, List<Integer>> entry : channelIdsByServerId.entrySet()) {
            Set<String> onlineStreams = new HashSet<>();
            try {
                List<StreamInfo> mediaList = mediaServerService.getMediaList(serverById.get(entry.getKey()), null, null, null);
                if (mediaList != null) {
                    for (StreamInfo info : mediaList) {
                        onlineStreams.add(info.getApp() + "/" + info.getStream());
                    }
                }
            } catch (Exception e) {
                // 节点查询失败时跳过该节点本轮对账, 防止误删记录
                log.error("[录像] 对账获取流列表失败, 跳过该节点本轮对账, 节点: {}", entry.getKey(), e);
                continue;
            }
            for (Integer channelId : entry.getValue()) {
                StreamInfo streamInfo = recordStreamMap.get(channelId);
                if (streamInfo == null) {
                    continue;
                }
                if (onlineStreams.contains(streamInfo.getApp() + "/" + streamInfo.getStream())) {
                    continue;
                }
                // 列表中不存在, 单路复核一次再移除, 防止列表异常导致误判
                Boolean ready = mediaServerService.isStreamReady(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
                if (ready == null || !ready) {
                    log.warn("[录像] 对账发现流实际不存在, 移除记录等待重新拉起, 通道ID: {}", channelId);
                    recordStreamMap.remove(channelId);
                }
            }
        }
    }

    /**
     * 获取当前时间段应该录像的通道Id列表
     */
    private List<Integer> queryCurrentChannelRecord(){
        // 获取当前时间在一周内的序号, 数据库存储的从第几个30分钟开始, 0-47, 包括首尾
        LocalDateTime now = LocalDateTime.now();
        int week = now.getDayOfWeek().getValue();
        int index = now.getHour() * 60 + now.getMinute();

        // 查询现在需要录像的通道Id
        return recordPlanMapper.queryRecordIng(week, index);
    }

    private void stopStreams(Collection<Integer> channelIds, Map<Integer, StreamInfo> recordStreamMap) {
        for (Integer channelId : channelIds) {
            try {
                StreamInfo streamInfo = recordStreamMap.get(channelId);
                if (streamInfo == null) {
                    continue;
                }
                // 查看是否有人观看,存在则不做处理,等待后续自然处理,如果无人观看,则关闭该流
                MediaInfo mediaInfo = mediaServerService.getMediaInfo(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
                if (mediaInfo.getReaderCount() == null ||  mediaInfo.getReaderCount() == 0) {
                    mediaServerService.closeStreams(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
                    log.info("[录制计划] 停止, 通道ID: {}", channelId);
                }
            }catch (Exception e) {
                log.error("[录制计划] 停止时异常", e);
            }finally {
                recordStreamMap.remove(channelId);
            }
        }
    }

    @Override
    public Integer recording(String app, String stream) {
        for (Integer channelId : recordStreamMap.keySet()) {
            StreamInfo streamInfo = recordStreamMap.get(channelId);
            if (streamInfo != null && streamInfo.getApp().equals(app) && streamInfo.getStream().equals(stream)) {
                return channelId;
            }
        }
        return null;
    }

    /**
     * 按流信息反查当前时间段需要录像的通道ID
     */
    private Integer queryRecordChannelIdByStream(String app, String stream, List<Integer> currentRecordChannels) {
        if (!MediaStreamUtil.RTP_APP.equals(app) || stream == null || currentRecordChannels.isEmpty()) {
            return null;
        }
        String[] streamArray = stream.split("_");
        if (streamArray.length != 2) {
            return null;
        }
        List<CommonGBChannel> channels = channelMapper.queryByDeviceId(streamArray[1]);
        for (CommonGBChannel channel : channels) {
            if (currentRecordChannels.contains(channel.getGbId())) {
                return channel.getGbId();
            }
        }
        return null;
    }

    @Override
    @Transactional
    public void add(RecordPlan plan) {
        plan.setCreateTime(DateUtil.getNow());
        plan.setUpdateTime(DateUtil.getNow());
        recordPlanMapper.add(plan);
        if (plan.getId() > 0 && !plan.getPlanItemList().isEmpty()) {
            for (RecordPlanItem recordPlanItem : plan.getPlanItemList()) {
                recordPlanItem.setPlanId(plan.getId());
            }
            recordPlanMapper.batchAddItem(plan.getId(), plan.getPlanItemList());
        }
        // TODO  更新录像队列
    }

    @Override
    public RecordPlan get(Integer planId) {
        RecordPlan recordPlan = recordPlanMapper.get(planId);
        if (recordPlan == null) {
            return null;
        }
        List<RecordPlanItem> recordPlanItemList = recordPlanMapper.getItemList(planId);
        if (!recordPlanItemList.isEmpty()) {
            recordPlan.setPlanItemList(recordPlanItemList);
        }
        return recordPlan;
    }

    @Override
    @Transactional
    public void update(RecordPlan plan) {
        plan.setUpdateTime(DateUtil.getNow());
        recordPlanMapper.update(plan);
        recordPlanMapper.cleanItems(plan.getId());
        if (plan.getPlanItemList() != null && !plan.getPlanItemList().isEmpty()){
            List<RecordPlanItem> planItemList = new ArrayList<>();
            for (RecordPlanItem recordPlanItem : plan.getPlanItemList()) {
                if (recordPlanItem.getStart() == null || recordPlanItem.getStop() == null || recordPlanItem.getWeekDay() == null){
                    continue;
                }
                if (recordPlanItem.getPlanId() == null) {
                    recordPlanItem.setPlanId(plan.getId());
                }
                planItemList.add(recordPlanItem);
            }
            if(!planItemList.isEmpty()) {
                recordPlanMapper.batchAddItem(plan.getId(), planItemList);
            }
        }
        // TODO  更新录像队列

    }

    @Override
    @Transactional
    public void delete(Integer planId) {
        RecordPlan recordPlan = recordPlanMapper.get(planId);
        if (recordPlan == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "录制计划不存在");
        }
        // 清理关联的通道
        channelMapper.removeRecordPlanByPlanId(recordPlan.getId());
        recordPlanMapper.cleanItems(planId);
        recordPlanMapper.delete(planId);
        // TODO  更新录像队列
    }

    @Override
    public PageInfo<RecordPlan> query(Integer page, Integer count, String query) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<RecordPlan> all = recordPlanMapper.query(query);
        return new PageInfo<>(all);
    }

    @Override
    public void link(List<Integer> channelIds, Integer planId) {
        if (channelIds == null || channelIds.isEmpty()) {
            log.info("[录制计划] 关联/移除关联时, 通道编号必须存在");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道编号必须存在");
        }
        if (planId == null) {
            channelMapper.removeRecordPlan(channelIds);
        }else {
            channelMapper.addRecordPlan(channelIds, planId);
        }
        // 查看当前的待录制列表是否变化,如果变化,则调用录制计划马上开始录制
        execution();
    }

    @Override
    public PageInfo<CommonGBChannel> queryChannelList(int page, int count, String query, Integer dataType, Boolean online, Integer planId, Boolean hasLink) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = channelMapper.queryForRecordPlanForWebList(planId, query, dataType, online, hasLink);
        return new PageInfo<>(all);
    }

    @Override
    public void linkAll(Integer planId) {
        channelMapper.addRecordPlanForAll(planId);
    }

    @Override
    public void cleanAll(Integer planId) {
        channelMapper.removeRecordPlanByPlanId(planId);
    }
}
