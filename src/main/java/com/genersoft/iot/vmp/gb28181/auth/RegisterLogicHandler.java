package com.genersoft.iot.vmp.gb28181.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;

/**    
 * @description:注册逻辑处理，当设备注册后触发逻辑。
 * @author: swwheihei
 * @date:   2020年5月8日 下午9:41:46     
 */
@Component
public class RegisterLogicHandler {

	private Logger logger = LoggerFactory.getLogger(RegisterLogicHandler.class);

	@Autowired
	private SIPCommander cmder;
	
	public void onRegister(Device device) {
		// 只有第一次注册时调用查询设备信息，如需更新调用更新API接口
		if (device.isFirsRegister()) {
			logger.info("[{}] 首次注册，查询设备信息以及通道信息", device.getDeviceId());
			cmder.deviceInfoQuery(device);
			cmder.catalogQuery(device, null);
		}
	}
}
