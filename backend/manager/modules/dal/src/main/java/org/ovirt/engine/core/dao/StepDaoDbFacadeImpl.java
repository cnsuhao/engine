package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.ovirt.engine.core.common.job.ExternalSystemType;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.job.Step;
import org.ovirt.engine.core.common.job.StepEnum;
import org.ovirt.engine.core.common.utils.EnumUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class StepDaoDbFacadeImpl extends DefaultGenericDaoDbFacade<Step, Guid> implements StepDao {

    private static StepRowMapper stepRowMapper = new StepRowMapper();

    public StepDaoDbFacadeImpl() {
        super("Step");
        setProcedureNameForGetAll("GetAllSteps");
    }

    @Override
    public boolean exists(Guid id) {
        return get(id) != null;
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("step_id", id);
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(Step entity) {
        return createIdParameterMapper(entity.getId())
                .addValue("parent_step_id", entity.getParentStepId())
                .addValue("job_id", entity.getJobId())
                .addValue("step_type", EnumUtils.nameOrNull(entity.getStepType()))
                .addValue("description", entity.getDescription())
                .addValue("step_number", entity.getStepNumber())
                .addValue("status", EnumUtils.nameOrNull(entity.getStatus()))
                .addValue("start_time", entity.getStartTime())
                .addValue("end_time", entity.getEndTime())
                .addValue("correlation_id", entity.getCorrelationId())
                .addValue("external_id", entity.getExternalSystem().getId())
                .addValue("external_system_type", EnumUtils.nameOrNull(entity.getExternalSystem().getType()));
    }

    @Override
    protected ParameterizedRowMapper<Step> createEntityRowMapper() {
        return stepRowMapper;
    }

    @Override
    public List<Step> getStepsByJobId(Guid jobId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource().addValue("job_id", jobId);
        return getCallsHandler().executeReadList("GetStepsByJobId", createEntityRowMapper(), parameterSource);
    }

    @Override
    public List<Step> getStepsByParentStepId(Guid parentStepId) {
        MapSqlParameterSource parameterSource =
                getCustomMapSqlParameterSource().addValue("parent_step_id", parentStepId);
        return getCallsHandler().executeReadList("GetStepsByParentStepId", createEntityRowMapper(), parameterSource);
    }

    @Override
    public void updateJobStepsCompleted(Guid jobId, JobExecutionStatus status, Date endTime) {
        MapSqlParameterSource parameterSource =
                getCustomMapSqlParameterSource().addValue("job_id", jobId)
                        .addValue("status", status.name())
                        .addValue("end_time", endTime);
        getCallsHandler().executeModification("updateJobStepsCompleted", parameterSource);

    }

    private static class StepRowMapper implements ParameterizedRowMapper<Step> {

        @Override
        public Step mapRow(ResultSet rs, int rowNum) throws SQLException {
            Step step = new Step();
            step.setId(Guid.createGuidFromString(rs.getString("step_id")));
            step.setParentStepId(NGuid.createGuidFromString(rs.getString("parent_step_id")));
            step.setJobId(Guid.createGuidFromString(rs.getString("job_id")));
            step.setStepType(StepEnum.valueOf(rs.getString("step_type")));
            step.setDescription(rs.getString("description"));
            step.setStepNumber(rs.getInt("step_number"));
            step.setStatus(JobExecutionStatus.valueOf(rs.getString("status")));
            step.setStartTime(DbFacadeUtils.fromDate(rs.getTimestamp("start_time")));
            step.setEndTime(DbFacadeUtils.fromDate(rs.getTimestamp("end_time")));
            step.setCorrelationId(rs.getString("correlation_id"));
            step.getExternalSystem().setId(NGuid.createGuidFromString(rs.getString("external_id")));
            step.getExternalSystem().setType(ExternalSystemType.safeValueOf(rs.getString("external_system_type")));
            return step;
        }
    }
}
