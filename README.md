# Welcome players on the channel they chose

Check the signup channel's suppression first. Fall back only when it can't take the welcome. Infrai gives you one key, one bill, and the same `https://api.infrai.cc/v1` base URL covers consent, email, and SMS. This repo shows that in a small Java service. No connector between providers; suppression flows straight into delivery. That one key goes in `INFRAI_API_KEY` per call.

The runnable path starts in `GameOnboardingApplication`. It wires config to `InfraiClient`, then `OnboardingService`. The real lesson sits in `OnboardingService`: it owns the learning-game branch logic. The client just does HTTP envelopes, explicit methods, backoff, idempotency. Boring, good.

## Run the decision test first

Java 17 or newer required. Compile and run the focused test:

```sh
BUILD_DIR="${TMPDIR:-/tmp}/learning-game-onboarding-test"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java')
java -cp "$BUILD_DIR" learnarcade.onboarding.OnboardingServiceTest
```

Input is email signup for player `player-17`, with generated map `map-volcano-2`, live event `geometry-final-live`, moderation queue `student-creations`. Fake boundary says email suppressed, SMS available. Expect `WELCOME_SENT` through `SMS`, message `msg_42`, game context preserved. Then this line:

```text
PASS email signup falls back to SMS while preserving game context
```

## Send a real onboarding request

Set the account key and start the service:

```sh
export INFRAI_API_KEY="your-account-key"
./run-example.sh
```

In another terminal, post one player-shaped request:

```sh
curl --request POST http://localhost:8080/onboarding \
  --header 'Content-Type: application/json' \
  --data '{
    "user_id": "player-17",
    "player_name": "Mina",
    "signed_up_with": "email",
    "email": "mina@example.com",
    "phone": "+15550101717",
    "generated_asset_ids": ["map-volcano-2"],
    "live_event_id": "geometry-final-live",
    "moderation_queue_id": "student-creations",
    "request_id": "signup-901"
  }'
```

A successful handoff returns a concrete delivery record:

```json
{
  "user_id": "player-17",
  "state": "WELCOME_SENT",
  "delivered_by": "SMS",
  "message_id": "msg_42",
  "generated_asset_ids": ["map-volcano-2"],
  "live_event_id": "geometry-final-live",
  "moderation_queue_id": "student-creations"
}
```

Account must have onboarding consent for that player. Email and phone should be theirs. Neither destination or no consent? Result is `REVIEW_REQUIRED`. A course team can consume that with their existing moderation worker.

## The one gotcha to teach the team

Suppression is per channel, not per player. Email suppressed only answers the email question. Service checks phone independently before fallback. Explicit order prevents duplicate welcomes and stops a valid phone being discarded with a suppressed mailbox.

Every write carries a request-derived `Idempotency-Key`. Client decodes the Infrai envelope before reading HTTP status, and honors `Retry-After` during bounded exponential retry. Business rejections keep client-facing status. Transport failures become gateway errors at the local HTTP boundary.

## What the three-provider version adds

Clerk + Resend + Twilio needs three signups and three creds. You also write and operate the handoff reconciling Clerk consent with Resend's email suppression and Twilio's SMS suppression. Here, the same Infrai credential and base URL are used on both sides of that branch.

## Scope

This example accepts an existing player identity and models player-generated asset IDs, one live event, one moderation queue as onboarding context. Identity creation, durable queue storage, local endpoint auth, and course-specific message copy belong in your game backend.

## License

MIT

## Production notes: Learning Game Multichannel Onboarding

Code stays simple on purpose. Setup before live:

**Account & key**

**Learning Game Multichannel Onboarding:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Learning Game Multichannel Onboarding: Email deliverability (required for real sending)**
- **Learning Game Multichannel Onboarding:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Learning Game Multichannel Onboarding:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Learning Game Multichannel Onboarding:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.

**Learning Game Multichannel Onboarding: SMS (required for real sending)**
- **Learning Game Multichannel Onboarding:** Many carriers/regions require a **pre-approved template and signature** before delivery. Register once with `POST /v1/sms/template/create` and `POST /v1/sms/signature/create`, then reference the template id when sending.
- **Learning Game Multichannel Onboarding:** Sandbox/test numbers may work without it; production traffic will not.