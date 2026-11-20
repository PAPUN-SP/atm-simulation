package com.atm;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** REST API + static file server using only the JDK. */
public class AtmServer {
    interface Handler { String handle(Account acc, String token, Map<String, String> body); }

    static final Bank bank = new Bank();
    static final Pattern KV = Pattern.compile("\"(\\w+)\"\\s*:\\s*(?:\"([^\"\\\\]*)\"|(-?[\\d.]+))");

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Path web = Path.of(System.getenv().getOrDefault("WEB_DIR", "web")).toAbsolutePath().normalize();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        api(server, "/api/login", "POST", false, (a, t, b) ->
                "{\"token\":\"" + bank.login(b.get("account"), b.get("pin")) + "\"}");
        api(server, "/api/logout", "POST", true, (a, t, b) -> { bank.logout(t); return "{\"ok\":true}"; });
        api(server, "/api/balance", "GET", true, (a, t, b) -> profile(a));
        api(server, "/api/withdraw", "POST", true, (a, t, b) ->
                "{\"balance\":" + bank.withdraw(a, num(b, "amount")) + ",\"message\":\"Please collect your cash\"}");
        api(server, "/api/deposit", "POST", true, (a, t, b) ->
                "{\"balance\":" + bank.deposit(a, num(b, "amount")) + ",\"message\":\"Deposit successful\"}");
        api(server, "/api/pin", "POST", true, (a, t, b) -> {
            bank.changePin(a, b.get("oldPin"), b.get("newPin"));
            return "{\"message\":\"PIN changed successfully\"}";
        });
        api(server, "/api/statement", "GET", true, (a, t, b) -> {
            List<Transaction> list = bank.statement(a);
            StringBuilder sb = new StringBuilder("{\"transactions\":[");
            for (int i = 0; i < list.size(); i++) {
                Transaction x = list.get(i);
                if (i > 0) sb.append(',');
                sb.append("{\"time\":\"").append(x.time().withNano(0)).append("\",\"type\":\"").append(x.type())
                  .append("\",\"amount\":").append(x.amount()).append(",\"balance\":").append(x.balanceAfter()).append('}');
            }
            return sb.append("]}").toString();
        });

        server.createContext("/", ex -> serveStatic(ex, web));
        server.start();
        System.out.println("ATM server running on http://localhost:" + port);
    }

    static String profile(Account a) {
        return "{\"holder\":\"" + a.getHolder() + "\",\"account\":\"" + a.getNumber() + "\",\"balance\":" + a.getBalance() + "}";
    }

    static double num(Map<String, String> b, String key) {
        try { return Double.parseDouble(b.get(key)); }
        catch (Exception e) { throw new AtmException(400, "Enter a valid amount"); }
    }

    static void api(HttpServer s, String path, String method, boolean needsAuth, Handler h) {
        s.createContext(path, ex -> {
            String out; int code = 200;
            try {
                if (!ex.getRequestMethod().equals(method)) throw new AtmException(405, "Method not allowed");
                String auth = ex.getRequestHeaders().getFirst("Authorization");
                String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
                Account acc = needsAuth ? bank.auth(token) : null;
                String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> body = new HashMap<>();
                Matcher m = KV.matcher(raw);
                while (m.find()) body.put(m.group(1), m.group(2) != null ? m.group(2) : m.group(3));
                out = h.handle(acc, token, body);
            } catch (AtmException e) {
                code = e.status; out = "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
            } catch (Exception e) {
                code = 500; out = "{\"error\":\"Server error\"}";
            }
            send(ex, code, "application/json", out.getBytes(StandardCharsets.UTF_8));
        });
    }

    static void serveStatic(HttpExchange ex, Path web) throws IOException {
        String p = ex.getRequestURI().getPath();
        Path file = web.resolve(p.equals("/") ? "index.html" : p.substring(1)).normalize();
        if (!file.startsWith(web) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain", "Not found".getBytes()); return;
        }
        String type = file.toString().endsWith(".html") ? "text/html; charset=utf-8" : "application/octet-stream";
        send(ex, 200, type, Files.readAllBytes(file));
    }

    static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }
}
