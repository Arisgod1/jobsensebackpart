package com.tmd.ai.WebSocketServer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tmd.ai.service.RunPythonWithConda;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/*约束
接口调用方式限制：不支持前端直接调用API，需通过后端中转。*/
/*
建立连接：客户端与服务端建立WebSocket连接。

开启任务：

客户端发送run-task指令以开启任务。

客户端收到服务端返回的task-started事件，标志着任务已成功开启，可以进行后续步骤。

发送音频流：

客户端开始发送音频流，并同时接收服务端持续返回的result-generated事件，该事件包含语音识别结果。

通知服务端结束任务：

客户端发送finish-task指令通知服务端结束任务，并继续接收服务端返回的result-generated事件。

任务结束：

客户端收到服务端返回的task-finished事件，标志着任务结束。

关闭连接：客户端关闭WebSocket连接。
*/
/*
在编写WebSocket客户端代码时，为了同时发送和接收消息，通常采用异步编程。您可以按照以下步骤来编写程序：

建立WebSocket连接：首先，初始化并建立与服务器的WebSocket连接。

异步监听服务器消息：启动一个单独的线程（具体实现方式因编程语言而异）来监听服务器返回的消息，根据消息内容进行相应的操作。

发送消息：在不同于监听服务器消息的线程中（例如主线程，具体实现方式因编程语言而异），向服务器发送消息。

关闭连接：在程序结束前，确保关闭WebSocket连接以释放资源。
*/



@Component
@ServerEndpoint("/realtime/audio/websocket/{sid}")
@Slf4j
public class realtimeAudio {
//服务端给客户端发消息
//存放会话对象
    public static  final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
   // private static final Logger logger = Logger.getLogger(WebSocketServer.class.getName());
  //  private static final String apikey="";
    static  String  message1="";
    static final Object message2=new Object();
    static  String response="";
    static final Object message3=new Object();
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid,@PathParam("remind") String remind) {
        JSONObject jsonObject= JSON.parseObject( remind);
        RunPythonWithConda runPythonWithConda = new RunPythonWithConda();
        runPythonWithConda.face();
        log.info("开始人脸识别");
        if(jsonObject!=null){
            String string2 = jsonObject.getString("sampleRate");
            String string1 = jsonObject.getString("channel");
            String string = jsonObject.getString("bitsPerSample");
            log.info("样本率{}",string2);
            log.info("通道数{}",string1);
            log.info("采样位数{}",string);
        }
        sessionMap.put(sid, session);
        log.info("[连接建立] 客户端: {} | 当前在线: {}", sid, sessionMap.size());
        sendToSpecificClient(sid);
    }
    public void sendToSpecificClient(String  sid){
        Session session = sessionMap.get(sid);
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText("客户端连接成功，下面准备处理你传递的数据");
            }
            else {
                log.error("客户端[{}]会话已关闭", sid);
                sessionMap.remove(sid); // 清理无效会话
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void sendToSpecificClient(String  sid,String message){
        Session session = sessionMap.get(sid);
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
            else {
                log.error("客户端[{}]会话已关闭", sid);
                sessionMap.remove(sid); // 清理无效会话
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendToSpecificClient1(String  sid){
        Session session = sessionMap.get(sid);
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText("您的数据已经传递给ai，敬请等待");
            }
            else {
                log.error("客户端[{}]会话已关闭", sid);
                sessionMap.remove(sid); // 清理无效会话
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final APIWebsocket apiWebsocket = new APIWebsocket(APIWebsocket.chatClient);
    @OnMessage
    public void onMessage(ByteBuffer  message, @PathParam("sid") String sid) throws InterruptedException {

            try {
                APIWebsocket.flag=false;
                log.info("[接收消息] 获取音频数据: {}", message);
                log.info("数据的大小为{}", message.capacity());
                log.info("[接受消息的人{}", sid);
                byte[] array = message.array();
                log.info("数组的的内容为{}", Arrays.toString(array));
                if(isContainsStopFlag(array)){
                    log.info("停止位");
                    APIWebsocket.flag=true;
                    Map<String, Object> connect = apiWebsocket.connect();
                    apiWebsocket.sendMessage(message,(Session)connect.get("session"));
                    synchronized (message3){
                        if(response.isEmpty()) {
                            message3.wait();
                        }
                    }
                    sendToSpecificClient(sid,response);
                }
                // 发送数据给API
                Map<String, Object> connect = apiWebsocket.connect();
                Integer success = (Integer)  connect.get("success");
                if (success == 1) {
                    log.info("[连接成功]");
                    apiWebsocket.sendMessage(message,(Session)connect.get("session"));
                    sendToSpecificClient1(sid);
                    try {
                        // 等待AI返回结果（假设通过message1获取）
                        log.info("通知成功");
                        //这里阻塞了音频数据的连续传输，开一个线程专门用来监听结果？？？？？？
                        APIWebsocket.executor.submit(() -> {
                            if(message1.isEmpty()){
                                synchronized (message2){
                                    log.info("等待APIWebsocket返回中");
                                    try {
                                        message2.wait(2000);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }// 等待APIWebsocket通知
                            }
                        });
                        log.info("返回的不为空的值为{}",message1);
                        sendToSpecificClient(sid, "AI识别结果：" + message1);
                        log.info("返回的值为{}",message);
                    } catch (Exception e) {
                        log.error("结果回传线程异常{}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("结果回传线程异常{}", e.getMessage());
            }
    }

    private boolean isContainsStopFlag(byte[] array) {
        // 处理数组为空的情况
        if (array == null) {
            return false;
        }
        // 停止标志为字节值-1，对应无符号值255
        final byte STOP_FLAG = (byte)0xFF;
        for (int i = 0; i < 16; i++) {
            if(array[i]!=STOP_FLAG){
                return false;
            }
        }
        return true;
    }

    @OnClose
    public void onClose(Session session, @PathParam("sid") String sid) {
        Session remove = sessionMap.remove(sid);
        if (remove != null) {
            log.info("[连接关闭] 客户端: {} | 当前在线: {}", sid, sessionMap.size());
        }
        log.info("[连接关闭] 剩余在线: {}", sessionMap.size());
    }
    @OnError
    public void onError(Throwable error) {
        log.error("[连接错误] 错误信息: {}", error.getMessage());
        log.error("发生错误,客户端与中转站之间发生错误");
    }
}











