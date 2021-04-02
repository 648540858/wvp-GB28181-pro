package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

@Service
public class GbStreamServiceImpl implements IGbStreamService {

    private final static Logger logger = LoggerFactory.getLogger(GbStreamServiceImpl.class);

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Override
    public PageInfo<GbStream> getAll(Integer page, Integer count) {
        PageHelper.startPage(page, count);
        List<GbStream> all = gbStreamMapper.selectAll();
        return new PageInfo<>(all);
    }

    @Override
    public void del(String app, String stream) {
        gbStreamMapper.del(app, stream);
    }


    @Override
    public boolean addPlatformInfo(List<GbStream> gbStreams, String platformId) {
        // 放在事务内执行
        boolean result = false;
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        try {
            for (GbStream gbStream : gbStreams) {
                gbStream.setPlatformId(platformId);
                platformGbStreamMapper.add(gbStream);
            }
            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
            result = true;
        }catch (Exception e) {
            logger.error("批量保存流与平台的关系时错误", e);
            dataSourceTransactionManager.rollback(transactionStatus);
        }
        return result;

    }

    @Override
    public boolean delPlatformInfo(List<GbStream> gbStreams) {
        // 放在事务内执行
        boolean result = false;
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        try {
            for (GbStream gbStream : gbStreams) {
                platformGbStreamMapper.delByAppAndStream(gbStream.getApp(), gbStream.getStream());
            }
            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
            result = true;
        }catch (Exception e) {
            logger.error("批量移除流与平台的关系时错误", e);
            dataSourceTransactionManager.rollback(transactionStatus);
        }
        return result;
    }
}
