package org.ovirt.engine.core.dal.dbbroker.auditloghandling;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ovirt.engine.core.common.AuditLogSeverity;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.AuditLog;
import org.ovirt.engine.core.compat.ApplicationException;
import org.ovirt.engine.core.compat.DateTime;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.backendcompat.PropertyInfo;
import org.ovirt.engine.core.compat.backendcompat.TypeCompat;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;


public final class AuditLogDirector {
    private static final Log log = LogFactory.getLog(AuditLogDirector.class);
    private static final Map<AuditLogType, String> messages = new EnumMap<AuditLogType, String>(AuditLogType.class);
    private static final Map<AuditLogType, AuditLogSeverity> severities =
            new EnumMap<AuditLogType, AuditLogSeverity>(AuditLogType.class);
    private static final Pattern pattern = Pattern.compile("\\$\\{\\w*\\}"); // match ${<alphanumeric>...}
    static final String UNKNOWN_VARIABLE_VALUE = "<UNKNOWN>";
    private static final String APP_ERRORS_MESSAGES_FILE_NAME = "bundles/AuditLogMessages";

    static {
        initMessages();
        initSeverities();
    }

    private static void initSeverities() {
        initDefaultSeverities();
        initNetworkSeverities();
        initImportExportSeverities();
        initEngineSeverities();
        initVMsPoolSeverities();
        initBookmarkSeverities();
        initVMSeverities();
        initQuotaSeverities();
        initTagSeverities();
        initClusterSeverities();
        initMLASeverities();
        initHostSeverities();
        initStorageSeverities();
        initTaskSeverities();
        initGlusterVolumeSeverities();
        initDwhSeverities();
        initConfigSeverities();
        initUserAccountSeverities();
        checkSeverities();
    }

    private static void initGlusterVolumeSeverities() {
        severities.put(AuditLogType.GLUSTER_VOLUME_CREATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_CREATE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_OPTION_SET, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_OPTION_SET_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_START, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_START_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_STOP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_STOP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_OPTIONS_RESET, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_OPTIONS_RESET_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_DELETE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_DELETE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_REMOVE_BRICKS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_REMOVE_BRICKS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_REBALANCE_START, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_REBALANCE_START_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_ADD_BRICK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_ADD_BRICK_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_SERVER_ADD_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_SERVER_REMOVE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_SERVER_REMOVE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_REPLACE_BRICK_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_REPLACE_BRICK_START, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_REPLACE_BRICK_START_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_SERVERS_LIST_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_PROFILE_START, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_PROFILE_START_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_PROFILE_STOP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.GLUSTER_VOLUME_PROFILE_STOP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_INFO_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_COMMAND_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.GLUSTER_VOLUME_CREATED_FROM_CLI, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.GLUSTER_VOLUME_DELETED_FROM_CLI, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.GLUSTER_VOLUME_OPTION_SET_FROM_CLI, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.GLUSTER_VOLUME_OPTION_RESET_FROM_CLI, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.GLUSTER_VOLUME_PROPERTIES_CHANGED_FROM_CLI, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.GLUSTER_VOLUME_BRICK_ADDED_FROM_CLI, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.GLUSTER_VOLUME_BRICK_REMOVED_FROM_CLI, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.GLUSTER_SERVER_REMOVED_FROM_CLI, AuditLogSeverity.WARNING);
    }

    private static void initDefaultSeverities() {
        severities.put(AuditLogType.UNASSIGNED, AuditLogSeverity.NORMAL);
    }

    private static void initTaskSeverities() {
        severities.put(AuditLogType.TASK_CLEARING_ASYNC_TASK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.TASK_STOPPING_ASYNC_TASK, AuditLogSeverity.NORMAL);
    }

    private static void initEngineSeverities() {
        severities.put(AuditLogType.VDC_STOP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDC_START, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.CERTIFICATE_FILE_NOT_FOUND, AuditLogSeverity.ERROR);
    }

    private static void initBookmarkSeverities() {
        severities.put(AuditLogType.USER_ADD_BOOKMARK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_BOOKMARK_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_BOOKMARK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_BOOKMARK_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_BOOKMARK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_BOOKMARK_FAILED, AuditLogSeverity.ERROR);
    }

    private static void initVMsPoolSeverities() {
        severities.put(AuditLogType.USER_ADD_VM_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_VM_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_VM_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_VM_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_VM_POOL_WITH_VMS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_VM_POOL_WITH_VMS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_VM_POOL_WITH_VMS_ADD_VDS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_VM_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_VM_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_VM_TO_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_VM_TO_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_VM_FROM_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_VM_FROM_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_POOL_INTERNAL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_POOL_FAILED_INTERNAL, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_USER_FROM_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_USER_FROM_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_VM_FROM_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_VM_FROM_POOL_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_VM_FROM_POOL_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_VM_FROM_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_TIME_LEASED_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_TIME_LEASED_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_USER_FROM_TIME_LEASED_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_USER_FROM_TIME_LEASED_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_AD_GROUP_TO_TIME_LEASED_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_AD_GROUP_TO_TIME_LEASED_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_AD_GROUP_FROM_TIME_LEASED_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_AD_GROUP_FROM_TIME_LEASED_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_USER_FROM_TIME_LEASED_POOL_INTERNAL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_USER_FROM_TIME_LEASED_POOL_FAILED_INTERNAL, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_AD_GROUP_FROM_TIME_LEASED_POOL_INTERNAL, AuditLogSeverity.NORMAL);
        severities
                .put(AuditLogType.USER_DETACH_AD_GROUP_FROM_TIME_LEASED_POOL_FAILED_INTERNAL, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_USER_TO_TIME_LEASED_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_USER_TO_TIME_LEASED_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_VM_POOL_WITH_VMS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_VM_POOL_MAX_SUBSEQUENT_FAILURES_REACHED, AuditLogSeverity.WARNING);
    }

    private static void initMLASeverities() {
        severities.put(AuditLogType.USER_VDC_LOGIN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_VDC_LOGIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_VDC_LOGOUT, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_VDC_LOGOUT_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_AD_GROUP_TO_TIME_LEASED_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_AD_GROUP_TO_TIME_LEASED_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.AD_COMPUTER_ACCOUNT_SUCCEEDED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.AD_COMPUTER_ACCOUNT_FAILED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_PERMISSION, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_PERMISSION_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_PERMISSION, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_PERMISSION_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_ROLE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_ROLE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_ROLE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_ROLE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_ROLE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_ROLE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACHED_ACTION_GROUP_TO_ROLE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACHED_ACTION_GROUP_TO_ROLE_FAILED,
                AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACHED_ACTION_GROUP_FROM_ROLE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACHED_ACTION_GROUP_FROM_ROLE_FAILED,
                AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_ROLE_WITH_ACTION_GROUP, AuditLogSeverity.NORMAL);
        severities
                .put(AuditLogType.USER_ADD_ROLE_WITH_ACTION_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_ADUSER, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_REMOVE_ADUSER, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_ADD_ADUSER, AuditLogSeverity.WARNING);
    }

    private static void initHostSeverities() {
        severities.put(AuditLogType.VDS_REGISTER_ERROR_UPDATING_HOST, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_REGISTER_ERROR_UPDATING_HOST_ALL_TAKEN, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_REGISTER_HOST_IS_ACTIVE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_REGISTER_ERROR_UPDATING_NAME, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_REGISTER_ERROR_UPDATING_NAMES_ALL_TAKEN, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_REGISTER_NAME_IS_ACTIVE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_REGISTER_AUTO_APPROVE_PATTERN, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_REGISTER_EMPTY_ID, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_REGISTER_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_REGISTER_SUCCEEDED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_REGISTER_EXISTING_VDS_UPDATE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_ALERT_FENCE_IS_NOT_CONFIGURED, AuditLogSeverity.ALERT);
        severities.put(AuditLogType.VDS_ALERT_FENCE_TEST_FAILED, AuditLogSeverity.ALERT);
        severities.put(AuditLogType.VDS_ALERT_FENCE_OPERATION_FAILED, AuditLogSeverity.ALERT);
        severities.put(AuditLogType.VDS_ALERT_FENCE_OPERATION_SKIPPED, AuditLogSeverity.ALERT);
        severities.put(AuditLogType.VDS_ALERT_FENCE_STATUS_VERIFICATION_FAILED, AuditLogSeverity.ALERT);
        severities.put(AuditLogType.VDS_RUN_IN_NO_KVM_MODE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_VERSION_NOT_SUPPORTED_FOR_CLUSTER, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_CPU_LOWER_THAN_CLUSTER, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.CPU_FLAGS_NX_IS_MISSING, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_CPU_RETRIEVE_FAILED, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_SET_NONOPERATIONAL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_SET_NONOPERATIONAL_NETWORK, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_SET_NONOPERATIONAL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_SET_NONOPERATIONAL_DOMAIN, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_SET_NONOPERATIONAL_DOMAIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_SET_NONOPERATIONAL_IFACE_DOWN, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_DOMAIN_DELAY_INTERVAL, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_ADD_VDS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_ADD_VDS, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_RECOVER, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_RECOVER_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_RECOVER_FAILED_VMS_UNKNOWN, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_MAINTENANCE, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_MAINTENANCE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_VDS_MAINTENANCE_MIGRATION_FAILED, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_VDS_SHUTDOWN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_VDS_SHUTDOWN, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.SYSTEM_VDS_RESTART, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.SYSTEM_FAILED_VDS_RESTART, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_ACTIVATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_ACTIVATE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_VDS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_UPDATE_VDS, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_VDS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_REMOVE_VDS, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_VDS_RESTART, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_VDS_RESTART, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_VDS_START, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_VDS_START, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_VDS_STOP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_VDS_STOP, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_SLOW_STORAGE_RESPONSE_TIME, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_FAILED_TO_RUN_VMS, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_INSTALL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_INSTALL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_INSTALL_IN_PROGRESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_INSTALL_IN_PROGRESS_WARNING, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_INSTALL_IN_PROGRESS_ERROR, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_INITIATED_RUN_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_INITIATED_RUN_VM_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_FENCE_STATUS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_FENCE_STATUS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_APPROVE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_APPROVE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_FAILED_TO_GET_HOST_HARDWARE_INFO, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_INITIALIZING, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_ADD_VM_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_DETECTED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IRS_HOSTED_ON_VDS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_ALREADY_IN_REQUESTED_STATUS, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_MANUAL_FENCE_STATUS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_MANUAL_FENCE_STATUS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_MANUAL_FENCE_FAILED_CALL_FENCE_SPM, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_VDS_MAINTENANCE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_ALERT_FENCE_NO_PROXY_HOST, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_LOW_MEM, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_STORAGE_CONNECTION_FAILED_BUT_LAST_VDS, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_STORAGES_CONNECTION_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_STORAGE_VDS_STATS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_LOW_DISK_SPACE, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_LOW_DISK_SPACE_ERROR, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VDS_ACTIVATE_ASYNC, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_ACTIVATE_FAILED_ASYNC, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_SET_NON_OPERATIONAL_VM_NETWORK_IS_BRIDGELESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VDS_TIME_DRIFT_ALERT, AuditLogSeverity.WARNING);
    }

    @SuppressWarnings("deprecation")
    private static void initStorageSeverities() {
        severities.put(AuditLogType.USER_ADD_STORAGE_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_STORAGE_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_STORAGE_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_STORAGE_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_STORAGE_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_STORAGE_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_STORAGE_DOMAIN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_STORAGE_DOMAIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_STORAGE_DOMAIN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_STORAGE_DOMAIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_STORAGE_DOMAIN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_STORAGE_DOMAIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_STORAGE_DOMAIN_TO_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_STORAGE_DOMAIN_TO_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_STORAGE_DOMAINS_TO_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_STORAGE_DOMAINS_TO_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_STORAGE_DOMAIN_FROM_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_STORAGE_DOMAIN_FROM_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ACTIVATED_STORAGE_DOMAIN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ACTIVATE_STORAGE_DOMAIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DEACTIVATED_STORAGE_DOMAIN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DEACTIVATE_STORAGE_DOMAIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.SYSTEM_DEACTIVATE_STORAGE_DOMAIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_EXTENDED_STORAGE_DOMAIN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_EXTENDED_STORAGE_DOMAIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_CONNECT_HOSTS_TO_LUN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_VG, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_VG_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ACTIVATE_STORAGE_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ACTIVATE_STORAGE_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.SYSTEM_FAILED_CHANGE_STORAGE_POOL_STATUS, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.SYSTEM_CHANGE_STORAGE_POOL_STATUS_NO_HOST_FOR_SPM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.SYSTEM_CHANGE_STORAGE_POOL_STATUS_PROBLEMATIC, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.SYSTEM_CHANGE_STORAGE_POOL_STATUS_PROBLEMATIC_SEARCHING_NEW_SPM,
                AuditLogSeverity.WARNING);
        severities
                .put(AuditLogType.SYSTEM_CHANGE_STORAGE_POOL_STATUS_PROBLEMATIC_WITH_ERROR, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_FORCE_REMOVE_STORAGE_DOMAIN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FORCE_REMOVE_STORAGE_DOMAIN_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.RECONSTRUCT_MASTER_DONE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.RECONSTRUCT_MASTER_FAILED_NO_MASTER, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.RECONSTRUCT_MASTER_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.SYSTEM_MASTER_DOMAIN_NOT_IN_SYNC, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.RECOVERY_STORAGE_POOL_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_MOVED_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_MOVED_TEMPLATE_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_MOVED_TEMPLATE_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_MOVE_TEMPLATE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_COPIED_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_COPIED_TEMPLATE_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_COPIED_TEMPLATE_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_COPY_TEMPLATE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_VM_DISK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_UPDATE_VM_DISK, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_HOTPLUG_DISK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_HOTPLUG_DISK, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_HOTUNPLUG_DISK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_HOTUNPLUG_DISK, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_COPIED_TEMPLATE_DISK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_COPY_TEMPLATE_DISK, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_COPIED_TEMPLATE_DISK_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_COPIED_TEMPLATE_DISK_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_MOVED_VM_DISK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_MOVED_VM_DISK, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_MOVED_VM_DISK_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_MOVED_VM_DISK_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_DISK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_REMOVE_DISK, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FINISHED_REMOVE_DISK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FINISHED_FAILED_REMOVE_DISK, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.IRS_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IRS_DISK_SPACE_LOW_ERROR, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IRS_DISK_SPACE_LOW, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.REFRESH_REPOSITORY_FILE_LIST_SUCCEEDED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.SYSTEM_DEACTIVATED_STORAGE_DOMAIN, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.SYSTEM_CHANGE_STORAGE_POOL_STATUS_PROBLEMATIC_FROM_NON_OPERATIONAL,
                AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.RECOVERY_STORAGE_POOL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.CONNECT_STORAGE_SERVERS_FAILED, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.CONNECT_STORAGE_POOL_FAILED, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.STORAGE_DOMAIN_ERROR, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.REFRESH_REPOSITORY_FILE_LIST_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.STORAGE_ALERT_VG_METADATA_CRITICALLY_FULL, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.STORAGE_ALERT_SMALL_VG_METADATA, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.STORAGE_ACTIVATE_ASYNC, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.SYSTEM_CHANGE_STORAGE_POOL_STATUS_RESET_IRS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ACTIVATED_STORAGE_DOMAIN_ASYNC, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ACTIVATE_STORAGE_DOMAIN_FAILED_ASYNC, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.STORAGE_DOMAIN_TASKS_ERROR, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.UPDATE_OVF_FOR_STORAGE_POOL_FAILED, AuditLogSeverity.WARNING);
    }

    private static void initQuotaSeverities() {
        severities.put(AuditLogType.USER_ADD_QUOTA, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_ADD_QUOTA, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_QUOTA, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_UPDATE_QUOTA, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DELETE_QUOTA, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_DELETE_QUOTA, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_EXCEEDED_QUOTA_VDS_GROUP_GRACE_LIMIT, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_EXCEEDED_QUOTA_VDS_GROUP_LIMIT, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_EXCEEDED_QUOTA_VDS_GROUP_THRESHOLD, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_EXCEEDED_QUOTA_STORAGE_GRACE_LIMIT, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_EXCEEDED_QUOTA_STORAGE_LIMIT, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_EXCEEDED_QUOTA_STORAGE_THRESHOLD, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.QUOTA_STORAGE_RESIZE_LOWER_THEN_CONSUMPTION, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.MISSING_QUOTA_STORAGE_PARAMETERS_PERMISSIVE_MODE, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.MISSING_QUOTA_CLUSTER_PARAMETERS_PERMISSIVE_MODE, AuditLogSeverity.WARNING);
    }

    private static void initVMSeverities() {
        severities.put(AuditLogType.USER_ATTACH_VM_TO_AD_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_VM_TO_AD_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_VM_POOL_TO_AD_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_VM_POOL_TO_AD_GROUP_INTERNAL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_VM_POOL_TO_AD_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_VM_POOL_TO_AD_GROUP_FAILED_INTERNAL, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_VM_TO_AD_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_VM_TO_AD_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_VM_POOL_TO_AD_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_VM_POOL_TO_AD_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_AD_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_AD_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.AUTO_SUSPEND_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.AUTO_SUSPEND_VM_FINISH_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.AUTO_SUSPEND_VM_FINISH_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.AUTO_FAILED_SUSPEND_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_MIGRATION_START, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VM_CANCEL_MIGRATION, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VM_CANCEL_MIGRATION_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_IMPORT, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VM_IMPORT_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.TEMPLATE_IMPORT, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.TEMPLATE_IMPORT_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_IMPORT_INFO, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VM_NOT_RESPONDING, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VM_MIGRATION_TRYING_RERUN, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VM_PAUSED_ENOSPC, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_PAUSED_ERROR, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_PAUSED_EIO, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_PAUSED_EPERM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_POWER_DOWN_FAILED, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_RUN_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_RUN_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_RUN_VM_AS_STATELESS_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_RUN_VM_AS_STATELESS_WITH_DISKS_NOT_ALLOWING_SNAPSHOT,
                AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_RUN_VM_FAILURE_STATELESS_SNAPSHOT_LEFT, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_PAUSE_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_PAUSE_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_SUSPEND_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_SUSPEND_VM_FINISH_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_SUSPEND_VM_FINISH_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_SUSPEND_VM_FINISH_FAILURE_WILL_TRY_AGAIN, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_SUSPEND_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_STOP_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_STOP_SUSPENDED_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_STOP_SUSPENDED_VM_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_SUSPEND_VM_OK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_STOP_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_VM_STARTED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_VM_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_VM_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_ADD_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_UPDATE_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_VM_FINISHED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_VM_FINISHED_WITH_ILLEGAL_DISKS, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_FAILED_REMOVE_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_CHANGE_DISK_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_EJECT_VM_DISK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_EJECT_VM_FLOPPY, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_CHANGE_DISK_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_RESUME_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_RESUME_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_INITIATED_RUN_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_STARTED_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_INITIATED_RUN_VM_FAILED, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_EXPORT_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_EXPORT_VM_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_EXPORT_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_EXPORT_TEMPLATE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_INITIATED_SHUTDOWN_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_SHUTDOWN_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_STOPPED_VM_INSTEAD_OF_SHUTDOWN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_STOPPING_VM_INSTEAD_OF_SHUTDOWN, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_RUN_VM_ON_NON_DEFAULT_VDS, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_ADD_DISK_TO_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_DISK_TO_VM_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_DISK_TO_VM_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_ADD_DISK_TO_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_DISK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_DISK_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_DISK_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_ADD_DISK, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_DISK_FROM_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_REMOVE_DISK_FROM_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_MOVED_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_MOVED_VM_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_MOVED_VM_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_MOVE_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_VM_TEMPLATE_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_VM_TEMPLATE_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_VM_TEMPLATE_FINISHED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_ADD_VM_TEMPLATE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_VM_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_UPDATE_VM_TEMPLATE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_VM_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_REMOVE_VM_TEMPLATE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_USER_TO_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_ATTACH_USER_TO_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_CREATE_SNAPSHOT, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_CREATE_SNAPSHOT_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_CREATE_SNAPSHOT_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_CREATE_LIVE_SNAPSHOT_FINISHED_FAILURE, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_FAILED_CREATE_SNAPSHOT, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_SNAPSHOT, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_SNAPSHOT_FINISHED_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_SNAPSHOT_FINISHED_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_REMOVE_SNAPSHOT, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_TRY_BACK_TO_SNAPSHOT, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_TRY_BACK_TO_SNAPSHOT_FINISH_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_TRY_BACK_TO_SNAPSHOT_FINISH_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_TRY_BACK_TO_SNAPSHOT, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_RESTORE_FROM_SNAPSHOT, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_RESTORE_FROM_SNAPSHOT_START, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_RESTORE_FROM_SNAPSHOT_FINISH_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_RESTORE_FROM_SNAPSHOT_FINISH_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_RESTORE_FROM_SNAPSHOT, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_PASSWORD_CHANGED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_PASSWORD_CHANGE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_CLEAR_UNKNOWN_VMS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_CLEAR_UNKNOWN_VMS, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_MIGRATION_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_MIGRATION_ABORT, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VM_MIGRATION_FAILED_DURING_MOVE_TO_MAINTANANCE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_DOWN_ERROR, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_MIGRATION_DONE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VM_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_WAS_SET_DOWN_DUE_TO_HOST_REBOOT_OR_MANUAL_FENCE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.RUN_VM_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_SET_TO_UNKNOWN_STATUS, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_LOGGED_OUT_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_USER_FROM_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_DETACH_USER_FROM_VM, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.VM_DOWN, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_CHANGE_FLOPPY_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_FAILED_CHANGE_FLOPPY_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VM_MIGRATION_FAILED_FROM_TO, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_VM_POOL_WITH_VMS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_LOGGED_IN_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_LOCKED_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UNLOCKED_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VM_MIGRATION_ON_CONNECT_CHECK_FAILED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VM_MIGRATION_ON_CONNECT_CHECK_SUCCEEDED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DEDICATE_VM_TO_POWERCLIENT, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DEDICATE_VM_TO_POWERCLIENT_FAILED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.VM_CLEARED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.CANNOT_HIBERNATE_RUNNING_VMS_AFTER_CLUSTER_CPU_UPGRADE, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.HA_VM_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.HA_VM_RESTART_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_FAILED_ATTACH_DISK_TO_VM, AuditLogSeverity.ERROR);

    }

    private static void initClusterSeverities() {
        severities.put(AuditLogType.USER_ADD_VDS_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_VDS_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_UPDATE_VDS_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_VDS_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_VDS_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_VDS_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.SYSTEM_UPDATE_VDS_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.SYSTEM_UPDATE_VDS_GROUP_FAILED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_VM_CLUSTER_DEFAULT_HOST_CLEARED, AuditLogSeverity.NORMAL);
    }

    private static void initTagSeverities() {
        severities.put(AuditLogType.USER_UPDATE_TAG, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_UPDATE_TAG_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ADD_TAG, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ADD_TAG_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_REMOVE_TAG, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_REMOVE_TAG_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_USER, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_USER_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_USER_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_USER_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_VM_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_VDS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_VDS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_VDS_FROM_TAG, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_VDS_FROM_TAG_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_VM_FROM_TAG, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_VM_FROM_TAG_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_USER_FROM_TAG, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_USER_FROM_TAG_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_DETACH_USER_GROUP_FROM_TAG, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_DETACH_USER_GROUP_FROM_TAG_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_USER_EXISTS, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_USER_GROUP_EXISTS, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_VM_EXISTS, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_ATTACH_TAG_TO_VDS_EXISTS, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.USER_MOVE_TAG, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.USER_MOVE_TAG_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.UPDATE_TAGS_VM_DEFAULT_DISPLAY_TYPE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.UPDATE_TAGS_VM_DEFAULT_DISPLAY_TYPE_FAILED, AuditLogSeverity.NORMAL);
    }

    private static void initImportExportSeverities() {
        severities.put(AuditLogType.IMPORTEXPORT_EXPORT_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_EXPORT_VM_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IMPORTEXPORT_IMPORT_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_IMPORT_VM_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IMPORTEXPORT_REMOVE_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_REMOVE_VM_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IMPORTEXPORT_GET_VMS_INFO_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IMPORTEXPORT_REMOVE_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_REMOVE_TEMPLATE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IMPORTEXPORT_EXPORT_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_EXPORT_TEMPLATE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.IMPORTEXPORT_STARTING_EXPORT_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_STARTING_IMPORT_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_STARTING_EXPORT_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_STARTING_IMPORT_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_STARTING_REMOVE_TEMPLATE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_STARTING_REMOVE_VM, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.IMPORTEXPORT_FAILED_TO_IMPORT_VM, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.IMPORTEXPORT_FAILED_TO_IMPORT_TEMPLATE, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.IMPORTEXPORT_IMPORT_VM_INVALID_INTERFACES, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE_INVALID_INTERFACES, AuditLogSeverity.NORMAL);
    }

    private static void initNetworkSeverities() {
        severities.put(AuditLogType.NETWORK_ATTACH_NETWORK_TO_VDS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_ATTACH_NETWORK_TO_VDS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_DETACH_NETWORK_FROM_VDS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_DETACH_NETWORK_FROM_VDS_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_ADD_BOND, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_ADD_BOND_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_REMOVE_BOND, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_REMOVE_BOND_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_VDS_NETWORK_MATCH_CLUSTER, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_VDS_NETWORK_NOT_MATCH_CLUSTER, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_REMOVE_VM_INTERFACE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_REMOVE_VM_INTERFACE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_ADD_VM_INTERFACE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_ADD_VM_INTERFACE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_UPDATE_VM_INTERFACE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_UPDATE_VM_INTERFACE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_ADD_TEMPLATE_INTERFACE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_ADD_TEMPLATE_INTERFACE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_REMOVE_TEMPLATE_INTERFACE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_REMOVE_TEMPLATE_INTERFACE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_UPDATE_TEMPLATE_INTERFACE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_UPDATE_TEMPLATE_INTERFACE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_ADD_NETWORK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_ADD_NETWORK_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_REMOVE_NETWORK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_REMOVE_NETWORK_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_UPDATE_NETWORK, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_UPDATE_NETWORK_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_ATTACH_NETWORK_TO_VDS_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_ATTACH_NETWORK_TO_VDS_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_DETACH_NETWORK_TO_VDS_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_DETACH_NETWORK_TO_VDS_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_UPDTAE_NETWORK_ON_CLUSTER, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_UPDTAE_NETWORK_ON_CLUSTER_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_UPDATE_DISPLAY_TO_VDS_GROUP, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_UPDATE_DISPLAY_TO_VDS_GROUP_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_UPDATE_NETWORK_TO_VDS_INTERFACE, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_UPDATE_NETWORK_TO_VDS_INTERFACE_FAILED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_HOST_USING_WRONG_CLUSER_VLAN, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.NETWORK_HOST_MISSING_CLUSER_VLAN, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.NETWORK_COMMINT_NETWORK_CHANGES, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_COMMINT_NETWORK_CHANGES_FAILED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.MAC_POOL_EMPTY, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.MAC_ADDRESS_IS_IN_USE, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.MAC_ADDRESSES_POOL_NOT_INITIALIZED, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.BRIDGED_NETWORK_OVER_MULTIPLE_INTERFACES, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.VDS_NETWORKS_OUT_OF_SYNC, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.NETWORK_ACTIVATE_VM_INTERFACE_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_ACTIVATE_VM_INTERFACE_FAILURE, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.NETWORK_DEACTIVATE_VM_INTERFACE_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.NETWORK_DEACTIVATE_VM_INTERFACE_FAILURE, AuditLogSeverity.ERROR);
        // External Events/Alerts
        severities.put(AuditLogType.EXTERNAL_EVENT_NORMAL, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.EXTERNAL_EVENT_WARNING, AuditLogSeverity.WARNING);
        severities.put(AuditLogType.EXTERNAL_EVENT_ERROR, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.EXTERNAL_ALERT, AuditLogSeverity.ALERT);
    }

    private static void initConfigSeverities() {
        severities.put(AuditLogType.RELOAD_CONFIGURATIONS_SUCCESS, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.RELOAD_CONFIGURATIONS_FAILURE, AuditLogSeverity.ERROR);
    }

    private static void initUserAccountSeverities() {
        severities.put(AuditLogType.USER_ACCOUNT_DISABLED_OR_LOCKED, AuditLogSeverity.ERROR);
        severities.put(AuditLogType.USER_ACCOUNT_PASSWORD_EXPIRED, AuditLogSeverity.ERROR);
    }

    private static void initDwhSeverities() {
        severities.put(AuditLogType.DWH_STOPPED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.DWH_STARTED, AuditLogSeverity.NORMAL);
        severities.put(AuditLogType.DWH_ERROR, AuditLogSeverity.ERROR);
    }

    private static void initMessages() {
        ResourceBundle bundle = readMessagesFromBundle();

        for (String key : bundle.keySet()) {
            try {
                AuditLogType type = AuditLogType.valueOf(key);
                if (!messages.containsKey(type)) {
                    messages.put(type, bundle.getString(key));
                } else {
                    log.errorFormat("The type {0} appears more then once in audit log messages bundle with the values '{1}' and '{2}'",
                            type,
                            messages.get(type),
                            bundle.getString(key));
                }
            } catch (Exception e) {
                log.errorFormat("Cannot convert the string {0} to AuditLogType, the key does not exist in the AuditLogType declared types",
                        bundle.getString(key));
            }
        }
        checkMessages();
    }

    private static ResourceBundle readMessagesFromBundle() {
        try {
            return ResourceBundle.getBundle(APP_ERRORS_MESSAGES_FILE_NAME);
        } catch (MissingResourceException e) {
            log.error("Could not load audit log messages from the file " + APP_ERRORS_MESSAGES_FILE_NAME);
            throw new ApplicationException(e);
        }
    }

    static void checkMessages() {
        AuditLogType[] values = AuditLogType.values();
        if (values.length != messages.size()) {
            for (AuditLogType value : values) {
                if (!messages.containsKey(value)) {
                    log.infoFormat("AuditLogType: {0} not exist in string table", value.toString());
                }
            }
        }
    }

    static void checkSeverities() {
        AuditLogType[] values = AuditLogType.values();
        if (values.length != severities.size()) {
            for (AuditLogType value : values) {
                if (!severities.containsKey(value)) {
                    log.warnFormat("AuditLogType: {0} not have severity. Assumed Normal", value.toString());
                }
            }
        }
    }

    /**
     * Gets the message.
     * @param logType
     *            Type of the log.
     * @return
     */
    public static String GetMessage(AuditLogType logType) {
        String value = "";
        if (messages.containsKey(logType)) {
            value = messages.get(logType);
        }
        return value;
    }

    public static void log(AuditLogableBase auditLogable) {
        AuditLogType logType = auditLogable.getAuditLogTypeValue();
        log(auditLogable, logType);
    }

    public static void log(AuditLogableBase auditLogable, AuditLogType logType) {
        log(auditLogable, logType, "");
    }

    public static void log(AuditLogableBase auditLogable, AuditLogType logType, String loggerString) {
        updateTimeoutLogableObject(auditLogable, logType);

        if (auditLogable == null || auditLogable.getLegal()) {
            String message = null;
            String resolvedMessage = null;
            AuditLogSeverity severity = severities.get(logType);
            if (severity == null) {
                severity = AuditLogSeverity.NORMAL;
                log.infoFormat("No severity for {0} audit log type, assuming Normal severity", logType);
            }
            AuditLog auditLog = null;
            if (auditLogable != null) {
                AuditLog tempVar = null;
                // handle external log messages invoked by plugins via the API
                if (auditLogable.isExternal()) {
                    resolvedMessage = message = loggerString; // message is sent as an argument, no need to resolve.
                    tempVar = new AuditLog(logType,
                                    severity,
                                    resolvedMessage,
                                    auditLogable.getUserId(),
                                    auditLogable.getUserName(),
                                    auditLogable.getVmIdRef(),
                                    auditLogable.getVmName(),
                                    auditLogable.getVdsIdRef(),
                                    auditLogable.getVdsName(),
                                    auditLogable.getVmTemplateIdRef(),
                                    auditLogable.getVmTemplateName(),
                                    auditLogable.getOrigin(),
                                    auditLogable.getCustomEventId(),
                                    auditLogable.getEventFloodInSec(),
                                    auditLogable.getCustomData());
                }
                else if ((message = messages.get(logType)) != null) { // Application log message from AuditLogMessages
                    resolvedMessage = resolveMessage(message, auditLogable);
                    tempVar = new AuditLog(logType, severity, resolvedMessage, auditLogable.getUserId(),
                            auditLogable.getUserName(), auditLogable.getVmIdRef(), auditLogable.getVmName(),
                            auditLogable.getVdsIdRef(), auditLogable.getVdsName(), auditLogable.getVmTemplateIdRef(),
                            auditLogable.getVmTemplateName());
                }
                if (tempVar != null) {
                    tempVar.setstorage_domain_id(auditLogable.getStorageDomainId());
                    tempVar.setstorage_domain_name(auditLogable.getStorageDomainName());
                    tempVar.setstorage_pool_id(auditLogable.getStoragePoolId());
                    tempVar.setstorage_pool_name(auditLogable.getStoragePoolName());
                    tempVar.setvds_group_id(auditLogable.getVdsGroupId());
                    tempVar.setvds_group_name(auditLogable.getVdsGroupName());
                    tempVar.setCorrelationId(auditLogable.getCorrelationId());
                    tempVar.setJobId(auditLogable.getJobId());
                    tempVar.setGlusterVolumeId(auditLogable.getGlusterVolumeId());
                    tempVar.setGlusterVolumeName(auditLogable.getGlusterVolumeName());
                    tempVar.setExternal(auditLogable.isExternal());
                    auditLog = tempVar;
                }
            } else {
                auditLog = new AuditLog(logType, severity, resolvedMessage, null, null, null, null, null, null,
                        null, null);
            }
            if (auditLog != null) {
                getDbFacadeInstance().getAuditLogDao().save(auditLog);
                if (!"".equals(loggerString)) {
                    log.infoFormat(loggerString, resolvedMessage);
                }
            }
        } else if (auditLogable != null) {
            log.infoFormat("No string for {0} type. Use default Log", auditLogable.getAuditLogTypeValue());
            defaultLog(auditLogable);
        }
    }

    /**
     * Update the logged object timeout attribute by log type definition
     * @param auditLogable
     *            the logable object to be updated
     * @param logType
     *            the log type which determine if timeout is used for it
     */
    private static void updateTimeoutLogableObject(AuditLogableBase auditLogable, AuditLogType logType) {
        int duplicateEventsIntrvalValue = (auditLogable.isExternal())
                ?
                Math.max(auditLogable.getEventFloodInSec(), 30) // Min duration for External Events is 30 sec
                :
                logType.getDuplicateEventsIntervalValue();
        if (duplicateEventsIntrvalValue > 0) {
            auditLogable.setEndTime(DateTime.getNow().AddSeconds(logType.getDuplicateEventsIntervalValue()));
            auditLogable.setTimeoutObjectId(ComposeObjectId(auditLogable, logType));
        }
    }

    public static DbFacade getDbFacadeInstance() {
        return DbFacade.getInstance();
    }

    /**
     * Composes an object id from all log id's to identify uniquely each instance.
     * @param logable
     *            the object to log
     * @param logType
     *            the log type associated with the object
     * @return a unique object id
     */
    private static String ComposeObjectId(AuditLogableBase logable, AuditLogType logType) {
        final char DELIMITER = ',';
        StringBuilder sb = new StringBuilder();
        sb.append("type=");
        sb.append(logType);
        sb.append(DELIMITER);
        sb.append("sd=");
        sb.append(logable.getStorageDomainId() == null ? "" : logable.getStorageDomainId().toString());
        sb.append(DELIMITER);
        sb.append("dc=");
        sb.append(logable.getStoragePoolId() == null ? "" : logable.getStoragePoolId().toString());
        sb.append(DELIMITER);
        sb.append("user=");
        sb.append(logable.getUserId() == null ? "" : logable.getUserId().toString());
        sb.append(DELIMITER);
        sb.append("cluster=");
        sb.append(logable.getVdsGroupId().toString());
        sb.append(DELIMITER);
        sb.append("vds=");
        sb.append(logable.getVdsId().toString());
        sb.append(DELIMITER);
        sb.append("vm=");
        sb.append(logable.getVmId().equals(Guid.Empty) ? "" : logable.getVmId().toString());
        sb.append(DELIMITER);
        sb.append("template=");
        sb.append(logable.getVmTemplateId().equals(Guid.Empty) ? "" : logable.getVmTemplateId().toString());
        sb.append(DELIMITER);
        sb.append("customId=");
        sb.append(logable.getCustomId() == null ? "" : logable.getCustomId().toString());
        sb.append(DELIMITER);

        return sb.toString();
    }

    static String resolveMessage(String message, AuditLogableBase logable) {
        String returnValue = message;
        if (logable != null) {
            Map<String, String> map = getAvailableValues(logable);
            returnValue = resolveMessage(message, map);
        }
        return returnValue;
    }

    /**
     * Resolves a message which contains place holders by replacing them with the value from the map.
     *
     * @param message
     *            A text representing a message with place holders
     * @param values
     *            a map of the place holder to its values
     * @return a resolved message
     */
    public static String resolveMessage(String message, Map<String, String> values) {
        Matcher matcher = pattern.matcher(message);

        StringBuffer buffer = new StringBuffer();
        String value;
        String token;
        while (matcher.find()) {
            token = matcher.group();

            // remove leading ${ and trailing }
            token = token.substring(2, token.length() - 1);

            // get value from value map
            value = values.get(token.toLowerCase());
            if (value == null || value.isEmpty()) {
                // replace value with UNKNOWN_VARIABLE_VALUE if value not defined
                value = UNKNOWN_VARIABLE_VALUE;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value)); // put the value into message
        }

        // append the rest of the message
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    static Map<String, String> getAvailableValues(AuditLogableBase logable) {
        Map<String, String> returnValue = new HashMap<String, String>(logable.getCustomValues());
        Class<?> type = AuditLogableBase.class;
        for (PropertyInfo propertyInfo : TypeCompat.GetProperties(type)) {
            Object value = propertyInfo.GetValue(logable, null);
            String stringValue = value != null ? value.toString() : null;
            if (!returnValue.containsKey(propertyInfo.getName().toLowerCase())) {
                returnValue.put(propertyInfo.getName().toLowerCase(), stringValue);
            } else {
                log.errorFormat("Try to add duplicate audit log values with the same name. Type: {0}. Value: {1}",
                        logable.getAuditLogTypeValue(), propertyInfo.getName().toLowerCase());
            }
        }
        List<String> attributes = AuditLogHelper.getCustomLogFields(logable.getClass(), true);
        if (attributes != null && attributes.size() > 0) {
            TypeCompat.getPropertyValues(logable, new HashSet<String>(attributes), returnValue);
        }
        return returnValue;
    }

    private static void defaultLog(AuditLogableBase auditLogable) {
        auditLogable.DefaultLog();
    }
}
