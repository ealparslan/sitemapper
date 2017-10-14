package com.sikayetvar.sitemap.generator;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class SlackSender {

    HttpURLConnection con;
    BufferedWriter bw;
    URL obj;

    public SlackSender(String webhookUrl){

        try {
            obj = new URL(webhookUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(String postData){
        String postJSONData = "{\"text\": \"" + postData + "\n========================\n\"}";

        try {
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type","application/json");
            con.setDoOutput(true);
            bw = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
            bw.write(postJSONData);
            bw.flush();
            bw.close();
            con.getInputStream();
            con.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
