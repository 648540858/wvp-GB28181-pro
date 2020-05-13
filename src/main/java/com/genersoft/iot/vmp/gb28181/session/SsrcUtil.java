package com.genersoft.iot.vmp.gb28181.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;

/**
 * @Description:SIP信令中的SSRC工具类。SSRC值由10位十进制整数组成的字符串，第一位为0代表实况，为1则代表回放；第二位至第六位由监控域ID的第4位到第8位组成；最后4位为不重复的4个整数
 * @author: songww
 * @date: 2020年5月10日 上午11:57:57
 */
public class SsrcUtil {

	private static String ssrcPrefix;

	private static List<String> isUsed;

	private static List<String> notUsed;

	private static void init() {
		SipConfig sipConfig = (SipConfig) SpringBeanFactory.getBean("sipConfig");
		ssrcPrefix = sipConfig.getSipDomain().substring(4, 9);
		isUsed = new ArrayList<String>();
		notUsed = new ArrayList<String>();
		for (int i = 1; i < 10000; i++) {
			if (i < 10) {
				notUsed.add("000" + i);
			} else if (i < 100) {
				notUsed.add("00" + i);
			} else if (i < 1000) {
				notUsed.add("0" + i);
			} else {
				notUsed.add(String.valueOf(i));
			}
		}
	}

	/**
	 * 获取视频预览的SSRC值,第一位固定为0
	 * 
	 */
	public static String getPlaySsrc() {
		return "0" + getSsrcPrefix() + getSN();
	}

	/**
	 * 获取录像回放的SSRC值,第一位固定为1
	 * 
	 */
	public static String getPlayBackSsrc() {
		return "1" + getSsrcPrefix() + getSN();
	}

	/**
	 * 释放ssrc，主要用完的ssrc一定要释放，否则会耗尽
	 * 
	 */
	public static void releaseSsrc(String ssrc) {
		String sn = ssrc.substring(6);
		isUsed.remove(sn);
		notUsed.add(sn);
	}

	/**
	 * 获取后四位数SN,随机数
	 * 
	 */
	private static String getSN() {
		String sn = null;
		if (notUsed.size() == 0) {
			throw new RuntimeException("ssrc已经用完");
		} else if (notUsed.size() == 1) {
			sn = notUsed.get(0);
		} else {
			sn = notUsed.get(new Random().nextInt(notUsed.size() - 1));
		}
		notUsed.remove(0);
		isUsed.add(sn);
		return sn;
	}

	private static String getSsrcPrefix() {
		if (ssrcPrefix == null) {
			init();
		}
		return ssrcPrefix;
	}
}
