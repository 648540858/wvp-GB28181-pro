package com.genersoft.iot.vmp.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * @author gaofuwang
 * @version 1.0
 * @date 2022/3/11 10:17
 */
public class UJson {

    private static Logger logger = LoggerFactory.getLogger(UJson.class);
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    static {
        JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    }

    private ObjectNode node;

    public UJson(){
        this.node = JSON_MAPPER.createObjectNode();
    }

    public UJson(String json){
        if(StringUtils.isBlank(json)){
            this.node = JSON_MAPPER.createObjectNode();
        }else{
            try {
                this.node = JSON_MAPPER.readValue(json, ObjectNode.class);
            }catch (Exception e){
                logger.error(e.getMessage(), e);
                this.node = JSON_MAPPER.createObjectNode();
            }
        }
    }

    public UJson(ObjectNode node){
        this.node = node;
    }

    public String asText(String key){
        JsonNode jsonNode = node.get(key);
        if(Objects.isNull(jsonNode)){
            return "";
        }
        return jsonNode.asText();
    }

    public String asText(String key, String defaultVal){
        JsonNode jsonNode = node.get(key);
        if(Objects.isNull(jsonNode)){
            return "";
        }
        return jsonNode.asText(defaultVal);
    }

    public UJson put(String key, String value){
        this.node.put(key, value);
        return this;
    }

    public UJson put(String key, Integer value){
        this.node.put(key, value);
        return this;
    }

    public static UJson json(){
        return new UJson();
    }

    public static UJson json(String json){
        return new UJson(json);
    }

    public static <T> T readJson(String json, Class<T> clazz){
        if(StringUtils.isBlank(json)){
            return null;
        }
        try {
            return JSON_MAPPER.readValue(json, clazz);
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public static String writeJson(Object object) {
        try{
            return JSON_MAPPER.writeValueAsString(object);
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            return "";
        }
    }

    @Override
    public String toString() {
        return node.toString();
    }

    public int asInt(String key, int defValue) {
        JsonNode jsonNode = this.node.get(key);
        if(Objects.isNull(jsonNode)){
            return defValue;
        }
        return jsonNode.asInt(defValue);
    }

    public UJson getSon(String key) {
        JsonNode sonNode = this.node.get(key);
        if(Objects.isNull(sonNode)){
            return new UJson();
        }
        return new UJson((ObjectNode) sonNode);
    }

    public UJson set(String key, ObjectNode sonNode) {
        this.node.set(key, sonNode);
        return this;
    }

    public UJson set(String key, UJson sonNode) {
        this.node.set(key, sonNode.node);
        return this;
    }

    public Iterator<Map.Entry<String, JsonNode>> fields() {
        return this.node.fields();
    }

    public ObjectNode getNode() {
        return this.node;
    }

    public UJson setAll(UJson json) {
        this.node.setAll(json.node);
        return this;
    }
}
