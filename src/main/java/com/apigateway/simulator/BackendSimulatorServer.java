package com.apigateway.simulator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class BackendSimulatorServer {
    private static volatile boolean healthy = true;

    public static void main(String[] args) throws IOException {
        Map<String, String> params = parseArgs(args);
        int port = Integer.parseInt(params.getOrDefault("port", "9001"));
        String name = params.getOrDefault("name", "backend");
        String mode = params.getOrDefault("healthy", "true");
        healthy = Boolean.parseBoolean(mode);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange -> handleHealth(exchange));
        server.createContext("/happy", exchange -> handleOk(exchange, "Hello from " + name));
        server.createContext("/fail", exchange -> handleStatus(exchange, 500, "Simulated failure from " + name));
        server.createContext("/slow", exchange -> handleSlow(exchange, "Slow response from " + name));
        server.createContext("/toggle/health", exchange -> handleToggle(exchange));
        server.createContext("/", new EchoHandler(name));
        server.setExecutor(null);
        server.start();

        System.out.println("Backend simulator running on port " + port + " name=" + name + " healthy=" + healthy);
        keepAlive();
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (healthy) {
            handleStatus(exchange, 200, "OK");
        } else {
            handleStatus(exchange, 500, "UNHEALTHY");
        }
    }

    private static void handleOk(HttpExchange exchange, String body) throws IOException {
        handleStatus(exchange, 200, body);
    }

    private static void handleSlow(HttpExchange exchange, String body) throws IOException {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        handleStatus(exchange, 200, body);
    }

    private static void handleToggle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String query = uri.getRawQuery();
        if (query != null && query.contains("state=down")) {
            healthy = false;
        } else if (query != null && query.contains("state=up")) {
            healthy = true;
        }
        handleStatus(exchange, 200, "health=" + (healthy ? "up" : "down"));
    }

    private static void handleStatus(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> out = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] parts = arg.substring(2).split("=", 2);
                out.put(parts[0], parts[1]);
            }
        }
        return out;
    }

    private static class EchoHandler implements HttpHandler {
        private final String name;

        private EchoHandler(String name) {
            this.name = name;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = "Echo from " + name + " path=" + exchange.getRequestURI().getPath();
            handleStatus(exchange, 200, body);
        }
    }

    private static void keepAlive() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {
        }
    }
}
