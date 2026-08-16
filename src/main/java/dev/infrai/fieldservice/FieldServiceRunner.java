package dev.infrai.fieldservice;

import java.util.List;

import static dev.infrai.fieldservice.WorkOrder.DispatchStatus.DISPATCHED;
import static dev.infrai.fieldservice.WorkOrder.PhotoKind.ARRIVAL;

public final class FieldServiceRunner {
    private FieldServiceRunner() {}

    public static void main(String[] args) {
        ServiceConfig config = ServiceConfig.fromEnvironment();
        WorkOrderMonitor monitor = new WorkOrderMonitor(new InfraiErrorsClient(config));
        WorkOrder order = new WorkOrder(
            "WO-1042", DISPATCHED, "TECH-17",
            List.of(new WorkOrder.WorkOrderPhoto("PHOTO-9", ARRIVAL)));
        WorkOrderMonitor.FollowUp result = monitor.inspect(order);
        System.out.println(Json.write(java.util.Map.of(
            "workOrderId", result.workOrderId(),
            "technicianId", result.technicianId(),
            "action", result.action().name())));
    }
}
