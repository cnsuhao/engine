package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.queries.VdsIdParametersBase;
import org.ovirt.engine.core.common.vdscommands.AddVdsVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.RemoveVdsVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.SetVdsStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Regex;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AlertDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.ThreadUtils;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;

public abstract class VdsCommand<T extends VdsActionParameters> extends CommandBase<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected VdsCommand(Guid commandId) {
        super(commandId);
    }

    public VdsCommand(T parameters) {
        super(parameters);
        setVdsId(parameters.getVdsId());
    }

    protected void InitializeVds() {
        Backend.getInstance()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.RemoveVds,
                        new RemoveVdsVDSCommandParameters(getVdsId()));
        Backend.getInstance().getResourceManager()
                .RunVdsCommand(VDSCommandType.AddVds, new AddVdsVDSCommandParameters(getVdsId()));
    }

    @Override
    protected String getDescription() {
        return getVdsName();
    }

    protected void RunSleepOnReboot() {
        ThreadPoolUtil.execute(new Runnable() {
            @Override
            public void run() {
                SleepOnReboot();
            }
        });
    }

    private void SleepOnReboot() {
        int sleepTimeInSec = Config.<Integer> GetValue(ConfigValues.ServerRebootTimeout);
        log.infoFormat("Waiting {0} seconds, for server to finish reboot process.",
                sleepTimeInSec);
        ThreadUtils.sleep(sleepTimeInSec * 1000);
        Backend.getInstance()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.SetVdsStatus,
                        new SetVdsStatusVDSCommandParameters(getVdsId(), VDSStatus.NonResponsive));
    }

    /**
     * Alerts the specified log type.
     *
     * @param logType
     *            Type of the log.
     */
    private void Alert(AuditLogType logType) {
        Alert(logType, null);
    }

    /**
     * Alerts the specified log type.
     *
     * @param logType
     *            Type of the log.
     * @param operation
     *            Operation name.
     */
    private void Alert(AuditLogType logType, String operation) {
        AuditLogableBase alert = new AuditLogableBase();
        alert.setVdsId(getVds().getId());
        String op = (operation == null) ? getActionType().name(): operation;
        alert.AddCustomValue("Operation",op);
        AlertDirector.Alert(alert, logType);
    }

    /**
     * Alerts if power management not configured.
     *
     * @param vdsStatic
     *            The VDS static.
     */
    protected void AlertIfPowerManagementNotConfigured(VdsStatic vdsStatic) {
        if (getVdsGroup() != null && !getVdsGroup().supportsVirtService()) {
            return;
        }

        if (!vdsStatic.getpm_enabled() || StringUtils.isEmpty(vdsStatic.getpm_type())) {
            Alert(AuditLogType.VDS_ALERT_FENCE_IS_NOT_CONFIGURED);
            // remove any test failure alerts
            AlertDirector.RemoveVdsAlert(vdsStatic.getId(),
                    AuditLogType.VDS_ALERT_FENCE_TEST_FAILED);
        } else {
            AlertDirector.RemoveVdsAlert(vdsStatic.getId(),
                    AuditLogType.VDS_ALERT_FENCE_IS_NOT_CONFIGURED);
        }
    }

    /**
     * Alerts if power management status failed.
     *
     * @param vdsStatic
     *            The VDS static.
     */
    protected void TestVdsPowerManagementStatus(VdsStatic vdsStatic) {
        if (vdsStatic.getpm_enabled()) {
            Backend.getInstance().runInternalQuery(VdcQueryType.GetVdsFenceStatus,
                    new VdsIdParametersBase(vdsStatic.getId()));
        }
    }

    /**
     * Alerts if power management operation failed.
     */
    protected void AlertIfPowerManagementOperationFailed() {
        Alert(AuditLogType.VDS_ALERT_FENCE_OPERATION_FAILED);
    }

    /**
     * Alerts if power management operation skipped.
     * @param operation The operation name.
     */
    protected void AlertIfPowerManagementOperationSkipped(String operation) {
        Alert(AuditLogType.VDS_ALERT_FENCE_OPERATION_SKIPPED,operation);
    }

    protected void LogSettingVmToDown(Guid vdsId, Guid vmId) {
        AuditLogableBase logable = new AuditLogableBase(vdsId, vmId);
        AuditLogDirector.log(logable,
                AuditLogType.VM_WAS_SET_DOWN_DUE_TO_HOST_REBOOT_OR_MANUAL_FENCE);
    }

    protected boolean IsPowerManagementLegal(VdsStatic vdsStatic, String clsuterCompatibilityVersion) {
        boolean result = true;

        if (vdsStatic.getpm_enabled()) {
            // check if pm_type is not null and if it in the supported fence types by version
            if (StringUtils.isEmpty(vdsStatic.getpm_type())) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_PM_ENABLED_WITHOUT_AGENT);
                result = false;
            } else if (!Regex.IsMatch(Config.<String> GetValue(ConfigValues.VdsFenceType,
                    clsuterCompatibilityVersion), String.format("(,|^)%1$s(,|$)",
                    vdsStatic.getpm_type()))) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_AGENT_NOT_SUPPORTED);
                result = false;
            }
            // Do not allow to pass empty/null value as the user/password agent credentials
            else if (StringUtils.isEmpty(vdsStatic.getpm_user()) ||
                    StringUtils.isEmpty(vdsStatic.getpm_password())) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_PM_ENABLED_WITHOUT_AGENT_CREDENTIALS);
                result = false;
            }
        }
        return result;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(getVdsId(), VdcObjectType.VDS,
                getActionType().getActionGroup()));
    }

    public void handleVdsError(VDSReturnValue returnValue) {
        getReturnValue().getFault().setError(returnValue.getVdsError().getCode());
        getReturnValue().getFault().setMessage(returnValue.getVdsError().getMessage());
        getReturnValue().getExecuteFailedMessages().add(returnValue.getVdsError().getMessage());
    }
}
