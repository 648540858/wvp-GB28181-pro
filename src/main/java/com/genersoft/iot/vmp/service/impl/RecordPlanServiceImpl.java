package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
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
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        // 流断开，检查是否还处于录像状态， 如果是则继续录像
        Integer channelId = recording(event.getApp(), event.getStream());
        if(channelId == null) {
            return;
        }
        // 重新拉起
        CommonGBChannel channel = channelMapper.queryById(channelId);
        if (channel == null) {
            log.warn("[录制计划] 流离开时拉起需要录像的流时, 发现通道不存在, id: {}", channelId);
            return;
        }
        // 开启点播,
        channelPlayService.play(channel, null, true, ((code, msg, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode() && streamInfo != null) {
                log.info("[录像] 流离开时拉起需要录像的流, 开启成功, 通道ID: {}", channel.getGbId());
                recordStreamMap.put(channel.getGbId(), streamInfo);
            } else {
                recordStreamMap.remove(channelId);
                log.info("[录像] 流离开时拉起需要录像的流, 开启失败, 十分钟后重试,  通道ID: {}", channel.getGbId());
            }
        }));
    }

    Map<Integer, StreamInfo> recordStreamMap = new HashMap<>();

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void execution() {
        // 查询现在需要录像的通道Id
        List<Integer> startChannelIdList = queryCurrentChannelRecord();

        if (startChannelIdList.isEmpty()) {
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
                                log.info("[录像] 开启失败, 十分钟后重试,  通道ID: {}", channel.getGbId());
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
