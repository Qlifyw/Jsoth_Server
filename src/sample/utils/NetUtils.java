package sample.utils;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetUtils {

    private final static Logger log = LogManager.getRootLogger();
    private static String FAILURE_CONNECTION = "<No info>";

    // Request county code (Upper case). Example: US, UK, GE etc.
    public static String getCountryCode(String IP) throws Exception {
        String URL_API = "http://ip-api.com/json/"+ IP +"?fields=countryCode";
        URL api = new URL(URL_API);
        HttpURLConnection connection = (HttpURLConnection) api.openConnection();
        connection.setRequestMethod("GET");
        try {
            connection.getResponseCode();
        } catch (IOException e) {
            log.error("NetRequest: get country code: | " + e.toString());
            return FAILURE_CONNECTION;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inline = br.readLine();
        StringBuilder stringBuilder = new StringBuilder();
        while (inline != null) {
            stringBuilder.append(inline);
            inline = br.readLine();
        }
        br.close();

        JSONObject response = new JSONObject(stringBuilder.toString());
        return response.getString("countryCode");
    }

    public static String getExternalIP() {
        String ip = "";
        BufferedReader br = null;
        try {
            URL externalIP = new URL("http://checkip.amazonaws.com");
            HttpURLConnection connection = (HttpURLConnection) externalIP.openConnection();
            try {
                connection.getResponseCode();
            } catch (IOException e) {
                log.error("NetRequest.getExternlIP: get response code IP: | " + e.toString());
                return FAILURE_CONNECTION;
            }
            br = new BufferedReader(new InputStreamReader(externalIP.openStream()));
            ip = br.readLine();
        } catch (IOException e1) {
            log.error("NetRequest.getExternalIP: I/O stream: | " + e1.toString());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error("NetRequest.getExternalIP: close connection: | " + e.toString());
                }
            }
        }
        return ip;
    }



}
