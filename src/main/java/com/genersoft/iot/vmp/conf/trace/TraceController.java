package com.genersoft.iot.vmp.conf.trace;

import com.genersoft.iot.vmp.common.ServerInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 链路追踪页面使用的集群节点接口。
 * <p>
 * 页面加载时调用同源的 /trace/cluster，自动获取当前 WVP 集群内的在线节点，
 * 远端集群仍然由用户手动添加。
 */
@Slf4j
@RestController
@RequestMapping("/trace")
public class TraceController {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @GetMapping("/cluster")
    public Map<String, Object> cluster() {
        List<ServerInfo> serverInfoList = redisCatchStorage.queryOnlineServerInfoList();
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (ServerInfo serverInfo : serverInfoList) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("serverId", serverInfo.getServerId());
            node.put("ip", serverInfo.getIp());
            node.put("port", serverInfo.getPort());
            nodes.add(node);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverId", userSetting.getServerId());
        result.put("nodes", nodes);
        return result;
    }
}
