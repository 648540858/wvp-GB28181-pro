package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.MyPageInfo;
import com.genersoft.iot.vmp.common.Page;
import com.genersoft.iot.vmp.common.reponse.ErrorResponseData;
import com.genersoft.iot.vmp.common.reponse.ResponseData;
import com.genersoft.iot.vmp.service.IRecordInfoServer;
import com.genersoft.iot.vmp.storager.dao.RecordInfoDao;
import com.genersoft.iot.vmp.storager.dao.dto.RecordInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RecordInfoServerImpl implements IRecordInfoServer {

    @Autowired
    private RecordInfoDao recordInfoDao;

    @Override
    public PageInfo<RecordInfo> getRecordList(int page, int count) {
        PageHelper.startPage(page, count);
        List<RecordInfo> all = recordInfoDao.selectAll();
        return new PageInfo<>(all);
    }

    @Override
    public ResponseData resetRecords(Map<String, Object> params) {
        Object listObj = params.get("list");
        List<Map<String, Object>> list;
        if (ObjectUtils.anyNotNull(listObj)) {
            list = (List<Map<String, Object>>) listObj;
        } else {
            return ErrorResponseData.error("未查询到记录");
        }

        int pageNo = Integer.parseInt(params.get("pageNo").toString());
        int pageSize = Integer.parseInt(params.get("pageSize").toString());

        ChannelReduce channelReduce = new ChannelReduce();
        if (params.get("deviceId") != null) channelReduce.setDeviceId(params.get("deviceId").toString());
        if (params.get("manufacturer") != null) channelReduce.setManufacturer(params.get("manufacturer").toString());
        if (params.get("name") != null) channelReduce.setName(params.get("name").toString());

        List<Map<String, Object>> resultList = new ArrayList<>();
        List<ChannelReduce> channelReduces = recordInfoDao.selectAllChannel(channelReduce);

        String isNVRRecord = params.get("NVR").toString();

        if ("NVR".equals(isNVRRecord)) {
            boolean stream = ObjectUtils.anyNotNull(params.get("stream"));
            boolean time = ObjectUtils.anyNotNull(params.get("time"));
            list.stream().filter(item -> {
                String streamId = item.get("stream").toString();
                String originalTime = item.get("time").toString();
                boolean isOK = !streamId.contains("_");
                if (isOK){
                    if (stream && time){
                        String paramStream = params.get("stream").toString();
                        String paramTime = params.get("time").toString();
                        return streamId.contains(paramStream) && originalTime.contains(paramTime);
                    }else if (stream){
                        return streamId.contains(params.get("stream").toString());
                    }else if (time){
                        return originalTime.contains(params.get("time").toString());
                    }
                    return true;
                }
                return false;
            }).forEach(resultList::add);
        } else {
            list.stream()
                    .filter(item -> {
                        String streamId = item.get("stream").toString();
                        return streamId.contains("_");
                    })
                    .forEach(item -> { //修改list内容
                        String streamId = item.get("stream").toString();
                        String[] streamIds = streamId.split("_");
                        String paramDeviceId = streamIds[0];
                        String paramChannelId = streamIds[1];
                        channelReduces.forEach(tempChannelReduce -> {
                            boolean isEq = tempChannelReduce.getDeviceId().equals(paramDeviceId) && tempChannelReduce.getChannelId().equals(paramChannelId);
                            if (isEq) {
                                item.put("manufacturer", tempChannelReduce.getManufacturer());
                                item.put("name", tempChannelReduce.getName());
                                item.put("channelId", tempChannelReduce.getChannelId());
                                item.put("deviceId", tempChannelReduce.getDeviceId());
                                item.put("hostAddress", tempChannelReduce.getHostAddress());
                                resultList.add(item);
                            }
                        });
                    });
        }
        MyPageInfo<Map<String, Object>> myPageInfo = new MyPageInfo<>(resultList);
        myPageInfo.startPage(pageNo, pageSize);
        return ResponseData.success(new Page<>(myPageInfo));
    }
}
