package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.ovirt.engine.core.common.businessentities.AutoNumaBalanceStatus;
import org.ovirt.engine.core.common.businessentities.KdumpStatus;
import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VdsDynamic;
import org.ovirt.engine.core.common.businessentities.VdsTransparentHugePagesState;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.RpmVersion;
import org.ovirt.engine.core.utils.serialization.json.JsonObjectDeserializer;
import org.ovirt.engine.core.utils.serialization.json.JsonObjectSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * <code>VdsDAODbFacadeImpl</code> provides an implementation of {@link VdsDAO} that uses previously written code from
 * {@link org.ovirt.engine.core.dal.dbbroker.DbFacade}.
 *
 *
 */
public class VdsDynamicDAODbFacadeImpl extends MassOperationsGenericDaoDbFacade<VdsDynamic, Guid> implements VdsDynamicDAO {

    private static final Logger log = LoggerFactory.getLogger(VdsDynamicDAODbFacadeImpl.class);

    public VdsDynamicDAODbFacadeImpl() {
        super("VdsDynamic");
    }

    private static final class VdcDynamicRowMapper implements RowMapper<VdsDynamic> {
        public static final VdcDynamicRowMapper instance = new VdcDynamicRowMapper();

        @Override
        public VdsDynamic mapRow(ResultSet rs, int rowNum) throws SQLException {
            VdsDynamic entity = new VdsDynamic();
            entity.setCpuCores((Integer) rs.getObject("cpu_cores"));
            entity.setCpuThreads((Integer) rs.getObject("cpu_threads"));
            entity.setCpuModel(rs.getString("cpu_model"));
            entity.setOnlineCpus(rs.getString("online_cpus"));
            entity.setCpuSpeedMh(rs.getDouble("cpu_speed_mh"));
            entity.setIfTotalSpeed(rs.getString("if_total_speed"));
            entity.setKvmEnabled((Boolean) rs.getObject("kvm_enabled"));
            entity.setMemCommited((Integer) rs.getObject("mem_commited"));
            entity.setPhysicalMemMb((Integer) rs
                    .getObject("physical_mem_mb"));
            entity.setStatus(VDSStatus.forValue(rs.getInt("status")));
            entity.setId(getGuidDefaultEmpty(rs, "vds_id"));
            entity.setVmActive((Integer) rs.getObject("vm_active"));
            entity.setVmCount(rs.getInt("vm_count"));
            entity.setVmsCoresCount(rs.getInt("vms_cores_count"));
            entity.setVmMigrating((Integer) rs.getObject("vm_migrating"));
            entity.setReservedMem((Integer) rs.getObject("reserved_mem"));
            entity.setGuestOverhead(rs.getInt("guest_overhead"));
            entity.setSoftwareVersion(rs.getString("software_version"));
            entity.setVersionName(rs.getString("version_name"));
            entity.setVersion(new RpmVersion(rs.getString("rpm_version")));
            entity.setBuildName(rs.getString("build_name"));
            entity.setPreviousStatus(VDSStatus.forValue(rs
                    .getInt("previous_status")));
            entity.setCpuFlags(rs.getString("cpu_flags"));
            entity.setPendingVcpusCount((Integer) rs
                    .getObject("pending_vcpus_count"));
            entity.setPendingVmemSize(rs.getInt("pending_vmem_size"));
            entity.setCpuSockets((Integer) rs.getObject("cpu_sockets"));
            entity.setNetConfigDirty((Boolean) rs
                    .getObject("net_config_dirty"));
            entity.setSupportedClusterLevels(rs
                    .getString("supported_cluster_levels"));
            entity.setSupportedEngines(rs.getString("supported_engines"));
            entity.setHostOs(rs.getString("host_os"));
            entity.setKvmVersion(rs.getString("kvm_version"));
            entity.setLibvirtVersion(new RpmVersion(rs.getString("libvirt_version")));
            entity.setSpiceVersion(rs.getString("spice_version"));
            entity.setGlusterVersion(new RpmVersion(rs.getString("gluster_version")));
            entity.setKernelVersion(rs.getString("kernel_version"));
            entity.setIScsiInitiatorName(rs
                    .getString("iscsi_initiator_name"));
            entity.setTransparentHugePagesState(VdsTransparentHugePagesState
                    .forValue(rs.getInt("transparent_hugepages_state")));
            entity.setHooksStr(rs.getString("hooks"));
            entity.setNonOperationalReason(NonOperationalReason.forValue(rs
                    .getInt("non_operational_reason")));
            entity.setHardwareManufacturer(rs.getString("hw_manufacturer"));
            entity.setHardwareProductName(rs.getString("hw_product_name"));
            entity.setHardwareVersion(rs.getString("hw_version"));
            entity.setHardwareSerialNumber(rs.getString("hw_serial_number"));
            entity.setHardwareUUID(rs.getString("hw_uuid"));
            entity.setHardwareFamily(rs.getString("hw_family"));
            entity.setHBAs(new JsonObjectDeserializer().deserialize(rs.getString("hbas"), HashMap.class));
            entity.setPowerManagementControlledByPolicy(rs.getBoolean("controlled_by_pm_policy"));
            entity.setKdumpStatus(KdumpStatus.valueOfNumber(rs.getInt("kdump_status")));
            entity.setSELinuxEnforceMode((Integer) rs.getObject("selinux_enforce_mode"));
            entity.setAutoNumaBalancing(AutoNumaBalanceStatus.forValue(rs.getInt("auto_numa_balancing")));
            entity.setNumaSupport(rs.getBoolean("is_numa_supported"));
            entity.setLiveSnapshotSupport(rs.getBoolean("is_live_snapshot_supported"));
            entity.setLiveMergeSupport(rs.getBoolean("is_live_merge_supported"));
            entity.setSupportedEmulatedMachines(rs.getString("supported_emulated_machines"));
            entity.getSupportedRngSources().addAll(VmRngDevice.csvToSourcesSet(rs.getString("supported_rng_sources")));
            return entity;
        }
    }

    @Override
    public VdsDynamic get(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vds_id", id);

        return getCallsHandler().executeRead("GetVdsDynamicByVdsId", VdcDynamicRowMapper.instance, parameterSource);
    }

    @Override
    public void save(VdsDynamic vds) {
        getCallsHandler().executeModification("InsertVdsDynamic", createFullParametersMapperForSave(vds));
    }

    @Override
    public void update(VdsDynamic vds) {
        getCallsHandler().executeModification("UpdateVdsDynamic", createFullParametersMapper(vds));
    }

    @Override
    public void remove(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vds_id", id);

        getCallsHandler().executeModification("DeleteVdsDynamic", parameterSource);
    }

    @Override
    public List<VdsDynamic> getAll() {
        throw new NotImplementedException();
    }

    @Override
    public void updateStatus(Guid id, VDSStatus status) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vds_guid", id)
                .addValue("status", status);

        getCallsHandler().executeModification("UpdateVdsDynamicStatus", parameterSource);
    }

    @Override
    public void updateNetConfigDirty(Guid id, Boolean netConfigDirty) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vds_guid", id)
                .addValue("net_config_dirty", netConfigDirty);

        getCallsHandler().executeModification("UpdateVdsDynamicNetConfigDirty", parameterSource);
    }

    @Override
    public void updatePartialVdsDynamicCalc(Guid id,
            int vmCount,
            int pendingVcpusCount,
            int pendingVmemSize,
            int memCommited,
            int vmsCoresCount) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vds_guid", id)
                .addValue("vmCount", vmCount)
                .addValue("pendingVcpusCount", pendingVcpusCount)
                .addValue("pendingVmemSize", pendingVmemSize)
                .addValue("memCommited", memCommited)
                .addValue("vmsCoresCount", vmsCoresCount);

        getCallsHandler().executeModification("UpdatePartialVdsDynamicCalc", parameterSource);

    }

    @Override
    public void updateVdsDynamicPowerManagementPolicyFlag(Guid id,
                                                          boolean controlledByPmPolicy) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vds_id", id)
                .addValue("controlled_by_pm_policy", controlledByPmPolicy);

        getCallsHandler().executeModification("UpdateVdsDynamicPowerManagementPolicyFlag", parameterSource);

    }

    private MapSqlParameterSource createFullParametersMapperForSave(VdsDynamic vds) {
        MapSqlParameterSource parameterSource = createFullParametersMapper(vds);
        parameterSource.addValue("controlled_by_pm_policy", vds.isPowerManagementControlledByPolicy());
        return parameterSource;
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(VdsDynamic vds) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cpu_cores", vds.getCpuCores())
                .addValue("cpu_threads", vds.getCpuThreads())
                .addValue("cpu_model", vds.getCpuModel())
                .addValue("online_cpus", vds.getOnlineCpus())
                .addValue("cpu_speed_mh", vds.getCpuSpeedMh())
                .addValue("if_total_speed", vds.getIfTotalSpeed())
                .addValue("kvm_enabled", vds.getKvmEnabled())
                .addValue("mem_commited", vds.getMemCommited())
                .addValue("physical_mem_mb", vds.getPhysicalMemMb())
                .addValue("status", vds.getStatus())
                .addValue("vds_id", vds.getId())
                .addValue("vm_active", vds.getVmActive())
                .addValue("vm_count", vds.getVmCount())
                .addValue("vms_cores_count", vds.getVmsCoresCount())
                .addValue("vm_migrating", vds.getVmMigrating())
                .addValue("reserved_mem", vds.getReservedMem())
                .addValue("guest_overhead", vds.getGuestOverhead())
                .addValue("rpm_version", vds.getVersion().getRpmName())
                .addValue("software_version", vds.getSoftwareVersion())
                .addValue("version_name", vds.getVersionName())
                .addValue("build_name", vds.getBuildName())
                .addValue("previous_status", vds.getPreviousStatus())
                .addValue("cpu_flags", vds.getCpuFlags())
                .addValue("pending_vcpus_count", vds.getPendingVcpusCount())
                .addValue("pending_vmem_size", vds.getPendingVmemSize())
                .addValue("cpu_sockets", vds.getCpuSockets())
                .addValue("net_config_dirty", vds.getNetConfigDirty())
                .addValue("supported_cluster_levels",
                        vds.getSupportedClusterLevels())
                .addValue("supported_engines", vds.getSupportedEngines())
                .addValue("host_os", vds.getHostOs())
                .addValue("kvm_version", vds.getKvmVersion())
                .addValue("libvirt_version", vds.getLibvirtVersion().getRpmName())
                .addValue("spice_version", vds.getSpiceVersion())
                .addValue("gluster_version", vds.getGlusterVersion().getRpmName())
                .addValue("kernel_version", vds.getKernelVersion())
                .addValue("iscsi_initiator_name", vds.getIScsiInitiatorName())
                .addValue("transparent_hugepages_state",
                        vds.getTransparentHugePagesState().getValue())
                .addValue("hooks", vds.getHooksStr())
                .addValue("non_operational_reason",
                        vds.getNonOperationalReason().getValue())
                .addValue("hw_manufacturer", vds.getHardwareManufacturer())
                .addValue("hw_product_name", vds.getHardwareProductName())
                .addValue("hw_version", vds.getHardwareVersion())
                .addValue("hw_serial_number", vds.getHardwareSerialNumber())
                .addValue("hw_uuid", vds.getHardwareUUID())
                .addValue("hw_family", vds.getHardwareFamily())
                .addValue("hbas", new JsonObjectSerializer().serialize(vds.getHBAs()))
                .addValue("supported_emulated_machines", vds.getSupportedEmulatedMachines())
                .addValue("kdump_status", vds.getKdumpStatus().getAsNumber())
                .addValue("selinux_enforce_mode", (vds.getSELinuxEnforceMode() != null)
                                                  ? vds.getSELinuxEnforceMode().toInt()
                                                  : null)
                .addValue("auto_numa_balancing", vds.getAutoNumaBalancing().getValue())
                .addValue("is_numa_supported", vds.isNumaSupport())
                .addValue("supported_rng_sources", VmRngDevice.sourcesToCsv(vds.getSupportedRngSources()))
                .addValue("supported_emulated_machines", vds.getSupportedEmulatedMachines())
                .addValue("is_live_snapshot_supported", vds.getLiveSnapshotSupport())
                .addValue("is_live_merge_supported", vds.getLiveMergeSupport());

        return parameterSource;
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("vds_id", id);
    }

    @Override
    protected RowMapper<VdsDynamic> createEntityRowMapper() {
        return VdcDynamicRowMapper.instance;
    }

    @Override
    public void updateIfNeeded(VdsDynamic vdsDynamic) {
        VdsDynamic dbData = get(vdsDynamic.getId());
        if (!dbData.equals(vdsDynamic)) {
            update(vdsDynamic);
        } else {
            log.debug("Ignored an unneeded update of VdsDynamic");
        }
    }

    @Override
    public void updateCpuFlags(Guid id, String cpuFlags) {
        getCallsHandler().executeModification(
                "updateCpuFlags",
                getCustomMapSqlParameterSource()
                        .addValue("vds_id", id)
                        .addValue("cpu_flags", cpuFlags));
    }
}
