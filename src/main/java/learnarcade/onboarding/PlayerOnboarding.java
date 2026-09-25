package learnarcade.onboarding;

import java.util.List;
import java.util.Map;

public final class PlayerOnboarding {
    private PlayerOnboarding() {}

    public enum Channel { EMAIL, SMS }

    public record Request(
            String userId,
            String playerName,
            Channel signedUpWith,
            String email,
            String phone,
            List<String> generatedAssetIds,
            String liveEventId,
            String moderationQueueId,
            String requestId) {
        public Request {
            generatedAssetIds = generatedAssetIds == null ? List.of() : List.copyOf(generatedAssetIds);
        }
    }

    public record Result(
            String userId,
            String state,
            String deliveredBy,
            String messageId,
            List<String> generatedAssetIds,
            String liveEventId,
            String moderationQueueId) {
        public Map<String, Object> asMap() {
            return Map.of(
                    "user_id", userId,
                    "state", state,
                    "delivered_by", deliveredBy,
                    "message_id", messageId,
                    "generated_asset_ids", generatedAssetIds,
                    "live_event_id", liveEventId,
                    "moderation_queue_id", moderationQueueId);
        }
    }
}
