package com.tmd.ai.WebSocketServerAndTongYi;

import com.alibaba.fastjson.JSONObject;
import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class ChatWebSocketClient {
    private JSONObject result;
    private Session userSession = null;
    private final StringBuilder messageBuilder;
    public static boolean flag=false;
    public static final Object lock = new Object();

    public String getResult() {
        return messageBuilder.toString();
    }
    public ChatWebSocketClient(URI endpointURI) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        messageBuilder = new StringBuilder();
    }

    @OnOpen
    public void onOpen(Session session) {
        this.userSession = session;
        System.out.println("WebSocket连接已建立");
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        this.userSession = null;
        System.out.println("WebSocket连接关闭，原因：" + reason);
    }

    @OnMessage
    public void onMessage(String message) throws IOException {
        System.out.println("接收到消息：" + message);

        JSONObject jsonObject = JSONObject.parseObject(message);
        JSONObject header = jsonObject.getJSONObject("header");
        int status = header.getIntValue("status");

        JSONObject payload = jsonObject.getJSONObject("payload");
        if (payload == null) {
            System.err.println("无payload，跳过处理");
            return;
        }
        JSONObject choices = payload.getJSONObject("choices");
        if (choices == null) {
            System.err.println("无choices，跳过处理");
            return;
        }

        // 取 text 数组
        var textArray = choices.getJSONArray("text");
        if (textArray == null) {
            System.err.println("无text数组，跳过处理");
            return;
        }

        // 遍历text数组，拼接所有content字段文本
        for (int i = 0; i < textArray.size(); i++) {
            JSONObject textItem = textArray.getJSONObject(i);
            if (textItem.containsKey("content")) {
                messageBuilder.append(textItem.getString("content"));
            }
            // 如果你也需要拼接reasoning_content，可以在这里添加逻辑，但你当前不需要
        }

        if (status == 0) {
            System.out.println("开始接收消息");
        } else if (status == 1) {
            System.out.println("消息处理中，已接收部分内容");
        } else if (status == 2) {
            System.out.println("消息接收结束，准备关闭连接");
            synchronized (lock){
                flag=true;
                lock.notifyAll();
            }
            this.userSession.close();
        }
    }


    public void sendMessage (String message){
        if (this.userSession != null && this.userSession.isOpen()) {
            this.userSession.getAsyncRemote().sendText(message);
        } else {
            System.err.println("WebSocket连接未建立，发送失败");
        }
    }
}
