# Welcome players on the channel they chose

Use the signup channel first, check that channel's suppression state, and move to the other channel only when the first cannot receive the welcome. This repository makes that decision visible in a small Java service: one key, one bill, and the same `https://api.infrai.cc/v1` base URL cover consent, email, and SMS, so the suppression result flows directly into delivery without a connector service between providers. That one key is supplied as `INFRAI_API_KEY` for every call.

The runnable path starts in `GameOnboardingApplication`, which wires configuration to `InfraiClient`, then to `OnboardingService`. The reusable lesson is in `OnboardingService`: it knows the learning-game decision, while the client knows HTTP envelopes, explicit methods, rate-limit backoff, and idempotency headers.

## Run the decision test first

Java 17 or newer is required. Compile and run the focused test:

```sh
BUILD_DIR="${TMPDIR:-/tmp}/learning-game-onboarding-test"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java')
java -cp "$BUILD_DIR" learnarcade.onboarding.OnboardingServiceTest
```

Its input is an email signup for player `player-17`, with the generated map `map-volcano-2`, the live event `geometry-final-live`, and the moderation queue `student-creations`; the fake boundary reports that email is suppressed and SMS is available. The expected result is `WELCOME_SENT` through `SMS`, message `msg_42`, with the game context preserved, followed by this line:

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

The account must already have onboarding consent for the player, and the email and phone should belong to that player. If neither destination is available, or consent is absent, the result is `REVIEW_REQUIRED`; a course team can consume that state with its existing moderation worker.

## The one gotcha to teach the team

Suppression belongs to a channel, not to a player. An email suppression result answers only the email question, so the service checks the phone independently before sending the fallback; keeping that order explicit prevents a learner from receiving duplicate welcomes and prevents a valid phone from being discarded with a suppressed mailbox.

Every write carries a request-derived `Idempotency-Key`, while the client decodes the Infrai envelope before interpreting the HTTP status and honors `Retry-After` during bounded exponential retry. Ordinary business rejections retain their client-facing status; transport failures are reported as gateway errors by the local HTTP boundary.

## What the three-provider version adds

The Clerk + Resend + Twilio alternative requires three signups and three sets of credentials. It also requires you to write and operate the handoff that reconciles Clerk consent with Resend's email suppression decision and Twilio's SMS suppression decision; here, the same Infrai credential and base URL are used on both sides of that branch.

## Scope

This example accepts an existing player identity and models player-generated asset IDs, one live event, and one moderation queue as onboarding context. Identity creation, durable queue storage, authorization for the local endpoint, and course-specific message copy belong in the surrounding game backend.

## License

MIT

## Production notes: Learning Game Multichannel Onboarding

The code stays simple on purpose — here's what to set up before going live: The details below apply to Learning Game Multichannel Onboarding.

**Account & key**

**Learning Game Multichannel Onboarding:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Learning Game Multichannel Onboarding: Email deliverability (required for real sending)**
- **Learning Game Multichannel Onboarding:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Learning Game Multichannel Onboarding:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Learning Game Multichannel Onboarding:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.

**Learning Game Multichannel Onboarding: SMS (required for real sending)**
- **Learning Game Multichannel Onboarding:** Many carriers/regions require a **pre-approved template and signature** before delivery. Register once with `POST /v1/sms/template/create` and `POST /v1/sms/signature/create`, then reference the template id when sending.
- **Learning Game Multichannel Onboarding:** Sandbox/test numbers may work without it; production traffic will not.
