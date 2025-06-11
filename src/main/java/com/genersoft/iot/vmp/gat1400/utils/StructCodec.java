package com.genersoft.iot.vmp.gat1400.utils;

import com.genersoft.iot.vmp.gat1400.backend.domain.dto.DispositionObject;
import com.genersoft.iot.vmp.gat1400.fontend.domain.VIIDPublishRequest;
import com.genersoft.iot.vmp.gat1400.fontend.domain.VIIDSubscribeRequest;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.APEObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.LaneObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.TollgateObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.APEDevice;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.Lane;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.TollgateDevice;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDDisposition;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDServer;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDSubscribe;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;


public class StructCodec {

    public static String VIID_AREA_CODE = "431002";

    public static SubscribeObject inputSubscribeBuilder(VIIDSubscribeRequest request, VIIDServer domain, VIIDServer setting) {
        SubscribeObject subscribe = new SubscribeObject();
        subscribe.setSubscribeId(randomSubscriberId(domain.getServerId()));
        String title = request.getTitle();
        subscribe.setTitle(title);
        String subscribeDetail = request.getSubscribeDetail();
        if (Constants.SubscribeDetail.TOLLGATE.equalsValue(subscribeDetail)
                || Constants.SubscribeDetail.DEVICE.equalsValue(subscribeDetail)) {
            // 防止低版本海口平台bug
//            subscribe.setSubscribeDetail(subscribeDetail + ",");
            subscribe.setSubscribeDetail(subscribeDetail);
        } else {
            subscribe.setSubscribeDetail(subscribeDetail);
        }
        subscribe.setResourceUri(assembleResourceUri(request));
        subscribe.setApplicationName(Optional.ofNullable(request.getApplicationName()).orElse("admin"));
        subscribe.setApplicationOrg(Optional.ofNullable(request.getApplicationOrg()).orElse("d1"));
        Date beginTime = Optional.ofNullable(request.getBeginTime()).orElseGet(() -> DateUtils.addHours(new Date(), -8));
        subscribe.setBeginTime(beginTime);
        Date endTime = Optional.ofNullable(request.getEndTime()).orElseGet(() -> {
            try {
                return DateUtils.parseDate("2099-12-31 23:59:59", "yyyy-MM-dd HH:mm:ss");
            } catch (Exception e) {
                return DateUtils.addYears(new Date(), 50);
            }
        });
        subscribe.setEndTime(endTime);
        if (StringUtils.isNotBlank(request.getReceiveAddr())) {
            subscribe.setReceiveAddr(request.getReceiveAddr());
        } else {
            if (Constants.SubscribeDetail.RAW.equalsValue(subscribeDetail)) {
                subscribe.setReceiveAddr(String.format("ws://%s:%s/VIID/Subscribe/WebSocket", setting.getHost(), setting.getPort()));
            } else {
                subscribe.setReceiveAddr(setting.httpUrlBuilder() + "/VIID/SubscribeNotifications");
            }
        }
        subscribe.setReportInterval(Optional.ofNullable(request.getReportInterval()).orElse(3));
        subscribe.setReason(Optional.ofNullable(request.getReason()).orElseGet(() -> "测试" + title));
        subscribe.setOperateType(0);
        subscribe.setSubscribeStatus(0);
        //0=卡口,1=设备,4=视图库
        subscribe.setResourceClass(request.getResourceClass());
        subscribe.setResultImageDeclare("-1");
        subscribe.setResultFeatureDeclare(1);
        return subscribe;
    }

    public static VIIDPublish publishBuilder(VIIDPublishRequest request, VIIDServer domain) {
        VIIDPublish publish = new VIIDPublish();
        if (StringUtils.isNotBlank(request.getSubscribeId())) {
            publish.setSubscribeId(request.getSubscribeId());
        } else {
            publish.setSubscribeId(randomSubscriberId(domain.getServerId()));
        }
        String title = request.getTitle();
        publish.setTitle(title);
        String subscribeDetail = request.getSubscribeDetail();
        publish.setSubscribeDetail(subscribeDetail);
        publish.setResourceUri(assembleResourceUri(request));
        publish.setApplicationName(Optional.ofNullable(request.getApplicationName()).orElse("admin"));
        publish.setApplicationOrg(Optional.ofNullable(request.getApplicationOrg()).orElse("d1"));
        Date beginTime = Optional.ofNullable(request.getBeginTime()).orElseGet(() -> DateUtils.addHours(new Date(), -8));
        publish.setBeginTime(beginTime);
        Date endTime = Optional.ofNullable(request.getEndTime()).orElseGet(() -> {
            try {
                return DateUtils.parseDate("2099-12-31 23:59:59", "yyyy-MM-dd HH:mm:ss");
            } catch (Exception e) {
                return DateUtils.addYears(new Date(), 50);
            }
        });
        publish.setEndTime(endTime);
        if (StringUtils.isNotBlank(request.getReceiveAddr())) {
            publish.setReceiveAddr(request.getReceiveAddr());
        } else {
            if (Constants.SubscribeDetail.RAW.equalsValue(subscribeDetail)) {
                publish.setReceiveAddr(String.format("ws://%s:%s/VIID/Subscribe/WebSocket", domain.getHost(), domain.getPort()));
            } else {
                publish.setReceiveAddr(domain.httpUrlBuilder() + "/VIID/SubscribeNotifications");
            }
        }
        publish.setReportInterval(Optional.ofNullable(request.getReportInterval()).orElse(3));
        publish.setReason(Optional.ofNullable(request.getReason()).orElseGet(() -> "测试" + title));
        publish.setOperateType(0);
        publish.setSubscribeStatus(0);
        //0=卡口,1=设备,4=视图库
        publish.setResourceClass(request.getResourceClass());
        if (StringUtils.isNotBlank(request.getResultImageDeclare())) {
            publish.setResultImageDeclare(request.getResultImageDeclare());
        } else {
            publish.setResultImageDeclare(Constants.ImageDeclare.Default.getValue());
        }
        publish.setResultFeatureDeclare(1);
        return publish;
    }

    private static String assembleResourceUri(VIIDSubscribeRequest request) {
        if (Constants.ResourceClass.Instance.equalsValue(request.getResourceClass())) {
            return String.join(",", request.getResourceUri());
        } else if (Constants.ResourceClass.Tollgate.equalsValue(request.getResourceClass())) {
            return String.join(",", request.getResourceUri());
        } else {
            throw new RuntimeException("ResourceClass输入错误");
        }
    }

    private static String assembleResourceUri(VIIDPublishRequest request) {
        if (Constants.ResourceClass.Instance.equalsValue(request.getResourceClass())) {
            return String.join(",", request.getResourceUri());
        } else if (Constants.ResourceClass.Tollgate.equalsValue(request.getResourceClass())) {
            return String.join(",", request.getResourceUri());
        } else {
            throw new RuntimeException("ResourceClass输入错误");
        }
    }

    public static SubscribeObject castSubscribe(VIIDPublish entity) {
        if (Objects.nonNull(entity)) {
            SubscribeObject subscribe = new SubscribeObject();
            BeanUtils.copyProperties(entity, subscribe);
            return subscribe;
        }
        return null;
    }

    public static VIIDSubscribe castSubscribe(SubscribeObject subscribe) {
        VIIDSubscribe entity = new VIIDSubscribe();
        BeanUtils.copyProperties(subscribe, entity);
        return entity;
    }

    public static VIIDPublish castPublish(SubscribeObject subscribe) {
        VIIDPublish entity = new VIIDPublish();
        BeanUtils.copyProperties(subscribe, entity);
        return entity;
    }

    public static APEDevice castApeDevice(APEObject src) {
        APEDevice entity = new APEDevice();
        entity.setApeId(src.getApeID());
        entity.setName(src.getName());
        entity.setModel(src.getModel());
        entity.setIpAddr(src.getIPAddr());
        entity.setIpv6Addr(src.getIPV6Addr());
        entity.setPort(src.getPort());
        entity.setLongitude(Optional.ofNullable(src.getLatitude()).map(Object::toString).orElse(null));
        entity.setLatitude(Optional.ofNullable(src.getLongitude()).map(Object::toString).orElse(null));
        entity.setPlaceCode(src.getPlaceCode());
        entity.setPlace(src.getPlace());
        entity.setOrgCode(src.getOrgCode());
        entity.setCapDirection(src.getCapDirection());
        entity.setMonitorDirection(src.getMonitorDirection());
        entity.setMonitorAreaDesc(src.getMonitorAreaDesc());
        entity.setOwnerApsId(src.getOwnerApsID());
        entity.setIsOnline(src.getIsOnline());
        entity.setUserId(src.getUserId());
        entity.setPassword(src.getPassword());
        return entity;
    }

    public static APEObject castApeObject(APEDevice src) {
        APEObject entity = new APEObject();
        entity.setApeID(src.getApeId());
        entity.setName(src.getName());
        entity.setModel(src.getModel());
        entity.setIPAddr(src.getIpAddr());
        entity.setIPV6Addr(src.getIpv6Addr());
        entity.setPort(src.getPort());
        entity.setLongitude(Optional.ofNullable(src.getLatitude()).map(NumberUtils::toDouble).orElse(null));
        entity.setLatitude(Optional.ofNullable(src.getLongitude()).map(NumberUtils::toDouble).orElse(null));
        entity.setPlaceCode(src.getPlaceCode());
        entity.setPlace(src.getPlace());
        entity.setOrgCode(src.getOrgCode());
        entity.setCapDirection(src.getCapDirection());
        entity.setMonitorDirection(src.getMonitorDirection());
        entity.setMonitorAreaDesc(src.getMonitorAreaDesc());
        entity.setOwnerApsID(src.getOwnerApsId());
        entity.setIsOnline(src.getIsOnline());
        entity.setUserId(src.getUserId());
        entity.setPassword(src.getPassword());
        return entity;
    }

    public static SubscribeObject castPublish(VIIDPublish publish) {
        SubscribeObject subscribe = new SubscribeObject();
        BeanUtils.copyProperties(publish, subscribe);
        return subscribe;
    }

    public static TollgateDevice castTollgateDevice(TollgateObject src) {
        TollgateDevice device = new TollgateDevice();
        device.setTollgateId(src.getTollgateID());
        device.setLaneNum(src.getLaneNum());
        device.setLatitude(Optional.ofNullable(src.getLatitude()).map(Object::toString).orElse(null));
        device.setLongitude(Optional.ofNullable(src.getLongitude()).map(Object::toString).orElse(null));
        device.setName(src.getName());
        device.setOrgCode(src.getOrgCode());
        device.setPlaceCode(src.getPlaceCode());
        device.setStatus(src.getStatus());
        device.setTollgateCat(src.getTollgateCat());
        device.setTollgateUsage(Optional.ofNullable(src.getTollgateUsage()).map(Object::toString).orElse(null));
        return device;
    }

    public static TollgateObject castTollgateObject(TollgateDevice src) {
        TollgateObject device = new TollgateObject();
        device.setTollgateID(src.getTollgateId());
        device.setLaneNum(src.getLaneNum());
        device.setLatitude(Optional.ofNullable(src.getLatitude()).map(NumberUtils::toDouble).orElse(null));
        device.setLongitude(Optional.ofNullable(src.getLongitude()).map(NumberUtils::toDouble).orElse(null));
        device.setName(src.getName());
        device.setOrgCode(src.getOrgCode());
        device.setPlaceCode(src.getPlaceCode());
        device.setStatus(src.getStatus());
        device.setTollgateCat(src.getTollgateCat());
        device.setTollgateUsage(Optional.ofNullable(src.getTollgateUsage()).map(NumberUtils::toInt).orElse(null));
        return device;
    }

    public static VIIDDisposition castVIIDDisposition(DispositionObject src) {
        VIIDDisposition model = new VIIDDisposition();
        model.setDispositionId(src.getDispositionID());
        model.setTitle(src.getTitle());
        model.setDispositionCategory(src.getDispositionCategory());
        model.setTargetFeature(src.getTargetFeature());
        model.setTargetImageUri(src.getTargetImageURI());
        model.setPriorityLevel(src.getPriorityLevel());
        model.setApplicantName(src.getApplicantName());
        model.setApplicantInfo(src.getApplicantInfo());
        model.setApplicantOrg(src.getApplicantOrg());
        model.setBeginTime(src.getBeginTime());
        model.setEndTime(src.getEndTime());
        model.setOperateType(src.getOperateType());
        model.setDispositionStatus(src.getDispositionStatus());
        model.setDispositionRange(src.getDispositionRange());
        model.setTollgateList(src.getTollgateList());
        model.setDispositionArea(src.getDispositionArea());
        model.setReceiveAddr(src.getReceiveAddr());
        model.setReceiveMobile(src.getReceiveMobile());
        model.setReason(src.getReason());
        model.setSubImageList(src.getSubImageList());
        return model;
    }

    public static DispositionObject castDisposition(VIIDDisposition src) {
        DispositionObject model = new DispositionObject();
        model.setDispositionID(src.getDispositionId());
        model.setTitle(src.getTitle());
        model.setDispositionCategory(src.getDispositionCategory());
        model.setTargetFeature(src.getTargetFeature());
        model.setTargetImageURI(src.getTargetImageUri());
        model.setPriorityLevel(src.getPriorityLevel());
        model.setApplicantName(src.getApplicantName());
        model.setApplicantInfo(src.getApplicantInfo());
        model.setApplicantOrg(src.getApplicantOrg());
        model.setBeginTime(src.getBeginTime());
        model.setEndTime(src.getEndTime());
        model.setOperateType(src.getOperateType());
        model.setDispositionStatus(src.getDispositionStatus());
        model.setDispositionRange(src.getDispositionRange());
        model.setTollgateList(src.getTollgateList());
        model.setDispositionArea(src.getDispositionArea());
        model.setReceiveAddr(src.getReceiveAddr());
        model.setReceiveMobile(src.getReceiveMobile());
        model.setReason(src.getReason());
        model.setSubImageList(src.getSubImageList());
        return model;
    }

    public static LaneObject toLaneObject(Lane src) {
        if (src == null)
            return null;
        LaneObject data = new LaneObject();
        BeanUtils.copyProperties(src, data);
        return data;
    }

    public static String randomDispositionId() {
        String prefix = VIID_AREA_CODE.substring(0, 6);
        String suffix = "000000";
        String op = "01";
        String timestamp = DateFormatUtils.format(new Date(), "yyyyMMddHHmm");
        String random = RandomStringUtils.randomNumeric(7);
        // 6+6+2+12+7
        return prefix + suffix + op + timestamp + random;
    }

    private static String randomSubscriberId(String organization) {
        if (StringUtils.length(organization) < 6)
            organization = VIID_AREA_CODE;
        String prefix = organization.substring(0, 6);
        String suffix = "000000";
        String op = "03";
        String timestamp = DateFormatUtils.format(new Date(), "yyyyMMddHHmm");
        String random = RandomStringUtils.randomNumeric(7);
        // 6+6+2+12+7
        return prefix + suffix + op + timestamp + random;
    }

    public static String randomNotificationID(String subscribeId) {
        String prefix = subscribeId.substring(0, 12);
        String op = "04";
        String timestamp = DateFormatUtils.format(new Date(), "yyyyMMddHHmm");
        String random = RandomStringUtils.randomNumeric(7);
        // 12+2+12+7
        return prefix + op + timestamp + random;
    }


    public static VIIDServer createDefaultVIIDServer() {
        VIIDServer server = new VIIDServer();
        server.setServerId(randomVIIDServerId(VIID_AREA_CODE));
        server.setServerName("初始化视图库");
        server.setCategory(Constants.InstanceCategory.THIS.getValue());
        server.setEnabled(true);
        server.setScheme("http");
        server.setKeepalive(false);
        server.setHost("127.0.0.1");
        server.setPort(1400);
        server.setUsername("admin");
        server.setAuthenticate("admin");
        server.setTransmission(Constants.VIID_SERVER.TRANSMISSION.HTTP);
        server.setProxyNetwork(Constants.ServerProxyNetwork.Direct.getValue());
        server.setCreateTime(new Date());
        return server;
    }

    public static String randomVIIDServerId(String organization) {
        if (StringUtils.length(organization) < 6)
            organization = VIID_AREA_CODE;
        String prefix = organization.substring(0, 6);
        String suffix = DateFormatUtils.format(new Date(), "yyyy");
        String op = "503";
        String random = RandomStringUtils.randomNumeric(7);
        // 6+6+3+7
        return prefix + suffix + op + random;
    }

    public static boolean isServerId(String str) {
        if (StringUtils.length(str) < 20)
            return false;
        String type = StringUtils.substring(str, 10, 13);
        return "503".equals(type);
    }

    public static String randomVIIDTollgateId(String organization) {
        if (StringUtils.length(organization) < 6)
            organization = VIID_AREA_CODE;
        String prefix = organization.substring(0, 6);
        String suffix = "0000121";
        String random = RandomStringUtils.randomNumeric(7);
        // 6+7+7
        return prefix + suffix + random;
    }

    public static String randomVIIDDeviceId(String organization) {
        if (StringUtils.length(organization) < 6)
            organization = VIID_AREA_CODE;
        String prefix = organization.substring(0, 6);
        String suffix = "0000132";
        String random = RandomStringUtils.randomNumeric(7);
        // 6+7+7
        return prefix + suffix + random;
    }

    public static boolean isDeviceId(String str) {
        if (StringUtils.length(str) < 20)
            return false;
        String type = StringUtils.substring(str, 10, 13);
        return "132".equals(type);
    }

    public static String randomVIIDMotorVehicleId(String deviceId) {
        String suffix = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
        String random = RandomStringUtils.randomNumeric(5);
        String random2 = RandomStringUtils.randomNumeric(5);
        //20+2+14+5+2+5
        return deviceId + "02" + suffix + random + "02" + random2;
    }

    public static String randomVIIDImageId(String deviceId) {
        String suffix = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
        String random = RandomStringUtils.randomNumeric(5);
        //20+2+14+5
        return deviceId + "02" + suffix + random;
    }
}
