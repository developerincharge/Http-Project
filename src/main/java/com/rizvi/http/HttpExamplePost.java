package com.rizvi.http;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;

public class HttpExamplePost {

    public static void main(String[] args) {
        try {
            //URL url = new URL("http://example.com ");
            URL url = new URL("http://localhost:8080");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Chrome");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept","text/html");
            connection.setReadTimeout(30000);

            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            String parameters ="first=Joe&last=Smith";
            int length = parameters.getBytes().length;
            connection.setRequestProperty("Content-Length", String.valueOf(length));

            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.writeBytes(parameters);
            output.flush();
            output.close();

            int responseCode = connection.getResponseCode();
            System.out.printf("Response Code: %d%n" , responseCode);
            if (responseCode != HTTP_OK ){
                System.out.println("Failed to read from the server : url =>> : "+url);
                System.out.printf("Error : %s%n" , connection.getResponseMessage());
                return;
            }
            printContents(connection.getInputStream());
            connection.disconnect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printContents(InputStream is){
        try(BufferedReader inputStream = new BufferedReader(new InputStreamReader(is))){
            String line;
            while((line = inputStream.readLine()) != null){
                System.out.println(line);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> parseParameters(String requestBody){

        Map<String, String> parameters = new HashMap<>();
        String [] pairs = requestBody.split("&");
        for(String pair : pairs){
            String[] keyValue = pair.split("=");
            if(keyValue.length == 2){
                parameters.put(keyValue[0], keyValue[1]);
            }
        }
        return parameters;
    }
}
