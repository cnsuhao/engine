package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.SetVmTicketParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.interfaces.IVdcUser;
import org.ovirt.engine.core.common.vdscommands.SetVmTicketVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dao.VmDynamicDAO;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.Ticketing;

@InternalCommandAttribute
public class SetVmTicketCommand<T extends SetVmTicketParameters> extends VmOperationCommandBase<T> {
    // Serialization id:
    private static final long serialVersionUID = -5403502087156599801L;

    // The log:
    private static final Log log = LogFactory.getLog(SetVmTicketCommand.class);

    private String mTicket;
    private final int mValidTime;

    // This flag is calculated during the authorization phase and indicates if
    // the user needed additional permission in order to connect to the console
    // of the virtual machine:
    private boolean neededPermissions = false;

    public SetVmTicketCommand(T parameters) {
        super(parameters);
        mTicket = parameters.getTicket();
        mValidTime = parameters.getValidTime();
    }

    @Override
    protected void setActionMessageParameters () {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__SET);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM_TICKET);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects () {
        final List<PermissionSubject> permissions = super.getPermissionCheckSubjects();
        if (needPermissionForConnectingToConsole()) {
            permissions.add(new PermissionSubject(getVmId(), VdcObjectType.VM, ActionGroup.RECONNECT_TO_VM, VdcBllMessages.USER_CANNOT_FORCE_RECONNECT_TO_VM));
            neededPermissions = true;
        }
        return permissions;
    }

    /**
     * Checks if the user needs additional permissions in order to connect
     * to the console.
     *
     * @return <code>true</code> if additional permissions are needed,
     *   <code>false</code> otherwise
     */
    private boolean needPermissionForConnectingToConsole() {
        // Check if the virtual machine has the flag that allows forced connection to
        // any user, in that case no additional permission is needed:
        final VM vm = getVm();
        if (vm == null || vm.getAllowConsoleReconnect()) {
            return false;
        }

        // If this is not the first user to connect to the console then it does need
        // additional permissions:
        final NGuid currentId = getCurrentUser().getUserId();
        final NGuid previousId = vm.getConsoleUserId();
        if (previousId != null && !previousId.equals(currentId)) {
            log.warnFormat("User \"{0}\" is trying to take the console of virtual machine \"{1}\", but the console is already taken by user \"{2}\".", currentId, vm.getId(), previousId);
            return true;
        }

        // If we are here then the user is the first to connect to the console, so no
        // additional permissions are needed:
        return false;
    }

    @Override
    protected boolean canDoAction() {
        // Check that the virtual machine exists:
        final VM vm = getVm();
        if (vm == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_EXIST);

            return false;
        }

        // Check that the virtual machine is in state that allows connections
        // to the console:
        final VMStatus status = vm.getStatus();
        if (status != VMStatus.Up && status != VMStatus.Paused && status != VMStatus.PoweringUp && status != VMStatus.PoweringDown && status != VMStatus.RebootInProgress) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_STATUS_ILLEGAL);
            return false;
        }

        // Nothing else, all checks have been performed using permission
        // subjects:
        return true;
    }

    @Override
    protected void Perform() {
        // Generate the ticket if needed (in some situations the client will not send
        // a ticket):
        if (StringHelper.isNullOrEmpty(mTicket)) {
            mTicket = Ticketing.GenerateOTP();
        }

        // Update the dynamic information of the virtual machine in memory (we need it
        // to update the database later):
        final VM vm = getVm();
        final IVdcUser user = getCurrentUser();
        vm.setConsoleUserId(user.getUserId());
        vm.setConsoleCurrentUserName(user.getFQN());

        // If the virtual machine has the allow reconnect flag or the user
        // needed additional permissions to connect to the console then we just
        // have to save the user id to the database, regardless of what was
        // there before and without locking.
        //
        // Note that the fact that the user needed permissions actually means
        // that it has them, otherwise we will not be here, performing the
        // operation.
        //
        // In any other situation we try to save the new user to the database
        // and proceed only if the previous user in the database is null. This
        // is needed to prevent races between different users trying to access
        // the console of the same virtual machine simultaneously.
        final VmDynamicDAO dao = DbFacade.getInstance().getVmDynamicDao();
        if (vm.getAllowConsoleReconnect() || neededPermissions) {
            dao.update(vm.getDynamicData());
            sendTicket();
        }
        else {
            final boolean saved = dao.updateConsoleUserWithOptimisticLocking(vm.getDynamicData());
            if (saved) {
                sendTicket();
            }
            else {
                dontSendTicket();
            }
        }
    }

    /**
     * Don't send the ticket to the virtual machine and send a warning indicating
     * that two users are trying to connect to the console of the virtual machine
     * simultaneously.
     */
    private void dontSendTicket() {
        // Send messages to the log explaining the situation:
        final VM vm = getVm();
        final IVdcUser user = getCurrentUser();
        log.warnFormat("Can't give console of virtual machine \"{0}\" to user \"{1}\", it has probably been taken by another user.", vm.getId(), user.getUserId());

        // Set the result messages indicating that the operation failed:
        addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_IN_USE_BY_OTHER_USER);

        // The command failed:
        setSucceeded(false);
    }

    /**
     * Send a previously generated ticket to the virtual machine. If sending
     * the ticket fails the command will be marked as failed.
     */
    private void sendTicket() {
        // Send the ticket to the virtual machine:
        final IVdcUser user = getCurrentUser();
        final boolean sent = Backend
            .getInstance()
            .getResourceManager()
            .RunVdsCommand(VDSCommandType.SetVmTicket,
                new SetVmTicketVDSCommandParameters(getVdsId(), getVmId(), mTicket, mValidTime, user.getUserName(), user.getUserId())).getSucceeded();

        // Return the ticket only if sending it to the virtual machine succeeded:
        if (sent) {
            setActionReturnValue(mTicket);
        }
        setSucceeded(sent);
    }
}
