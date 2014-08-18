package org.ovirt.engine.core.dao.qos;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.ovirt.engine.core.common.businessentities.qos.QosType;
import org.ovirt.engine.core.common.businessentities.qos.StorageQos;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class StorageQosDaoDbFacadeImpl extends QosBaseDaoFacadeImpl<StorageQos> implements StorageQosDao {
    private static final StorageDaoDbFacadaeImplMapper MAPPER = new StorageDaoDbFacadaeImplMapper();

    public StorageQosDaoDbFacadeImpl() {
        super(QosType.STORAGE);
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(StorageQos obj) {
        MapSqlParameterSource map = super.createFullParametersMapper(obj);
        map.addValue("max_throughput", obj.getMaxThroughput());
        map.addValue("max_read_throughput", obj.getMaxReadThroughput());
        map.addValue("max_write_throughput", obj.getMaxWriteThroughput());
        map.addValue("max_iops", obj.getMaxIops());
        map.addValue("max_read_iops", obj.getMaxReadIops());
        map.addValue("max_write_iops", obj.getMaxWriteIops());

        return map;
    }

    @Override
    protected RowMapper<StorageQos> createEntityRowMapper() {
        return MAPPER;
    }

    protected static class StorageDaoDbFacadaeImplMapper extends QosBaseDaoFacadaeImplMapper<StorageQos> {

        @Override
        protected StorageQos createQosEntity(ResultSet rs) throws SQLException {
            StorageQos entity = new StorageQos();
            entity.setMaxThroughput(getInteger(rs, "max_throughput"));
            entity.setMaxReadThroughput(getInteger(rs, "max_read_throughput"));
            entity.setMaxWriteThroughput(getInteger(rs, "max_write_throughput"));
            entity.setMaxIops(getInteger(rs, "max_iops"));
            entity.setMaxReadIops(getInteger(rs, "max_read_iops"));
            entity.setMaxWriteIops(getInteger(rs, "max_write_iops"));
            return entity;
        }
    }

}