package com.example.coolweather.util;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    /**
     * 传入请求地址，并注册一个回调来处理服务器的响应
     * @param address
     * @param callback
     */
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
