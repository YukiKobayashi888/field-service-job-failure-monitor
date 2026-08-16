package dev.infrai.fieldservice;

@FunctionalInterface
public interface FailureSink {
    void capture(WorkOrder order, String exceptionPayload);
}
