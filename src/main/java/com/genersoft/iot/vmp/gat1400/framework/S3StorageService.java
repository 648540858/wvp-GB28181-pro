package com.genersoft.iot.vmp.gat1400.framework;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.genersoft.iot.vmp.gat1400.framework.config.OSSProperties;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubImageInfoObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubImageList;
import com.genersoft.iot.vmp.gat1400.framework.exception.VIIDRuntimeException;
import com.genersoft.iot.vmp.gat1400.rpc.ResourceClient;
import com.genersoft.iot.vmp.gat1400.utils.Base64Utils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Resource;

import feign.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class S3StorageService implements InitializingBean {

    private AmazonS3 ossClient;

    private String domain;

    @Resource
    OSSProperties properties;
    @Resource
    ResourceClient resourceClient;


    @Override
    public void afterPropertiesSet() throws Exception {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setConnectionTimeout(3000);
        this.domain = properties.getDomain();
        String endpoint = properties.getEndpoint();
        if (endpoint.startsWith("https")) {
            configuration.setProtocol(Protocol.HTTPS);
        } else {
            configuration.setProtocol(Protocol.HTTP);
        }
        AWSCredentials awsCredentials = new BasicAWSCredentials(properties.getAccessKey(), properties.getAccessSecret());
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        ossClient = AmazonS3Client.builder()
                .withClientConfiguration(configuration)
                .withCredentials(credentialsProvider)
                .enablePathStyleAccess()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, properties.getRegion()))
                .build();
        String bucketName = properties.getBucket();
        log.info("检查对象存储OSS是否存在Bucket[{}]", bucketName);
        if (!ossClient.doesBucketExistV2(bucketName)) {
            log.info("OSS不存在存储桶[{}],自动创建该存储桶", bucketName);
            ossClient.createBucket(bucketName);
            JSONObject payload = new JSONObject();
            payload.put("Version", "2012-10-17");
            payload.put("Statement", new JSONArray()
                    .fluentAdd(
                            new JSONObject()
                                    .fluentPut("Effect", "Allow")
                                    .fluentPut("Principal", new JSONObject().fluentPut("AWS", new JSONArray().fluentAdd("*")))
                                    .fluentPut("Action", new JSONArray().fluentAdd("s3:GetBucketLocation").fluentAdd("s3:ListBucket"))
                                    .fluentPut("Resource", new JSONArray().fluentAdd("arn:aws:s3:::" + bucketName))
                    )
                    .fluentAdd(
                            new JSONObject()
                                    .fluentPut("Effect", "Allow")
                                    .fluentPut("Principal", new JSONObject().fluentPut("AWS", new JSONArray().fluentAdd("*")))
                                    .fluentPut("Action", new JSONArray().fluentAdd("s3:GetObject"))
                                    .fluentPut("Resource", new JSONArray().fluentAdd("arn:aws:s3:::" + bucketName + "/*"))
                    )
            );
            //为了让页面能直接访问图片
            log.info("自动设置存储桶[{}]公开读权限", bucketName);
            ossClient.setBucketPolicy(bucketName, payload.toJSONString());
        }
    }

    public void subImageListStorage(String deviceId, SubImageList imageList) {
        if (Boolean.TRUE.equals(properties.getEnable())) {
            List<SubImageInfoObject> images = Optional.ofNullable(imageList)
                    .map(SubImageList::getSubImageInfoObject)
                    .filter(CollectionUtils::isNotEmpty)
                    .orElse(null);
            if (Objects.isNull(images)) {
                return;
            }
            for (SubImageInfoObject image : images) {
                if (StringUtils.isBlank(image.getData()))
                    continue;
                if (StringUtils.isNotBlank(deviceId)) {
                    image.setDeviceID(deviceId);
                }
                if (StringUtils.isNotBlank(image.getDeviceID())) {
                    String imageId = image.getImageID();
                    if (StringUtils.isNotBlank(imageId)
                            && imageId.length() > image.getDeviceID().length()
                            && !StringUtils.startsWith(imageId, image.getDeviceID())) {
                        //todo 修正 imageId
                        String subId = StringUtils.substring(imageId, image.getDeviceID().length());
                        image.setImageID(image.getDeviceID() + subId);
                    }
                }
                String url = this.upload(image);
                image.setData(null);
                image.setStoragePath(url);
            }
        }
    }

    public void subImageListRestore(SubImageList imageList) {
        if (Boolean.TRUE.equals(properties.getEnable())) {
            List<SubImageInfoObject> images = Optional.ofNullable(imageList)
                    .map(SubImageList::getSubImageInfoObject)
                    .filter(CollectionUtils::isNotEmpty)
                    .orElse(null);
            if (Objects.isNull(images)) {
                return;
            }
            for (SubImageInfoObject image : images) {
                if (StringUtils.isBlank(image.getStoragePath())
                        || StringUtils.isNotBlank(image.getData()))
                    continue;
                String storagePath = image.getStoragePath();
                InputStream inputStream = null;
                try {
                    inputStream = this.download(storagePath);
                    if (inputStream != null) {
                        byte[] byteArray = StreamUtils.copyToByteArray(inputStream);
                        String base64Image = Base64.getEncoder().encodeToString(byteArray);
                        image.setData(base64Image);
                        image.setStoragePath(null);
                    }
                } catch (Exception e) {
                    log.warn("S3图片下载失败: {}", e.getMessage());
                } finally {
                    if (Objects.nonNull(inputStream)) {
                        try {
                            inputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    }

    public String upload(SubImageInfoObject image) {
        String bucketName = properties.getBucket();
        String fileFormat = StringUtils.isNotBlank(image.getFileFormat()) ?
                StringUtils.lowerCase(image.getFileFormat()) : "jpeg";
        byte[] bytes = Base64Utils.decodeStringToBytes(image.getData());
        InputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        metadata.setContentType("image/" + fileFormat);
        String filename = image.getDeviceID() + IdWorker.getIdStr() + "." + fileFormat;
        PutObjectRequest request = new PutObjectRequest(bucketName, filename, inputStream, metadata);
        ossClient.putObject(request);
        return this.domain + "/" + bucketName + "/" + filename;
    }

    public InputStream download(String path) {
        if (StringUtils.isBlank(this.domain)) {
            if (StringUtils.startsWith(path, "/")) {
                return downloadFromOSS(path);
            } else {
                return downloadFromNetwork(path, 0);
            }
        } else {
            if (StringUtils.startsWith(path, this.domain)) {
                return downloadFromOSS(path);
            } else {
                return downloadFromNetwork(path, 0);
            }
        }
    }

    public InputStream downloadFromOSS(String path) {
        int start = StringUtils.lastIndexOf(path, "/");
        if (start < 0 || start >= path.length() - 1) {
            throw new VIIDRuntimeException("图片存储路径不正确:" + path);
        }
        String bucketName = properties.getBucket();
        String filename = StringUtils.substring(path, start + 1);
        S3Object s3Object = ossClient.getObject(bucketName, filename);
        return s3Object.getObjectContent();
    }

    public InputStream downloadFromNetwork(String path, int redirectCount) {
        Response response = null;
        try {
            response = resourceClient.getResource(URI.create(path));
            if (response.status() == 200) {
                InputStream inputStream = response.body().asInputStream();
                byte[] bytes = StreamUtils.copyToByteArray(inputStream);
                inputStream.close();
                return new ByteArrayInputStream(bytes);
            } else if (response.status() == 301 || response.status() == 302) {
                if (redirectCount > 3) {
                    return null;
                }
                Collection<String> location = response.headers().getOrDefault(HttpHeaders.LOCATION, Collections.emptyList());
                if (CollectionUtils.isEmpty(location)) {
                    return null;
                }
                String redirectPath = location.stream().findFirst().orElseThrow(() -> new VIIDRuntimeException("图片下载重定向次数过多"));
                return downloadFromNetwork(redirectPath, ++redirectCount);
            } else {
                return null;
            }
        } catch (Exception e) {
            log.warn("网络图片下载失败: {} - 错误: {}", path, e.getMessage());
            return null;
        } finally {
            if (Objects.nonNull(response))
                response.close();
        }
    }
}
