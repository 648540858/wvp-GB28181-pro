package com.genersoft.iot.vmp.gat1400.utils;

import com.genersoft.iot.vmp.gat1400.backend.task.action.KeepaliveAction;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class VIIDRandomUtil {
    public static String VIID_AREA_CODE = "431002";

    // 用于修复设备上推数据结构ID部分前缀为0的情况
    public static String fixObjectId(String deviceId, String id) {
        if (StringUtils.isNotBlank(deviceId)) {
            if (StringUtils.isNotBlank(id)
                    && id.length() > deviceId.length()
                    && !StringUtils.startsWith(id, deviceId)) {
                //修正 objectId
                String subId = StringUtils.substring(id, deviceId.length());
                return deviceId + subId;
            }
        }
        return id;
    }

    public static boolean validDeviceId(String deviceId) {
        if (StringUtils.length(deviceId) != 20)
            return false;
        String str = StringUtils.substring(deviceId, 10, 13);
        return "119".equals(str) || "132".equals(str);
    }

    public static boolean validTollgateId(String tollgate) {
        if (StringUtils.length(tollgate) != 20)
            return false;
        String str = StringUtils.substring(tollgate, 10, 13);
        return "121".equals(str);
    }

    public static boolean validInstanceId(String instanceId) {
        if (StringUtils.length(instanceId) != 20)
            return false;
        String str = StringUtils.substring(instanceId, 10, 13);
        return "503".equals(str);
    }

    //判断是否是行政区编号 2/4/6位
    public static boolean isOrgAreaNumber(String value) {
        int length = StringUtils.length(value);
        return (length == 2 || length == 4 || length == 6) && NumberUtils.isCreatable(value);
    }

    //判断是否是30位案(事)件编号
    public static boolean validCaseId(String value) {
        int length = StringUtils.length(value);
        return length == 30;
    }

    public static boolean isResourceId(String value) {
        return validTollgateId(value) || validDeviceId(value) || validCaseId(value) || isOrgAreaNumber(value);
    }

    //20位ID, 第11~13位等于503表示类型为 视图库
    public static String randomVIIDInstanceId(String organization) {
        if (StringUtils.length(organization) < 6)
            organization = VIID_AREA_CODE;
        String prefix = organization.substring(0, 6);
        String suffix = DateFormatUtils.format(new Date(), "yyyy");
        String op = "503";
        String random = RandomStringUtils.randomNumeric(7);
        //6位中心编码 + 2位基层接入单位编码 + 2位行业编码 + 3位类型编码 + 7位序号
        return prefix + suffix + op + random;
    }

    public static String empty20ResourceId() {
        return "00000000000000000000";
    }

    public static Set<String> getResourceUris(String resourceUri) {
        if (StringUtils.isBlank(resourceUri))
            return Collections.emptySet();
        return Arrays.stream(StringUtils.split(resourceUri, ","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    public static Set<String> getSubscribeDetails(String subscribeDetail) {
        if (StringUtils.isBlank(subscribeDetail))
            return Collections.emptySet();
        return Arrays.stream(StringUtils.split(subscribeDetail, ","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    public static Set<Pattern> getSubscribeDetailPatterns(String resourceUri, String subscribeDetail) {
        Set<String> resources = getResourceUris(resourceUri);
        Set<String> prefixSet = getSubscribeDetails(subscribeDetail).stream()
                .map(Constants.SubscribeDetail::match)
                .map(Constants.SubscribeDetail::prefix)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> patternSet = new HashSet<>();
        for (String prefix : prefixSet) {
            Set<String> patterns = new HashSet<>();
            for (String resource : resources) {
                if (resource.equals(KeepaliveAction.CURRENT_SERVER_ID) || isResourceId(resource)) {
                    patterns.clear();
                    patterns.add(prefix + ".*");
                    break;
                } else if (validInstanceId(resource)) {
                    patterns.add(prefix + resource);
                }
            }
            patternSet.addAll(patterns);
        }
        return patternSet.stream().map(Pattern::compile).collect(Collectors.toSet());
    }
}
