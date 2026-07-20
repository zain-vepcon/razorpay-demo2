#!/usr/bin/env bash
#
# Simulates Razorpay sending a webhook to your locally running app.
# Useful for testing webhook handling WITHOUT needing a live Razorpay dashboard trigger.
#
# Usage:
#   ./simulate_webhook.sh captured   -> sends a payment.captured event
#   ./simulate_webhook.sh failed     -> sends a payment.failed event
#   ./simulate_webhook.sh tampered   -> sends payment.captured but with a WRONG signature (should be rejected)
#   ./simulate_webhook.sh replay     -> sends the same captured event twice (should be de-duplicated on 2nd send)
#
# Requires: curl, openssl
# Make sure WEBHOOK_SECRET below matches razorpay.webhook.secret in application.properties

set -euo pipefail

APP_URL="http://localhost:8080/api/webhooks/razorpay"
WEBHOOK_SECRET="webhook-secret-2026"
ORDER_ID="${ORDER_ID:-order_TEST123456789}"
PAYMENT_ID="${PAYMENT_ID:-pay_TEST987654321}"
EVENT_ID="evt_$(date +%s)"

SCENARIO="${1:-captured}"

build_captured_payload() {
  cat <<EOF
{
  "entity": "event",
  "account_id": "acc_test",
  "event": "payment.captured",
  "contains": ["payment"],
  "payload": {
    "payment": {
      "entity": {
        "id": "${PAYMENT_ID}",
        "entity": "payment",
        "amount": 50000,
        "currency": "INR",
        "status": "captured",
        "order_id": "${ORDER_ID}",
        "method": "card",
        "captured": true
      }
    }
  },
  "created_at": $(date +%s)
}
EOF
}

build_failed_payload() {
  cat <<EOF
{
  "entity": "event",
  "account_id": "acc_test",
  "event": "payment.failed",
  "contains": ["payment"],
  "payload": {
    "payment": {
      "entity": {
        "id": "${PAYMENT_ID}",
        "entity": "payment",
        "amount": 50000,
        "currency": "INR",
        "status": "failed",
        "order_id": "${ORDER_ID}",
        "method": "card",
        "error_code": "BAD_REQUEST_ERROR",
        "error_description": "Payment failed due to insufficient funds (simulated)"
      }
    }
  },
  "created_at": $(date +%s)
}
EOF
}

send() {
  local payload="$1"
  local signature="$2"
  local event_id="$3"

  echo "---- Sending webhook (event_id=${event_id}) ----"
  curl -s -i -X POST "${APP_URL}" \
    -H "Content-Type: application/json" \
    -H "X-Razorpay-Signature: ${signature}" \
    -H "X-Razorpay-Event-Id: ${event_id}" \
    -d "${payload}"
  echo
  echo "-------------------------------------------------"
}

case "$SCENARIO" in
  captured)
    PAYLOAD=$(build_captured_payload)
    SIGNATURE=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | sed 's/^.* //')
    send "$PAYLOAD" "$SIGNATURE" "$EVENT_ID"
    ;;
  failed)
    PAYLOAD=$(build_failed_payload)
    SIGNATURE=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | sed 's/^.* //')
    send "$PAYLOAD" "$SIGNATURE" "$EVENT_ID"
    ;;
  tampered)
    PAYLOAD=$(build_captured_payload)
    # Intentionally sign with the WRONG secret to simulate a forged/tampered request
    SIGNATURE=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "wrong_secret_here" | sed 's/^.* //')
    send "$PAYLOAD" "$SIGNATURE" "$EVENT_ID"
    echo "Expected result: HTTP 400 'Invalid signature'"
    ;;
  replay)
    PAYLOAD=$(build_captured_payload)
    SIGNATURE=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | sed 's/^.* //')
    echo ">>> First send (should process normally):"
    send "$PAYLOAD" "$SIGNATURE" "$EVENT_ID"
    echo ">>> Second send with SAME event id (should be detected as duplicate):"
    send "$PAYLOAD" "$SIGNATURE" "$EVENT_ID"
    ;;
  *)
    echo "Unknown scenario: $SCENARIO"
    echo "Usage: $0 [captured|failed|tampered|replay]"
    exit 1
    ;;
esac
