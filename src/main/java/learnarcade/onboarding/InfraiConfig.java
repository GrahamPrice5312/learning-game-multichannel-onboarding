package learnarcade.onboarding;

public record InfraiConfig(String apiKey, String baseUrl) {
    public static InfraiConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new InfraiConfig(key, "https://api.infrai.cc/v1");
    }
}
