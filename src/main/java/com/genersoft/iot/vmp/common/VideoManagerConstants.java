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

	public static final String CACHEKEY_PREFIX = "VMP_CHANNEL_";

	public static final String KEEPLIVEKEY_PREFIX = "VMP_KEEPALIVE_";

	// 此处多了一个_，暂不修改
	public static final String PLAYER_PREFIX = "VMP_PLAYER_";
	public static final String PLAY_BLACK_PREFIX = "VMP_PLAYBACK_";

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

	//************************** redis 消息*********************************
	public static final String WVP_MSG_STREAM_CHANGE_PREFIX = "WVP_MSG_STREAM_CHANGE_";

	//**************************    第三方  ****************************************
	public static final String WVP_STREAM_GB_ID_PREFIX = "memberNo_";
}
