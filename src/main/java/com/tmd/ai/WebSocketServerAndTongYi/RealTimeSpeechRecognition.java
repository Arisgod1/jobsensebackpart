package com.tmd.ai.WebSocketServerAndTongYi;/*package com.tmd.ai.WebSocketServer;
import com.alibaba.fastjson.JSONObject;
import com.tmd.ai.config.CustomConfiguration;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Component
@ServerEndpoint("/realtime/audio/websocket/{sid}")
@Slf4j
public class RealTimeSpeechRecognition {
    public static final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
    public static String message1 = "";
    private static final APIWebsocket apiWebsocket = new APIWebsocket();
    
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        sessionMap.put(sid, session);
        log.info("[客户端连接] sid: {} | 在线数: {}", sid, sessionMap.size());
        sendMessageToClient(sid, "连接成功，准备接收音频数据");
    }
    
    @OnMessage
    public void onMessage(ByteBuffer audioBuffer, @PathParam("sid") String sid) {
        log.info("[接收音频] sid: {} | 数据大小: {} 字节", sid, audioBuffer.remaining());
        
        // 确保音频数据大小符合要求 (16kHz, 16位, 单声道, 100ms = 3200字节)
        if (audioBuffer.remaining() != 3200) {
            log.warn("[音频格式警告] 期望3200字节，实际接收: {} 字节", audioBuffer.remaining());
            // 可以选择在此处调整音频大小
        }

        sendMessageToClient(sid, "已接收音频数据，正在进行语音识别...");

        // 发送音频到APIWebsocket处理
        apiWebsocket.sendAudio(audioBuffer);

        // 启动线程等待识别结果并返回给客户端
        executor.submit(() -> {
            try {
                // 等待识别结果（最多30秒）
                long startTime = System.currentTimeMillis();
                while (message1 == null || message1.isEmpty()) {
                    if (System.currentTimeMillis() - startTime > 30000) {
                        log.warn("等待识别结果超时");
                        break;
                    }
                    Thread.sleep(500);
                }

                if (message1 != null && !message1.isEmpty()) {
                    sendMessageToClient(sid, "识别结果: " + message1);
                    message1 = ""; // 清空结果，准备下一次识别
                } else {
                    sendMessageToClient(sid, "语音识别失败或超时");
                }
            } catch (InterruptedException e) {
                log.error("等待识别结果线程异常", e);
            }
        });
    }
    
    @OnClose
    public void onClose(Session session, @PathParam("sid") String sid) {
        sessionMap.remove(sid);
        log.info("[客户端断开] sid: {} | 在线数: {}", sid, sessionMap.size());
    }
    
    @OnError
    public void onError(Throwable error) {
        log.error("[WebSocket错误] sid: {} | 错误信息: {}", getActiveSid(), error.getMessage());
    }
    
    private void sendMessageToClient(String sid, String message) {
        Session session = sessionMap.get(sid);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("发送消息到客户端异常", e);
            }
        }
    }
    
    private String getActiveSid() {
        for (String sid : sessionMap.keySet()) {
            if (sessionMap.get(sid).isOpen()) {
                return sid;
            }
        }
        return "未知";
    }
}*/