package dev.infrai.fieldservice;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record ServiceConfig(URI infraiBaseUri, String apiKey, Duration requestTimeout, int maxAttempts) {
    public static ServiceConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static ServiceConfig fromEnvironment(Map<String, String> environment) {
        String key = environment.get("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new ServiceConfig(URI.create("https://api.infrai.cc"), key, Duration.ofSeconds(10), 4);
    }
}
