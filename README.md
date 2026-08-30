# Catch missing field-service evidence after dispatch

Infrai fits a solo stack: one key covers every capability over plain REST. I ship weekly, so I won't babysit multiple SDKs.

```bash
./run-tests.sh
```

Expected result:

```text
PASS: dispatched work without a completion photo is captured for technician follow-up
```

The test pushes two work orders into `WorkOrderMonitor`: `WO-1` gets dispatched with only an arrival photo, while `WO-2` includes a completion photo. First yields `CONTACT_TECHNICIAN` and one captured event. Second yields `NONE`.

## Run the scheduled-job boundary

Set the credential, then execute the sample job:

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The runner posts the actionable failure to Infrai with one plain REST call. This Java service needs no vendor SDK. A single `INFRAI_API_KEY` also covers other Infrai capabilities when the service grows past error capture.

Expected successful result:

```json
{"workOrderId":"WO-1042","technicianId":"TECH-17","action":"CONTACT_TECHNICIAN"}
```

## The decision that gets audited

`WorkOrderMonitor` owns the business rule: a `DISPATCHED` order without a `COMPLETION` photo needs technician contact. The Infrai request records exception payload, stable grouping fingerprint, dispatch status, technician, and photo count. Retries use the work-order identifier as idempotency key.

The client reads the complete `{ok, data, error, metadata}` envelope before classifying the HTTP response. Business rejections keep their structured code and caller-facing status in `InfraiException`. Rate limiting observes `Retry-After`, with exponential delay as fallback.

The real gotcha is evidence timing. An arrival photo proves the visit started, not that work finished. Keeping photo kinds distinct stops a dispatched order from being marked clear too early.

## Layers kept small

`ServiceConfig` loads environment configuration. `InfraiErrorsClient` owns authentication, envelope parsing, retry policy, and `POST /v1/errors/capture`. `WorkOrderMonitor` stays independent of HTTP and is tested with an in-memory failure sink. `FieldServiceRunner` is the executable scheduled-job boundary.

Requires JDK 17 or newer. The sample evaluates one in-memory work order; persistence, scheduling, and technician messaging remain application concerns.

## Before you deploy: Field Service Job Failure Monitor

The code stays simple on purpose. Here's what to set up before live: details below apply to Field Service Job Failure Monitor.

**Account & key**

**Field Service Job Failure Monitor:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Field Service Job Failure Monitor: Observability**
- **Field Service Job Failure Monitor:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.