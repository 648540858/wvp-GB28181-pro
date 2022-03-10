package com.genersoft.iot.vmp.gb28181.auth;

import com.genersoft.iot.vmp.storager.impl.VideoManagerStoragerImpl;
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

	@Autowired
	private VideoManagerStoragerImpl storager;
	
	public void onRegister(Device device) {
		// 只有第一次注册时调用查询设备信息，如需更新调用更新API接口
//		// TODO 此处错误无法获取到通道
//		Device device1 = storager.queryVideoDevice(device.getDeviceId());
//		if (device.isFirsRegister()) {
//			logger.info("[{}] 首次注册，查询设备信息以及通道信息", device.getDeviceId());
//			try {
//				Thread.sleep(100);
//				cmder.deviceInfoQuery(device);
//				Thread.sleep(100);
//				cmder.catalogQuery(device, null);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
	}
}
