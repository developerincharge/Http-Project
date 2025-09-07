package com.rizvi.http;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import static java.net.HttpURLConnection.HTTP_OK;

public class SimpleHttpServer {
    private static long visitorCounter = 0;
    public static void main(String[] args) {

        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/", exchange -> {
                String requestMethod = exchange.getRequestMethod();
                System.out.println("Request Method: " + requestMethod);

                String data = new String(exchange.getRequestBody().readAllBytes());
                System.out.println("Body Data: " + data);

                if (requestMethod.equals("POST")) {
                    visitorCounter++;
                }

                String response = """
                <html>
                    <body>
                        <h1>Hello World from My Http Server</h1>
                        <p>Number of Visitors who signed up = %d<p>
                        <form method="post">
                        <input type="submit" value="Submit">
                        </form>
                    </body>
                </html>""".formatted(visitorCounter);

                var bytes = response.getBytes();
                exchange.sendResponseHeaders(HTTP_OK, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            });
            server.start();
            System.out.println("Server started on port 8080");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
