package com.genersoft.iot.vmp.gat1400.framework.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class Constants {
    public static class KAFKA_CONSUMER {
        public static final String APP_DEFAULT_GROUP = "viid_default";
//        public static final String APP_DEFAULT_GROUP = "viid_dev2";
    }
    @Getter
    @AllArgsConstructor
    public enum ImageDeclare {

        Default("0", "默认格式"),
        Base64("1", "base64图片"),
        Url("2", "URL图片");
        private final String value;
        private final String describe;

        public boolean equalsValue(String value) {
            return this.value.equals(value);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum InstanceCategory {
        THIS("0", "当前实例"),
        DOWN("1", "下级节点"),
        UP("2", "上级节点");
        private final String value;
        private final String describe;
    }

    @Getter
    @AllArgsConstructor
    public enum ServerProxyNetwork {
        Direct("1", "直连网络"),
        Boundary("2", "跨网边界")
        ;

        private final String value;
        private final String describe;

        public boolean equalsValue(String value) {
            return this.value.equals(value);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum SubscribeDetail {
        DEVICE("3", "采集设备目录"),
        TOLLGATE("7", "视频卡口目录"),
        Lanes("9", "车道目录"),
        PERSON_INFO("11", "自动采集的人员信息"),
        FACE_INFO("12", "自动采集的人脸信息"),
        PLATE_INFO("13", "自动采集的车辆信息"),
        PLATE_MIRCO_INFO("14", "自动采集的非机动车辆信息"),
        RAW("999", "其他"),
        ;

        private final String value;
        private final String describe;

        public static SubscribeDetail match(String value) {
            for (SubscribeDetail subscribeDetail : SubscribeDetail.values()) {
                if (subscribeDetail.getValue().equalsIgnoreCase(value)) {
                    return subscribeDetail;
                }
            }
            return null;
        }

        public boolean equalsValue(String value) {
            return this.value.equals(value);
        }

        public static String prefix(SubscribeDetail detail) {
            if (detail == SubscribeDetail.DEVICE) {
                return DEFAULT_TOPIC_PREFIX.APE_DEVICE;
            } else if (detail == SubscribeDetail.TOLLGATE) {
                return DEFAULT_TOPIC_PREFIX.TOLLGATE_DEVICE;
            } else if (detail == SubscribeDetail.Lanes) {
                return DEFAULT_TOPIC_PREFIX.LANE;
            } else if (detail == SubscribeDetail.PERSON_INFO) {
                return DEFAULT_TOPIC_PREFIX.PERSON_RECORD;
            } else if (detail == SubscribeDetail.FACE_INFO) {
                return DEFAULT_TOPIC_PREFIX.FACE_RECORD;
            } else if (detail == SubscribeDetail.PLATE_INFO) {
                return DEFAULT_TOPIC_PREFIX.MOTOR_VEHICLE;
            } else if (detail == SubscribeDetail.PLATE_MIRCO_INFO) {
                return DEFAULT_TOPIC_PREFIX.NON_MOTOR_VEHICLE;
            } else {
                return null;
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public enum DeviceStatus {

        Online("1", "在线"),
        Offline("2", "离线"),
        Other("9", "其它");
        private final String value;
        private final String describe;

        public boolean equalsValue(String value) {
            return this.value.equals(value);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum SubscribeStatus {
        In(0, "订阅中"),
        Canceled(1, "已取消"),
        Expire(2, "订阅过期");
        private final Integer value;
        private final String describe;

        public boolean equalsValue(Integer value) {
            return this.value.equals(value);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum SubscribeOperateType {
        In(0, "订阅"),
        Cancel(1, "取消订阅");
        private final Integer value;
        private final String describe;

        public boolean equalsValue(Integer value) {
            return this.value.equals(value);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ResourceClass {
        Tollgate(1, "卡口"),
        Device(2, "采集设备"),
        Instance(4, "视图库");
        private final Integer value;
        private final String describe;

        public boolean equalsValue(Integer value) {
            return this.value.equals(value);
        }
    }

    public static class DEFAULT_TOPIC_PREFIX {
        //卡口设备前缀
        public static final String TOLLGATE_DEVICE = "ga1400.tollgate_device-";

        public static final String LANE = "ga1400.lane-";
        //车辆抓拍前缀
        public static final String MOTOR_VEHICLE = "ga1400.motor_vehicle-";
        //采集设备前缀
        public static final String APE_DEVICE = "ga1400.ape_device-";
        //人脸抓拍前缀
        public static final String FACE_RECORD = "ga1400.face_record-";
        //非机动车前缀
        public static final String NON_MOTOR_VEHICLE = "ga1400.non_motor_vehicle-";
        //人员抓拍前缀
        public static final String PERSON_RECORD = "ga1400.person_record-";
        //布控告警前缀
        public static final String DISPOSITION_RECORD = "ga1400.disposition_record-";

        public static final String RAW = "ga1400.raw-";
    }

    public static class VIID_SERVER {
        public static class TRANSMISSION {
            public static final String HTTP = "http";
            public static final String WEBSOCKET = "websocket";
            //设备直推方式
            public static final String DEVICE = "device";
        }
    }

}
