package com.genersoft.iot.vmp.gat1400.utils;

import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubImageInfoObject;

import org.apache.commons.lang3.StringUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceUtil {

    /**
     * 解析base64图片宽高
     * @param data base64图片
     * @return SubImageInfoObject载体
     */
    public static SubImageInfoObject resolverImageObject(String data, Consumer<SubImageInfoObject> consumer) {
        if (StringUtils.isBlank(data))
            return null;
        SubImageInfoObject image = new SubImageInfoObject();
        image.setData(data);
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] bytes = decoder.decode(data);
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
            image.setWidth(String.valueOf(bufferedImage.getWidth()));
            image.setHeight(String.valueOf(bufferedImage.getHeight()));
        } catch (Exception e) {
            log.warn("解析图片元数据错误: {}", e.getMessage());
            image.setWidth("0");
            image.setHeight("0");
        }
        consumer.accept(image);
        return image;
    }

    /**
     * 解析base64图片宽高
     * @param data base64图片
     * @return SubImageInfoObject载体
     */
    public static SubImageInfoObject resolverImageObject(String data, String FileFormat) {
        return resolverImageObject(data, image -> {
            image.setFileFormat(FileFormat);
        });
    }

}
