package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.RecordItem;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

/**    
 * @Description:MESSAGE请求处理器
 * @author: songww
 * @date:   2020年5月3日 下午5:32:41     
 */
@Component
public class MessageRequestProcessor implements ISIPRequestProcessor {

	private ServerTransaction transaction;
	
	private SipLayer layer;
	
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private IVideoManagerStorager storager;
	
	@Autowired
	private EventPublisher publisher;
	
	@Autowired
	private DeferredResultHolder deferredResultHolder;
	
	/**   
	 * 处理MESSAGE请求
	 *  
	 * @param evt
	 * @param layer
	 * @param transaction  
	 */  
	@Override
	public void process(RequestEvent evt, SipLayer layer, ServerTransaction transaction) {
		
		this.layer = layer;
		this.transaction = transaction;
		
		Request request = evt.getRequest();
		
		if (new String(request.getRawContent()).contains("<CmdType>Keepalive</CmdType>")) {
			processMessageKeepAlive(evt);
		} else if (new String(request.getRawContent()).contains("<CmdType>Catalog</CmdType>")) {
			processMessageCatalogList(evt);
		} else if (new String(request.getRawContent()).contains("<CmdType>DeviceInfo</CmdType>")) {
			processMessageDeviceInfo(evt);
		} else if (new String(request.getRawContent()).contains("<CmdType>Alarm</CmdType>")) {
			processMessageAlarm(evt);
		} else if (new String(request.getRawContent()).contains("<CmdType>recordInfo</CmdType>")) {
			processMessageRecordInfo(evt);
		}
		
	}
	
	/**
	 * 收到deviceInfo设备信息请求 处理
	 * @param evt
	 */
	private void processMessageDeviceInfo(RequestEvent evt) {
		try {
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getText().toString();
			
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				return;
			}
			device.setName(XmlUtil.getText(rootElement,"DeviceName"));
			device.setManufacturer(XmlUtil.getText(rootElement,"Manufacturer"));
			device.setModel(XmlUtil.getText(rootElement,"Model"));
			device.setFirmware(XmlUtil.getText(rootElement,"Firmware"));
			storager.update(device);
			
			RequestMessage msg = new RequestMessage();
			msg.setDeviceId(deviceId);
			msg.setType(DeferredResultHolder.CALLBACK_CMD_DEVICEINFO);
			msg.setData(device);
			deferredResultHolder.invokeResult(msg);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/***
	 * 收到catalog设备目录列表请求 处理
	 * @param evt
	 */
	private void processMessageCatalogList(RequestEvent evt) {
		try {
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getText().toString();
			Element deviceListElement = rootElement.element("DeviceList");
			if (deviceListElement == null) {
				return;
			}
			Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
			if (deviceListIterator != null) {
				Device device = storager.queryVideoDevice(deviceId);
				if (device == null) {
					return;
				}
				Map<String, DeviceChannel> channelMap = device.getChannelMap();
				if (channelMap == null) {
					channelMap = new HashMap<String, DeviceChannel>(5);
					device.setChannelMap(channelMap);
				}
				// 遍历DeviceList
				while (deviceListIterator.hasNext()) {
					Element itemDevice = deviceListIterator.next();
					Element channelDeviceElement = itemDevice.element("DeviceID");
					if (channelDeviceElement == null) {
						continue;
					}
					String channelDeviceId = channelDeviceElement.getText().toString();
					Element channdelNameElement = itemDevice.element("Name");
					String channelName = channdelNameElement != null ? channdelNameElement.getText().toString() : "";
					Element statusElement = itemDevice.element("Status");
					String status = statusElement != null ? statusElement.getText().toString() : "ON";
					DeviceChannel deviceChannel = channelMap.containsKey(channelDeviceId) ? channelMap.get(channelDeviceId) : new DeviceChannel();
					deviceChannel.setName(channelName);
					deviceChannel.setChannelId(channelDeviceId);
					if(status.equals("ON")) {
						deviceChannel.setStatus(1);
					}
					if(status.equals("OFF")) {
						deviceChannel.setStatus(0);
					}

					deviceChannel.setManufacture(XmlUtil.getText(itemDevice,"Manufacturer"));
					deviceChannel.setModel(XmlUtil.getText(itemDevice,"Model"));
					deviceChannel.setOwner(XmlUtil.getText(itemDevice,"Owner"));
					deviceChannel.setCivilCode(XmlUtil.getText(itemDevice,"CivilCode"));
					deviceChannel.setBlock(XmlUtil.getText(itemDevice,"Block"));
					deviceChannel.setAddress(XmlUtil.getText(itemDevice,"Address"));
					deviceChannel.setParental(itemDevice.element("Parental") == null? 0:Integer.parseInt(XmlUtil.getText(itemDevice,"Parental")));
					deviceChannel.setParentId(XmlUtil.getText(itemDevice,"ParentId"));
					deviceChannel.setSafetyWay(itemDevice.element("SafetyWay") == null? 0:Integer.parseInt(XmlUtil.getText(itemDevice,"SafetyWay")));
					deviceChannel.setRegisterWay(itemDevice.element("RegisterWay") == null? 1:Integer.parseInt(XmlUtil.getText(itemDevice,"RegisterWay")));
					deviceChannel.setCertNum(XmlUtil.getText(itemDevice,"CertNum"));
					deviceChannel.setCertifiable(itemDevice.element("Certifiable") == null? 0:Integer.parseInt(XmlUtil.getText(itemDevice,"Certifiable")));
					deviceChannel.setErrCode(itemDevice.element("ErrCode") == null? 0:Integer.parseInt(XmlUtil.getText(itemDevice,"ErrCode")));
					deviceChannel.setEndTime(XmlUtil.getText(itemDevice,"EndTime"));
					deviceChannel.setSecrecy(XmlUtil.getText(itemDevice,"Secrecy"));
					deviceChannel.setIpAddress(XmlUtil.getText(itemDevice,"IPAddress"));
					deviceChannel.setPort(itemDevice.element("Port") == null? 0:Integer.parseInt(XmlUtil.getText(itemDevice,"Port")));
					deviceChannel.setPassword(XmlUtil.getText(itemDevice,"Password"));
					deviceChannel.setLongitude(itemDevice.element("Longitude") == null? 0.00:Double.parseDouble(XmlUtil.getText(itemDevice,"Longitude")));
					deviceChannel.setLatitude(itemDevice.element("Latitude") == null? 0.00:Double.parseDouble(XmlUtil.getText(itemDevice,"Latitude")));
					channelMap.put(channelDeviceId, deviceChannel);
				}
				// 更新
				storager.update(device);
				RequestMessage msg = new RequestMessage();
				msg.setDeviceId(deviceId);
				msg.setType(DeferredResultHolder.CALLBACK_CMD_CATALOG);
				msg.setData(device);
				deferredResultHolder.invokeResult(msg);
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/***
	 * 收到alarm设备报警信息 处理
	 * @param evt
	 */
	private void processMessageAlarm(RequestEvent evt) {
		try {
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getText().toString();
			
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				return;
			}
			device.setName(XmlUtil.getText(rootElement,"DeviceName"));
			device.setManufacturer(XmlUtil.getText(rootElement,"Manufacturer"));
			device.setModel(XmlUtil.getText(rootElement,"Model"));
			device.setFirmware(XmlUtil.getText(rootElement,"Firmware"));
			storager.update(device);
			cmder.catalogQuery(device);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/***
	 * 收到keepalive请求 处理
	 * @param evt
	 */
	private void processMessageKeepAlive(RequestEvent evt){
		try {
			Request request = evt.getRequest();
			Response response = layer.getMessageFactory().createResponse(Response.OK,request);
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			transaction.sendResponse(response);
			publisher.onlineEventPublish(deviceIdElement.getText(), VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/***
	 * 收到catalog设备目录列表请求 处理
	 * @param evt
	 */
	private void processMessageRecordInfo(RequestEvent evt) {
		try {
			RecordInfo recordInfo = new RecordInfo();
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getText().toString();
			recordInfo.setDeviceId(deviceId);
			recordInfo.setName(XmlUtil.getText(rootElement,"Name"));
			recordInfo.setSumNum(Integer.parseInt(XmlUtil.getText(rootElement,"SumNum")));
			Element recordListElement = rootElement.element("RecordList");
			if (recordListElement == null) {
				return;
			}
			
			Iterator<Element> recordListIterator = recordListElement.elementIterator();
			if (recordListIterator != null) {
				
				List<RecordItem> recordList = new ArrayList<RecordItem>();
				RecordItem record = new RecordItem();
				// 遍历DeviceList
				while (recordListIterator.hasNext()) {
					Element itemRecord = recordListIterator.next();
					Element recordElement = itemRecord.element("DeviceID");
					if (recordElement == null) {
						continue;
					}
					record.setDeviceId(XmlUtil.getText(itemRecord,"DeviceID"));
					record.setName(XmlUtil.getText(itemRecord,"Name"));
					record.setFilePath(XmlUtil.getText(itemRecord,"FilePath"));
					record.setAddress(XmlUtil.getText(itemRecord,"Address"));
					record.setStartTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(XmlUtil.getText(itemRecord,"StartTime")));
					record.setEndTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(XmlUtil.getText(itemRecord,"EndTime")));
					record.setSecrecy(itemRecord.element("Secrecy") == null? 0:Integer.parseInt(XmlUtil.getText(itemRecord,"Secrecy")));
					record.setType(XmlUtil.getText(itemRecord,"Type"));
					record.setRecordId(XmlUtil.getText(itemRecord,"RecordID"));
					recordList.add(record);
				}
				recordInfo.setRecordList(recordList);
			}
			
			
			RequestMessage msg = new RequestMessage();
			msg.setDeviceId(deviceId);
			msg.setType(DeferredResultHolder.CALLBACK_CMD_RECORDINFO);
			msg.setData(recordInfo);
			deferredResultHolder.invokeResult(msg);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	private Element getRootElement(RequestEvent evt) throws DocumentException {
		Request request = evt.getRequest();
		SAXReader reader = new SAXReader();
		reader.setEncoding("GB2312");
		Document xml = reader.read(new ByteArrayInputStream(request.getRawContent()));
		return xml.getRootElement();
	}

}
