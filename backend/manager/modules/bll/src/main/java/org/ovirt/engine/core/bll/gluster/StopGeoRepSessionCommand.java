package org.ovirt.engine.core.bll.gluster;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeGeoRepSessionParameters;
import org.ovirt.engine.core.common.businessentities.gluster.GeoRepSessionStatus;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeGeoRepSessionVDSParameters;

/**
 * BLL command to stop a geo-replication session
 */
@NonTransactiveCommandAttribute
public class StopGeoRepSessionCommand extends GeoRepSessionCommandBase<GlusterVolumeGeoRepSessionParameters> {

    public StopGeoRepSessionCommand(GlusterVolumeGeoRepSessionParameters params) {
        super(params);
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Execution).withWait(false);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__STOP);
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        if (getGeoRepSession().getStatus().equals(GeoRepSessionStatus.STOPPED)) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_GEOREP_SESSION_STOPPED);
        }

        return true;
    }

    @Override
    protected void executeCommand() {
        VDSReturnValue returnValue =
                runVdsCommand(
                        VDSCommandType.StopGlusterVolumeGeoRepSession,
                        new GlusterVolumeGeoRepSessionVDSParameters(upServer.getId(),
                                getGeoRepSession().getMasterVolumeName(), getGeoRepSession().getSlaveHostName(),
                                getGeoRepSession().getSlaveVolumeName(), getParameters().isForce()));

        setSucceeded(returnValue.getSucceeded());
        if (getSucceeded()) {
            getGeoRepSession().setStatus(GeoRepSessionStatus.STOPPED);
            getGlusterGeoRepDao().updateSession(getGeoRepSession());
        } else {
            handleVdsError(AuditLogType.GEOREP_SESSION_STOP_FAILED, returnValue.getVdsError().getMessage());
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.GEOREP_SESSION_STOP;
        } else {
            return errorType == null ? AuditLogType.GEOREP_SESSION_STOP_FAILED : errorType;
        }
    }

}
