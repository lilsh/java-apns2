package me.ifelseif.apns2.demo;

import me.ifelseif.apns2.ResponseListener;
import me.ifelseif.apns2.impl.ApnsHttp2ClientImpl;
import me.ifelseif.apns2.model.Notification;

/**
 * Created by linmingchang on 16/12/20.
 */
public class Demo {
    public static void main(String[] args){
        ApnsHttp2ClientImpl client = new ApnsHttp2ClientImpl.Builder()
                .key("/production-195-0.p12")
                .password("apple")
                .connectTimeout(60)
                .pushTimeout(3000)
                .topic("com.weather.NOBWeather")
                .build()
                .start();

        Notification notification = new Notification.Builder()
                .alertBody("hello")
                .alertTitle("titletest")
                .badge(1)
                .build();

        client.push("afae802f3bb27e5606c74495453bb4534fc36c5606f663ad4b92afe392e5d7d2", notification, new ResponseListener() {
            @Override
            public void success(String deviceToken,Notification notification) {
                System.out.println(notification.getPayload());
            }

            @Override
            public void failure(String deviceToken,Notification notification,int status,String reason) {
                System.out.println("status:"+status+" reason:"+reason);
            }
        });

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.stop();
    }
}