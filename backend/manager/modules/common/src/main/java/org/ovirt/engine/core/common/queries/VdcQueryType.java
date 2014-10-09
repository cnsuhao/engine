package org.ovirt.engine.core.common.queries;

import java.io.Serializable;

public enum VdcQueryType implements Serializable {
    // VM queries
    IsVmWithSameNameExist(VdcQueryAuthType.User),
    GetVmByVmId(VdcQueryAuthType.User),
    GetAllVms(VdcQueryAuthType.User),
    GetVmsRunningOnVDS,
    GetVmsRunningOnVDSCount,
    GetTopSizeVmsFromStorageDomain,
    GetVmCustomProperties(VdcQueryAuthType.User),
    GetVmConfigurationBySnapshot(VdcQueryAuthType.User),
    GetVmsByDiskGuid,
    GetVmPayload(VdcQueryAuthType.User),
    IsBalloonEnabled(VdcQueryAuthType.User),

    // Vds queries
    IsVdsWithSameNameExist,
    IsVdsWithSameHostExist,
    IsVdsWithSameIpExists,
    GetVdsByVdsId(VdcQueryAuthType.User),
    GetVdsByHost,
    GetVdsByName,
    GetVdsByType,
    GetVdsFenceStatus,
    GetNewVdsFenceStatus,
    CanFenceVds,
    GetAgentFenceOptions,
    GetAgentFenceOptions2,
    GetAllChildVlanInterfaces,
    GetAllSiblingVlanInterfaces,
    GetVlanParent,
    GetVdsHooksById,
    GetVdsHooksById2,
    GetAllHosts(VdcQueryAuthType.User),
    GetHostsByClusterId(VdcQueryAuthType.User),
    GetAllVdsByStoragePool(VdcQueryAuthType.User),

    // Vds Networks
    GetVdsInterfacesByVdsId(VdcQueryAuthType.User),
    GetVdsFreeBondsByVdsId,
    GetAllNetworks(VdcQueryAuthType.User),
    GetAllNetworksByClusterId(VdcQueryAuthType.User),
    GetNonOperationalVds,
    GetManagementInterfaceAddressByVmId(VdcQueryAuthType.User),
    // Vm Network
    GetVmInterfacesByVmId(VdcQueryAuthType.User),
    GetVmGuestAgentInterfacesByVmId(VdcQueryAuthType.User),

    // Template Network
    GetTemplateInterfacesByTemplateId(VdcQueryAuthType.User),

    // Networks
    GetVdsGroupsAndNetworksByNetworkId,
    GetVdsAndNetworkInterfacesByNetworkId,
    GetVdsWithoutNetwork,
    GetVmsAndNetworkInterfacesByNetworkId,
    GetVmTemplatesAndNetworkInterfacesByNetworkId,

    // VdsGroups
    GetVdsCertificateSubjectByVdsId(VdcQueryAuthType.User),
    GetVdsCertificateSubjectByVmId(VdcQueryAuthType.User),
    GetAllVdsGroups(VdcQueryAuthType.User),
    GetVdsGroupByVdsGroupId(VdcQueryAuthType.User), // needed when updating VM
    GetVdsGroupById(VdcQueryAuthType.User),
    GetVdsGroupByName(VdcQueryAuthType.User),
    IsVdsGroupWithSameNameExist,
    GetVdsGroupsByStoragePoolId(VdcQueryAuthType.User),

    // Certificate
    GetCACertificate(VdcQueryAuthType.User),

    // VM Templates queries
    IsVmTemlateWithSameNameExist(VdcQueryAuthType.User),
    GetVmTemplate(VdcQueryAuthType.User),
    GetAllVmTemplates(VdcQueryAuthType.User),
    GetVmsByVmTemplateGuid,
    GetVmTemplatesDisks(VdcQueryAuthType.User),
    GetVmTemplatesByStoragePoolId,
    GetVmTemplatesByImageGuid,
    GetSystemPermissions,

    // VM Snapshot queries
    GetAllVmSnapshotsByVmId(VdcQueryAuthType.User),

    // Images queries
    GetAllIsoImagesList(VdcQueryAuthType.User),
    GetAllFloppyImagesList(VdcQueryAuthType.User),
    GetAllIsoImagesListByStoragePoolId(VdcQueryAuthType.User),
    GetAllFloppyImagesListByStoragePoolId(VdcQueryAuthType.User),
    GetAllImagesList(VdcQueryAuthType.User),
    GetAllDisksByVmId(VdcQueryAuthType.User),
    GetAllAttachableDisks(VdcQueryAuthType.User),
    GetAllDisksByStorageDomainId,
    GetAllDisks(VdcQueryAuthType.User),
    GetImageByImageId,
    GetDiskByDiskId,

    // Users queries
    GetUserVmsByUserIdAndGroups(VdcQueryAuthType.User),
    GetTimeLeasedUsersByVmPoolId,
    GetAllDbUsers(VdcQueryAuthType.User),
    GetDbUserByUserId(VdcQueryAuthType.User),
    GetUsersByVmid,
    GetVmsByUserid,
    GetUserMessage,
    GetUserBySessionId(VdcQueryAuthType.User),

    // AdGroups queries
    GetAllAdGroups,
    GetAdGroupsAttachedToTimeLeasedVmPool,
    GetAdGroupById,

    // VM pools queries
    GetVmPoolById(VdcQueryAuthType.User),
    GetVmPoolsMapByVmPoolId,
    GetAllVmPools,
    HasFreeVmsInPool,
    GetAllVmPoolsAttachedToUser(VdcQueryAuthType.User),
    IsVmPoolWithSameNameExists,
    GetVmDataByPoolId(VdcQueryAuthType.User),

    // Tags queries
    GetAllTags,
    GetAllNotReadonlyTags,
    GetRootTag,
    GetTagByTagId,
    GetTagByTagName,
    GetTagsByUserGroupId,
    GetTagsByUserId,
    GetTagsByVmId,
    GetTagsByVdsId,
    GetTagUserMapByTagName,
    GetTagUserGroupMapByTagName,
    GetTagVmMapByTagName,
    GetTagVdsMapByTagName,
    GetTagIdsAndChildrenIdsByRegExp,
    GetTagIdAndChildrenIds,

    // System
    GetSystemStatistics,
    GetStorageStatistics,

    // Bookmarks
    GetBookmarkById,
    GetBookmarkByName,
    GetAllBookmarks,

    // FieldsUpdating
    CanUpdateFieldGeneric,

    // Configuration values
    GetConfigurationValue(VdcQueryAuthType.User),
    GetConfigurationValues(VdcQueryAuthType.User),
    GetTimeZones(VdcQueryAuthType.User),
    GetDefualtTimeZone(VdcQueryAuthType.User),
    GetDiskConfigurationList(VdcQueryAuthType.User),
    GetAvailableStoragePoolVersions(VdcQueryAuthType.User),
    GetAvailableClusterVersionsByStoragePool,

    // AuditLog
    GetVdsMessages,
    GetVmsMessages,
    GetUserMessages,
    GetEventMessages,
    GetAllEventMessages(VdcQueryAuthType.User),
    GetTemplateMessages,
    GetAllAuditLogsByVMName(VdcQueryAuthType.User),
    GetAllAuditLogsByVMTemplateName(VdcQueryAuthType.User),
    GetAuditLogById,

    // Search queries
    Search,
    RegisterSearch,
    UnregisterSearch,
    AdUsersSearch(VdcQueryAuthType.User),
    AdGroupsSearch(VdcQueryAuthType.User),

    // Public services
    GetDomainList(VdcQueryAuthType.User),
    RegisterVds(VdcQueryAuthType.User),
    CheckDBConnection(VdcQueryAuthType.User),
    ValidateSession(VdcQueryAuthType.User),

    // License queries
    GetLicenseProperties(VdcQueryAuthType.User),
    GetLicenseProductType,
    GetResourceUsage,
    GetPowerClient,
    AddPowerClient,
    GetDedicatedVm,
    GetAllServerCpuList,
    GetAvailableClustersByServerCpu,

    // Multi Level Administration queries
    GetAllRoles(VdcQueryAuthType.User),
    GetRolesByAdElement,
    GetRolesByAdElementIdAndNullTag,
    GetRoleById(VdcQueryAuthType.User),
    GetRoleByName,
    GetPermissionById,
    GetPermissionByRoleId,
    GetPermissionsByAdElement,
    GetRolesByAdElementId,
    GetPermissionsByAdElementId(VdcQueryAuthType.User),
    GetRoleActionGroupsByRoleId(VdcQueryAuthType.User),
    IsUserPowerUserOrAbove,
    GetRolesForDelegationByUser,
    GetPermissionsForObject(VdcQueryAuthType.User),
    GetAllStoragePools(VdcQueryAuthType.User),
    GetDataCentersWithPermittedActionOnClusters(VdcQueryAuthType.User),
    GetClustersWithPermittedAction(VdcQueryAuthType.User),
    GetVmTemplatesWithPermittedAction(VdcQueryAuthType.User),

    // Storage
    IsStoragePoolWithSameNameExist,
    GetStorageDomainById(VdcQueryAuthType.User),
    GetStorageServerConnectionById,
    GetStoragePoolById(VdcQueryAuthType.User),
    GetStorageDomainsByConnection,
    GetStorageDomainsByStoragePoolId(VdcQueryAuthType.User),
    GetStorageDomainsByImageId,
    GetStorageServerConnections,
    GetVgList,
    GetVGInfo,
    GetDeviceList,
    DiscoverSendTargets,
    GetStorageSessionsList,
    GetStorageDomainsByVmTemplateId(VdcQueryAuthType.User),
    GetVmsFromExportDomain("org.ovirt.engine.core.bll.storage"),
    GetTemplatesFromExportDomain,
    GetVmTemplatesFromStorageDomain(VdcQueryAuthType.User),
    GetAllIdsFromExportDomain,
    GetAllStorageDomains(VdcQueryAuthType.User),
    GetExistingStorageDomainList,
    GetStorageDomainByIdAndStoragePoolId,
    GetStoragePoolsByStorageDomainId,
    GetStorageDomainListById,
    GetLunsByVgId,
    GetPermittedStorageDomainsByStoragePoolId(VdcQueryAuthType.User),
    GetPermittedStorageDomainsByTemplateId(VdcQueryAuthType.User),

    // Event Notification
    GetEventNotificationMethods,
    GetEventNotificationMethodByType,
    GetNotificationEventMap,
    GetAllEventSubscribers,
    GetEventSubscribersBySubscriberId,
    GetEventSubscribersBySubscriberIdGrouped,

    // oVirt
    GetoVirtISOs,

    // Async Tasks
    GetTasksStatusesByTasksIDs,

    // Quota
    GetQuotaByStoragePoolId,
    GetQuotaByQuotaId,
    GetQuotaVdsGroupByQuotaId,
    GetQuotaStorageByQuotaId,
    GetDisksForQuotaId,
    GetVmsRelatedToQuotaId,
    GetTemplatesRelatedToQuotaId,
    GetPermissionsToConsumeQuotaByQuotaId,
    GetQuotasByAdElementId,
    GetQuotasConsumptionForCurrentUser(VdcQueryAuthType.User),
    GetAllRelevantQuotasForStorage(VdcQueryAuthType.User),
    GetAllRelevantQuotasForVdsGroup(VdcQueryAuthType.User),

    // Jobs
    GetJobByJobId,
    GetJobsByCorrelationId,
    GetJobsByOffset,

    // Commands
    GetCommandsCompatibilityVersions(VdcQueryAuthType.User),

    // Disks
    GetNextAvailableDiskAliasNameByVMId(VdcQueryAuthType.User),

    // Gluster
    GetGlusterVolumeById,
    GetGlusterVolumeOptionsInfo,
    GetGlusterVolumeBricks,
    GetGlusterBrickById,
    GetServerSSHKeyFingerprint,
    GetGlusterServersForImport,
    GetAddedGlusterServers,
    GetGlusterVolumeAdvancedDetails,
    GetGlusterVolumeProfileInfo,
    GetDefaultConfigurationVersion(VdcQueryAuthType.User),
    // Default type instead of having to null check
    Unknown(VdcQueryAuthType.User);

    /**
     * What kind of authorization the query requires. Although this is essentially a <code>boolean</code>, it's
     * implemented as an enum for future extendability.
     */
    public static enum VdcQueryAuthType {
        Admin,
        User
    }

    private static final String DEFAULT_PACKAGE_NAME = "org.ovirt.engine.core.bll";

    private String packageName;

    private VdcQueryAuthType authType;

    private VdcQueryType() {
        packageName = DEFAULT_PACKAGE_NAME;
        authType = VdcQueryAuthType.Admin;
    }

    private VdcQueryType(String packageName) {
        this.packageName = packageName;
        authType = VdcQueryAuthType.Admin;
    }

    private VdcQueryType(VdcQueryAuthType authType) {
        packageName = DEFAULT_PACKAGE_NAME;
        this.authType = authType;
    }

    public int getValue() {
        return this.ordinal();
    }

    public static VdcQueryType forValue(int value) {
        return values()[value];
    }

    public String getPackageName() {
        return packageName;
    }

    public VdcQueryAuthType getAuthType() {
        return authType;
    }

    public boolean isAdmin() {
        return authType == VdcQueryAuthType.Admin;
    }
}
