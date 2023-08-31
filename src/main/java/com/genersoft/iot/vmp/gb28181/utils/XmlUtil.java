package com.genersoft.iot.vmp.gb28181.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.apache.commons.lang3.math.NumberUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import javax.sip.RequestEvent;
import javax.sip.message.Request;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 基于dom4j的工具包
 *
 *
 */
public class XmlUtil {
    /**
     * 日志服务
     */
    private static Logger logger = LoggerFactory.getLogger(XmlUtil.class);

    /**
     * 解析XML为Document对象
     *
     * @param xml 被解析的XMl
     *
     * @return Document
     */
    public static Element parseXml(String xml) {
        Document document = null;
        //
        StringReader sr = new StringReader(xml);
        SAXReader saxReader = new SAXReader();
        try {
            document = saxReader.read(sr);
        } catch (DocumentException e) {
            logger.error("解析失败", e);
        }
        return null == document ? null : document.getRootElement();
    }

    /**
     * 获取element对象的text的值
     *
     * @param em  节点的对象
     * @param tag 节点的tag
     * @return 节点
     */
    public static String getText(Element em, String tag) {
        if (null == em) {
            return null;
        }
        Element e = em.element(tag);
        //
        return null == e ? null : e.getText().trim();
    }

    /**
     * 递归解析xml节点，适用于 多节点数据
     *
     * @param node     node
     * @param nodeName nodeName
     * @return List<Map<String, Object>>
     */
    public static List<Map<String, Object>> listNodes(Element node, String nodeName) {
        if (null == node) {
            return null;
        }
        // 初始化返回
        List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
        // 首先获取当前节点的所有属性节点
        List<Attribute> list = node.attributes();

        Map<String, Object> map = null;
        // 遍历属性节点
        for (Attribute attribute : list) {
            if (nodeName.equals(node.getName())) {
                if (null == map) {
                    map = new HashMap<String, Object>();
                    listMap.add(map);
                }
                // 取到的节点属性放到map中
                map.put(attribute.getName(), attribute.getValue());
            }

        }
        // 遍历当前节点下的所有节点 ，nodeName 要解析的节点名称
        // 使用递归
        Iterator<Element> iterator = node.elementIterator();
        while (iterator.hasNext()) {
            Element e = iterator.next();
            listMap.addAll(listNodes(e, nodeName));
        }
        return listMap;
    }

    /**
     * xml转json
     *
     * @param element
     * @param json
     */
    public static void node2Json(Element element, JSONObject json) {
        // 如果是属性
        for (Object o : element.attributes()) {
            Attribute attr = (Attribute) o;
            if (!ObjectUtils.isEmpty(attr.getValue())) {
                json.put("@" + attr.getName(), attr.getValue());
            }
        }
        List<Element> chdEl = element.elements();
        if (chdEl.isEmpty() && !ObjectUtils.isEmpty(element.getText())) {// 如果没有子元素,只有一个值
            json.put(element.getName(), element.getText());
        }

        for (Element e : chdEl) {   // 有子元素
            if (!e.elements().isEmpty()) {  // 子元素也有子元素
                JSONObject chdjson = new JSONObject();
                node2Json(e, chdjson);
                Object o = json.get(e.getName());
                if (o != null) {
                    JSONArray jsona = null;
                    if (o instanceof JSONObject) {  // 如果此元素已存在,则转为jsonArray
                        JSONObject jsono = (JSONObject) o;
                        json.remove(e.getName());
                        jsona = new JSONArray();
                        jsona.add(jsono);
                        jsona.add(chdjson);
                    }
                    if (o instanceof JSONArray) {
                        jsona = (JSONArray) o;
                        jsona.add(chdjson);
                    }
                    json.put(e.getName(), jsona);
                } else {
                    if (!chdjson.isEmpty()) {
                        json.put(e.getName(), chdjson);
                    }
                }
            } else { // 子元素没有子元素
                for (Object o : element.attributes()) {
                    Attribute attr = (Attribute) o;
                    if (!ObjectUtils.isEmpty(attr.getValue())) {
                        json.put("@" + attr.getName(), attr.getValue());
                    }
                }
                if (!e.getText().isEmpty()) {
                    json.put(e.getName(), e.getText());
                }
            }
        }
    }
    public static  Element getRootElement(RequestEvent evt) throws DocumentException {

        return getRootElement(evt, "gb2312");
    }

    public static Element getRootElement(RequestEvent evt, String charset) throws DocumentException {
        Request request = evt.getRequest();
        return getRootElement(request.getRawContent(), charset);
    }

    public static Element getRootElement(byte[] content, String charset) throws DocumentException {
        if (charset == null) {
            charset = "gb2312";
        }
        SAXReader reader = new SAXReader();
        reader.setEncoding(charset);
        Document xml = reader.read(new ByteArrayInputStream(content));
        return xml.getRootElement();
    }

    private enum ChannelType{
        CivilCode, BusinessGroup,VirtualOrganization,Other
    }

    public static DeviceChannel channelContentHandler(Element itemDevice, Device device, String event, CivilCodeFileConf civilCodeFileConf){
        DeviceChannel deviceChannel = new DeviceChannel();
        deviceChannel.setDeviceId(device.getDeviceId());
        Element channdelIdElement = itemDevice.element("DeviceID");
        if (channdelIdElement == null) {
            logger.warn("解析Catalog消息时发现缺少 DeviceID");
            return null;
        }
        String channelId = channdelIdElement.getTextTrim();
        if (ObjectUtils.isEmpty(channelId)) {
            logger.warn("解析Catalog消息时发现缺少 DeviceID");
            return null;
        }
        deviceChannel.setChannelId(channelId);
        if (event != null && !event.equals(CatalogEvent.ADD) && !event.equals(CatalogEvent.UPDATE)) {
            // 除了ADD和update情况下需要识别全部内容，
            return deviceChannel;
        }
        Element nameElement = itemDevice.element("Name");
        if (nameElement != null) {
            deviceChannel.setName(nameElement.getText());
        }
        if(channelId.length() <= 8) {
            deviceChannel.setHasAudio(false);
            CivilCodePo parentCode = civilCodeFileConf.getParentCode(channelId);
            if (parentCode != null) {
                deviceChannel.setParentId(parentCode.getCode());
                deviceChannel.setCivilCode(parentCode.getCode());
            }else {
                logger.warn("[xml解析] 无法确定行政区划{}的上级行政区划", channelId);
            }
            deviceChannel.setStatus(true);
            return deviceChannel;
        }else {
            if(channelId.length() != 20) {
                logger.warn("[xml解析] 失败，编号不符合国标28181定义： {}", channelId);
                return null;
            }

            int code = Integer.parseInt(channelId.substring(10, 13));
            if (code == 136 || code == 137 || code == 138) {
                deviceChannel.setHasAudio(true);
            }else {
                deviceChannel.setHasAudio(false);
            }
            // 设备厂商
            String manufacturer = getText(itemDevice, "Manufacturer");
            // 设备型号
            String model = getText(itemDevice, "Model");
            // 设备归属
            String owner = getText(itemDevice, "Owner");
            // 行政区域
            String civilCode = getText(itemDevice, "CivilCode");
            // 虚拟组织所属的业务分组ID,业务分组根据特定的业务需求制定,一个业务分组包含一组特定的虚拟组织
            String businessGroupID = getText(itemDevice, "BusinessGroupID");
            // 父设备/区域/系统ID
            String parentID = getText(itemDevice, "ParentID");
            if (parentID != null && parentID.equalsIgnoreCase("null")) {
                parentID = null;
            }
            // 注册方式(必选)缺省为1;1:符合IETFRFC3261标准的认证注册模式;2:基于口令的双向认证注册模式;3:基于数字证书的双向认证注册模式
            String registerWay = getText(itemDevice, "RegisterWay");
            // 保密属性(必选)缺省为0;0:不涉密,1:涉密
            String secrecy = getText(itemDevice, "Secrecy");
            // 安装地址
            String address = getText(itemDevice, "Address");

            switch (code){
                case 200:
                    // 系统目录
                    if (!ObjectUtils.isEmpty(manufacturer)) {
                        deviceChannel.setManufacture(manufacturer);
                    }
                    if (!ObjectUtils.isEmpty(model)) {
                        deviceChannel.setModel(model);
                    }
                    if (!ObjectUtils.isEmpty(owner)) {
                        deviceChannel.setOwner(owner);
                    }
                    if (!ObjectUtils.isEmpty(civilCode)) {
                        deviceChannel.setCivilCode(civilCode);
                        deviceChannel.setParentId(civilCode);
                    }else {
                        if (!ObjectUtils.isEmpty(parentID)) {
                            deviceChannel.setParentId(parentID);
                        }
                    }
                    if (!ObjectUtils.isEmpty(address)) {
                        deviceChannel.setAddress(address);
                    }
                    deviceChannel.setStatus(true);
                    if (!ObjectUtils.isEmpty(registerWay)) {
                        try {
                            deviceChannel.setRegisterWay(Integer.parseInt(registerWay));
                        }catch (NumberFormatException exception) {
                            logger.warn("[xml解析] 从通道数据获取registerWay失败： {}", registerWay);
                        }
                    }
                    if (!ObjectUtils.isEmpty(secrecy)) {
                        deviceChannel.setSecrecy(secrecy);
                    }
                    return deviceChannel;
                case 215:
                    // 业务分组
                    deviceChannel.setStatus(true);
                    if (!ObjectUtils.isEmpty(parentID)) {
                        if (!parentID.trim().equalsIgnoreCase(device.getDeviceId())) {
                            deviceChannel.setParentId(parentID);
                        }
                    }else {
                        logger.warn("[xml解析] 业务分组数据中缺少关键信息->ParentId");
                        if (!ObjectUtils.isEmpty(civilCode)) {
                            deviceChannel.setCivilCode(civilCode);
                        }
                    }
                    break;
                case 216:
                    // 虚拟组织
                    deviceChannel.setStatus(true);
                    if (!ObjectUtils.isEmpty(businessGroupID)) {
                        deviceChannel.setBusinessGroupId(businessGroupID);
                    }

                    if (!ObjectUtils.isEmpty(parentID)) {
                        if (parentID.contains("/")) {
                            String[] parentIdArray = parentID.split("/");
                            parentID = parentIdArray[parentIdArray.length - 1];
                        }
                        deviceChannel.setParentId(parentID);
                    }else {
                        if (!ObjectUtils.isEmpty(businessGroupID)) {
                            deviceChannel.setParentId(businessGroupID);
                        }
                    }
                    break;
                default:
                    // 设备目录
                    if (!ObjectUtils.isEmpty(manufacturer)) {
                        deviceChannel.setManufacture(manufacturer);
                    }
                    if (!ObjectUtils.isEmpty(model)) {
                        deviceChannel.setModel(model);
                    }
                    if (!ObjectUtils.isEmpty(owner)) {
                        deviceChannel.setOwner(owner);
                    }
                    if (!ObjectUtils.isEmpty(civilCode)
                            && civilCode.length() <= 8
                            && NumberUtils.isParsable(civilCode)
                            && civilCode.length()%2 == 0
                    ) {
                        deviceChannel.setCivilCode(civilCode);
                    }
                    if (!ObjectUtils.isEmpty(businessGroupID)) {
                        deviceChannel.setBusinessGroupId(businessGroupID);
                    }

                    // 警区
                    String block = getText(itemDevice, "Block");
                    if (!ObjectUtils.isEmpty(block)) {
                        deviceChannel.setBlock(block);
                    }
                    if (!ObjectUtils.isEmpty(address)) {
                        deviceChannel.setAddress(address);
                    }

                    if (!ObjectUtils.isEmpty(secrecy)) {
                        deviceChannel.setSecrecy(secrecy);
                    }

                    // 当为设备时,是否有子设备(必选)1有,0没有
                    String parental = getText(itemDevice, "Parental");
                    if (!ObjectUtils.isEmpty(parental)) {
                        try {
                            // 由于海康会错误的发送65535作为这里的取值,所以这里除非是0否则认为是1
                            if (!ObjectUtils.isEmpty(parental) && parental.length() == 1 && Integer.parseInt(parental) == 0) {
                                deviceChannel.setParental(0);
                            }else {
                                deviceChannel.setParental(1);
                            }
                        }catch (NumberFormatException e) {
                            logger.warn("[xml解析] 从通道数据获取 parental失败： {}", parental);
                        }
                    }
                    // 父设备/区域/系统ID

                    if (!ObjectUtils.isEmpty(parentID) ) {
                        if (parentID.contains("/")) {
                            String[] parentIdArray = parentID.split("/");
                            deviceChannel.setParentId(parentIdArray[parentIdArray.length - 1]);
                        }else {
                            if (parentID.length()%2 == 0) {
                                deviceChannel.setParentId(parentID);
                            }else {
                                logger.warn("[xml解析] 不规范的parentID：{}, 已舍弃", parentID);
                            }
                        }
                    }else {
                        if (!ObjectUtils.isEmpty(businessGroupID)) {
                            deviceChannel.setParentId(businessGroupID);
                        }else {
                            if (!ObjectUtils.isEmpty(deviceChannel.getCivilCode())) {
                                deviceChannel.setParentId(deviceChannel.getCivilCode());
                            }
                        }
                    }
                    // 注册方式
                    if (!ObjectUtils.isEmpty(registerWay)) {
                        try {
                            int registerWayInt = Integer.parseInt(registerWay);
                            deviceChannel.setRegisterWay(registerWayInt);
                        }catch (NumberFormatException exception) {
                            logger.warn("[xml解析] 从通道数据获取registerWay失败： {}", registerWay);
                            deviceChannel.setRegisterWay(1);
                        }
                    }else {
                        deviceChannel.setRegisterWay(1);
                    }

                    // 信令安全模式(可选)缺省为0; 0:不采用;2:S/MIME 签名方式;3:S/MIME加密签名同时采用方式;4:数字摘要方式
                    String safetyWay = getText(itemDevice, "SafetyWay");
                    if (!ObjectUtils.isEmpty(safetyWay)) {
                        try {
                            deviceChannel.setSafetyWay(Integer.parseInt(safetyWay));
                        }catch (NumberFormatException e) {
                            logger.warn("[xml解析] 从通道数据获取 safetyWay失败： {}", safetyWay);
                        }
                    }

                    // 证书序列号(有证书的设备必选)
                    String certNum = getText(itemDevice, "CertNum");
                    if (!ObjectUtils.isEmpty(certNum)) {
                        deviceChannel.setCertNum(certNum);
                    }

                    // 证书有效标识(有证书的设备必选)缺省为0;证书有效标识:0:无效 1:有效
                    String certifiable = getText(itemDevice, "Certifiable");
                    if (!ObjectUtils.isEmpty(certifiable)) {
                        try {
                            deviceChannel.setCertifiable(Integer.parseInt(certifiable));
                        }catch (NumberFormatException e) {
                            logger.warn("[xml解析] 从通道数据获取 Certifiable失败： {}", certifiable);
                        }
                    }

                    // 无效原因码(有证书且证书无效的设备必选)
                    String errCode = getText(itemDevice, "ErrCode");
                    if (!ObjectUtils.isEmpty(errCode)) {
                        try {
                            deviceChannel.setErrCode(Integer.parseInt(errCode));
                        }catch (NumberFormatException e) {
                            logger.warn("[xml解析] 从通道数据获取 ErrCode失败： {}", errCode);
                        }
                    }

                    // 证书终止有效期(有证书的设备必选)
                    String endTime = getText(itemDevice, "EndTime");
                    if (!ObjectUtils.isEmpty(endTime)) {
                        deviceChannel.setEndTime(endTime);
                    }


                    // 设备/区域/系统IP地址
                    String ipAddress = getText(itemDevice, "IPAddress");
                    if (!ObjectUtils.isEmpty(ipAddress)) {
                        deviceChannel.setIpAddress(ipAddress);
                    }

                    // 设备/区域/系统端口
                    String port = getText(itemDevice, "Port");
                    if (!ObjectUtils.isEmpty(port)) {
                        try {
                            deviceChannel.setPort(Integer.parseInt(port));
                        }catch (NumberFormatException e) {
                            logger.warn("[xml解析] 从通道数据获取 Port失败： {}", port);
                        }
                    }

                    // 设备口令
                    String password = getText(itemDevice, "Password");
                    if (!ObjectUtils.isEmpty(password)) {
                        deviceChannel.setPassword(password);
                    }


                    // 设备状态
                    String status = getText(itemDevice, "Status");
                    if (status != null) {
                        // ONLINE OFFLINE HIKVISION DS-7716N-E4 NVR的兼容性处理
                        if (status.equals("ON") || status.equals("On") || status.equals("ONLINE") || status.equals("OK")) {
                            deviceChannel.setStatus(true);
                        }
                        if (status.equals("OFF") || status.equals("Off") || status.equals("OFFLINE")) {
                            deviceChannel.setStatus(false);
                        }
                    }else {
                        deviceChannel.setStatus(true);
                    }

                    // 经度
                    String longitude = getText(itemDevice, "Longitude");
                    if (NumericUtil.isDouble(longitude)) {
                        deviceChannel.setLongitude(Double.parseDouble(longitude));
                    } else {
                        deviceChannel.setLongitude(0.00);
                    }

                    // 纬度
                    String latitude = getText(itemDevice, "Latitude");
                    if (NumericUtil.isDouble(latitude)) {
                        deviceChannel.setLatitude(Double.parseDouble(latitude));
                    } else {
                        deviceChannel.setLatitude(0.00);
                    }

                    deviceChannel.setGpsTime(DateUtil.getNow());

                    // -摄像机类型扩展,标识摄像机类型:1-球机;2-半球;3-固定枪机;4-遥控枪机。当目录项为摄像机时可选
                    String ptzType = getText(itemDevice, "PTZType");
                    if (ObjectUtils.isEmpty(ptzType)) {
                        //兼容INFO中的信息
                        Element info = itemDevice.element("Info");
                        String ptzTypeFromInfo = XmlUtil.getText(info, "PTZType");
                        if(!ObjectUtils.isEmpty(ptzTypeFromInfo)){
                            try {
                                deviceChannel.setPTZType(Integer.parseInt(ptzTypeFromInfo));
                            }catch (NumberFormatException e){
                                logger.warn("[xml解析] 从通道数据info中获取PTZType失败： {}", ptzTypeFromInfo);
                            }
                        }
                    } else {
                        try {
                            deviceChannel.setPTZType(Integer.parseInt(ptzType));
                        }catch (NumberFormatException e){
                            logger.warn("[xml解析] 从通道数据中获取PTZType失败： {}", ptzType);
                        }
                    }

                    // TODO 摄像机位置类型扩展。
                    // 1-省际检查站、
                    // 2-党政机关、
                    // 3-车站码头、
                    // 4-中心广场、
                    // 5-体育场馆、
                    // 6-商业中心、
                    // 7-宗教场所、
                    // 8-校园周边、
                    // 9-治安复杂区域、
                    // 10-交通干线。
                    // String positionType = getText(itemDevice, "PositionType");

                    // TODO 摄像机安装位置室外、室内属性。1-室外、2-室内。
                    // String roomType = getText(itemDevice, "RoomType");
                    // TODO 摄像机用途属性
                    // String useType = getText(itemDevice, "UseType");
                    // TODO 摄像机补光属性。1-无补光、2-红外补光、3-白光补光
                    // String supplyLightType = getText(itemDevice, "SupplyLightType");
                    // TODO 摄像机监视方位属性。1-东、2-西、3-南、4-北、5-东南、6-东北、7-西南、8-西北。
                    // String directionType = getText(itemDevice, "DirectionType");
                    // TODO 摄像机支持的分辨率,可有多个分辨率值,各个取值间以“/”分隔。分辨率取值参见附录 F中SDPf字段规定
                    // String resolution = getText(itemDevice, "Resolution");

                    // TODO 下载倍速范围(可选),各可选参数以“/”分隔,如设备支持1,2,4倍速下载则应写为“1/2/4
                    // String downloadSpeed = getText(itemDevice, "DownloadSpeed");
                    // TODO 空域编码能力,取值0:不支持;1:1级增强(1个增强层);2:2级增强(2个增强层);3:3级增强(3个增强层)
                    // String svcSpaceSupportMode = getText(itemDevice, "SVCSpaceSupportMode");
                    // TODO 时域编码能力,取值0:不支持;1:1级增强;2:2级增强;3:3级增强
                    // String svcTimeSupportMode = getText(itemDevice, "SVCTimeSupportMode");


                    deviceChannel.setSecrecy(secrecy);
                    break;
            }
        }

        return deviceChannel;
    }

    /**
     * 新增方法支持内部嵌套
     *
     * @param element xmlElement
     * @param clazz 结果类
     * @param <T> 泛型
     * @return 结果对象
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <T> T loadElement(Element element, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        T t = clazz.getDeclaredConstructor().newInstance();
        for (Field field : fields) {
            ReflectionUtils.makeAccessible(field);
            MessageElement annotation = field.getAnnotation(MessageElement.class);
            if (annotation == null) {
                continue;
            }
            String value = annotation.value();
            String subVal = annotation.subVal();
            Element element1 = element.element(value);
            if (element1 == null) {
                continue;
            }
            if ("".equals(subVal)) {
                // 无下级数据
                Object fieldVal = element1.isTextOnly() ? element1.getText() : loadElement(element1, field.getType());
                Object o = simpleTypeDeal(field.getType(), fieldVal);
                ReflectionUtils.setField(field, t,  o);
            } else {
                // 存在下级数据
                ArrayList<Object> list = new ArrayList<>();
                Type genericType = field.getGenericType();
                if (!(genericType instanceof ParameterizedType)) {
                    continue;
                }
                Class<?> aClass = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                for (Element element2 : element1.elements(subVal)) {
                    list.add(loadElement(element2, aClass));
                }
                ReflectionUtils.setField(field, t, list);
            }
        }
        return t;
    }

    /**
     * 简单类型处理
     *
     * @param tClass
     * @param val
     * @return
     */
    private static Object simpleTypeDeal(Class<?> tClass, Object val) {
        if (tClass.equals(String.class)) {
            return val.toString();
        }
        if (tClass.equals(Integer.class)) {
            return Integer.valueOf(val.toString());
        }
        if (tClass.equals(Double.class)) {
            return Double.valueOf(val.toString());
        }
        if (tClass.equals(Long.class)) {
            return Long.valueOf(val.toString());
        }
        return val;
    }
}