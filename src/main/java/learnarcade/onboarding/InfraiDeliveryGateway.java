package learnarcade.onboarding;

import java.io.IOException;

public final class InfraiDeliveryGateway implements OnboardingService.DeliveryGateway {
    private final InfraiClient infrai;

    public InfraiDeliveryGateway(InfraiClient infrai) { this.infrai = infrai; }
    public boolean hasOnboardingConsent(String userId) throws IOException, InterruptedException { return infrai.hasOnboardingConsent(userId); }
    public boolean isEmailSuppressed(String email) throws IOException, InterruptedException { return infrai.isEmailSuppressed(email); }
    public boolean isPhoneSuppressed(String phone) throws IOException, InterruptedException { return infrai.isPhoneSuppressed(phone); }
    public String sendWelcomeEmail(String email, String playerName, String key) throws IOException, InterruptedException { return infrai.sendWelcomeEmail(email, playerName, key); }
    public String sendWelcomeSms(String phone, String playerName, String key) throws IOException, InterruptedException { return infrai.sendWelcomeSms(phone, playerName, key); }
}
