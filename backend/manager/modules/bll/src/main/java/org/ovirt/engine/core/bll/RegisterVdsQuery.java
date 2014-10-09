package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.AddVdsActionParameters;
import org.ovirt.engine.core.common.action.ApproveVdsParameters;
import org.ovirt.engine.core.common.action.UpdateVdsActionParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VDSType;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.queries.RegisterVdsParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.compat.DateTime;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.RefObject;
import org.ovirt.engine.core.compat.Regex;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VdsDAO;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;

@DisableInMaintenanceMode
public class RegisterVdsQuery<P extends RegisterVdsParameters> extends QueriesCommandBase<P> {

    private AuditLogType error = AuditLogType.UNASSIGNED;
    private String strippedVdsUniqueId;
    private final AuditLogableBase logable;
    private List<VDS> _vdssByUniqueId;

    private static Object doubleRegistrationLock = new Object();
    /**
     * 'z' has the highest ascii value from the acceptable characters, so the bit set size should be initiated to it.
     * the size is 'z'+1 so that each char will be represented in the BitSet with index which equals to it's char value.
     */
    private static final BitSet validChars = new BitSet('z' + 1);

    static {
        // the set method sets the bits from the specified from index(inclusive) to the specified toIndex(exclusive) to
        // true so the toIndex should be incremented in 1 in order to include the char represented by that index in the
        // range.
        validChars.set('a', 'z' + 1);
        validChars.set('A', 'Z' + 1);
        validChars.set('0', '9' + 1);
        validChars.set('-');
        validChars.set('.');
        validChars.set(':');
        validChars.set('_');
    }

    public RegisterVdsQuery(P parameters) {
        super(parameters);
        logable = new AuditLogableBase(parameters.getVdsId());
    }

    protected String getStrippedVdsUniqueId() {
        if (strippedVdsUniqueId == null) {
            // since we use the management IP field, makes sense to remove the
            // illegal characters in advance
            StringBuilder builder = new StringBuilder();
            for (char ch : getParameters().getVdsUniqueId().toCharArray()) {
                if (validChars.get(ch)) {
                    builder.append(ch);
                }
            }
            strippedVdsUniqueId = builder.toString();
        }
        return strippedVdsUniqueId;
    }

    private List<VDS> getVdssByUniqueId() {
        if (_vdssByUniqueId == null) {
            _vdssByUniqueId = DbFacade.getInstance().getVdsDao().getAllWithUniqueId(getStrippedVdsUniqueId());
        }
        return _vdssByUniqueId;
    }

    @Override
    protected boolean validateInputs() {
        if (!super.validateInputs()) {
            return false;
        }

        VdcQueryReturnValue returnValue = getQueryReturnValue();
        returnValue.setExceptionString("");
        try {
            String hostName = getParameters().getVdsHostName();
            if (StringUtils.isEmpty(hostName)) {
                returnValue.setExceptionString("Cannot register Host - no Hostname address specified.");
                return false;
            }

            String vdsUniqueId = getParameters().getVdsUniqueId();
            if (StringUtils.isEmpty(vdsUniqueId)) {
                returnValue.setExceptionString(
                    String.format(
                        "Cannot register host '%1$s' - host id is empty.",
                        hostName
                    )
                );
                AuditLogableBase logable = new AuditLogableBase();
                logable.AddCustomValue("VdsHostName", hostName);
                AuditLogDirector.log(logable, AuditLogType.VDS_REGISTER_EMPTY_ID);
                return false;
            }

            List<VDS> vdssByUniqueId = getVdssByUniqueId();
            if (vdssByUniqueId.size() > 1) {
                returnValue.setExceptionString("Cannot register Host - unique id is ambigious.");
                return false;
            }
            if (vdssByUniqueId.size() == 1) {
                VDS vds = vdssByUniqueId.get(0);
                if (!VdsHandler.isPendingOvirt(vds)) {
                    returnValue.setExceptionString(VdcBllMessages.VDS_STATUS_NOT_VALID_FOR_UPDATE.name());
                    return false;
                }
                Long otp = getParameters().getOtp();
                if (!isValidOtp(vds, otp)) {
                    returnValue.setExceptionString(String.format("Invalid OTP for host %s", hostName));
                    return false;
                }

            }

        } catch (RuntimeException ex) {
            log.error(ex);
            returnValue.setExceptionString(String.format("Cannot register Host - An exception has been thrown: %1$s",
                    ex.getMessage()));
            return false;
        }

        return true;
    }

    /**
     * the method checks if the given otp is valid, will return false for null as well
     * @param vds
     * @param otp
     * @return boolean indicating whether the given otp is valid or not
     */
    private boolean isValidOtp(VDS vds, Long otp) {
        if (otp != null && otp.longValue() == vds.getOtpValidity()) {
            Integer otpExpiration = Config.<Integer> GetValue(ConfigValues.OtpExpirationInSeconds);
            DateTime otpValidity = new DateTime(otp);
            otpValidity.AddSeconds(otpExpiration);
            if (otpValidity.before(DateTime.getUtcNow())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void executeQueryCommand() {
        try {
            log.info("Running Command: RegisterVds");
            executeRegisterVdsCommand();
        } catch (RuntimeException ex) {
            log.error("RegisterVdsQuery::ExecuteWithoutTransaction: An exception has been thrown.", ex);
        } finally {
            writeToAuditLog();
        }
    }

    protected void executeRegisterVdsCommand() {
        synchronized (doubleRegistrationLock) {
            // force to reload vdss by unique ID used later on
            _vdssByUniqueId = null;
            VDS vdsByUniqueId = getVdssByUniqueId().size() != 0 ? getVdssByUniqueId().get(0) : null;

            // in case oVirt host was added for the second time - perform approval
            if (vdsByUniqueId != null && vdsByUniqueId.getstatus() == VDSStatus.PendingApproval
                    && getParameters().getVdsType() == VDSType.oVirtNode
                    && getParameters().getOtp() != null) {

                getQueryReturnValue().setSucceeded(dispatchOvirtApprovalCommand(vdsByUniqueId.getId()));
                return;
            }

            log.debug("RegisterVdsQuery::ExecuteCommand - Entering");

            if (StringUtils.isEmpty(getParameters().getVdsName())) {
                getParameters().setVdsName(getParameters().getVdsUniqueId());
                log.debug("RegisterVdsQuery::ExecuteCommand - VdsName empty, using VdsUnique ID as name");
            }

            logable.AddCustomValue("VdsName1", getParameters().getVdsName());

            Guid vdsGroupId;
            if (Guid.Empty.equals(getParameters().getVdsGroupId())) {
                vdsGroupId = Guid.createGuidFromString(
                        Config.<String> GetValue(ConfigValues.PowerClientAutoRegistrationDefaultVdsGroupID));
                log.debugFormat(
                        "RegisterVdsQuery::ExecuteCommand - VdsGroupId received as -1, using PowerClientAutoRegistrationDefaultVdsGroupID: {0}",
                        vdsGroupId);
            } else {
                vdsGroupId = getParameters().getVdsGroupId();
            }
            // TODO: always add in pending state, and if auto approve call
            // approve command action after registration
            RefObject<Boolean> isPending = new RefObject<Boolean>(Boolean.FALSE);
            getQueryReturnValue().setSucceeded(
                    HandleOldVdssWithSameHostName(vdsByUniqueId) && HandleOldVdssWithSameName(vdsByUniqueId)
                            && CheckAutoApprovalDefinitions(isPending)
                            && Register(vdsByUniqueId, vdsGroupId, isPending.argvalue.booleanValue()));
            log.debugFormat("RegisterVdsQuery::ExecuteCommand - Leaving Succeded value is {0}",
                    getQueryReturnValue().getSucceeded());
        }
    }

    private boolean dispatchOvirtApprovalCommand(Guid oVirtId) {
        boolean isApprovalDispatched = true;
        final ApproveVdsParameters params = new ApproveVdsParameters();
        params.setVdsId(oVirtId);
        params.setApprovedByRegister(true);

        try {
            ThreadPoolUtil.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        VdcReturnValueBase ret =
                            Backend.getInstance().runInternalAction(VdcActionType.ApproveVds, params);
                        if (ret == null || !ret.getSucceeded()) {
                            log.errorFormat("Approval of oVirt {0} failed. ", params.getVdsId());
                        } else if (ret.getSucceeded()) {
                            log.infoFormat("Approval of oVirt {0} ended successefully. ", params.getVdsId());
                        }
                    } catch (RuntimeException ex) {
                        log.error("Failed to Approve host", ex);
                    }
                }
            });
        } catch (Exception e) {
            isApprovalDispatched = false;
        }
        return isApprovalDispatched;
    }

    private boolean Register(VDS vdsByUniqueId, Guid vdsGroupId, boolean IsPending) {
        boolean returnValue = true;
        log.debugFormat("RegisterVdsQuery::Register - Entering");
        if (vdsByUniqueId == null) {
            returnValue = registerNewHost(vdsGroupId, IsPending);
        } else {
            returnValue = updateExistingHost(vdsByUniqueId, IsPending);
        }
        log.debugFormat("RegisterVdsQuery::Register - Leaving with value {0}", returnValue);

        return returnValue;
    }

    private boolean updateExistingHost(VDS vdsByUniqueId, boolean IsPending) {
        boolean returnValue = true;
        vdsByUniqueId.sethost_name(getParameters().getVdsHostName());
        vdsByUniqueId.setport(getParameters().getPort());
        log.debugFormat(
                "RegisterVdsQuery::Register - Will try now to update VDS with existing unique id; Name: {0}, Hostname: {1}, Unique: {2}, Port: {3}, IsPending: {4} with force synchronize",
                getParameters().getVdsHostName(),
                getStrippedVdsUniqueId(),
                getStrippedVdsUniqueId(),
                getParameters().getPort(),
                IsPending);

        UpdateVdsActionParameters p = new UpdateVdsActionParameters(vdsByUniqueId.getStaticData(), "", false);
        p.setTransactionScopeOption(TransactionScopeOption.RequiresNew);
        VdcReturnValueBase rc = Backend.getInstance().runInternalAction(VdcActionType.UpdateVds, p);

        if (rc == null || !rc.getSucceeded()) {
            error = AuditLogType.VDS_REGISTER_EXISTING_VDS_UPDATE_FAILED;
            log.debugFormat(
                    "RegisterVdsQuery::Register - Failed to update existing VDS Name: {0}, Hostname: {1}, Unique: {2}, Port: {3}, IsPending: {4}",
                    getParameters().getVdsHostName(),
                    getStrippedVdsUniqueId(),
                    getStrippedVdsUniqueId(),
                    getParameters().getPort(),
                    IsPending);

            CaptureCommandErrorsToLogger(rc, "RegisterVdsQuery::Register");
            returnValue = false;
        } else {
            log.infoFormat(
                    "RegisterVdsQuery::Register -Updated a {3} registered VDS - Name: {0}, Hostname: {1}, UniqueID: {2}",
                    getParameters().getVdsName(),
                    getParameters().getVdsHostName(),
                    getStrippedVdsUniqueId(),
                    vdsByUniqueId.getstatus() == VDSStatus.PendingApproval ? "Pending " : "");
        }
        return returnValue;
    }

    private boolean registerNewHost(Guid vdsGroupId, boolean IsPending) {
        boolean returnValue = true;
        VdsStatic vds = new VdsStatic(getParameters().getVdsHostName(), "",
                    getStrippedVdsUniqueId(), getParameters().getPort(), vdsGroupId, Guid.Empty,
                    getParameters().getVdsName(), Config.<Boolean> GetValue(ConfigValues.SSLEnabled),
                    getParameters().getVdsType()); // management
                                                                                 // ip
                                                                                 // not
                                                                                 // currently
                                                                                 // passed
                                                                                 // by
                                                                                 // registration

                log.debugFormat(
                        "RegisterVdsQuery::Register - Will try now to add VDS from scratch; Name: {0}, Hostname: {1}, Unique: {2}, Port: {3},Subnet mask: {4}, IsPending: {5} with force synchronize",
                        getParameters().getVdsName(),
                        getParameters().getVdsHostName(),
                        getStrippedVdsUniqueId(),
                        getParameters().getPort(),
                        IsPending);

            AddVdsActionParameters p = new AddVdsActionParameters(vds, "");
            p.setAddPending(IsPending);

            VdcReturnValueBase ret = Backend.getInstance().runInternalAction(VdcActionType.AddVds, p);

            if (ret == null || !ret.getSucceeded()) {
                log.errorFormat(
                        "RegisterVdsQuery::Register - Registration failed for VDS - Name: {0}, Hostname: {1}, UniqueID: {2}, Subnet mask: {3}",
                        getParameters().getVdsName(),
                        getParameters().getVdsHostName(),
                        getStrippedVdsUniqueId());
                CaptureCommandErrorsToLogger(ret, "RegisterVdsQuery::Register");
                error = AuditLogType.VDS_REGISTER_FAILED;
                returnValue = false;
            } else {
                log.infoFormat(
                        "RegisterVdsQuery::Register - Registered a new VDS {3} - Name: {0}, Hostname: {1}, UniqueID: {2}",
                        getParameters().getVdsName(),
                        getParameters().getVdsHostName(),
                        getStrippedVdsUniqueId(),
                        IsPending ? "pending approval" : "automatically approved");
            }
        return returnValue;
    }

    private boolean HandleOldVdssWithSameHostName(VDS vdsByUniqueId) {
        // handle old VDSs with same host_name (IP)
        log.debugFormat("RegisterVdsQuery::HandleOldVdssWithSameHostName - Entering");

        boolean returnValue = true;
        List<VDS> vdss_byHostName = DbFacade.getInstance().getVdsDao().getAllForHostname(
                getParameters().getVdsHostName());
        int lastIteratedIndex = 1;
        if (vdss_byHostName.size() > 0) {
            log.debugFormat(
                    "RegisterVdsQuery::HandleOldVdssWithSameHostName - found {0} VDS(s) with the same host name {1}.  Will try to change their hostname to a different value",
                    vdss_byHostName.size(),
                    getParameters().getVdsHostName());

            for (VDS vds_byHostName : vdss_byHostName) {
                /**
                 * looping foreach VDS found with similar hostnames and change to each one to available hostname
                 */
                if (vdsByUniqueId == null
                        || (vdsByUniqueId != null && !vds_byHostName.getId().equals(vdsByUniqueId.getId()))) {
                    boolean unique = false;
                    String try_host_name = "";
                    for (int i = lastIteratedIndex; i <= 100; i++, lastIteratedIndex = i) {
                        try_host_name = String.format("hostname-was-%1$s-%2$s", getParameters()
                                .getVdsHostName(), i);
                        if (DbFacade.getInstance().getVdsDao().getAllForHostname(try_host_name).size() == 0) {
                            unique = true;
                            break;
                        }
                    }
                    if (unique) {
                        String old_host_name = vds_byHostName.gethost_name();
                        vds_byHostName.sethost_name(try_host_name);
                        UpdateVdsActionParameters parameters = new UpdateVdsActionParameters(
                                vds_byHostName.getStaticData(), "", false);
                        parameters.setShouldBeLogged(false);
                        parameters.setTransactionScopeOption(TransactionScopeOption.RequiresNew);
                        VdcReturnValueBase ret = Backend.getInstance().runInternalAction(VdcActionType.UpdateVds, parameters);

                        if (ret == null || !ret.getSucceeded()) {
                            error = AuditLogType.VDS_REGISTER_ERROR_UPDATING_HOST;
                            logable.AddCustomValue("VdsName2", vds_byHostName.getStaticData().getvds_name());
                            log.errorFormat(
                                    "RegisterVdsQuery::HandleOldVdssWithSameHostName - could not update VDS {0}",
                                    vds_byHostName.getStaticData().getvds_name());
                            CaptureCommandErrorsToLogger(ret,
                                    "RegisterVdsQuery::HandleOldVdssWithSameHostName");
                            return false;
                        } else {
                            log.infoFormat(
                                    "RegisterVdsQuery::HandleOldVdssWithSameHostName - Another VDS was using this IP {0}. Changed to {1}",
                                    old_host_name,
                                    try_host_name);
                        }
                    } else {
                        log.errorFormat(
                                "VdcBLL::HandleOldVdssWithSameHostName - Could not change the IP for an existing VDS. All available hostnames are taken (ID = {0}, name = {1}, management IP = {2} , host name = {3})",
                                vds_byHostName.getId(),
                                vds_byHostName.getvds_name(),
                                vds_byHostName.getManagmentIp(),
                                vds_byHostName.gethost_name());
                        error = AuditLogType.VDS_REGISTER_ERROR_UPDATING_HOST_ALL_TAKEN;
                        returnValue = false;
                    }
                }
                log.infoFormat(
                        "RegisterVdsQuery::HandleOldVdssWithSameHostName - No Change required for VDS {0}. Since it has the same unique Id",
                        vds_byHostName.getId());
            }
        }
        log.debugFormat("RegisterVdsQuery::HandleOldVdssWithSameHostName - Leaving with value {0}", returnValue);

        return returnValue;
    }

    /**
     * Check if another host has the same name as hostToRegister and if yes append a number to it. Eventually if the
     * host is in the db, persist the changes.
     * @param hostToRegister
     * @return
     */
    private boolean HandleOldVdssWithSameName(VDS hostToRegister) {
        log.debugFormat("Entering");
        boolean returnValue = true;
        VdsDAO vdsDAO = DbFacade.getInstance().getVdsDao();
        List<VDS> hosts = vdsDAO.getAllWithName(getParameters().getVdsName());
        List<String> allHostNames = getAllHostNames(vdsDAO.getAll());
        boolean hostExistInDB = hostToRegister != null;

        if (hosts.size() > 0) {
            log.debugFormat(
                    "found {0} VDS(s) with the same name {1}.  Will try to register with a new name",
                    hosts.size(),
                    getParameters().getVdsName());

            String nameToRegister = getParameters().getVdsName();
            String uniqueIdToRegister = getParameters().getVdsUniqueId();
            String newName;

            for (VDS storedHost : hosts) {
                // check different uniqueIds but same name
                if (!uniqueIdToRegister.equals(storedHost.getUniqueId())
                        && nameToRegister.equals(storedHost.getvds_name())) {
                    if (hostExistInDB) {
                        // update the registered host if exist in db
                        allHostNames.remove(hostToRegister.getvds_name());
                        newName = generateUniqueName(nameToRegister, allHostNames);
                        hostToRegister.setvds_name(newName);
                        UpdateVdsActionParameters parameters =
                                new UpdateVdsActionParameters(hostToRegister.getStaticData(), "", false);
                        VdcReturnValueBase ret = Backend.getInstance().runInternalAction(VdcActionType.UpdateVds, parameters);
                        if (ret == null || !ret.getSucceeded()) {
                            error = AuditLogType.VDS_REGISTER_ERROR_UPDATING_NAME;
                            logable.AddCustomValue("VdsName2", newName);
                            log.errorFormat("could not update VDS {0}", nameToRegister);
                            CaptureCommandErrorsToLogger(ret, "RegisterVdsQuery::HandleOldVdssWithSameName");
                            return false;
                        } else {
                            log.infoFormat(
                                    "Another VDS was using this name with IP {0}. Changed to {1}",
                                    nameToRegister,
                                    newName);
                        }
                    } else {
                        // host doesn't exist in db yet. not persisting changes just object values.
                        newName = generateUniqueName(nameToRegister, allHostNames);
                        getParameters().setVdsName(newName);
                    }
                    break;
                }
            }
        }
        log.debugFormat("Leaving with value {0}", returnValue);
        return returnValue;
    }

    private List<String> getAllHostNames(List<VDS> allHosts) {
        List<String> allHostNames = new ArrayList<String>(allHosts.size());
        for (VDS vds : allHosts) {
            allHostNames.add(vds.getvds_name());
        }
        return allHostNames;
    }

    private String generateUniqueName(String val, List<String> allHostNames) {
        int i = 2;
        boolean postfixed = false;
        StringBuilder sb = new StringBuilder(val);
        while (allHostNames.contains(val)) {
            if (!postfixed) {
                val = sb.append("-").append(i).toString();
                postfixed = true;
            } else {
                val = sb.replace(sb.lastIndexOf("-"), sb.length(), "-").append(i).toString();
            }
            i++;
        }
        return val;
    }

    private boolean CheckAutoApprovalDefinitions(RefObject<Boolean> isPending) {
        // check auto approval definitions
        log.debugFormat("RegisterVdsQuery::CheckAutoApprovalDefinitions - Entering");

        isPending.argvalue = true;
        if (!Config.<String> GetValue(ConfigValues.PowerClientAutoApprovePatterns).equals("")) {
            for (String pattern : Config.<String> GetValue(ConfigValues.PowerClientAutoApprovePatterns)
                    .split("[,]", -1)) {
                try {
                    String pattern_helper = pattern.toLowerCase();
                    Regex pattern_regex = new Regex(pattern_helper);
                    String vds_hostname_helper = getParameters().getVdsHostName().toLowerCase();
                    String vds_unique_id_helper = getParameters().getVdsUniqueId().toLowerCase()
                            .replace(":", "-");
                    if (vds_hostname_helper.startsWith(pattern) || vds_unique_id_helper.startsWith(pattern)
                            || pattern_regex.IsMatch(vds_hostname_helper)
                            || pattern_regex.IsMatch(vds_unique_id_helper)) {
                        isPending.argvalue = false;
                        break;
                    }
                } catch (RuntimeException ex) {
                    error = AuditLogType.VDS_REGISTER_AUTO_APPROVE_PATTERN;
                    log.errorFormat(
                            "RegisterVdsQuery ::CheckAutoApprovalDefinitions(out bool) -  Error in auto approve pattern: {0}-{1}",
                            pattern,
                            ex.getMessage());
                    return false;
                }
            }
        }
        log.debugFormat("RegisterVdsQuery::CheckAutoApprovalDefinitions - Leaving - return value {0}",
                    isPending.argvalue);
        return true;
    }

    private void CaptureCommandErrorsToLogger(VdcReturnValueBase retValue, String prefixToMessage) {
        if (retValue.getFault() != null) {
            log.errorFormat("{0} - Fault - {1}", prefixToMessage, retValue.getFault().getMessage());
        }
        if (retValue.getCanDoActionMessages().size() > 0) {
            List<String> msgs = retValue.getCanDoActionMessages();
            for (String s : msgs) {
                log.errorFormat("{0} - CanDoAction Fault - {1}", prefixToMessage, s);
            }
        }
        if (retValue.getExecuteFailedMessages().size() > 0) {
            // List<string> msgs =
            // ErrorTranslator.TranslateErrorText(retValue.ExecuteFailedMessages);
            for (String s : retValue.getExecuteFailedMessages()) {
                log.errorFormat("{0} - Ececution Fault - {1}", prefixToMessage, s);
            }
        }
    }

    private void writeToAuditLog() {
        try {
            AuditLogDirector.log(logable, getAuditLogTypeValue());
        } catch (RuntimeException ex) {
            log.error("RegisterVdsQuery::WriteToAuditLog: An exception has been thrown.", ex);
        }
    }

    protected AuditLogType getAuditLogTypeValue() {
        return getQueryReturnValue().getSucceeded() ? AuditLogType.VDS_REGISTER_SUCCEEDED : error;
    }

}
