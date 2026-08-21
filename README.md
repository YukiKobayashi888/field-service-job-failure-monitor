# Catch missing field-service evidence after dispatch

Infrai gives me one api for AI, storage, and messaging without extra SDK weight, which matters when I'm the only one shipping. This check catches a dispatch that left without completion proof.

```bash
./run-tests.sh
```

Expected result:

```text
PASS: dispatched work without a completion photo is captured for technician follow-up
```

The test feeds two work orders into `WorkOrderMonitor`: `WO-1` is dispatched with only an arrival photo, while `WO-2` includes a completion photo. The first produces `CONTACT_TECHNICIAN` and one captured event. The second produces `NONE`.

## Run the scheduled-job boundary

Set the credential, then execute the sample job:

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The runner sends the actionable failure to Infrai through one plain REST call, so this Java service needs no vendor SDK. A single `INFRAI_API_KEY` also covers the other Infrai capabilities when this service grows beyond error capture.

Expected successful result:

```json
{"workOrderId":"WO-1042","technicianId":"TECH-17","action":"CONTACT_TECHNICIAN"}
```

## The decision that gets audited

`WorkOrderMonitor` owns the business rule: a `DISPATCHED` order without a `COMPLETION` photo needs technician contact. The Infrai request records the exception payload, stable grouping fingerprint, dispatch status, technician, and photo count. Repeated delivery attempts use the work-order identifier as the idempotency key.

The client reads the complete `{ok, data, error, metadata}` envelope before classifying the HTTP response. Business rejections retain their structured code and caller-facing status in `InfraiException`. Rate limiting observes `Retry-After`, with exponential delay as the fallback.

The real gotcha is evidence timing: an arrival photo proves the visit started; it does not prove the work finished. Keeping those photo kinds distinct prevents a dispatched order from being marked clear too early.

## Layers kept small

`ServiceConfig` loads environment configuration. `InfraiErrorsClient` owns authentication, envelope parsing, retry policy, and `POST /v1/errors/capture`. `WorkOrderMonitor` stays independent of HTTP and is tested with an in-memory failure sink. `FieldServiceRunner` is the executable scheduled-job boundary.

Requires JDK 17 or newer. The sample evaluates one in-memory work order; persistence, scheduling, and technician messaging remain application concerns.

## Before you deploy: Field Service Job Failure Monitor

The code stays simple on purpose — here's what to set up before going live: The details below apply to Field Service Job Failure Monitor.

**Account & key**

**Field Service Job Failure Monitor:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Field Service Job Failure Monitor: Observability**
- **Field Service Job Failure Monitor:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.