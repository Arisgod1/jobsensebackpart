package com.tmd.ai.controller;

import com.alibaba.fastjson.JSONObject;
import com.tmd.ai.entity.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

@RestController
@Api("音频接口")
@RequestMapping("/wordAudio")
@Slf4j
@CrossOrigin
public class WordAudioController {
    //音频转文字，如果用户不想输入，可以先说话，然后传到我这里转化成文字，显示在聊天框上面。。。。
    @PostMapping("/handleAudioFile")
    @ApiOperation("处理音频文件")
    private Result handleAudioFile(MultipartFile audioFile) throws IOException {
        log.info("接收到音频文件{}", audioFile);
        // 处理音频文件逻辑
        CloseableHttpClient client= HttpClients.createDefault();
        String access_token = null;
        String expire_in = null;
        try {
            String requestUrl="https://aip.baidubce.com/oauth/2.0/token";
            String grant_type="client_credentials";
            String client_id="5qWDgiQz8xJJbvoIFRbTOAQs";
            String client_secret="Gwo9trMLb4k3C0GNy86KxKrH7IWRkMCY";
            URIBuilder uriBuilder = new URIBuilder(requestUrl);
            uriBuilder.addParameter("grant_type",grant_type);
            uriBuilder.addParameter("client_id",client_id);
            uriBuilder.addParameter("client_secret",client_secret);
            URI uri = uriBuilder.build();
            HttpPost post = new HttpPost(uri);
            post.setHeader("Content-Type","application/json");
            post.setHeader("Accept","application/json");
            CloseableHttpResponse execute = client.execute(post);

            if (execute.getStatusLine().getStatusCode()==200){
                log.info("获取百度access_token成功");
                HttpEntity entity = execute.getEntity();
                String string = EntityUtils.toString(entity);
                JSONObject jsonObject = JSONObject.parseObject(string);
                access_token = jsonObject.getString("access_token");
                expire_in = jsonObject.getString("expire_in");
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        // 鉴权完毕，下面进行语音转文字操作
        HttpPost  post = new HttpPost("https://vop.baidu.com/server_api");
        post.setHeader("Content-Type","application/json");
        String speech;
        try {
            speech = Base64.getEncoder().encodeToString(audioFile.getBytes());
            log.info("音频文件转码成功");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int length = audioFile.getBytes().length;
        String json = "{\"format\":\"pcm\",\"rate\":16000,\"channel\":1,\"cuid\":\""+1+"\",\"token\":\""+access_token+"\",\"len\":"+length+",\"speech\":\""+speech+"\",\"dev_pid\":\"1537\",\"lm_id\":\"dev_pid=1537\"}";
        post.setEntity(new StringEntity(json));
        CloseableHttpResponse response = client.execute(post);
        if (response.getStatusLine().getStatusCode()==200){
            log.info("处理结果成功");
            HttpEntity entity = response.getEntity();
            String string = EntityUtils.toString(entity);
            JSONObject jsonObject = JSONObject.parseObject(string);
            if(jsonObject.getString("err_no").equals("2000")){
                throw new RuntimeException(jsonObject.getString("err_msg"));
            }else {
                log.info("处理结果为{}",jsonObject.getString("result"));
                String string1 = jsonObject.getString("result");
                //去掉两个中括号,["穿一穿。"],比如去掉"["和"]
                string1 = string1.substring(1, string1.length()-1);
                log.info("处理结果为{}",string1);
                return Result.success(string1);
            }
        }else{
            return Result.success(response.getStatusLine().toString());
        }
    }


}
