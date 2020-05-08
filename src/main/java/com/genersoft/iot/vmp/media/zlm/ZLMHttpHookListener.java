package com.genersoft.iot.vmp.media.zlm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**    
 * @Description:针对 ZLMediaServer的hook事件监听
 * @author: songww
 * @date:   2020年5月8日 上午10:46:48     
 */
@RestController
@RequestMapping("/hook/zlm")
public class ZLMHttpHookListener {

	private final static Logger logger = LoggerFactory.getLogger(ZLMHttpHookListener.class);
	
	/**
	 * 流量统计事件，播放器或推流器断开时并且耗用流量超过特定阈值时会触发此事件，阈值通过配置文件general.flowThreshold配置；此事件对回复不敏感。
	 *  
	 */
	@PostMapping("/on_flow_report")
	public ResponseEntity onFlowReport(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * 访问http文件服务器上hls之外的文件时触发。
	 *  
	 */
	@PostMapping("/on_http_access")
	public ResponseEntity onHttpAccess(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * 播放器鉴权事件，rtsp/rtmp/http-flv/ws-flv/hls的播放都将触发此鉴权事件。
	 *  
	 */
	@PostMapping("/on_play")
	public ResponseEntity onPlay(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * rtsp/rtmp/rtp推流鉴权事件。
	 *  
	 */
	@PostMapping("/on_publish")
	public ResponseEntity onPublish(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * 录制mp4完成后通知事件；此事件对回复不敏感。
	 *  
	 */
	@PostMapping("/on_record_mp4")
	public ResponseEntity onRecordMp4(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * 该rtsp流是否开启rtsp专用方式的鉴权事件，开启后才会触发on_rtsp_auth事件。需要指出的是rtsp也支持url参数鉴权，它支持两种方式鉴权。
	 *  
	 */
	@PostMapping("/on_rtsp_auth")
	public ResponseEntity onRtspAuth(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * rtsp专用的鉴权事件，先触发on_rtsp_realm事件然后才会触发on_rtsp_auth事件。
	 *  
	 */
	@PostMapping("/on_rtsp_realm")
	public ResponseEntity onRtspRealm(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * shell登录鉴权，ZLMediaKit提供简单的telnet调试方式，使用telnet 127.0.0.1 9000能进入MediaServer进程的shell界面。
	 *  
	 */
	@PostMapping("/on_shell_login")
	public ResponseEntity onShellLogin(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * rtsp/rtmp流注册或注销时触发此事件；此事件对回复不敏感。
	 *  
	 */
	@PostMapping("/on_stream_changed")
	public ResponseEntity onStreamChanged(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * 流无人观看时事件，用户可以通过此事件选择是否关闭无人看的流。
	 *  
	 */
	@PostMapping("/on_stream_none_reader")
	public ResponseEntity onStreamNoneReader(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * 流未找到事件，用户可以在此事件触发时，立即去拉流，这样可以实现按需拉流；此事件对回复不敏感。
	 *  
	 */
	@PostMapping("/on_stream_not_found")
	public ResponseEntity onStreamNotFound(){
		// TODO Auto-generated method stub
		
		return null;
	}
	
	/**
	 * 服务器启动事件，可以用于监听服务器崩溃重启；此事件对回复不敏感。
	 *  
	 */
	@PostMapping("/on_server_started")
	public ResponseEntity onServerStarted(){
		// TODO Auto-generated method stub
		
		return null;
	}
}
