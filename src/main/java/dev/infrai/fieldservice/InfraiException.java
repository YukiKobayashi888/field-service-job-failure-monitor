package dev.infrai.fieldservice;

import java.util.Map;

public final class InfraiException extends RuntimeException {
    private final int statusCode;
    private final String code;
    private final Map<String, Object> error;

    InfraiException(int statusCode, String code, Map<String, Object> error) {
        super(code + ": " + error);
        this.statusCode = statusCode;
        this.code = code;
        this.error = Map.copyOf(error);
    }

    public int statusCode() { return statusCode; }
    public String code() { return code; }
    public Map<String, Object> error() { return error; }
}
