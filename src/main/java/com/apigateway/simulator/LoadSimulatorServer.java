package com.apigateway.simulator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadSimulatorServer {
    private static final String DEFAULT_GATEWAY = "http://localhost:8080";
    private static final String DEFAULT_BACKEND1 = "http://localhost:9001";
    private static final String DEFAULT_BACKEND2 = "http://localhost:9002";

    public static void main(String[] args) throws IOException {
        int port = 9100;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/simulate", LoadSimulatorServer::handle);
        server.setExecutor(null);
        server.start();
        System.out.println("Load simulator running on port " + port);
        keepAlive();
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String response;
        if (path.endsWith("/auth/happy")) {
            response = simulateAuth(true);
        } else if (path.endsWith("/auth/worst")) {
            response = simulateAuth(false);
        } else if (path.endsWith("/rate/happy")) {
            response = simulateRate(false);
        } else if (path.endsWith("/rate/worst")) {
            response = simulateRate(true);
        } else if (path.endsWith("/circuit/happy")) {
            response = simulateCircuit(false);
        } else if (path.endsWith("/circuit/worst")) {
            response = simulateCircuit(true);
        } else if (path.endsWith("/health/happy")) {
            response = toggleHealth(true);
        } else if (path.endsWith("/health/worst")) {
            response = toggleHealth(false);
        } else if (path.endsWith("/load/happy")) {
            response = simulateLoad(false);
        } else if (path.endsWith("/load/worst")) {
            response = simulateLoad(true);
        } else {
            response = "{\"error\":\"Unknown simulation\"}";
        }

        byte[] bytes = response.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String simulateAuth(boolean happy) {
        HttpClient client = HttpClient.newHttpClient();
        try {
            if (happy) {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(DEFAULT_GATEWAY + "/happy"))
                        .header("X-API-Key", "demo-key-1")
                        .GET();
                HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                return "{\"scenario\":\"auth\",\"happy\":true,\"requests\":1,\"status\":" + response.statusCode() + ",\"body\":\"" + escape(response.body()) + "\"}";
            }

            HttpRequest overloadOn = HttpRequest.newBuilder()
                    .uri(URI.create(DEFAULT_GATEWAY + "/admin/overload?state=on"))
                    .GET()
                    .build();
            client.send(overloadOn, HttpResponse.BodyHandlers.ofString());

            int total = 25;
            Map<Integer, Integer> statusCounts = new HashMap<>();
            for (int i = 0; i < total; i++) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(DEFAULT_GATEWAY + "/happy"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
                statusCounts.merge(response.statusCode(), 1, Integer::sum);
            }
            return "{\"scenario\":\"auth\",\"happy\":false,\"requests\":" + total + ",\"statusCounts\":" + mapToJson(statusCounts) + ",\"note\":\"gateway overloaded until reset\"}";
        } catch (Exception e) {
            return error("auth", e);
        }
    }

    private static String simulateRate(boolean worst) {
        HttpClient client = HttpClient.newHttpClient();
        int total = worst ? 20 : 3;
        Map<Integer, Integer> statusCounts = new HashMap<>();
        try {
            for (int i = 0; i < total; i++) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(DEFAULT_GATEWAY + "/happy"))
                        .header("X-API-Key", "demo-key-1")
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                statusCounts.merge(response.statusCode(), 1, Integer::sum);
                if (!worst) {
                    Thread.sleep(300);
                }
            }
            return "{\"scenario\":\"rate\",\"worst\":" + worst + ",\"statusCounts\":" + mapToJson(statusCounts) + "}";
        } catch (Exception e) {
            return error("rate", e);
        }
    }

    private static String simulateCircuit(boolean worst) {
        HttpClient client = HttpClient.newHttpClient();
        try {
            if (worst) {
                HttpRequest down = HttpRequest.newBuilder()
                        .uri(URI.create(DEFAULT_BACKEND2 + "/toggle/health?state=down"))
                        .GET()
                        .build();
                client.send(down, HttpResponse.BodyHandlers.ofString());
                Thread.sleep(300);
                for (int i = 0; i < 5; i++) {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(DEFAULT_GATEWAY + "/fail"))
                            .header("X-API-Key", "demo-key-1")
                            .GET()
                            .build();
                    client.send(req, HttpResponse.BodyHandlers.ofString());
                }
            } else {
                HttpRequest up = HttpRequest.newBuilder()
                        .uri(URI.create(DEFAULT_BACKEND2 + "/toggle/health?state=up"))
                        .GET()
                        .build();
                client.send(up, HttpResponse.BodyHandlers.ofString());
            }
            HttpRequest okReq = HttpRequest.newBuilder()
                    .uri(URI.create(DEFAULT_GATEWAY + "/happy"))
                    .header("X-API-Key", "demo-key-1")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(okReq, HttpResponse.BodyHandlers.ofString());
            return "{\"scenario\":\"circuit\",\"worst\":" + worst + ",\"status\":" + response.statusCode() + ",\"body\":\"" + escape(response.body()) + "\"}";
        } catch (Exception e) {
            return error("circuit", e);
        }
    }

    private static String toggleHealth(boolean happy) {
        try {
            String state = happy ? "up" : "down";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEFAULT_BACKEND1 + "/toggle/health?state=" + state))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return "{\"scenario\":\"health\",\"happy\":" + happy + ",\"backend1\":\"" + escape(response.body()) + "\"}";
        } catch (Exception e) {
            return error("health", e);
        }
    }

    private static String simulateLoad(boolean worst) {
        HttpClient client = HttpClient.newHttpClient();
        Map<String, Integer> backendCounts = new HashMap<>();
        List<Integer> statuses = new ArrayList<>();
        try {
            if (worst) {
                HttpRequest down = HttpRequest.newBuilder()
                        .uri(URI.create(DEFAULT_BACKEND2 + "/toggle/health?state=down"))
                        .GET()
                        .build();
                client.send(down, HttpResponse.BodyHandlers.ofString());
                Thread.sleep(500);
            }
            for (int i = 0; i < 10; i++) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(DEFAULT_GATEWAY + "/happy"))
                        .header("X-API-Key", "demo-key-1")
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
                statuses.add(response.statusCode());
                String backendId = response.headers().firstValue("X-Backend-Id").orElse("unknown");
                backendCounts.merge(backendId, 1, Integer::sum);
            }
            return "{\"scenario\":\"load\",\"worst\":" + worst + ",\"backendCounts\":" + mapToJson(backendCounts) + ",\"statusCodes\":" + listToJson(statuses) + "}";
        } catch (Exception e) {
            return error("load", e);
        }
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String listToJson(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String error(String scenario, Exception e) {
        return "{\"scenario\":\"" + scenario + "\",\"error\":\"" + escape(e.toString()) + "\"}";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    private static void keepAlive() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {
        }
    }
}
