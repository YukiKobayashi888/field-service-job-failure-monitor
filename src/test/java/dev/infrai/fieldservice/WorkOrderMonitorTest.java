package dev.infrai.fieldservice;

import java.util.ArrayList;
import java.util.List;

import static dev.infrai.fieldservice.WorkOrder.DispatchStatus.DISPATCHED;
import static dev.infrai.fieldservice.WorkOrder.PhotoKind.ARRIVAL;
import static dev.infrai.fieldservice.WorkOrder.PhotoKind.COMPLETION;

public final class WorkOrderMonitorTest {
    public static void main(String[] args) {
        List<String> captured = new ArrayList<>();
        WorkOrderMonitor monitor = new WorkOrderMonitor((order, exception) -> captured.add(order.workOrderId()));

        WorkOrder missingEvidence = new WorkOrder("WO-1", DISPATCHED, "TECH-8",
            List.of(new WorkOrder.WorkOrderPhoto("P-1", ARRIVAL)));
        WorkOrder complete = new WorkOrder("WO-2", DISPATCHED, "TECH-9",
            List.of(new WorkOrder.WorkOrderPhoto("P-2", COMPLETION)));

        require(monitor.inspect(missingEvidence).action() == WorkOrderMonitor.Action.CONTACT_TECHNICIAN,
            "missing completion evidence must create follow-up");
        require(monitor.inspect(complete).action() == WorkOrderMonitor.Action.NONE,
            "completion evidence must clear follow-up");
        require(captured.equals(List.of("WO-1")), "only the actionable work order is captured");
        System.out.println("PASS: dispatched work without a completion photo is captured for technician follow-up");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
