# Razorpay + Spring Boot Sample Project

A complete, runnable Spring Boot backend demonstrating:
- Creating a Razorpay order (Orders API)
- Verifying the Checkout.js client-side callback signature
- Receiving and verifying Razorpay **webhooks** (server-to-server, the authoritative source of truth)
- Idempotent webhook processing (handles Razorpay's automatic retries safely)
- A local H2 database so you can inspect order/webhook state after each test

```
razorpay-springboot-demo/
├── pom.xml
├── src/main/java/com/example/razorpay/
│   ├── RazorpayDemoApplication.java
│   ├── config/RazorpayConfig.java          # builds the RazorpayClient bean
│   ├── controller/PaymentController.java   # /api/payments/create-order, /verify
│   ├── controller/WebhookController.java   # /api/webhooks/razorpay
│   ├── service/PaymentService.java
│   ├── service/WebhookService.java
│   ├── util/SignatureUtil.java             # HMAC-SHA256 verification (shared logic)
│   ├── model/PaymentOrder.java             # JPA entity
│   ├── model/WebhookEvent.java             # JPA entity (idempotency log)
│   ├── repository/...
│   ├── dto/...
│   └── exception/GlobalExceptionHandler.java
├── src/main/resources/application.properties
├── src/test/java/.../SignatureUtilTest.java
└── scripts/simulate_webhook.sh             # simulate webhooks locally, no dashboard needed
```

> **Note on this sandbox:** I wrote and manually reviewed all the code against the Razorpay Java SDK APIs, but this sandbox can only reach a fixed allowlist of domains and could not download from Maven Central to run `mvn compile` here. Run the build yourself once you unzip the project on your machine — standard `mvn spring-boot:run` from a normal internet connection will work.

---

## 1. Prerequisites

- Java 17+
- Maven 3.8+
- A free [Razorpay account](https://dashboard.razorpay.com/signup) (test mode needs no KYC/business docs)
- [ngrok](https://ngrok.com/download) (or any tunnel tool) — needed so Razorpay's servers can reach your local machine to deliver webhooks
- `curl` and `openssl` (for manual testing; both come preinstalled on Mac/Linux; on Windows use Git Bash or WSL)

---

## 2. Get your test API keys

1. Log into the [Razorpay Dashboard](https://dashboard.razorpay.com/).
2. Toggle **Test Mode** ON (top-right switch) — this is important, it's a separate key set from Live Mode.
3. Go to **Settings → API Keys → Generate Test Key**.
4. Copy the **Key Id** (`rzp_test_...`) and **Key Secret**.
5. Paste them into `src/main/resources/application.properties`:

```properties
razorpay.key.id=rzp_test_XXXXXXXXXXXXXX
razorpay.key.secret=XXXXXXXXXXXXXXXXXXXXXXXX
```

---

## 3. Run the project

```bash
cd razorpay-springboot-demo
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. H2 console (to inspect saved orders/webhook events) is at `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:razorpaydb`, user `sa`, blank password.

---

## 4. Set up the webhook (so Razorpay can call your local server)

1. Start ngrok pointing at your local port:
   ```bash
   ngrok http 8080
   ```
   Copy the forwarding URL, e.g. `https://a1b2-xx-xx.ngrok-free.app`.

2. In the Razorpay Dashboard: **Settings → Webhooks → Add New Webhook**.
   - **Webhook URL**: `https://a1b2-xx-xx.ngrok-free.app/api/webhooks/razorpay`
   - **Secret**: pick any string, e.g. `my_webhook_test_secret_123`, and put the *same* value in `application.properties` as `razorpay.webhook.secret`. (This secret is separate from your API Key Secret — don't confuse the two.)
   - **Active events**: at minimum select `payment.captured`, `payment.failed`, `order.paid`, `refund.created`, `refund.processed`.
3. Save. Razorpay will send a test ping — check your app logs for a `Webhook processed` or signature log line.

---

## 5. Test scenario 1 — Create an order

```bash
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{"amount": 500, "currency": "INR", "receipt": "receipt_001"}'
```

Expected: `200 OK` with a `razorpayOrderId` like `order_XXXXXXXXXXXXX`. This order is now saved locally with status `CREATED`, and exists on Razorpay's side too — check **Dashboard → Transactions → Orders**.

---

## 6. Test scenario 2 — Pay using Razorpay's official test cards

Since this is a backend-only sample, use Razorpay's **hosted Checkout** or **Postman/cURL against the Orders API test flow** to actually attempt a payment against the order id from step 5. The simplest way: build a one-page test checkout (Razorpay provides Checkout.js purely client-side) or use their **Test Payment Simulation** in the dashboard.

Razorpay's standard test cards (test mode only, never real money):

| Scenario | Card Number | CVV | Expiry | Result |
|---|---|---|---|---|
| Successful payment | `4111 1111 1111 1111` | any 3 digits | any future date | Payment captured |
| Payment failure | `4000 0000 0000 0002` | any 3 digits | any future date | Card declined |
| International card | `5104 0155 0000 0008` | any 3 digits | any future date | Success (Mastercard) |

For UPI test mode, use UPI ID `success@razorpay` (always succeeds) or `failure@razorpay` (always fails).
For test OTP prompts, use `1234` or `otp` when Razorpay's simulated bank page asks.

Full official list (kept current by Razorpay): https://razorpay.com/docs/payments/payments/test-card-upi-details/

After a successful test payment, Razorpay's Checkout.js callback gives you `razorpay_order_id`, `razorpay_payment_id`, `razorpay_signature`. Verify them:

```bash
curl -X POST http://localhost:8080/api/payments/verify \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayOrderId": "order_XXXXXXXXXXXXX",
    "razorpayPaymentId": "pay_XXXXXXXXXXXXX",
    "razorpaySignature": "<signature_from_checkout_callback>"
  }'
```

Expected: `200 OK, {"success":true,"data":true}` for a genuine callback. If you paste in a modified value, expect `400` with a tampering warning — this is what proves the endpoint is actually checking the signature and not just trusting the client.

---

## 7. Test scenario 3 — Webhook: payment captured (real, via dashboard)

After a real test payment succeeds (step 6), Razorpay automatically fires `payment.captured` to your ngrok URL. Check:
- App logs show `Payment captured: order=..., payment=...`
- H2 console: `payment_orders` table row now has `status = PAID`
- `webhook_events` table has a new row with `signature_valid = true`

---

## 8. Test scenario 4 — Webhook: simulate locally without waiting on the dashboard

Use the included script for fast, repeatable testing of edge cases:

```bash
cd scripts
chmod +x simulate_webhook.sh

./simulate_webhook.sh captured   # simulate a successful payment webhook
./simulate_webhook.sh failed     # simulate a failed payment webhook
./simulate_webhook.sh tampered   # simulate a forged webhook (wrong secret) -> expect HTTP 400
./simulate_webhook.sh replay     # simulate Razorpay retrying the same event twice -> 2nd is deduplicated
```

Make sure `WEBHOOK_SECRET` inside the script matches `razorpay.webhook.secret` in your `application.properties`.

What to check for each:

| Scenario | Expected HTTP status | Expected DB / log outcome |
|---|---|---|
| `captured` | 200 | order status → `PAID`, `webhook_events` row `signature_valid=true` |
| `failed` | 200 | order status → `FAILED`, `failureReason` populated |
| `tampered` | 400 `Invalid signature` | `webhook_events` row saved with `signature_valid=false`, order **not** updated |
| `replay` | 200 both times | only ONE row in `webhook_events` for that event id — 2nd call logged as "Duplicate event acknowledged" |

---

## 9. Test scenario 5 — Refunds

1. In the Razorpay Dashboard, find a captured test payment → **Refund** → full or partial.
2. Your webhook will receive `refund.created` then `refund.processed`.
3. Check logs for `Refund created event received` / `Refund processed event received`. (The sample logs these; extend `WebhookService.process()` with your own refund-ledger logic as needed.)

---

## 10. Test scenario 6 — Missing/garbled signature header

```bash
curl -i -X POST http://localhost:8080/api/webhooks/razorpay \
  -H "Content-Type: application/json" \
  -d '{"event":"payment.captured"}'
```
Expected: `400 Bad Request`, `"Missing signature header"` — proves the endpoint won't silently accept unsigned requests.

---

## 11. Test scenario 7 — Validation errors on order creation

```bash
curl -i -X POST http://localhost:8080/api/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{"amount": 0, "currency": "INR", "receipt": ""}'
```
Expected: `400 Bad Request` with field-level validation messages (amount must be ≥ 1, receipt must not be blank).

---

## 12. Key implementation notes worth understanding

- **Amount units**: Razorpay's API always works in the smallest currency unit (paise for INR). The `OrderRequest.amount` field takes rupees for convenience; `PaymentService` converts it. Double-check this conversion if you add other currencies.
- **Checkout signature vs. webhook signature**: these use the *same* HMAC-SHA256 algorithm but different payload formats and different secrets:
  - Checkout: `HMAC_SHA256(order_id + "|" + payment_id, key_secret)`
  - Webhook: `HMAC_SHA256(raw_request_body, webhook_secret)`
- **Never trust the client-side callback alone.** `/api/payments/verify` is for instant UI feedback only. Order fulfillment (shipping, unlocking content, etc.) should be driven by the webhook, since only Razorpay's servers can send that call, and only after you've verified its signature.
- **Idempotency matters.** Razorpay retries webhooks on timeout or non-2xx response, potentially delivering the same event multiple times. `WebhookEvent.eventId` has a unique constraint and `WebhookService.isDuplicate()` guards against double-processing (e.g. accidentally shipping an order twice).
- **Respond fast.** The webhook handler should stay lightweight — if you need to do slow work (send emails, call other services), queue it and return 200 immediately; Razorpay times out around 5 seconds per attempt.

---

## 13. Common pitfalls when going live later

- Test mode and Live mode have **completely separate** key pairs and webhook secrets — switching to production means updating both, not just the key id/secret.
- The webhook secret is set manually by you when creating the webhook in the dashboard; it is **not** returned by any API and is different from `key.secret`.
- Amounts must be integers (no decimals) in the smallest unit — sending `500.5` rupees as paise will error out if not rounded first.



razorpay-payment-service
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── razorpay
│   │   │           └── payment
│   │   │               ├── RazorpayPaymentApplication.java
│   │   │               │
│   │   │               ├── config
│   │   │               │   ├── RazorpayConfig.java
│   │   │               │   └── WebConfig.java
│   │   │               │
│   │   │               ├── controller
│   │   │               │   ├── PaymentController.java
│   │   │               │   └── WebhookController.java
│   │   │               │
│   │   │               ├── service
│   │   │               │   ├── RazorpayService.java
│   │   │               │   ├── WebhookService.java
│   │   │               │   └── impl
│   │   │               │       ├── RazorpayServiceImpl.java
│   │   │               │       └── WebhookServiceImpl.java
│   │   │               │
│   │   │               ├── repository
│   │   │               │   └── PaymentRepository.java
│   │   │               │
│   │   │               ├── entity
│   │   │               │   └── Payment.java
│   │   │               │
│   │   │               ├── dto
│   │   │               │   ├── request
│   │   │               │   │   ├── CreateOrderRequest.java
│   │   │               │   │   └── VerifyPaymentRequest.java
│   │   │               │   │
│   │   │               │   └── response
│   │   │               │       ├── CreateOrderResponse.java
│   │   │               │       └── PaymentResponse.java
│   │   │               │
│   │   │               ├── exception
│   │   │               │   ├── GlobalExceptionHandler.java
│   │   │               │   └── PaymentException.java
│   │   │               │
│   │   │               ├── util
│   │   │               │   ├── SignatureVerifier.java
│   │   │               │   └── Constants.java
│   │   │               │
│   │   │               └── enums
│   │   │                   └── PaymentStatus.java
│   │   │
│   │   └── resources
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── static
│   │           └── index.html
│   │
│   └── test
│       └── java
│           └── com
│               └── razorpay
│                   └── payment
│                       ├── controller
│                       ├── service
│                       └── repository
│
├── docs
│   ├── api-documentation.md
│   ├── architecture.md
│   └── postman-collection.json
│
├── scripts
│   ├── database.sql
│   └── sample-data.sql
│
├── .env.example
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── LICENSE

Highlights of this architecture
  • Layered architecture (Controller → Service → Repository → Database) 
  • DTOs separated into request and response 
  • Service interfaces separated from implementations (service/impl) 
  • Centralized exception handling with a global exception handler 
  • Utility classes for signature verification and constants 
  • Environment-specific configuration (application-dev.properties, application-prod.properties) 
  • API documentation and database scripts kept in dedicated folders 
  • Unit test structure mirrors the main source structure 
  • Static frontend placed under src/main/resources/static instead of a top-level frontend folder, allowing Spring Boot to serve it directly.

How Razorpay Integration Works
The payment process is divided into Backend, Frontend, Razorpay Server, and Webhook.


Customer
    │
    ▼
Frontend (HTML/JavaScript)
    │
    │ 1. Request Order
    ▼
Spring Boot Backend
    │
    │ 2. Create Razorpay Order
    ▼
Razorpay Server
    │
    │ 3. Returns Order ID
    ▼
Spring Boot Backend
    │
    │ 4. Sends Order ID
    ▼
Frontend
    │
    │ 5. Opens Razorpay Checkout
    ▼
Customer Pays
    │
    ▼
Razorpay
    │
    ├────────► Frontend receives Payment ID
    │
    └────────► Sends Webhook to Backend


Step-by-Step Payment Flow
Step 1: User Clicks "Pay Now"
The customer opens your website.

http://localhost:8080
The frontend displays

Pay ₹500
When the button is clicked

fetch("/payment/create-order")
is called.

Step 2: Backend Creates an Order
The frontend never talks directly to Razorpay to create an order.
Instead,

Frontend
      │
      ▼
Spring Boot
Spring Boot calls

RazorpayClient.orders.create(...)
Example response from Razorpay

{
   "id":"order_Q1A2B3C4",
   "amount":50000,
   "currency":"INR"
}
Spring Boot returns this Order ID to the frontend.

Step 3: Frontend Opens Razorpay Checkout
The frontend now has

Order ID
It loads

checkout.js
and opens

new Razorpay(options)
Options contain

Key ID
Order ID
Amount
Customer Name
Email
A Razorpay popup appears.

+-----------------------------+
| Razorpay Secure Checkout    |
|                             |
| Pay ₹500                    |
|                             |
| UPI                          |
| Card                         |
| Net Banking                  |
| Wallet                       |
+-----------------------------+

Step 4: Customer Pays
Suppose the customer chooses

UPI
The money goes

Customer Bank
      │
      ▼
Razorpay
Razorpay processes the payment.

Step 5: Razorpay Returns Payment Details
After payment success,
Razorpay sends the frontend

{
   "razorpay_payment_id":"pay_ABC123",
   "razorpay_order_id":"order_Q1A2B3",
   "razorpay_signature":"xyz123"
}
These values prove a payment attempt occurred.

Step 6: Frontend Sends Data to Backend
The frontend immediately sends

{
   "paymentId":"pay_ABC123",
   "orderId":"order_Q1A2B3",
   "signature":"xyz123"
}
to

POST /payment/verify

Step 7: Backend Verifies the Signature
The backend uses the Razorpay Secret Key.

Order ID
+
Payment ID
+
Secret Key
↓
Generate HMAC SHA256
↓
Compare with

razorpay_signature
If both signatures match:

Payment is genuine.
Otherwise:

Reject the payment.

Step 8: Save Payment
Store the payment details.

Payment ID
Order ID
Amount
Status
Date
Customer
Example table:
Payment ID	Order ID	Amount	Status
pay_123	order_123	500	SUCCESS

Where Does the Money Go?

Customer
      │
      ▼
Customer Bank
      │
      ▼
Razorpay
      │
      ▼
Merchant Razorpay Account
      │
(Settlement after processing)
      ▼
Merchant Bank Account
Your Spring Boot application never handles the money. It only creates orders, verifies payments, and stores records.

What is a Webhook?
A webhook is an automatic notification from Razorpay to your backend.
Even if:
  • the customer closes the browser, 
  • loses internet connectivity, 
  • or never returns to your website, 
Razorpay still informs your backend about the payment status.

Customer
     │
     ▼
Pays Successfully
     │
     ▼
Razorpay
     │
     ▼
POST /webhook
     │
     ▼
Spring Boot

Why Use Webhooks?
Imagine this situation:

Customer paid ₹500
But immediately after payment:

Internet disconnected
The frontend never sends

paymentId
to your backend.
Without a webhook:

Database
Status = FAILED ❌
even though the money was deducted.
With a webhook:

Razorpay
       │
       ▼
POST /webhook
       │
       ▼
Backend updates database
Status = SUCCESS ✅
Webhooks make your system reliable because the payment provider notifies your server directly.

Complete Flow Diagram

                CUSTOMER
                    │
                    ▼
        Click "Pay Now"
                    │
                    ▼
             Frontend (HTML)
                    │
                    │ Create Order
                    ▼
        Spring Boot Backend
                    │
                    │ Razorpay API
                    ▼
             Razorpay Server
                    │
             Returns Order ID
                    ▼
              Spring Boot
                    │
                    ▼
             Frontend
                    │
                    │ Opens Checkout
                    ▼
        Razorpay Payment Window
                    │
          Customer Pays Money
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
Payment Details           Webhook Event
        │                       │
        ▼                       ▼
Frontend               Spring Boot Backend
        │                       │
Verify Payment         Update Payment Status
        │                       │
        └───────────────┬───────┘
                        ▼
                  MySQL Database
Summary
  • Frontend: Displays the payment button, requests an order, opens the Razorpay Checkout, and sends payment details back to the backend. 
  • Backend (Spring Boot): Creates Razorpay orders, verifies the payment signature, records payment information, and exposes a webhook endpoint. 
  • Razorpay: Processes the payment securely, returns payment details to the frontend, and independently sends webhook events to the backend. 
  • Webhook: Ensures your backend receives the final payment status even if the user's browser closes or the frontend never completes its callback. 
  • Database: Stores the payment lifecycle (created, authorized, captured, failed, refunded, etc.) so your application has a reliable record of every transaction.

                 simulate_webhook.sh
                        │
                        │
         Builds JSON Payload
                        │
                        ▼
          Generates HMAC SHA256 Signature
                        │
                        ▼
                 curl POST Request
                        │
      ┌─────────────────┴─────────────────┐
      │                                   │
Header: X-Razorpay-Signature     JSON Payload
Header: X-Razorpay-Event-Id
      │                                   │
      └─────────────────┬─────────────────┘
                        ▼
          Spring Boot Webhook Controller
                        │
                        ▼
          Verify Signature with Secret
                        │
             ┌──────────┴──────────┐
             │                     │
        Valid Signature      Invalid Signature
             │                     │
             ▼                     ▼
      Process Event          Return HTTP 400
             │
             ▼
      Update Payment Status
             │
             ▼
          Save to Database

Assuming your application is running on:

http://localhost:8080
here are the cURL commands to test each endpoint.

1. Create Razorpay Order
POST

curl --location 'http://localhost:8080/api/payments/create-order' \
--header 'Content-Type: application/json' \
--data '{
    "amount": 500,
    "currency": "INR",
    "receipt": "receipt_001"
}'
Postman
  • Method: POST 
  • URL: 

http://localhost:8080/api/payments/create-order
Body (raw → JSON)

{
    "amount": 500,
    "currency": "INR",
    "receipt": "receipt_001"
}

2. Verify Payment Signature
Replace these values with the ones returned by Razorpay Checkout.

curl --location 'http://localhost:8080/api/payments/verify' \
--header 'Content-Type: application/json' \
--data '{
    "razorpayOrderId":"order_xxxxxxxxx",
    "razorpayPaymentId":"pay_xxxxxxxxx",
    "razorpaySignature":"xxxxxxxxxxxxxxxx"
}'
Postman Body

{
    "razorpayOrderId":"order_xxxxxxxxx",
    "razorpayPaymentId":"pay_xxxxxxxxx",
    "razorpaySignature":"xxxxxxxxxxxxxxxx"
}

3. Get All Payments

curl --location 'http://localhost:8080/api/payments'

4. Get Payment By Database ID
Example:

curl --location 'http://localhost:8080/api/payments/1'
Replace 1 with the actual database ID.

5. Get Payment By Razorpay Payment ID
Example:

curl --location 'http://localhost:8080/api/payments/payment-id/pay_TEST987654321'
Replace with the payment ID stored in your database.

6. Get Payment By Razorpay Order ID
Example:

curl --location 'http://localhost:8080/api/payments/order/order_TEST123456789'
Replace with the Razorpay order ID stored in your database.

7. Test Webhook (Payment Captured)

curl --location 'http://localhost:8080/api/webhooks/razorpay' \
--header 'Content-Type: application/json' \
--header 'X-Razorpay-Signature: YOUR_SIGNATURE' \
--data '{
  "event":"payment.captured",
  "payload":{
    "payment":{
      "entity":{
        "id":"pay_TEST987654321",
        "order_id":"order_TEST123456789",
        "amount":50000,
        "currency":"INR",
        "status":"captured"
      }
    }
  }
}'

8. Test Webhook (Payment Failed)

curl --location 'http://localhost:8080/api/webhooks/razorpay' \
--header 'Content-Type: application/json' \
--header 'X-Razorpay-Signature: YOUR_SIGNATURE' \
--data '{
  "event":"payment.failed",
  "payload":{
    "payment":{
      "entity":{
        "id":"pay_TEST987654321",
        "order_id":"order_TEST123456789",
        "amount":50000,
        "currency":"INR",
        "status":"failed"
      }
    }
  }
}'

Recommended Testing Order
  1. Create Order

POST /api/payments/create-order
  2. Complete a payment using the returned orderId in the Razorpay Checkout. 
  3. Verify the payment signature

POST /api/payments/verify
  4. Retrieve all payments

GET /api/payments
  5. Retrieve a payment by database ID

GET /api/payments/{id}
  6. Retrieve by Razorpay Order ID

GET /api/payments/order/{orderId}
  7. Retrieve by Razorpay Payment ID

GET /api/payments/payment-id/{paymentId}
  8. Test the webhook 
    ○ Use your simulate_webhook.sh script for valid signatures, or configure a webhook in the Razorpay Test Dashboard to send real webhook events. These approaches are preferable to manually crafting webhook requests because they produce correctly signed payloads.

What is this script?
This script pretends to be Razorpay.
Normally the flow is:

Customer
      │
      ▼
Pays ₹500
      │
      ▼
Razorpay Server
      │
      ▼
POST /api/webhooks/razorpay
But during development, maybe you don't want to:
  • open Checkout 
  • make a payment 
  • wait for Razorpay 
So this script does:

Your Script
      │
      ▼
POST /api/webhooks/razorpay
It sends a fake webhook exactly like Razorpay would.

Example
Suppose your application is running.
Normally you wait for a payment.
Instead, run:

./simulate_webhook.sh captured
It sends

{
   "event":"payment.captured",
   ...
}
to

http://localhost:8080/api/webhooks/razorpay
Your application thinks
  Razorpay sent a payment.captured webhook.

What each scenario does
1. captured

./simulate_webhook.sh captured
Sends

payment.captured
Expected result

Order -> PAID

2. failed

./simulate_webhook.sh failed
Sends

payment.failed
Expected

Order -> FAILED

3. tampered

./simulate_webhook.sh tampered
Signs with

wrong_secret_here
instead of

webhook-secret-2026
Your application should reply

400 Invalid Signature
This tests your signature verification.

4. replay

./simulate_webhook.sh replay
It sends the same

eventId
twice.
Expected
First

Webhook processed
Second

Duplicate event
This tests your duplicate protection.

Do we use this after webhook is configured?
Once you have
  • ngrok 
  • Razorpay Dashboard 
  • real payments 
then NO.
Your flow becomes

Customer
↓

Razorpay
↓

Webhook
↓

Spring Boot
You no longer need the script.

When do professionals use it?
During development.
Suppose tomorrow you are implementing

refund.processed
Instead of making a real refund every time
you can simply modify

simulate_webhook.sh
to send

{
   "event":"refund.processed"
}
Testing takes

1 second
instead of

5 minutes

Should this file be committed to GitHub?
Yes.
Usually developers keep it inside

project
│
├── src
├── docs
├── scripts
│     simulate_webhook.sh
or

tools/
or

dev-tools/
It is considered a developer utility, not application code.


http://127.0.0.1:4040/  - ngrok

http://localhost:8080/index.html

https://dashboard.razorpay.com/app/website-app-settings/webhooks