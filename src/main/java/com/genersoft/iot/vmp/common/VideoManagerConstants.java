package com.genersoft.iot.vmp.common;

/**    
 * @description: 定义常量   
 * @author: swwheihei
 * @date:   2019年5月30日 下午3:04:04   
 *   
 */
public class VideoManagerConstants {
	
	public static final String WVP_SERVER_PREFIX = "VMP_SIGNALLING_SERVER_INFO_";

	public static final String WVP_SERVER_STREAM_PREFIX = "VMP_SIGNALLING_STREAM_";

	public static final String MEDIA_SERVER_PREFIX = "VMP_MEDIA_SERVER_";

	public static final String MEDIA_SERVER_KEEPALIVE_PREFIX = "VMP_MEDIA_SERVER_KEEPALIVE_";

	public static final String MEDIA_SERVERS_ONLINE_PREFIX = "VMP_MEDIA_ONLINE_SERVERS_";

	public static final String MEDIA_STREAM_PREFIX = "VMP_MEDIA_STREAM";

	public static final String DEVICE_PREFIX = "VMP_DEVICE_";

	// 设备同步完成
	public static final String DEVICE_SYNC_PREFIX = "VMP_DEVICE_SYNC_";

	public static final String CACHEKEY_PREFIX = "VMP_CHANNEL_";

	public static final String KEEPLIVEKEY_PREFIX = "VMP_KEEPALIVE_";

	// 此处多了一个_，暂不修改
	public static final String PLAYER_PREFIX = "VMP_PLAYER_";
	public static final String PLAY_BLACK_PREFIX = "VMP_PLAYBACK_";
	public static final String PLAY_INFO_PREFIX = "VMP_PLAY_INFO_";

	public static final String DOWNLOAD_PREFIX = "VMP_DOWNLOAD_";

	public static final String PLATFORM_KEEPALIVE_PREFIX = "VMP_PLATFORM_KEEPALIVE_";

	public static final String PLATFORM_CATCH_PREFIX = "VMP_PLATFORM_CATCH_";

	public static final String PLATFORM_REGISTER_PREFIX = "VMP_PLATFORM_REGISTER_";

	public static final String PLATFORM_REGISTER_INFO_PREFIX = "VMP_PLATFORM_REGISTER_INFO_";

	public static final String PLATFORM_SEND_RTP_INFO_PREFIX = "VMP_PLATFORM_SEND_RTP_INFO_";

	public static final String EVENT_ONLINE_REGISTER = "1";
	
	public static final String EVENT_ONLINE_KEEPLIVE = "2";

	public static final String EVENT_ONLINE_MESSAGE = "3";

	public static final String EVENT_OUTLINE_UNREGISTER = "1";
	
	public static final String EVENT_OUTLINE_TIMEOUT = "2";

	public static final String MEDIA_SSRC_USED_PREFIX = "VMP_MEDIA_USED_SSRC_";

	public static final String MEDIA_TRANSACTION_USED_PREFIX = "VMP_MEDIA_TRANSACTION_";

	public static final String SIP_CSEQ_PREFIX = "VMP_SIP_CSEQ_";

	public static final String SIP_SN_PREFIX = "VMP_SIP_SN_";

	public static final String SIP_SUBSCRIBE_PREFIX = "VMP_SIP_SUBSCRIBE_";

	public static final String SYSTEM_INFO_CPU_PREFIX = "VMP_SYSTEM_INFO_CPU_";

	public static final String SYSTEM_INFO_MEM_PREFIX = "VMP_SYSTEM_INFO_MEM_";

	public static final String SYSTEM_INFO_NET_PREFIX = "VMP_SYSTEM_INFO_NET_";


	//************************** redis 消息*********************************

	// 流变化的通知
	public static final String WVP_MSG_STREAM_CHANGE_PREFIX = "WVP_MSG_STREAM_CHANGE_";

	// 接收推流设备的GPS变化通知
	public static final String VM_MSG_GPS = "VM_MSG_GPS";

	// redis 消息通知设备推流到平台
	public static final String VM_MSG_STREAM_PUSH_REQUESTED = "VM_MSG_STREAM_PUSH_REQUESTED";

	// 移动位置订阅通知
	public static final String VM_MSG_SUBSCRIBE_MOBILE_POSITION = "mobileposition";

	// 报警订阅的通知（收到报警向redis发出通知）
	public static final String VM_MSG_SUBSCRIBE_ALARM = "alarm";

	// 报警通知的发送 （收到redis发出的通知，转发给其他平台）
	public static final String VM_MSG_SUBSCRIBE_ALARM_RECEIVE= "alarm_receive";

	// 设备状态订阅的通知
	public static final String VM_MSG_SUBSCRIBE_DEVICE_STATUS = "device";

	//**************************    第三方  ****************************************
	public static final String WVP_STREAM_GB_ID_PREFIX = "memberNo_";
	public static final String WVP_STREAM_GPS_MSG_PREFIX = "WVP_STREAM_GPS_MSG_";
}
