package com.tmd.ai.utils;

import okhttp3.HttpUrl;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class AuthUtils {
    // 鉴权方法
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" + "date: " + date + "\n" + "GET " + url.getPath() + " HTTP/1.1";
        // System.err.println(preStr);
        // SHA256加密
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);

        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        // Base64加密
        String sha = Base64.getEncoder().encodeToString(hexDigits);
        // System.err.println(sha);
        // 拼接
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        // 拼接地址
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();

        // System.err.println(httpUrl.toString());
        return httpUrl.toString();
    }
    public static String getServerTime(){
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US);
        dateFormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "GMT"));
        return dateFormat.format(new Date());
    }

    public static String generateUrl(String ws, String authorization, String date, String host) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("authorization", authorization);
        params.put("date", date);
        params.put("host", host);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(entry.getKey()).append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return ws + "?" + sb;
    }

}
