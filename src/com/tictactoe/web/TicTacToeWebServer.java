package com.tictactoe.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tictactoe.service.GameEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedded HTTP Server for Tic-Tac-Toe.
 * Binds to 0.0.0.0 so that any browser on any device (phone, tablet, computer)
 * connected to the local Wi-Fi, hotspot, or network can play.
 */
public class TicTacToeWebServer {

    private final int port;
    private final GameEngine engine;
    private HttpServer server;

    public TicTacToeWebServer(int port, GameEngine engine) {
        this.port = port;
        this.engine = engine;
    }

    public void start() throws IOException {
        // Bind to 0.0.0.0 for universal LAN/Wi-Fi/Internet access
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // Static Web Dashboard Handler
        server.createContext("/", new StaticWebHandler());

        // REST API Endpoints
        server.createContext("/api/state", new StateHandler());
        server.createContext("/api/move", new MoveHandler());
        server.createContext("/api/reset-round", new ResetRoundHandler());
        server.createContext("/api/restart-round", new RestartRoundHandler());
        server.createContext("/api/restart", new NewGameHandler());
        server.createContext("/api/new-game", new NewGameHandler());
        server.createContext("/api/mode", new ModeHandler());
        server.createContext("/api/players", new PlayersHandler());
        server.createContext("/api/network", new NetworkInfoHandler());

        server.setExecutor(null); // Default single-thread executor
        server.start();

        System.out.println("[Web Server] Tic-Tac-Toe Universal Game Server running on port " + port + ":");
        System.out.println("  -> Local Browser: http://localhost:" + port);
        List<String> ips = getNetworkIps();
        for (String ip : ips) {
            System.out.println("  -> Device Network URL: http://" + ip + ":" + port + " (Connect phone, tablet, or laptop)");
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public int getPort() {
        return port;
    }

    /**
     * Enumerates non-loopback IPv4 addresses across network cards (Wi-Fi, Ethernet, Hotspot).
     */
    public static List<String> getNetworkIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}
        return ips;
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] data) throws IOException {
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
            os.flush();
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseParams(String input) {
        Map<String, String> map = new HashMap<>();
        if (input == null || input.trim().isEmpty()) return map;
        input = input.trim();

        // 1. Check for JSON keys and values using regex: "key": "value" or "key": 123
        java.util.regex.Pattern jsonPattern = java.util.regex.Pattern.compile("\"?([a-zA-Z0-9_]+)\"?\\s*:\\s*\"?([^,\"}]+)\"?");
        java.util.regex.Matcher jsonMatcher = jsonPattern.matcher(input);
        boolean matchedJson = false;
        while (jsonMatcher.find()) {
            map.put(jsonMatcher.group(1).trim(), jsonMatcher.group(2).trim());
            matchedJson = true;
        }
        if (matchedJson) return map;

        // 2. Fallback to URL encoded / query format: key=value&key2=val2
        String[] pairs = input.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    /**
     * Serves index.html from disk or embedded resource.
     */
    private class StaticWebHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Look for file in working directory
            File file = new File("." + path);
            if (!file.exists()) {
                // Try parent or current directory
                file = new File("index.html");
            }

            if (file.exists() && file.isFile()) {
                String mime = path.endsWith(".html") ? "text/html" :
                              path.endsWith(".css") ? "text/css" :
                              path.endsWith(".js") ? "application/javascript" :
                              path.endsWith(".json") ? "application/json" :
                              path.endsWith(".png") ? "image/png" : "text/plain";
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] bytes = fis.readAllBytes();
                    sendResponse(exchange, 200, mime, bytes);
                    return;
                }
            }

            // Fallback message
            String fallback = "<!DOCTYPE html><html><head><title>Tic-Tac-Toe Server</title></head>"
                    + "<body style='background:#0f172a;color:#fff;font-family:sans-serif;text-align:center;padding:50px;'>"
                    + "<h1>Tic-Tac-Toe Game Server Running</h1>"
                    + "<p>Place index.html in the project root directory.</p>"
                    + "<p><a style='color:#38bdf8' href='/api/state'>View Game State JSON API</a></p>"
                    + "</body></html>";
            sendResponse(exchange, 200, "text/html", fallback.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class StateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String json = engine.getState().toJson();
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class MoveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String body = readRequestBody(exchange);
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseParams(body);
            if (query != null && !query.isEmpty()) {
                params.putAll(parseParams(query));
            }

            int row = -1;
            int col = -1;
            try {
                if (params.containsKey("row")) row = Integer.parseInt(params.get("row"));
                if (params.containsKey("col")) col = Integer.parseInt(params.get("col"));
            } catch (NumberFormatException ignored) {}

            boolean success = false;
            if (row >= 0 && col >= 0) {
                success = engine.makeMove(row, col);
            }

            String json = "{\"success\":" + success + ",\"state\":" + engine.getState().toJson() + "}";
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class ResetRoundHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            engine.resetRound();
            String json = "{\"success\":true,\"state\":" + engine.getState().toJson() + "}";
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class RestartRoundHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            engine.restartRound();
            String json = "{\"success\":true,\"state\":" + engine.getState().toJson() + "}";
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class NewGameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            engine.newGame();
            String json = "{\"success\":true,\"state\":" + engine.getState().toJson() + "}";
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class ModeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> params = parseParams(body);
            String mode = params.getOrDefault("mode", "PVP");
            engine.setGameMode(mode);
            String json = "{\"success\":true,\"state\":" + engine.getState().toJson() + "}";
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> params = parseParams(body);
            String nameX = params.getOrDefault("nameX", "Player X");
            String nameO = params.getOrDefault("nameO", "Player O");
            engine.setPlayerNames(nameX, nameO);
            String json = "{\"success\":true,\"state\":" + engine.getState().toJson() + "}";
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class NetworkInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            List<String> ips = getNetworkIps();
            StringBuilder sb = new StringBuilder();
            sb.append("{\"port\":").append(port).append(",");
            sb.append("\"localUrl\":\"http://localhost:").append(port).append("\",");
            sb.append("\"networkUrls\":[");
            for (int i = 0; i < ips.size(); i++) {
                sb.append("\"http://").append(ips.get(i)).append(":").append(port).append("\"");
                if (i < ips.size() - 1) sb.append(",");
            }
            sb.append("]}");

            sendResponse(exchange, 200, "application/json", sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
