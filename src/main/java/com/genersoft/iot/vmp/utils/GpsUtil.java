package com.genersoft.iot.vmp.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;

import com.genersoft.iot.vmp.gb28181.bean.BaiduPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpsUtil {

    private static Logger logger = LoggerFactory.getLogger(GpsUtil.class);

    public static BaiduPoint Wgs84ToBd09(String xx, String yy) {


//        try {
//            Socket s = new Socket("api.map.baidu.com", 80);
//            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
//            OutputStream out = s.getOutputStream();
//            StringBuffer sb = new StringBuffer("GET /ag/coord/convert?from=0&to=4");
//            sb.append("&x=" + xx + "&y=" + yy);
//            sb.append("&callback=BMap.Convertor.cbk_3976 HTTP/1.1\r\n");
//            sb.append("User-Agent: Java/1.6.0_20\r\n");
//            sb.append("Host: api.map.baidu.com:80\r\n");
//            sb.append("Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r\n");
//            sb.append("Connection: Close\r\n");
//            sb.append("\r\n");
//            out.write(sb.toString().getBytes());
//            String json = "";
//            String tmp = "";
//            while ((tmp = br.readLine()) != null) {
//                // logger.info(tmp);
//                json += tmp;
//            }
//
//            s.close();
//            int start = json.indexOf("cbk_3976");
//            int end = json.lastIndexOf("}");
//            if (start != -1 && end != -1 && json.contains("\"x\":\"")) {
//                json = json.substring(start, end);
//                String[] point = json.split(",");
//                String x = point[1].split(":")[1].replace("\"", "");
//                String y = point[2].split(":")[1].replace("\"", "");
//                BaiduPoint bdPoint= new BaiduPoint();
//                bdPoint.setBdLng(new String(decode(x)));
//                bdPoint.setBdLat(new String(decode(y)));
//                return bdPoint;
//                //return (new String(decode(x)) + "," + new String(decode(y)));
//            } else {
//                logger.info("gps坐标无效！！");
//            }
//            out.close();
//            br.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        double lng = Double.parseDouble(xx);
        double lat = Double.parseDouble(yy);
        Double[] gcj02 = Coordtransform.WGS84ToGCJ02(lng, lat);
        Double[] doubles = Coordtransform.GCJ02ToBD09(gcj02[0], gcj02[1]);
        BaiduPoint bdPoint= new BaiduPoint();
        bdPoint.setBdLng(doubles[0] + "");
        bdPoint.setBdLat(doubles[1] + "");
        return bdPoint;
    }

    /**
     * BASE64解码
     * @param str
     * @return string
     */
    public static byte[] decode(String str) {
        byte[] bt = null;
        final Base64.Decoder decoder = Base64.getDecoder();
        bt = decoder.decode(str); // .decodeBuffer(str);
        return bt;
    }
}
