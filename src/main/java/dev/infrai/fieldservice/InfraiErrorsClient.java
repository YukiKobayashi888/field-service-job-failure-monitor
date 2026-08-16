package dev.infrai.fieldservice;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class InfraiErrorsClient implements FailureSink {
    private final ServiceConfig config;
    private final HttpClient http;

    public InfraiErrorsClient(ServiceConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build());
    }

    InfraiErrorsClient(ServiceConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    /** infrai.errors.capture maps to POST /v1/errors/capture. */
    @Override
    public void capture(WorkOrder order, String exceptionPayload) {
        Map<String, Object> payload = Map.of(
            "title", "Dispatched work order is missing completion photos",
            "message", "Work order " + order.workOrderId() + " requires technician follow-up",
            "exception", exceptionPayload,
            "level", "error",
            "fingerprint", List.of("field-service", "missing-completion-photo"),
            "tags", Map.of("dispatch_status", order.dispatchStatus().name()),
            "context", Map.of(
                "work_order_id", order.workOrderId(),
                "technician_id", order.technicianId(),
                "photo_count", order.photos().size()));
        send(payload, "work-order-photo:" + order.workOrderId());
    }

    private void send(Map<String, Object> payload, String idempotencyKey) {
        for (int attempt = 0; attempt < config.maxAttempts(); attempt++) {
            HttpRequest request = HttpRequest.newBuilder(config.infraiBaseUri().resolve("/v1/errors/capture"))
                .timeout(config.requestTimeout())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .method("POST", HttpRequest.BodyPublishers.ofString(Json.write(payload)))
                .build();
            HttpResponse<String> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new IllegalStateException("Infrai transport failed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request interrupted", e);
            }

            Map<String, Object> envelope = Json.readObject(response.body());
            if (response.statusCode() == 429 && attempt + 1 < config.maxAttempts()) {
                pause(retryDelay(response, attempt));
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) throw envelopeError(response.statusCode(), envelope);
            if (response.statusCode() >= 500) throw new IllegalStateException("Infrai server response: " + response.statusCode());
            return;
        }
        throw new IllegalStateException("Infrai retry budget exhausted");
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
            .map(value -> {
                try { return Duration.ofSeconds(Long.parseLong(value)); }
                catch (NumberFormatException ignored) { return Duration.ofMillis(250L << attempt); }
            })
            .orElse(Duration.ofMillis(250L << attempt));
    }

    private static void pause(Duration delay) {
        try { Thread.sleep(delay.toMillis()); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }

    private static InfraiException envelopeError(int status, Map<String, Object> envelope) {
        Object raw = envelope.get("error");
        @SuppressWarnings("unchecked")
        Map<String, Object> error = raw instanceof Map<?, ?> ? (Map<String, Object>) raw : Map.of("message", String.valueOf(raw));
        return new InfraiException(status, String.valueOf(error.getOrDefault("code", "INFRAI_REJECTION")), error);
    }
}
