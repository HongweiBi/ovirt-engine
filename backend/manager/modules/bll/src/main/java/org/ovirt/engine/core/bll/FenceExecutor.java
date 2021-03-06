package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.FenceActionType;
import org.ovirt.engine.core.common.businessentities.FenceStatusReturnValue;
import org.ovirt.engine.core.common.businessentities.FencingPolicy;
import org.ovirt.engine.core.common.businessentities.FenceAgent;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VdsSpmStatus;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.vdscommands.FenceVdsVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.SpmStopVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSFenceReturnValue;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.pm.VdsFenceOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FenceExecutor {
    private static final Logger log = LoggerFactory.getLogger(FenceExecutor.class);

    private final VDS _vds;
    private FencingPolicy fencingPolicy;
    private FenceProxyLocator proxyLocator;
    private VdsArchitectureHelper architectureHelper;

    public FenceExecutor(VDS vds) {
        this(vds, null);
    }

    public FenceExecutor(VDS vds, FencingPolicy fencingPolicy) {
        // TODO remove if block after UI patch that should set also cluster & proxy preferences in GetNewVdsFenceStatusParameters
        if (! vds.getId().equals(Guid.Empty)) {
            VDS dbVds =  DbFacade.getInstance().getVdsDao().get(vds.getId());
            if (vds.getVdsGroupId() == null) {
                vds.setVdsGroupId(dbVds.getVdsGroupId());
            }
            if (vds.getPmProxyPreferences() == null) {
                vds.setPmProxyPreferences(dbVds.getPmProxyPreferences());
            }
        }
        this._vds = vds;
        this.fencingPolicy = fencingPolicy;
        this.proxyLocator = new FenceProxyLocator(_vds, fencingPolicy);
        this.architectureHelper = new VdsArchitectureHelper();
    }

    /**
     * Use all fencing agents of this host sequentially, until one succeeds, to check the status of the host.
     *
     */
    public VDSFenceReturnValue checkStatus() {
        VDSFenceReturnValue returnValue = null;
        VDS proxyHost = proxyLocator.findProxyHost();
        if (proxyHost == null) {
            returnValue = proxyNotFound();
        } else {
            for (FenceAgent agent : _vds.getFenceAgents()) {
                returnValue = fence(FenceActionType.Status, agent, proxyHost);
                if (returnValue.getSucceeded()) {
                    returnValue.setProxyHostUsed(proxyHost);
                    returnValue.setFenceAgentUsed(agent);
                    break;
                }
            }
        }
        if (returnValue == null) {
            returnValue = new VDSFenceReturnValue();
            returnValue.setSucceeded(false);
            returnValue.setExceptionString("No fence-agents found for host " + _vds.getName());
        }
        return returnValue;
    }


    public VDSFenceReturnValue fence(FenceActionType action, FenceAgent agent) {
        boolean withRetries = action != FenceActionType.Status; // for status check, no retries on proxy-host selection.
        VDS proxyHost = proxyLocator.findProxyHost(withRetries);
        if (proxyHost == null) {
            return proxyNotFound();
        } else {
            return fence(action, agent, proxyHost);
        }
    }

    private VDSFenceReturnValue proxyNotFound() {
        VDSFenceReturnValue returnValue = new VDSFenceReturnValue();
        returnValue.setSucceeded(false);
        returnValue.setExceptionString("Failed to run Power Management command on Host " + getNameOrId(_vds)
                + " no running proxy Host was found");
        return returnValue;
    }

    public VDSFenceReturnValue fence(FenceActionType action, FenceAgent agent, VDS proxyHost) {
        VDSReturnValue result = null;
        try {
            if (action == FenceActionType.Restart || action == FenceActionType.Stop) {
                stopSPM(action);
            }
            result = runFenceAction(action, agent, proxyHost);
            // if fence failed, retry with another proxy.
            if (!result.getSucceeded()) {
                log.warn("Fence operation failed with proxy host {}, trying another proxy...",
                        proxyHost.getId());
                boolean withRetries = action != FenceActionType.Status;
                VDS alternativeProxy =
                        proxyLocator.findProxyHost(withRetries, proxyHost.getId());
                if (alternativeProxy != null) {
                    result = runFenceAction(action, agent, alternativeProxy);
                } else {
                    log.warn("Failed to find other proxy to re-run failed fence operation, retrying with the same proxy...");
                    AuditLogDirector.log(getAuditParams(action, agent, proxyHost),
                            AuditLogType.FENCE_OPERATION_FAILED_USING_PROXY);
                    result = runFenceAction(action, agent, proxyHost);
                }
            }
        } catch (VdcBLLException e) {
            result = new VDSReturnValue();
            result.setReturnValue(new FenceStatusReturnValue("unknown", e.getMessage()));
            result.setExceptionString(e.getMessage());
            result.setSucceeded(false);
        }
        VDSFenceReturnValue returnVal = new VDSFenceReturnValue(result);
        returnVal.setFenceAgentUsed(agent);
        returnVal.setSucceeded(result.getSucceeded() || returnVal.isSkipped()); // skipping due to policy
        return returnVal;
    }

    private void stopSPM(FenceActionType action) {
        // skip following code in case of testing a new host status
        if (_vds.getId() != null && !_vds.getId().equals(Guid.Empty)) {
            // get the host spm status again from the database in order to test it's current state.
            VdsSpmStatus spmStatus = DbFacade.getInstance().getVdsDao().get(_vds.getId()).getSpmStatus();
            // try to stop SPM if action is Restart or Stop and the vds is SPM
            if (spmStatus != VdsSpmStatus.None) {
                getBackend().getResourceManager()
                        .RunVdsCommand(VDSCommandType.SpmStop,
                                new SpmStopVDSCommandParameters(_vds.getId(), _vds.getStoragePoolId()));
            }
        }
    }

    /**
     * Run the specified fence action.
     * @param actionType The action to run.
     * @return The result of running the fence command.
     */
    private VDSReturnValue runFenceAction(FenceActionType action, FenceAgent agent, VDS proxyHost) {
        auditFenceAction(action, agent, proxyHost);
        return getBackend().getResourceManager()
                    .RunVdsCommand(
                            VDSCommandType.FenceVds,
                        new FenceVdsVDSCommandParameters(proxyHost.getId(), _vds.getId(), agent.getIp(),
                                String.valueOf(agent.getPort()),
                                VdsFenceOptions.getRealAgent(agent.getType()),
                                agent.getUser(),
                                agent.getPassword(),
                                getOptions(agent), action, fencingPolicy));
    }

    private void auditFenceAction(FenceActionType action, FenceAgent agent, VDS proxyHost) {
        log.info("Executing <{}> Power Management command, Proxy Host:{}, "
                + "Agent:{}, Agent Type:{}, Target Host:{}, Management IP:{}, User:{}, Options:{}, Fencing policy:{}",
                action,
                getNameOrId(proxyHost),
                agent.getId(),
                VdsFenceOptions.getRealAgent(agent.getType()),
                getNameOrId(_vds),
                agent.getIp(),
                agent.getUser(),
                getOptions(agent),
                fencingPolicy);
        AuditLogableBase logable = getAuditParams(action, agent, proxyHost);
        AuditLogDirector.log(logable, AuditLogType.FENCE_USING_AGENT_AND_PROXY_HOST);
    }

    private AuditLogableBase getAuditParams(FenceActionType action, FenceAgent agent, VDS proxyHost) {
        AuditLogableBase logable = new AuditLogableBase();
        logable.addCustomValue("Action", getActionPresentTense(action));
        logable.addCustomValue("Host", getNameOrId(_vds));
        logable.addCustomValue("Agent", agent.getId() == null ? "New Agent (no ID)" : agent.getId().toString());
        logable.addCustomValue("ProxyHost", getNameOrId(proxyHost));
        logable.setVdsId(_vds.getId());
        return logable;
    }

    private String getActionPresentTense(FenceActionType action) {
        switch (action) {
        case Start:
            return "Starting";
        case Restart:
            return "Restarting";
        case Stop:
            return "Stopping";
        case Status:
            return "Checking status of";
        default:
            return "";// should never get here.
        }
    }

    private String getOptions(FenceAgent agent) {
        ArchitectureType architectureType = architectureHelper.getArchitecture(_vds.getStaticData());
        String managementOptions =
                VdsFenceOptions.getDefaultAgentOptions(agent.getType(),
                        agent.getOptions() == null ? "" : agent.getOptions(),
                        architectureType);
        return managementOptions;
    }

    /**
     * We prefer to log the host name, if it's available, but we won't query the database especially for it. So return
     * the host name if it's available, otherwise the host-ID.
     */
    private String getNameOrId(VDS host) {
        if (host.getName() != null && !host.getName().isEmpty()) {
            return host.getName();
        } else {
            return host.getId().toString();
        }
    }

    BackendInternal getBackend() {
        return Backend.getInstance();
    }

    public VdsArchitectureHelper getArchitectureHelper() {
        return architectureHelper;
    }

    public void setArchitectureHelper(VdsArchitectureHelper architectureHelper) {
        this.architectureHelper = architectureHelper;
    }

    public FenceProxyLocator getProxyLocator() {
        return proxyLocator;
    }

    public void setProxyLocator(FenceProxyLocator proxyLocator) {
        this.proxyLocator = proxyLocator;
    }
}
