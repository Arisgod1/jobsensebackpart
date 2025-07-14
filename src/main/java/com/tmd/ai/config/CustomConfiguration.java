package com.tmd.ai.config;

import jakarta.websocket.ClientEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CustomConfiguration extends ClientEndpointConfig.Configurator {
    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        /*log.info("设置请求头...");
        super.beforeRequest(headers);
        headers.put("Authorization", Collections.singletonList("sk-6ba44564c1ab4c80b1e012a7fb89dd87"));
      //  headers.put("user-agent", Collections.singletonList("voice-recognition-client/1.0"));
        headers.put("X-DashScope-DataInspection", Collections.singletonList("enable"));
        log.info("请求头设置完成: {}", headers);*/
    }
}