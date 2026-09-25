package learnarcade.onboarding;

import java.io.IOException;

import static learnarcade.onboarding.PlayerOnboarding.Channel.EMAIL;
import static learnarcade.onboarding.PlayerOnboarding.Channel.SMS;

public final class OnboardingService {
    public interface DeliveryGateway {
        boolean hasOnboardingConsent(String userId) throws IOException, InterruptedException;
        boolean isEmailSuppressed(String email) throws IOException, InterruptedException;
        boolean isPhoneSuppressed(String phone) throws IOException, InterruptedException;
        String sendWelcomeEmail(String email, String playerName, String idempotencyKey) throws IOException, InterruptedException;
        String sendWelcomeSms(String phone, String playerName, String idempotencyKey) throws IOException, InterruptedException;
    }

    private final DeliveryGateway gateway;

    public OnboardingService(DeliveryGateway gateway) { this.gateway = gateway; }

    public PlayerOnboarding.Result onboard(PlayerOnboarding.Request request) throws IOException, InterruptedException {
        if (!gateway.hasOnboardingConsent(request.userId())) return result(request, "REVIEW_REQUIRED", "NONE", "none");

        if (request.signedUpWith() == EMAIL) {
            if (present(request.email()) && !gateway.isEmailSuppressed(request.email())) {
                return result(request, "WELCOME_SENT", "EMAIL",
                        gateway.sendWelcomeEmail(request.email(), request.playerName(), request.requestId() + ":email"));
            }
            if (present(request.phone()) && !gateway.isPhoneSuppressed(request.phone())) {
                return result(request, "WELCOME_SENT", "SMS",
                        gateway.sendWelcomeSms(request.phone(), request.playerName(), request.requestId() + ":sms"));
            }
        } else if (request.signedUpWith() == SMS) {
            if (present(request.phone()) && !gateway.isPhoneSuppressed(request.phone())) {
                return result(request, "WELCOME_SENT", "SMS",
                        gateway.sendWelcomeSms(request.phone(), request.playerName(), request.requestId() + ":sms"));
            }
            if (present(request.email()) && !gateway.isEmailSuppressed(request.email())) {
                return result(request, "WELCOME_SENT", "EMAIL",
                        gateway.sendWelcomeEmail(request.email(), request.playerName(), request.requestId() + ":email"));
            }
        }
        return result(request, "REVIEW_REQUIRED", "NONE", "none");
    }

    private static boolean present(String value) { return value != null && !value.isBlank(); }

    private static PlayerOnboarding.Result result(PlayerOnboarding.Request request, String state, String channel, String messageId) {
        return new PlayerOnboarding.Result(request.userId(), state, channel, messageId,
                request.generatedAssetIds(), request.liveEventId(), request.moderationQueueId());
    }
}
