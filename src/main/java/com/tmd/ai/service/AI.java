package com.tmd.ai.service;

import com.alibaba.fastjson.JSONObject;
import com.tmd.ai.WebSocketServer.ChatWebSocketClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tmd.ai.constants.SystemConstants.CHAT_SYSTEM_PROMPT;
import static com.tmd.ai.utils.AuthUtils.getAuthUrl;

@Api("聊天Service")
@Slf4j
@Service
public class AI {
    String apiKey = "000eaa4ac949b2a3d2638b8a49e08c6e";
    String apiSecret = "YjM5ZGIwOTc2MDJmNWNhMTQwOWI2NDU2";
    String appid = "42a626ce";
    String hosturl = "https://spark-api.xf-yun.com/v1/x1";
    String authUrl = "";

    @ApiOperation("处理问题")
    public String chat(String question,String chatId,String systemPrompt) throws Exception {
        log.info("开始处理：{}", question);
        authUrl = getAuthUrl(hosturl,apiKey,apiSecret);//鉴权
        //鉴权url生成完毕,现在设置请求体

        JSONObject json = new JSONObject();

        JSONObject header = new JSONObject();
        JSONObject parameter = new JSONObject();
        JSONObject payload = new JSONObject();
        //设置header
        header.put("app_id", appid);
        header.put("uid", "123456");
        //设置payload
        JSONObject message = new JSONObject();
        List<Map<String, String>> text = new ArrayList<>();
        text.add(new HashMap<>() {
            {
                put("role", "system");
                put("content",systemPrompt);
            }
        });
        text.add(new HashMap<>() {
            {
                put("role", "user");
                put("content",question);
            }
        });
        message.put("text", text);
        payload.put("message", message);
        //设置parameter
        JSONObject chat = new JSONObject();
        chat.put("domain", "x1");
        chat.put("chat_id",chatId);
        parameter.put("chat",chat);
        //设置请求体
        json.put("header",header);
        json.put("payload",payload);
        json.put("parameter",parameter);
        String requestbody = json.toString();
        System.out.println(requestbody);
        //开始请求
        OkHttpClient client = new OkHttpClient.Builder().build();
        String url = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        //发送请求
        ChatWebSocketClient chatWebSocketClient = new ChatWebSocketClient(new URI(url));
        chatWebSocketClient.sendMessage(requestbody);//发送请求体
        if(!ChatWebSocketClient.flag){
            log.info("等待结果");
            synchronized (ChatWebSocketClient.lock){
                ChatWebSocketClient.lock.wait();
            }
            log.info("结果获取完毕");
            String result = chatWebSocketClient.getResult();
            System.out.println( result);
            ChatWebSocketClient.flag=false;
            return result;
        }
        return "请求失败";
    }

}