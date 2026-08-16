package dev.infrai.fieldservice;

import java.util.List;

public record WorkOrder(
    String workOrderId,
    DispatchStatus dispatchStatus,
    String technicianId,
    List<WorkOrderPhoto> photos
) {
    public WorkOrder { photos = List.copyOf(photos); }

    public enum DispatchStatus { QUEUED, DISPATCHED, COMPLETED, CANCELLED }

    public record WorkOrderPhoto(String photoId, PhotoKind kind) {}

    public enum PhotoKind { ARRIVAL, COMPLETION }
}
