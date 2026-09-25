package learnarcade.onboarding;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public final class InfraiClient {
    private final InfraiConfig config;
    private final HttpClient http;

    public InfraiClient(InfraiConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    InfraiClient(InfraiConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public boolean hasOnboardingConsent(String userId) throws IOException, InterruptedException {
        Map<String, Object> data = request("GET", "/auth/consent/check/" + segment(userId) + "/onboarding", null, null);
        return Boolean.TRUE.equals(data.get("granted"));
    }

    public boolean isEmailSuppressed(String email) throws IOException, InterruptedException {
        Map<String, Object> data = request("GET", "/email/suppression/check/" + segment(email), null, null);
        return Boolean.TRUE.equals(data.get("suppressed"));
    }

    public boolean isPhoneSuppressed(String phone) throws IOException, InterruptedException {
        Map<String, Object> data = request("POST", "/sms/suppression/check", Map.of("phone", phone), null);
        return Boolean.TRUE.equals(data.get("suppressed"));
    }

    public String sendWelcomeEmail(String email, String playerName, String idempotencyKey)
            throws IOException, InterruptedException {
        Map<String, Object> data = request("POST", "/email/send", Map.of(
                "to", email,
                "subject", "Welcome to the live lesson arena",
                "body", "Hi " + playerName + ", your learning game profile is ready."
        ), idempotencyKey);
        return String.valueOf(data.get("message_id"));
    }

    public String sendWelcomeSms(String phone, String playerName, String idempotencyKey)
            throws IOException, InterruptedException {
        Map<String, Object> data = request("POST", "/sms/send", Map.of(
                "to", phone,
                "body", "Hi " + playerName + ", your learning game profile is ready."
        ), idempotencyKey);
        return String.valueOf(data.get("message_id"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(String method, String path, Map<String, Object> body, String idempotencyKey)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json");
            if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(Json.stringify(body)));

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Object decoded;
            try { decoded = Json.parse(response.body()); }
            catch (IllegalArgumentException parseError) {
                throw new IOException("Infrai returned an unreadable response", parseError);
            }
            if (!(decoded instanceof Map<?, ?> rawEnvelope)) throw new IOException("Infrai returned an invalid envelope");
            Map<String, Object> envelope = (Map<String, Object>) rawEnvelope;
            if (Boolean.TRUE.equals(envelope.get("ok"))) {
                Object data = envelope.get("data");
                return data instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
            }
            if (response.statusCode() == 429 && attempt < 3) {
                Thread.sleep(retryDelayMillis(response, attempt));
                continue;
            }
            Map<String, Object> error = envelope.get("error") instanceof Map<?, ?> map
                    ? (Map<String, Object>) map : Map.of();
            throw new InfraiException(String.valueOf(error.get("code")), String.valueOf(error.get("message")), response.statusCode());
        }
        throw new IOException("Infrai request exhausted its retry budget");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try { return Math.max(0, Long.parseLong(retryAfter)) * 1000L; }
        catch (NumberFormatException ignored) { return 250L * (1L << attempt); }
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static final class InfraiException extends IOException {
        private final String code;
        private final int status;

        InfraiException(String code, String message, int status) {
            super(message);
            this.code = code;
            this.status = status;
        }

        public String code() { return code; }
        public int status() { return status; }
    }
}
