
package com.rizvi.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

// Custom thread-safe file handler for managing orderTracking.json with Path responses
class FileHandler {
    private final Path filePath;
    private final ReentrantLock lock;
    private final List<Path> tempFiles;

    public FileHandler(Path filePath) {
        this.filePath = filePath;
        this.lock = new ReentrantLock();
        this.tempFiles = new CopyOnWriteArrayList<>();
        initializeFile();
    }

    // Initialize the file with an empty JSON array if it doesn't exist
    private void initializeFile() {
        lock.lock();
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                Files.writeString(filePath, "[]");
                System.out.println("Created " + filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error initializing file: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize file: " + filePath, e);
        } finally {
            lock.unlock();
        }
    }

    // Add a response Path and aggregate into JSON
    public void addResponse(Path responsePath) {
        tempFiles.add(responsePath);
        writeToFile();
    }

    // Thread-safe aggregation of responses into a JSON array
    private void writeToFile() {
        lock.lock();
        try {
            List<String> responses = new ArrayList<>();
            for (Path tempFile : tempFiles) {
                if (Files.exists(tempFile)) {
                    String content = Files.readString(tempFile).trim();
                    System.out.println("Read temp file " + tempFile + ": " + content);
                    if (!content.isEmpty()) {
                        responses.add(content);
                    }
                    // Comment out deletion for debugging
                    /*
                    try {
                        Files.deleteIfExists(tempFile);
                        System.out.println("Deleted temp file " + tempFile);
                    } catch (IOException e) {
                        System.err.println("Error deleting temp file " + tempFile + ": " + e.getMessage());
                    }
                    */
                } else {
                    System.err.println("Temp file does not exist: " + tempFile);
                }
            }
            if (!responses.isEmpty()) {
                String jsonContent = "[" + String.join(",", responses) + "]";
                Files.writeString(filePath, jsonContent, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Wrote " + responses.size() + " responses to " + filePath.toAbsolutePath());
            } else {
                System.out.println("No valid responses to write to " + filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}

public class ConcurrentRequests1 {
    private static final Path ORDER_TRACKING = Path.of("orderTracking.json");

    public static void main(String[] args) {
        // Initialize order data
        Map<String, Integer> orderMap = Map.of(
                "apples", 500,
                "oranges", 1000,
                "bananas", 750,
                "carrots", 2000,
                "cantaloupes", 100
        );
        String urlParams = "product=%s&amount=%d";
        // Use httpbin.org for testing; replace with "http://localhost:8080" for local server
        String urlBase = "http://localhost:8080";

        // Create list of URIs for GET requests
        List<URI> sites = new ArrayList<>();
        orderMap.forEach((k, v) -> sites.add(URI.create(
                urlBase + "?" + urlParams.formatted(k, v))));

        // Initialize HttpClient
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMinutes(1))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // Initialize custom file handler
        FileHandler fileHandler = new FileHandler(ORDER_TRACKING);

        // Uncomment to enable GET requests for debugging
        // sendGets(client, sites);

        // Send POST requests with file handling
        sendPostsSafeFileWrite(client, urlBase, urlParams, orderMap, fileHandler);
    }

    private static void sendGets(HttpClient client, List<URI> uris) {
        List<CompletableFuture<Void>> futures = uris.stream()
                .map(uri -> HttpRequest.newBuilder(uri).build())
                .map(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            System.out.println("GET response from " + request.uri() + ": " + response.body());
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Error in GET request to " + request.uri() + ": " + throwable.getMessage());
                            throwable.printStackTrace();
                            return null;
                        }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }

    private static void sendPostsSafeFileWrite(HttpClient client, String baseURI,
                                               String paramString, Map<String, Integer> orders,
                                               FileHandler fileHandler) {
        List<CompletableFuture<Void>> futures = orders.entrySet().stream()
                .map(e -> {
                    try {
                        // Create a unique temporary file for each request
                        Path tempFile = Files.createTempFile("order_", ".json");
                        System.out.println("Created temp file: " + tempFile);
                        String params = paramString.formatted(e.getKey(), e.getValue());
                        HttpRequest request = HttpRequest.newBuilder(URI.create(baseURI))
                                .POST(HttpRequest.BodyPublishers.ofString(params))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .build();
                        return new Object() {
                            final Path tempFilePath = tempFile;
                            final HttpRequest httpRequest = request;
                        };
                    } catch (IOException ex) {
                        System.err.println("Error creating temp file for " + e.getKey() + ": " + ex.getMessage());
                        ex.printStackTrace();
                        throw new RuntimeException("Failed to create temp file", ex);
                    }
                })
                .map(obj -> client.sendAsync(obj.httpRequest, HttpResponse.BodyHandlers.ofFile(obj.tempFilePath, StandardOpenOption.WRITE))
                        .thenAcceptAsync(response -> {
                            System.out.println("POST response status for " + obj.httpRequest.uri() + ": " + response.statusCode());
                            if (response.statusCode() == 200) {
                                fileHandler.addResponse(obj.tempFilePath);
                            } else {
                                System.err.println("Error in POST request to " + obj.httpRequest.uri() +
                                        ": Status code " + response.statusCode());
                                // Clean up temporary file on error
                                try {
                                    Files.deleteIfExists(obj.tempFilePath);
                                    System.out.println("Deleted temp file on error: " + obj.tempFilePath);
                                } catch (IOException ex) {
                                    System.err.println("Error deleting temp file " + obj.tempFilePath + ": " + ex.getMessage());
                                }
                            }
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Error in POST request to " + obj.httpRequest.uri() + ": " + throwable.getMessage());
                            throwable.printStackTrace();
                            // Clean up temporary file on exception
                            try {
                                Files.deleteIfExists(obj.tempFilePath);
                                System.out.println("Deleted temp file on exception: " + obj.tempFilePath);
                            } catch (IOException ex) {
                                System.err.println("Error deleting temp file " + obj.tempFilePath + ": " + ex.getMessage());
                            }
                            return null;
                        }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }
}