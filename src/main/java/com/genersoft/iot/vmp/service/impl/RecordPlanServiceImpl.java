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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
        if (recording(event.getApp(), event.getStream())) {
            // 重新拉起

        }
    }

    Map<Integer, StreamInfo> recordStreamMap = new HashMap<>();

    @Scheduled(cron = "0 */30 * * * *")
    public void execution() {
        // 执行计划

        // 获取当前时间在一周内的序号
        LocalDateTime now = LocalDateTime.now();
        int week = now.getDayOfWeek().getValue();
        int index = now.getHour() * 2 + (now.getMinute() > 30?1:0);
        // 查询startTime等于现在的， 开始录像
        List<Integer> startPlanList = recordPlanMapper.queryStart(week, index);

        Map<Integer, StreamInfo> channelMapWithoutRecord = new HashMap<>();
        if (startPlanList.isEmpty()) {
            // 停止所有正在录像的
            if(recordStreamMap.isEmpty()) {
                // 暂无录像任务
                return;
            }else {
                channelMapWithoutRecord.putAll(recordStreamMap);
                recordStreamMap.clear();
            }
        }else {
            channelMapWithoutRecord.putAll(recordStreamMap);
            // 获取所有的关联的通道
            List<CommonGBChannel> channelList = channelMapper.queryForRecordPlan(startPlanList);
            if (channelList.isEmpty()) {
                recordStreamMap.clear();
            }else {
                // 查找是否已经开启录像, 如果没有则开启录像
                for (CommonGBChannel channel : channelList) {
                    if (recordStreamMap.get(channel.getGbId()) != null) {
                        channelMapWithoutRecord.remove(channel.getGbId());
                    }else {
                        // 开启点播,
                        channelPlayService.play(channel, null, ((code, msg, streamInfo) -> {
                            if (code == InviteErrorCode.SUCCESS.getCode() && streamInfo != null) {
                                log.info("[录像] 开启成功, 通道ID: {}", channel.getGbId());
                                recordStreamMap.put(channel.getGbId(), streamInfo);
                                channelMapWithoutRecord.remove(channel.getGbId(), streamInfo);
                            }
                        }));
                    }
                }
            }
        }
        // 结束录像
        if(!channelMapWithoutRecord.isEmpty()) {
            for (Integer channelId : channelMapWithoutRecord.keySet()) {
                StreamInfo streamInfo = channelMapWithoutRecord.get(channelId);
                if (streamInfo == null) {
                    continue;
                }
                // 查看是否有人观看,存在则不做处理,等待后续自然处理,如果无人观看,则关闭该流
                MediaInfo mediaInfo = mediaServerService.getMediaInfo(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
                if (mediaInfo.getReaderCount() == null ||  mediaInfo.getReaderCount() == 0) {
                    mediaServerService.closeStreams(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
                    log.info("[录像] 停止, 通道ID: {}", channelId);
                }
            }
        }
    }

    // 系统启动时


    @Override
    public boolean recording(String app, String stream) {
        for (StreamInfo streamInfo : recordStreamMap.values()) {
            if (streamInfo.getApp().equals(app) && streamInfo.getStream().equals(stream)) {
                return true;
            }
        }
        return false;
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
        // TODO  更新录像队列
    }

    @Override
    public PageInfo<CommonGBChannel> queryChannelList(int page, int count, String query, Integer channelType, Boolean online, Integer planId, Boolean hasLink) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = channelMapper.queryForRecordPlanForWebList(planId, query, channelType, online, hasLink);
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
