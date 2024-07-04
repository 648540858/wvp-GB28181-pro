package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface CommonGBChannelMapper {

    @Select()
    CommonGBChannel queryByDeviceId(String gbDeviceId);
}
