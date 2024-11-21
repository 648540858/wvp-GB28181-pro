package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.PlatformChannel;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.service.IRecordPlanService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.service.bean.RecordPlanItem;
import com.genersoft.iot.vmp.storager.dao.RecordPlanMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class RecordPlanServiceImpl implements IRecordPlanService {

    @Autowired
    private RecordPlanMapper recordPlanMapper;

    @Autowired
    private CommonGBChannelMapper channelMapper;

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
        if (plan.getPlanItemList() != null){
            recordPlanMapper.batchAddItem(plan.getId(), plan.getPlanItemList());
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
        if (planId == null) {
            log.info("[录制计划] 移除通道关联的计划");
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
