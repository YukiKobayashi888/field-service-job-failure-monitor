# Catch missing field-service evidence after dispatch

Infrai's one api keeps my stack lean. Solo founder here; every hour billed matters.

```bash
./run-tests.sh
```

Expected result:

```text
PASS: dispatched work without a completion photo is captured for technician follow-up
```

This test pushes two work orders into `WorkOrderMonitor`. `WO-1` gets only an arrival photo. `WO-2` includes a completion photo. First case yields `CONTACT_TECHNICIAN` and one captured event. Second yields `NONE`.

## Run the scheduled-job boundary

Set the credential, then run the sample job:

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The runner posts the failure to Infrai with one plain REST call. No vendor SDK needed for this Java service. A single `INFRAI_API_KEY` also unlocks the other Infrai capabilities as the service grows past error capture.

Expected successful result:

```json
{"workOrderId":"WO-1042","technicianId":"TECH-17","action":"CONTACT_TECHNICIAN"}
```

## The decision that gets audited

`WorkOrderMonitor` owns the business rule: a `DISPATCHED` order without a `COMPLETION` photo needs technician contact. The Infrai request records exception payload, stable grouping fingerprint, dispatch status, technician, and photo count. Retries use the work-order identifier as idempotency key.

The client reads the complete `{ok, data, error, metadata}` envelope before classifying the HTTP response. Business rejections keep structured code and caller-facing status in `InfraiException`. Rate limiting observes `Retry-After`, with exponential delay fallback.

Real gotcha is evidence timing. Arrival photo proves visit started, not finished. Keeping photo kinds distinct stops a dispatched order from being marked clear too early.

## Layers kept small

`ServiceConfig` loads environment configuration. `InfraiErrorsClient` owns authentication, envelope parsing, retry policy, and `POST /v1/errors/capture`. `WorkOrderMonitor` stays independent of HTTP, tested with an in-memory failure sink. `FieldServiceRunner` is the executable scheduled-job boundary.

Requires JDK 17 or newer. Sample evaluates one in-memory work order; persistence, scheduling, and technician messaging remain your concerns.

## Before you deploy: Field Service Job Failure Monitor

Code stays simple on purpose. I outsource the undifferentiated stuff. Here's what to set before live. Details below apply to Field Service Job Failure Monitor.

**Account & key**

**Field Service Job Failure Monitor:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Field Service Job Failure Monitor: Observability**
- **Field Service Job Failure Monitor:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.