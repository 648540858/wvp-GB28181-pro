package com.genersoft.iot.vmp.gb28181.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.sip.RequestEvent;
import javax.sip.message.Request;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
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
    private static Logger LOG = LoggerFactory.getLogger(XmlUtil.class);

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
            LOG.error("解析失败", e);
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
            if (!StringUtils.isEmpty(attr.getValue())) {
                json.put("@" + attr.getName(), attr.getValue());
            }
        }
        List<Element> chdEl = element.elements();
        if (chdEl.isEmpty() && !StringUtils.isEmpty(element.getText())) {// 如果没有子元素,只有一个值
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
                    if (!StringUtils.isEmpty(attr.getValue())) {
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

    public static DeviceChannel channelContentHander(Element itemDevice, Device device){
        Element channdelNameElement = itemDevice.element("Name");
        String channelName = channdelNameElement != null ? channdelNameElement.getTextTrim().toString() : "";
        Element statusElement = itemDevice.element("Status");
        String status = statusElement != null ? statusElement.getTextTrim().toString() : "ON";
        DeviceChannel deviceChannel = new DeviceChannel();
        deviceChannel.setName(channelName);
        Element channdelIdElement = itemDevice.element("DeviceID");
        String channelId = channdelIdElement != null ? channdelIdElement.getTextTrim().toString() : "";
        deviceChannel.setChannelId(channelId);
        // ONLINE OFFLINE HIKVISION DS-7716N-E4 NVR的兼容性处理
        if (status.equals("ON") || status.equals("On") || status.equals("ONLINE") || status.equals("OK")) {
            deviceChannel.setStatus(1);
        }
        if (status.equals("OFF") || status.equals("Off") || status.equals("OFFLINE")) {
            deviceChannel.setStatus(0);
        }

        deviceChannel.setManufacture(XmlUtil.getText(itemDevice, "Manufacturer"));
        deviceChannel.setModel(XmlUtil.getText(itemDevice, "Model"));
        deviceChannel.setOwner(XmlUtil.getText(itemDevice, "Owner"));
        deviceChannel.setCivilCode(XmlUtil.getText(itemDevice, "CivilCode"));
        deviceChannel.setBlock(XmlUtil.getText(itemDevice, "Block"));
        deviceChannel.setAddress(XmlUtil.getText(itemDevice, "Address"));
        String businessGroupID = XmlUtil.getText(itemDevice, "BusinessGroupID");
        if (XmlUtil.getText(itemDevice, "Parental") == null
                || XmlUtil.getText(itemDevice, "Parental").equals("")) {
            if (deviceChannel.getChannelId().length() <= 10
                    || (deviceChannel.getChannelId().length() == 20 && (
                            Integer.parseInt(deviceChannel.getChannelId().substring(10, 13)) == 215
                                    || Integer.parseInt(deviceChannel.getChannelId().substring(10, 13)) == 216
                            )
                        )
            ) {
                deviceChannel.setParental(1);
            }else {
                deviceChannel.setParental(0);
            }
        } else {
            // 由于海康会错误的发送65535作为这里的取值,所以这里除非是0否则认为是1
            deviceChannel.setParental(Integer.parseInt(XmlUtil.getText(itemDevice, "Parental")) == 1?1:0);
        }
        /**
         * 行政区划展示设备树与业务分组展示设备树是两种不同的模式
         * 行政区划展示设备树 各个目录之间主要靠deviceId做关联,摄像头通过CivilCode指定其属于那个行政区划;都是不超过十位的编号; 结构如下:
         * 河北省
         *    --> 石家庄市
         *          --> 摄像头
         *          --> 正定县
         *                  --> 摄像头
         *                  --> 摄像头
         *
         * 业务分组展示设备树是顶级是业务分组,其下的虚拟组织靠BusinessGroupID指定其所属的业务分组;摄像头通过ParentId来指定其所属于的虚拟组织:
         * 业务分组
         *    --> 虚拟组织
         *         --> 摄像头
         *         --> 虚拟组织
         *             --> 摄像头
         *             --> 摄像头
         */
        String parentId = XmlUtil.getText(itemDevice, "ParentID");
        if (parentId != null) {
            if (parentId.contains("/")) {
                String lastParentId = parentId.substring(parentId.lastIndexOf("/") + 1);
                deviceChannel.setParentId(lastParentId);
            }else {
                deviceChannel.setParentId(parentId);
            }
        }else {
            if (deviceChannel.getChannelId().length() <= 10) { // 此时为行政区划, 上下级行政区划使用DeviceId关联
                deviceChannel.setParentId(deviceChannel.getChannelId().substring(0, deviceChannel.getChannelId().length() - 2));
            }else if (deviceChannel.getChannelId().length() == 20) {
                if (Integer.parseInt(deviceChannel.getChannelId().substring(10, 13)) == 216) { // 虚拟组织
                    deviceChannel.setParentId(businessGroupID);
                }else if (deviceChannel.getCivilCode() != null) {
                    // 设备， 无parentId的20位是使用CivilCode表示上级的设备，
                    // 注：215 业务分组是需要有parentId的
                    deviceChannel.setParentId(deviceChannel.getCivilCode());
                }
            }else {
                deviceChannel.setParentId(deviceChannel.getDeviceId());
            }
        }

        if (XmlUtil.getText(itemDevice, "SafetyWay") == null
                || XmlUtil.getText(itemDevice, "SafetyWay") == "") {
            deviceChannel.setSafetyWay(0);
        } else {
            deviceChannel.setSafetyWay(Integer.parseInt(XmlUtil.getText(itemDevice, "SafetyWay")));
        }
        if (XmlUtil.getText(itemDevice, "RegisterWay") == null
                || XmlUtil.getText(itemDevice, "RegisterWay") == "") {
            deviceChannel.setRegisterWay(1);
        } else {
            deviceChannel.setRegisterWay(Integer.parseInt(XmlUtil.getText(itemDevice, "RegisterWay")));
        }
        deviceChannel.setCertNum(XmlUtil.getText(itemDevice, "CertNum"));
        if (XmlUtil.getText(itemDevice, "Certifiable") == null
                || XmlUtil.getText(itemDevice, "Certifiable") == "") {
            deviceChannel.setCertifiable(0);
        } else {
            deviceChannel.setCertifiable(Integer.parseInt(XmlUtil.getText(itemDevice, "Certifiable")));
        }
        if (XmlUtil.getText(itemDevice, "ErrCode") == null
                || XmlUtil.getText(itemDevice, "ErrCode") == "") {
            deviceChannel.setErrCode(0);
        } else {
            deviceChannel.setErrCode(Integer.parseInt(XmlUtil.getText(itemDevice, "ErrCode")));
        }
        deviceChannel.setEndTime(XmlUtil.getText(itemDevice, "EndTime"));
        deviceChannel.setSecrecy(XmlUtil.getText(itemDevice, "Secrecy"));
        deviceChannel.setIpAddress(XmlUtil.getText(itemDevice, "IPAddress"));
        if (XmlUtil.getText(itemDevice, "Port") == null || XmlUtil.getText(itemDevice, "Port") == "") {
            deviceChannel.setPort(0);
        } else {
            deviceChannel.setPort(Integer.parseInt(XmlUtil.getText(itemDevice, "Port")));
        }
        deviceChannel.setPassword(XmlUtil.getText(itemDevice, "Password"));
        if (NumericUtil.isDouble(XmlUtil.getText(itemDevice, "Longitude"))) {
            deviceChannel.setLongitude(Double.parseDouble(XmlUtil.getText(itemDevice, "Longitude")));
        } else {
            deviceChannel.setLongitude(0.00);
        }
        if (NumericUtil.isDouble(XmlUtil.getText(itemDevice, "Latitude"))) {
            deviceChannel.setLatitude(Double.parseDouble(XmlUtil.getText(itemDevice, "Latitude")));
        } else {
            deviceChannel.setLatitude(0.00);
        }
        if (deviceChannel.getLongitude()*deviceChannel.getLatitude() > 0) {
            if ("WGS84".equals(device.getGeoCoordSys())) {
                deviceChannel.setLongitudeWgs84(deviceChannel.getLongitude());
                deviceChannel.setLatitudeWgs84(deviceChannel.getLatitude());
                Double[] position = Coordtransform.WGS84ToGCJ02(deviceChannel.getLongitude(), deviceChannel.getLatitude());
                deviceChannel.setLongitudeGcj02(position[0]);
                deviceChannel.setLatitudeGcj02(position[1]);
            }else if ("GCJ02".equals(device.getGeoCoordSys())) {
                deviceChannel.setLongitudeGcj02(deviceChannel.getLongitude());
                deviceChannel.setLatitudeGcj02(deviceChannel.getLatitude());
                Double[] position = Coordtransform.GCJ02ToWGS84(deviceChannel.getLongitude(), deviceChannel.getLatitude());
                deviceChannel.setLongitudeWgs84(position[0]);
                deviceChannel.setLatitudeWgs84(position[1]);
            }else {
                deviceChannel.setLongitudeGcj02(0.00);
                deviceChannel.setLatitudeGcj02(0.00);
                deviceChannel.setLongitudeWgs84(0.00);
                deviceChannel.setLatitudeWgs84(0.00);
            }
        }else {
            deviceChannel.setLongitudeGcj02(deviceChannel.getLongitude());
            deviceChannel.setLatitudeGcj02(deviceChannel.getLatitude());
            deviceChannel.setLongitudeWgs84(deviceChannel.getLongitude());
            deviceChannel.setLatitudeWgs84(deviceChannel.getLatitude());
        }
        if (XmlUtil.getText(itemDevice, "PTZType") == null || "".equals(XmlUtil.getText(itemDevice, "PTZType"))) {
            //兼容INFO中的信息
            Element info = itemDevice.element("Info");
            if(XmlUtil.getText(info, "PTZType") == null || "".equals(XmlUtil.getText(info, "PTZType"))){
                deviceChannel.setPTZType(0);
            }else{
                deviceChannel.setPTZType(Integer.parseInt(XmlUtil.getText(info, "PTZType")));
            }
        } else {
            deviceChannel.setPTZType(Integer.parseInt(XmlUtil.getText(itemDevice, "PTZType")));
        }
        deviceChannel.setHasAudio(true); // 默认含有音频，播放时再检查是否有音频及是否AAC
        return deviceChannel;
    }
}