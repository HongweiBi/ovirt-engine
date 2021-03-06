package org.ovirt.engine.core.common.businessentities.gluster;

public enum GlusterSnapshotStatus {
    STARTED,
    STOPPED,
    UNKNOWN;

    public static GlusterSnapshotStatus from(String status) {
        for (GlusterSnapshotStatus snapshotStatus : values()) {
            if (snapshotStatus.name().equals(status)) {
                return snapshotStatus;
            }
        }

        return GlusterSnapshotStatus.UNKNOWN;
    }
}
