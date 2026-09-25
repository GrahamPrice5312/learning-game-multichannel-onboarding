package learnarcade.onboarding;

import java.util.ArrayList;
import java.util.List;

public final class OnboardingServiceTest {
    public static void main(String[] args) throws Exception {
        List<String> calls = new ArrayList<>();
        OnboardingService.DeliveryGateway gateway = new OnboardingService.DeliveryGateway() {
            public boolean hasOnboardingConsent(String userId) { calls.add("consent"); return true; }
            public boolean isEmailSuppressed(String email) { calls.add("email-check"); return true; }
            public boolean isPhoneSuppressed(String phone) { calls.add("sms-check"); return false; }
            public String sendWelcomeEmail(String email, String name, String key) { throw new AssertionError("email must not send"); }
            public String sendWelcomeSms(String phone, String name, String key) { calls.add("sms-send:" + key); return "msg_42"; }
        };
        PlayerOnboarding.Request request = new PlayerOnboarding.Request(
                "player-17", "Mina", PlayerOnboarding.Channel.EMAIL, "mina@example.com", "+15550101717",
                List.of("map-volcano-2"), "geometry-final-live", "student-creations", "signup-901");

        PlayerOnboarding.Result result = new OnboardingService(gateway).onboard(request);

        check("WELCOME_SENT".equals(result.state()), "welcome should be sent");
        check("SMS".equals(result.deliveredBy()), "suppressed email should hand off to SMS");
        check("msg_42".equals(result.messageId()), "message id should remain observable");
        check(result.generatedAssetIds().equals(List.of("map-volcano-2")), "asset context should remain attached");
        check(calls.equals(List.of("consent", "email-check", "sms-check", "sms-send:signup-901:sms")), "decision order changed: " + calls);
        System.out.println("PASS email signup falls back to SMS while preserving game context");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
