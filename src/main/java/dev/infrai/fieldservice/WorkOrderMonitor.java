package dev.infrai.fieldservice;

import static dev.infrai.fieldservice.WorkOrder.DispatchStatus.DISPATCHED;
import static dev.infrai.fieldservice.WorkOrder.PhotoKind.COMPLETION;

public final class WorkOrderMonitor {
    private final FailureSink failures;

    public WorkOrderMonitor(FailureSink failures) { this.failures = failures; }

    public FollowUp inspect(WorkOrder order) {
        boolean hasCompletionPhoto = order.photos().stream().anyMatch(photo -> photo.kind() == COMPLETION);
        if (order.dispatchStatus() == DISPATCHED && !hasCompletionPhoto) {
            failures.capture(order, "MissingCompletionPhoto: dispatched work requires completion evidence");
            return new FollowUp(order.workOrderId(), order.technicianId(), Action.CONTACT_TECHNICIAN);
        }
        return new FollowUp(order.workOrderId(), order.technicianId(), Action.NONE);
    }

    public record FollowUp(String workOrderId, String technicianId, Action action) {}
    public enum Action { NONE, CONTACT_TECHNICIAN }
}
