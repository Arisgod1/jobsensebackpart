package com.tmd.ai.WebSocketServerAndTongYi;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmd.ai.config.CustomConfiguration;
import com.tmd.ai.controller.InterviewController;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

 

@Component
@ClientEndpoint(configurator = CustomConfiguration.class)
@Slf4j
public class APIWebsocket {

    public static ChatClient chatClient;
    private final ChatClient instanceChatClient;
    @Autowired
    public APIWebsocket(@Qualifier("chatClient") ChatClient chatClient) {
        APIWebsocket.chatClient = chatClient;
        this.instanceChatClient = chatClient;
    }

   private Session session;
   private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            Math.max(4, Runtime.getRuntime().availableProcessors()) * 2,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
   private String task_id="";
   private int count=0;
   private final Object resultLock = new Object();
   private final Object finishLock = new Object();
   private String partialResult = "";
   private String finalResponse = "";

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) throws DeploymentException, IOException {
          log.info("session对象为{},EndpointConfig对象为{}", session, config);
       log.info("连接成功");

    }
    @OnMessage
    public void onMessage(String message) throws IOException, InterruptedException {
        log.info("[APIWEBSOCKET接收消息] 获取服务端的数据: {}", message);
        EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {

            /*if(message.isEmpty()){
                APIWebsocket.class.wait();
            }*/
                log.debug("我进入了实时语音的message锁中");
                try {

                    JSONObject jsonObject = JSONObject.parseObject(message);
                    JSONObject head = jsonObject.getJSONObject("header");
                    log.info("event:{}", head.getString("event"));

                    if (head.getString("event").equals("task-started")) {
                        String string1 = head.getString("task_id");
                        task_id = string1;
                        log.info("[开始识别]，服务器知道任务已经开启");
                        // 开始识别，调用sendmessage发送消息，激活下面的锁
                        log.info("激活锁，准备发送数据，task_id为{}", string1);
                    } else if (head.getString("event").equals("result-generated")) {
                            JSONObject payload = jsonObject.getJSONObject("payload");
                            JSONObject output = payload.getJSONObject("output");
                            JSONObject sentence = output.getJSONObject("sentence");
                            synchronized (resultLock){
                                if("true".equals(sentence.getString("sentence_end"))){
                                    partialResult += sentence.getString("text");
                                }
                                resultLock.notifyAll();
                            }
                    } else if (head.getString("event").equals("task-finished")) {
                        log.info("[任务完成]");
                        synchronized (finishLock){
                            String pdfContent=InterviewController.pdfContent;
                            String prompt="这是这个人的简历内容:  "+pdfContent+"这是这个人的自我介绍或者回答的问题，count为1证明是自我介绍，大于一的你就当作是回答问题，下面是count的值:"+count+partialResult+"    请根据上述提供的信酝酿一个能测试出面试者真正水平的问题，不要太长，五十字以内足以";
                            String response = instanceChatClient.prompt()
                                    .user(prompt)
                                    .call()
                                    .content();
                            finalResponse=response;
                            log.info("[结果消息]{}", response);
                            count++;
                            finishLock.notifyAll();
                        }
                        if(count==5){
                            session.close();
                            log.info("任务结束，session已经关闭");
                        }
                        //这里发完之后需要复用连接
                            string=UUID.randomUUID().toString();
                            task_id="";
                            String runTaskMessage1 = "{\"header\":{\"action\":\"run-task\",\"task_id\":\""+string+"\",\"streaming\":\"duplex\"},\"payload\":{\"task_group\":\"audio\"" +
                                ",\"task\":\"asr\",\"function\":\"recognition\",\"model\":\"paraformer-realtime-v2\",\"parameters\":{\"format\":\"pcm\",\"sample_rate\":16000,\"disfluency_removal_enabled\":false,\"language_hints\":[\"zh\"]}" +
                                ",\"input\":{}}}";
                            session.getBasicRemote().sendText(runTaskMessage1);
                            log.info("[发送消息] 发送runTaskMessage数据: {}", runTaskMessage1);
                        //断开realtime和API的链接
                        //把message1和简历内容一起传递给ai，返回一个问题，然后问题发送给前端

                    } else {
                        log.info("[错误消息]{}", head.getString("error_message"));
                    }
                    //实时语音识别.message1 = message;
                    //实时语音识别.message.notify(); // 通知等待的线程
                } catch (Exception e) {
                    log.info("[错误消息]{}", e.getMessage());
                }

            }

        } );
                log.info("[接收消息] 获取数据: {}",message);
    }
    @OnClose
    public void onClose() {
        System.out.println("连接关闭");
    }
    @OnError
    public void onError(Throwable error) {
        System.out.println("发生错误");
    }
    private String string = UUID.randomUUID().toString();
   /*static String runTaskMessage = "{\"header\":{" +
            "\"streaming\":\"duplex\"," +
            "\"task_id\":\""+string+"\"," +
            "\"action\":\"run-task\"}," +
            "\"payload\":{" +
            "\"model\":\"gummy-realtime-v1\"," +
            "\"parameters\":{" +
            "\"sample_rate\":16000," +
            "\"format\":\"pcm\"," +
            "\"source_language\":\"zh\"," +
            "\"transcription_enabled\":true," +
            "\"translation_enabled\":false," +
            "\"translation_target_languages\":[]}," +
            "\"input\":{}," +
            "\"task\":\"asr\"," +
            "\"task_group\":\"audio\"," +
            "\"function\":\"recognition\"}}";*/
   String runTaskMessage = "{\"header\":{\"action\":\"run-task\",\"task_id\":\""+string+"\",\"streaming\":\"duplex\"},\"payload\":{\"task_group\":\"audio\"" +
           ",\"task\":\"asr\",\"function\":\"recognition\",\"model\":\"paraformer-realtime-v2\",\"parameters\":{\"format\":\"pcm\",\"sample_rate\":16000,\"disfluency_removal_enabled\":false,\"language_hints\":[\"zh\"]}" +
           ",\"input\":{}}}";
     private static final ObjectMapper objectMapper = new ObjectMapper();
     




    private static byte[] convertShortToByteArray(short[] samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            // 小端序：低字节在前，高字节在后
            bytes[i * 2] = (byte)(samples[i] & 0xFF);
            bytes[i * 2 + 1] = (byte)(samples[i] >> 8);
        }
        return bytes;
    }

    private static short[] generate16BitSamples(int sampleCount, double frequency, int sampleRate) {
        short[] samples = new short[sampleCount];
        double amplitude = 10000; // 振幅（避免溢出）

        for (int i = 0; i < sampleCount; i++) {
            double t = (double) i / sampleRate;
            double sineValue = Math.sin(2 * Math.PI * frequency * t);
            samples[i] = (short)(sineValue * amplitude);
        }
        return samples;
    }

    public void sendMessage(ByteBuffer message, Session session) {
        try {
           // session.getBasicRemote().sendText(message);
                    try {
                        //先发送run-task指令，等到上面返回task-started消息，再发送音频流，发送完之后。发送finish-task指令
                        //所以需要一个锁来锁住，保证先发送run-task，再发送音频流，最后发送finish-tas
                            if(!runTaskMessage.isEmpty()){
                               this.session.getBasicRemote().sendText(runTaskMessage);
                                log.info("[发送消息] 发送runTaskMessage数据: {}", runTaskMessage);
                                runTaskMessage="";
                            }
                        //被唤醒之后
                        //将数据类型转换成pcm，而且源数据格式就是pcm
                        EXECUTOR.submit(new Runnable() {
                            @Override
                            public void run() {
                                byte[] array = message.array();
                                if (array[0] != -1&&array[1]!=-1){

                                    log.info("开始发送数据,又大又圆的书,task_id为{}", task_id);
                                    if (!task_id.isEmpty()){

                                        log.info("开始发送数据");
                                        try {
                                            // 检查是否有数据可读
                                            if (message.hasRemaining()) { // 等价于 remaining() > 0
                                                //判断这个音频数据是否不是静音音频
                                                //将message数据转成可以打印出来的十六进制
                                            /*String messageInspect=UUID.randomUUID().toString();
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            DataOutputStream dos = new DataOutputStream(baos);
                                            dos.write(message.array());
                                            dos.write(messageInspect.getBytes());
                                            dos.write(message.capacity());
                                            APIWebsocket.session.getBasicRemote().sendBinary(ByteBuffer.wrap(baos.toByteArray()));
                                            log.info("我已经发送检测网络层的信息{}", messageInspect);*/
                                                // 新增：验证WebSocket连接状态
                                                if (session == null || !session.isOpen()) {
                                                    log.error("WebSocket连接未打开，无法发送数据");
                                                    throw new RuntimeException("WebSocket连接异常");
                                                }
                                                // 新增：获取连接状态信息
                                                String connectionInfo = String.format(
                                                        "WebSocket状态: %s,最大消息大小: %d",
                                                        session.isOpen() ? "已连接" : "已关闭",
                                                        session.getMaxBinaryMessageBufferSize()
                                                );
                                                log.info(connectionInfo);
                                                String s = HexFormat.of().formatHex(message.array(),0,32);
                                                log.info("十六进制数据为：{}", s);
                                                log.info("ByteBuffer中有 {} 字节数据待发送", message.remaining());
                                                //每次发送100ms的数据
                                                ByteBuffer byteBuffer = ByteBuffer.allocate( 31*1024);
                                                byteBuffer.put(message);
                                                byteBuffer.flip();
                                                //把message切成小块一点一点发送过去

                                                session.getBasicRemote().sendBinary(byteBuffer);
                                                try{
                                                    log.info("分块发送了哦");
                                                    // APIWebsocket.session.getBasicRemote().sendBinary(message);
                                                    // 发送测试消息请求确认
                                                    // 包含header和payload的完整消息结构
                                               /* String pingMessage = "{\"header\":{\"action\":\"ping\",\"timestamp\":" +
                                                        System.currentTimeMillis() + "},\"payload\":{\"input\":{}}}";
                                                APIWebsocket.session.getAsyncRemote().sendText(pingMessage, pingResult -> {
                                                    if (pingResult.isOK()) {
                                                        log.info("Ping消息已发送，等待服务端响应");
                                                    } else {
                                                        log.error("Ping消息发送失败: {}", pingResult.getException().getMessage());
                                                    }
                                                });*/
                                                }catch (Exception e){
                                                    log.error("发送数据异常{}", e.getMessage());
                                                    // 获取详细的网络错误信息
                                                    if (session.getUserProperties().containsKey("javax.websocket.endpoint.remoteAddress")) {
                                                        Socket socket = (Socket) session.getUserProperties().get("javax.websocket.endpoint.remoteAddress");
                                                        if (socket != null) {
                                                            log.error("网络连接状态: {}", socket.isConnected() ? "已连接" : "已断开");
                                                            log.error("网络错误详情: {}", socket.getLocalSocketAddress());
                                                        }
                                                    }
                                                    throw new RuntimeException(e);
                                                }
                                                log.info("成功发送 {} 字节数据", message.capacity());
                                                //等待100ms，继续发送
                                                try {
                                                    Thread.sleep(200);
                                                }
                                                catch (InterruptedException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            /*// 复制数据到byte数组
                                            byte[] byteArray = new byte[message.remaining()];
                                            message.get(byteArray);
                                            log.info("到这里之后，session的值为{}", APIWebsocket.session);

                                            // 发送数据
                                            APIWebsocket.session.getBasicRemote().sendBinary(ByteBuffer.wrap(byteArray));*/
                                                //log.info("成功发送 {} 字节数据", byteArray.length);
                                            } else {
                                                log.warn("ByteBuffer中没有可用数据，position={}", message.position());
                                            }
                                        } catch (Exception e) {
                                            log.error("发送数据失败", e);
                                            throw new RuntimeException(e);
                                        }
                                        log.info("[发送消息] Message获取数据: {}",  message);



                                }

                            }
                            }
                        });
                        

                        /*String finishTaskMessage = "{\"header\":{\"streaming\":\"duplex\",\"task_id\":" + task_id + ",\"action\":\"finish-task\"},\"payload\":{\"input\":\"{}\"}}";
                        APIWebsocket.session.getBasicRemote().sendText(finishTaskMessage); // 直接发送，无需二次序列化      APIWebsocket.session.getBasicRemote().sendText(finishTaskMessage); // 直接发送，无需二次序列化
                        log.info("[发送消息] 发送finish-task数据: {}", "{\"header\":{" +
                                "\"streaming\":\"duplex\"," +
                                "\"task_id\":"+string+"\"\"," +
                                "\"action\":\"finish-task\"}," +
                                "\"payload\":{" +
                                "\"input\":\"{}\",}");
*/
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                }
        } catch (Exception e){
            e.printStackTrace();
            log.error("发送消息失败{}",e.getMessage());
        }
    }

    public Map<String, Object> connect(){
        try {
            if((session== null||!session.isOpen()) && count==0) {
                URI uri = new URI("wss://dashscope.aliyuncs.com/api-ws/v1/inference");
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                log.info("container容器为{}", container);
                Session session = container.connectToServer(this, uri);
                log.info("session为{}", session);
                this.session = session;
                count++;
            }
            Map<String, Object> config = new HashMap<>();
            config.put("success",1);
            config.put("session", session);
            return config;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendFinish(){
        String json = "{\n" +
                "    \"header\": {\n" +
                "        \"action\": \"finish-task\",\n" +
                "        \"task_id\": \"" + task_id + "\",\n" +
                "        \"streaming\": \"duplex\"\n" +
                "    },\n" +
                "    \"payload\": {\n" +
                "        \"input\": {}\n" +
                "    }\n" +
                "}";
        try {
            this.session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String awaitPartialResult(long timeoutMillis){
        long start = System.currentTimeMillis();
        synchronized (resultLock){
            while (partialResult.isEmpty() && System.currentTimeMillis() - start < timeoutMillis){
                try { resultLock.wait(timeoutMillis); } catch (InterruptedException ignored) {}
            }
            String r = partialResult;
            partialResult = "";
            return r;
        }
    }

    public String awaitFinalResponse(long timeoutMillis){
        long start = System.currentTimeMillis();
        synchronized (finishLock){
            while (finalResponse.isEmpty() && System.currentTimeMillis() - start < timeoutMillis){
                try { finishLock.wait(timeoutMillis); } catch (InterruptedException ignored) {}
            }
            String r = finalResponse;
            finalResponse = "";
            return r;
        }
    }
}
