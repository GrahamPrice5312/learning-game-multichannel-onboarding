package learnarcade.onboarding;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class GameOnboardingApplication {
    private GameOnboardingApplication() {}

    public static void main(String[] args) throws IOException {
        InfraiConfig config = InfraiConfig.fromEnvironment();
        OnboardingService service = new OnboardingService(new InfraiDeliveryGateway(new InfraiClient(config)));
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/onboarding", exchange -> handle(exchange, service));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Learning-game onboarding listening on http://localhost:8080/onboarding");
    }

    @SuppressWarnings("unchecked")
    private static void handle(HttpExchange exchange, OnboardingService service) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("error", "method_not_allowed"));
            return;
        }
        try {
            Object decoded = Json.parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (!(decoded instanceof Map<?, ?> raw)) throw new IllegalArgumentException("Expected a JSON object");
            Map<String, Object> input = (Map<String, Object>) raw;
            PlayerOnboarding.Request request = new PlayerOnboarding.Request(
                    required(input, "user_id"), required(input, "player_name"),
                    PlayerOnboarding.Channel.valueOf(required(input, "signed_up_with").toUpperCase()),
                    optional(input, "email"), optional(input, "phone"), strings(input.get("generated_asset_ids")),
                    required(input, "live_event_id"), required(input, "moderation_queue_id"), required(input, "request_id"));
            send(exchange, 200, service.onboard(request).asMap());
        } catch (InfraiClient.InfraiException error) {
            int status = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
            send(exchange, status, Map.of("error", error.code(), "message", error.getMessage()));
        } catch (IllegalArgumentException error) {
            send(exchange, 400, Map.of("error", "invalid_request", "message", error.getMessage()));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            send(exchange, 503, Map.of("error", "request_interrupted"));
        } catch (IOException error) {
            send(exchange, 502, Map.of("error", "delivery_unavailable"));
        }
    }

    private static String required(Map<String, Object> input, String key) {
        String value = optional(input, key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static String optional(Map<String, Object> input, String key) {
        Object value = input.get(key);
        return value == null ? null : value.toString();
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(Object::toString).toList();
    }

    private static void send(HttpExchange exchange, int status, Map<String, Object> payload) throws IOException {
        byte[] body = Json.stringify(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
