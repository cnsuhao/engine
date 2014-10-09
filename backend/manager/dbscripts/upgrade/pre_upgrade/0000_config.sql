/************************************************************************************
                DATABASE APPLICATION CONFIGURATION FILE

This file is used to update the vdc_options configuration table.
The following sections are available:
    Add Section
    Update section (w/o overriding current value)
    Delete section
    Split config section
    Simple upgrades not available using a fn_db* function call
    Complex upgrades using temporary functions

In each section (except simple/function sections), entries are ordered by key,
please keep this when modifing this file.

PLEASE NOTE THAT THIS SCRIPT MUST REMAIN RE-ENTRANT!

************************************************************************************/


------------------------------------------------------------------------------------
--                  Add configuration values section
------------------------------------------------------------------------------------

select fn_db_add_config_value('BootstrapMinimalVdsmVersion','4.9','general');
select fn_db_add_config_value('CpuPinMigrationEnabled','true','general');
select fn_db_add_config_value('CpuPinningEnabled','false','3.0');
select fn_db_add_config_value('CpuPinningEnabled','true','3.1');
select fn_db_add_config_value('CpuPinningEnabled','true','3.2');
select fn_db_add_config_value('AdminDomain','internal','general');
select fn_db_add_config_value('AdminPassword','','general');
select fn_db_add_config_value('AdminUser','admin','general');
select fn_db_add_config_value('AdUserId','','general');
select fn_db_add_config_value('AdUserName','','general');
select fn_db_add_config_value('AdUserPassword','','general');
select fn_db_add_config_value('AdvancedNFSOptionsEnabled','false','3.0');
select fn_db_add_config_value('AdvancedNFSOptionsEnabled','true','3.1');
select fn_db_add_config_value('AdvancedNFSOptionsEnabled','true','3.2');
select fn_db_add_config_value('AgentAppName','RHEV-Agent','general');
select fn_db_add_config_value('AllowClusterWithVirtGlusterEnabled','true','general');
select fn_db_add_config_value('AllowDuplicateMacAddresses','false','general');
select fn_db_add_config_value('ApplicationMode','255','general');
select fn_db_add_config_value('AsyncTaskPollingRate','10','general');
select fn_db_add_config_value('AsyncTaskStatusCacheRefreshRateInSeconds','30','general');
select fn_db_add_config_value('AsyncTaskStatusCachingTimeInMinutes','1','general');
select fn_db_add_config_value('AsyncTaskZombieTaskLifeInMinutes','300','general');
select fn_db_add_config_value('AuditLogAgingThreashold','30','general');
select fn_db_add_config_value('AuditLogCleanupTime','03:35:35','general');
select fn_db_add_config_value('OnlyRequiredNetworksMandatoryForVdsSelection','false','general');
--Handling Authentication Method
select fn_db_add_config_value('AuthenticationMethod','LDAP','general');
select fn_db_add_config_value('AutoRecoverySchedule','0 0/5 * * * ?','general');
select fn_db_add_config_value('AutoRepoDomainRefreshTime','60','general');
select fn_db_add_config_value('BlockMigrationOnSwapUsagePercentage','0','general');
--Handling CA Base Directory
select fn_db_add_config_value('CABaseDirectory','','general');
--Handling CA certificate path
select fn_db_add_config_value('CACertificatePath','ca/certs.pem','general');
--Handling Certificate alias
select fn_db_add_config_value('CertAlias','1','general');
--Handling Certificate File Name
select fn_db_add_config_value('CertificateFileName','','general');
select fn_db_add_config_value('CipherSuite','DEFAULT','general');
--Handling Configuration directory for ENGINE
select fn_db_add_config_value('ConfigDir','/etc/engine','general');
select fn_db_add_config_value('ConnectToServerTimeoutInSeconds','20','general');
select fn_db_add_config_value('CpuOverCommitDurationMinutes','2','general');
--Handling Data directory for ENGINE
select fn_db_add_config_value('DataDir','/usr/share/engine','general');
select fn_db_add_config_value('DBEngine','Postgres','general');
select fn_db_add_config_value('DebugTimerLogging','true','general');
select fn_db_add_config_value('DefaultMaxThreadPoolSize','500','general');
select fn_db_add_config_value('DefaultMinThreadPoolSize','50','general');
select fn_db_add_config_value('DefaultTimeZone','(GMT) GMT Standard Time','general');
--Handling Default Workgroup
select fn_db_add_config_value('DefaultWorkgroup','WORKGROUP','general');
select fn_db_add_config_value('DesktopAudioDeviceType','WindowsXP,ac97,RHEL4,ac97,RHEL3,ac97,Windows2003x64,ac97,RHEL4x64,ac97,RHEL3x64,ac97,OtherLinux,ac97,Other,ac97,default,ich6','3.0');
select fn_db_add_config_value('DesktopAudioDeviceType','WindowsXP,ac97,RHEL4,ac97,RHEL3,ac97,Windows2003x64,ac97,RHEL4x64,ac97,RHEL3x64,ac97,OtherLinux,ac97,Other,ac97,default,ich6','3.1');
select fn_db_add_config_value('DesktopAudioDeviceType','WindowsXP,ac97,RHEL4,ac97,RHEL3,ac97,Windows2003x64,ac97,RHEL4x64,ac97,RHEL3x64,ac97,OtherLinux,ac97,Other,ac97,default,ich6','3.2');
select fn_db_add_config_value('DisableFenceAtStartupInSec','300','general');
select fn_db_add_config_value('DiskConfigurationList','System,Sparse,COW,true;Data,Preallocated,RAW,false;Shared,Preallocated,RAW,false;Swap,Preallocated,RAW,false;Temp,Sparse,COW,false','general');
select fn_db_add_config_value('DirectLUNDiskEnabled','false','3.0');
select fn_db_add_config_value('DirectLUNDiskEnabled','true','3.1');
select fn_db_add_config_value('DirectLUNDiskEnabled','true','3.2');
select fn_db_add_config_value('DocsURL','docs','general');
--Handling NetBIOS Domain Name
select fn_db_add_config_value('DomainName','example.com','general');
select fn_db_add_config_value('EmulatedMachine','rhel6.0.0','3.0');
select fn_db_add_config_value('EmulatedMachine','pc-0.14','3.1');
select fn_db_add_config_value('EmulatedMachine','pc-0.14','3.2');
-- Host time drift
select fn_db_add_config_value('EnableHostTimeDrift','false','general');
--Handling Enable Spice Root Certification Validation
select fn_db_add_config_value('EnableSpiceRootCertificateValidation','true','general');
select fn_db_add_config_value('EnableSwapCheck','true','general');
--Handling Enable USB devices attachment to the VM by default
select fn_db_add_config_value('EnableUSBAsDefault','true','general');
--Handling Enables Host Load Balancing system.
select fn_db_add_config_value('EnableVdsLoadBalancing','true','general');
select fn_db_add_config_value('ENGINEEARLib','','general');
--Handling Engine working mode
select fn_db_add_config_value('EngineMode','Active','general');
--Handling Use Default Credentials
select fn_db_add_config_value('FailedJobCleanupTimeInMinutes','60','general');
select fn_db_add_config_value('FenceAgentDefaultParams','ilo3:lanplus,power_wait=4;ilo4:lanplus,power_wait=4','general');
select fn_db_add_config_value('FenceAgentMapping','ilo2=ilo,ilo3=ipmilan,ilo4=ipmilan','general');
select fn_db_add_config_value('FenceProxyDefaultPreferences','cluster,dc','general');
select fn_db_add_config_value('FenceQuietTimeBetweenOperationsInSec','180','general');
select fn_db_add_config_value('FenceStartStatusDelayBetweenRetriesInSec','60','general');
select fn_db_add_config_value('FenceStartStatusRetries','3','general');
select fn_db_add_config_value('FenceStopStatusDelayBetweenRetriesInSec','60','general');
select fn_db_add_config_value('FenceStopStatusRetries','3','general');
select fn_db_add_config_value('FilteringLUNsEnabled','true','3.0');
select fn_db_add_config_value('FilteringLUNsEnabled','false','3.1');
select fn_db_add_config_value('FilteringLUNsEnabled','false','3.2');
select fn_db_add_config_value('FindFenceProxyDelayBetweenRetriesInSec','30','general');
select fn_db_add_config_value('FindFenceProxyRetries','3','general');
select fn_db_add_config_value('FreeSpaceCriticalLowInGB','5','general');
select fn_db_add_config_value('FreeSpaceLow','10','general');
select fn_db_add_config_value('GlusterVolumeOptionGroupVirtValue','virt','general');
select fn_db_add_config_value('GlusterVolumeOptionOwnerUserVirtValue','36','general');
select fn_db_add_config_value('GlusterVolumeOptionOwnerGroupVirtValue','36','general');
select fn_db_add_config_value('GuestToolsSetupIsoPrefix','RHEV-toolsSetup_','general');
select fn_db_add_config_value('HardwareInfoEnabled','false','3.0');
select fn_db_add_config_value('HardwareInfoEnabled','false','3.1');
select fn_db_add_config_value('HardwareInfoEnabled','true','3.2');
select fn_db_add_config_value('HighUtilizationForEvenlyDistribute','75','general');
select fn_db_add_config_value('HighUtilizationForPowerSave','75','general');
select fn_db_add_config_value('HostTimeDriftInSec','300','general');
select fn_db_add_config_value('HotPlugEnabled','false','3.0');
select fn_db_add_config_value('HotPlugEnabled','true','3.1');
select fn_db_add_config_value('HotPlugEnabled','true','3.2');
select fn_db_add_config_value('NetworkLinkingSupported','false','3.0');
select fn_db_add_config_value('NetworkLinkingSupported','false','3.1');
select fn_db_add_config_value('NetworkLinkingSupported','true','3.2');
select fn_db_add_config_value('DisconnectPoolOnReconstruct','0,2','general');
select fn_db_add_config_value('HotPlugUnsupportedOsList','','general');
select fn_db_add_config_value('InitStorageSparseSizeInGB','1','general');
--Handling Install virtualization software on Add Host

select fn_db_add_config_value('InstallVds','true','general');
select fn_db_add_config_value('IoOpTimeoutSec','10','general');
select fn_db_add_config_value('IPTablesConfig',
'# oVirt default firewall configuration. Automatically generated by vdsm bootstrap script.
*filter
:INPUT ACCEPT [0:0]
:FORWARD ACCEPT [0:0]
:OUTPUT ACCEPT [0:0]
-A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
-A INPUT -p icmp -j ACCEPT
-A INPUT -i lo -j ACCEPT
# vdsm
-A INPUT -p tcp --dport 54321 -j ACCEPT
# libvirt tls
-A INPUT -p tcp --dport 16514 -j ACCEPT
# SSH
-A INPUT -p tcp --dport 22 -j ACCEPT
# guest consoles
-A INPUT -p tcp -m multiport --dports 5634:6166 -j ACCEPT
# migration
-A INPUT -p tcp -m multiport --dports 49152:49216 -j ACCEPT
# snmp
-A INPUT -p udp --dport 161 -j ACCEPT
# Reject any other input traffic
-A INPUT -j REJECT --reject-with icmp-host-prohibited
-A FORWARD -m physdev ! --physdev-is-bridged -j REJECT --reject-with icmp-host-prohibited
COMMIT
','general');
select fn_db_add_config_value('IPTablesConfigForGluster',
'
# glusterd
-A INPUT -p tcp -m tcp --dport 24007 -j ACCEPT

# portmapper
-A INPUT -p udp -m udp --dport 111   -j ACCEPT
-A INPUT -p tcp -m tcp --dport 38465 -j ACCEPT
-A INPUT -p tcp -m tcp --dport 38466 -j ACCEPT

# nfs
-A INPUT -p tcp -m tcp --dport 38467 -j ACCEPT

# status
-A INPUT -p tcp -m tcp --dport 39543 -j ACCEPT
-A INPUT -p tcp -m tcp --dport 55863 -j ACCEPT

# nlockmgr
-A INPUT -p tcp -m tcp --dport 38468 -j ACCEPT
-A INPUT -p udp -m udp --dport 963   -j ACCEPT
-A INPUT -p tcp -m tcp --dport 965   -j ACCEPT

# ctdbd
-A INPUT -p tcp -m tcp --dport 4379  -j ACCEPT

# smbd
-A INPUT -p tcp -m tcp --dport 139   -j ACCEPT
-A INPUT -p tcp -m tcp --dport 445   -j ACCEPT

# Ports for gluster volume bricks (default 100 ports)
-A INPUT -p tcp -m tcp --dport 24009:24108 -j ACCEPT
','general');
select fn_db_add_config_value('IPTablesConfigForVirt',
'
# libvirt tls
-A INPUT -p tcp --dport 16514 -j ACCEPT

# guest consoles
-A INPUT -p tcp -m multiport --dports 5634:6166 -j ACCEPT

# migration
-A INPUT -p tcp -m multiport --dports 49152:49216 -j ACCEPT
', 'general');


select fn_db_add_config_value('IsMultilevelAdministrationOn','true','general');
select fn_db_add_config_value('JobCleanupRateInMinutes','10','general');
select fn_db_add_config_value('JobPageSize','100','general');
select fn_db_add_config_value('keystorePass','','general');
--Handling Keystore URL
select fn_db_add_config_value('keystoreUrl','','general');
select fn_db_add_config_value('LdapQueryPageSize','1000','general');
select fn_db_add_config_value('LDAPQueryTimeout','30','general');
select fn_db_add_config_value('LDAPConnectTimeout','30','general');
select fn_db_add_config_value('LDAPOperationTimeout','30','general');
--Handling LDAP Security Authentication Method
select fn_db_add_config_value('LDAPSecurityAuthentication','GSSAPI','general');
select fn_db_add_config_value('LDAPServerPort','389','general');
select fn_db_add_config_value('LdapServers','','general');
select fn_db_add_config_value('LDAPProviderTypes','','general');
select fn_db_add_config_value('LeaseRetries','3','general');
select fn_db_add_config_value('LeaseTimeSec','60','general');
select fn_db_add_config_value('LicenseCertificateFingerPrint','5f 38 41 89 b1 33 49 0c 24 13 6b b3 e5 ba 9e c7 fd 83 80 3b','general');
select fn_db_add_config_value('LiveSnapshotEnabled','false','3.0');
select fn_db_add_config_value('LiveSnapshotEnabled','true','3.1');
select fn_db_add_config_value('LiveSnapshotEnabled','true','3.2');
select fn_db_add_config_value('LiveStorageMigrationEnabled','false','3.0');
select fn_db_add_config_value('LiveStorageMigrationEnabled','false','3.1');
select fn_db_add_config_value('LiveStorageMigrationEnabled','true','3.2');
select fn_db_add_config_value('LocalAdminPassword','123456','general');
--Handling Enable lock policy for Storage Pool Manager on activation
select fn_db_add_config_value('LockPolicy','ON','general');
select fn_db_add_config_value('LockRenewalIntervalSec','5','general');
select fn_db_add_config_value('LogPhysicalMemoryThresholdInMB','1024','general');
--Handling Log XML-RPC Data
select fn_db_add_config_value('LowUtilizationForEvenlyDistribute','0','general');
select fn_db_add_config_value('LowUtilizationForPowerSave','20','general');
select fn_db_add_config_value('MacPoolRanges','00:1A:4A:16:01:51-00:1A:4A:16:01:e6','general');
select fn_db_add_config_value('ManagedDevicesWhiteList','','general');
select fn_db_add_config_value('ManagementNetwork','ovirtmgmt','general');
select fn_db_add_config_value('MaxAuditLogMessageLength','10000','general');
select fn_db_add_config_value('MaxBlockDiskSize','8192','general');
select fn_db_add_config_value('MaxLDAPQueryPartsNumber','100','general');
select fn_db_add_config_value('MaxMacsCountInPool','100000','general');
select fn_db_add_config_value('MaxNumberOfHostsInStoragePool','250','general');
select fn_db_add_config_value('MaxNumOfCpuPerSocket','16','3.0');
select fn_db_add_config_value('MaxNumOfCpuPerSocket','16','3.1');
select fn_db_add_config_value('MaxNumOfCpuPerSocket','16','3.2');
select fn_db_add_config_value('MaxNumOfVmCpus','64','3.0');
select fn_db_add_config_value('MaxNumOfVmCpus','160','3.1');
select fn_db_add_config_value('MaxNumOfVmCpus','160','3.2');
select fn_db_add_config_value('MaxNumOfVmSockets','16','3.0');
select fn_db_add_config_value('MaxNumOfVmSockets','16','3.1');
select fn_db_add_config_value('MaxNumOfVmSockets','16','3.2');
select fn_db_add_config_value('MaxRerunVmOnVdsCount','3','general');
select fn_db_add_config_value('MaxStorageVdsDelayCheckSec','5','general');
select fn_db_add_config_value('MaxStorageVdsTimeoutCheckSec','30','general');
select fn_db_add_config_value('MaxVdsMemOverCommit','200','general');
select fn_db_add_config_value('MaxVdsMemOverCommitForServers','150','general');
select fn_db_add_config_value('MaxVdsNameLength','255','general');
select fn_db_add_config_value('MaxVmNameLengthNonWindows','64','general');
select fn_db_add_config_value('MaxVmNameLengthWindows','15','general');
select fn_db_add_config_value('MaxVmsInPool','1000','general');
select fn_db_add_config_value('MinimalETLVersion','3.0.0','general');
select fn_db_add_config_value('NativeUSBEnabled','false','3.0');
select fn_db_add_config_value('NativeUSBEnabled','true','3.1');
select fn_db_add_config_value('NativeUSBEnabled','true','3.2');
select fn_db_add_config_value('NicDHCPDelayGraceInMS','60','general');
select fn_db_add_config_value('NonVmNetworkSupported','false','3.0');
select fn_db_add_config_value('NonVmNetworkSupported','true','3.1');
select fn_db_add_config_value('NonVmNetworkSupported','true','3.2');
select fn_db_add_config_value('NumberOfFailedRunsOnVds','3','general');
select fn_db_add_config_value('NumberOfUSBSlots','4','general');
select fn_db_add_config_value('NumberOfVmsForTopSizeVms','10','general');
select fn_db_add_config_value('NumberVmRefreshesBeforeSave','5','general');
select fn_db_add_config_value('SupportBridgesReportByVDSM','false','3.0');
select fn_db_add_config_value('SupportBridgesReportByVDSM','false','3.1');
select fn_db_add_config_value('SupportBridgesReportByVDSM','true','3.2');
select fn_db_add_config_value('EnableMACAntiSpoofingFilterRules','false', '3.0');
select fn_db_add_config_value('EnableMACAntiSpoofingFilterRules','false', '3.1');
select fn_db_add_config_value('EnableMACAntiSpoofingFilterRules','true', '3.2');
select fn_db_add_config_value('MTUOverrideSupported','false','3.0');
select fn_db_add_config_value('MTUOverrideSupported','true','3.1');
select fn_db_add_config_value('MTUOverrideSupported','true','3.2');
--Handling Organization Name
select fn_db_add_config_value('OrganizationName','oVirt','general');
select fn_db_add_config_value('OriginType','OVIRT','general');
select fn_db_add_config_value('OvfVirtualSystemType','ENGINE','general');
--Handling The ovirt-node installation files path
select fn_db_add_config_value('OvirtInitialSupportedIsoVersion','2.5.5','general');
select fn_db_add_config_value('OvirtIsoPrefix','ovirt-node','general');
select fn_db_add_config_value('oVirtISOsRepositoryPath','/usr/share/ovirt-node-iso','general');
select fn_db_add_config_value('oVirtUpgradeScriptName','/usr/share/vdsm-reg/vdsm-upgrade','general');
select fn_db_add_config_value('oVirtUploadPath','/data/updates/ovirt-node-image.iso','general');
select fn_db_add_config_value('OvfUpdateIntervalInMinutes','60','general');
select fn_db_add_config_value('OvfItemsCountPerUpdate','100','general');
select fn_db_add_config_value('PayloadSize','8192','general');
select fn_db_add_config_value('PosixStorageEnabled','false','3.0');
select fn_db_add_config_value('PosixStorageEnabled','true','3.1');
select fn_db_add_config_value('PosixStorageEnabled','true','3.2');
select fn_db_add_config_value('PostgresI18NPrefix','','general');
select fn_db_add_config_value('PostgresLikeSyntax','ILIKE','general');
select fn_db_add_config_value('PostgresPagingSyntax',E' OFFSET (%1$s -1) LIMIT %2$s','general');
select fn_db_add_config_value('PostgresPagingType','Offset','general');
select fn_db_add_config_value('PostgresSearchTemplate',E'SELECT * FROM (%2$s) %1$s) as T1 %3$s','general');
--Handling Allow Running Guests Without Tools
select fn_db_add_config_value('PowerClientAllowRunningGuestsWithoutTools','false','general');
--Handling Auto-AdjustMemory Base On Available Memory
select fn_db_add_config_value('PowerClientAutoAdjustMemoryBaseOnAvailableMemory','false','general');
--Handling Client Auto Adjust Memory
select fn_db_add_config_value('PowerClientAutoAdjustMemory','false','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemoryGeneralReserve','768','general');
--Handling Auto-Adjust Memory Log
select fn_db_add_config_value('PowerClientAutoAdjustMemoryLog','false','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemoryMaxMemory','2048','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemoryModulus','64','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemorySpicePerMonitorReserve','0','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemorySpicePerSessionReserve','0','general');
--Handling Auto Approve Patterns
select fn_db_add_config_value('PowerClientAutoApprovePatterns','','general');
select fn_db_add_config_value('PowerClientAutoInstallCertificateOnApprove','true','general');
select fn_db_add_config_value('PowerClientAutoMigrateFromPowerClientToVdsWhenConnectingFromRegularClient','false','general');
--Handling AutoMigrate To PowerClient On Connect
select fn_db_add_config_value('PowerClientAutoMigrateToPowerClientOnConnect','false','general');
select fn_db_add_config_value('PowerClientAutoRegistrationDefaultVdsGroupID','99408929-82CF-4DC7-A532-9D998063FA95','general');
select fn_db_add_config_value('PowerClientDedicatedVmLaunchOnVdsWhilePowerClientStarts','false','general');
--Handling Enable Power Client GUI
select fn_db_add_config_value('PowerClientLogDetection','false','general');
select fn_db_add_config_value('PowerClientMaxNumberOfConcurrentVMs','1','general');
select fn_db_add_config_value('PowerClientRunVmShouldVerifyPendingVMsAsWell','false','general');
--Handling Spice Dynamic Compression Management
select fn_db_add_config_value('PowerClientSpiceDynamicCompressionManagement','false','general');
select fn_db_add_config_value('PredefinedVMProperties','sap_agent=^(true|false)$;sndbuf=^[0-9]+$;vhost=^(([a-zA-Z0-9_]*):(true|false))(,(([a-zA-Z0-9_]*):(true|false)))*$;viodiskcache=^(none|writeback|writethrough)$','3.0');
select fn_db_add_config_value('PredefinedVMProperties','sap_agent=^(true|false)$;sndbuf=^[0-9]+$;vhost=^(([a-zA-Z0-9_]*):(true|false))(,(([a-zA-Z0-9_]*):(true|false)))*$;viodiskcache=^(none|writeback|writethrough)$','3.1');
select fn_db_add_config_value('PredefinedVMProperties','sap_agent=^(true|false)$;sndbuf=^[0-9]+$;vhost=^(([a-zA-Z0-9_]*):(true|false))(,(([a-zA-Z0-9_]*):(true|false)))*$;viodiskcache=^(none|writeback|writethrough)$','3.2');
select fn_db_add_config_value('ProductKey2003','','general');
select fn_db_add_config_value('ProductKey2003x64','','general');
select fn_db_add_config_value('ProductKey2008','','general');
select fn_db_add_config_value('ProductKey2008R2','','general');
select fn_db_add_config_value('ProductKey2008x64','','general');
--Handling Product Key (for Windows XP)
select fn_db_add_config_value('ProductKey','','general');
select fn_db_add_config_value('ProductKeyWindow7','','general');
select fn_db_add_config_value('ProductKeyWindow7x64','','general');
select fn_db_add_config_value('ProductKeyWindows8','','general');
select fn_db_add_config_value('ProductKeyWindows8x64','','general');
select fn_db_add_config_value('ProductKeyWindows2012x64','','general');
select fn_db_add_config_value('ProductRPMVersion','3.0.0.0','general');
select fn_db_add_config_value('QuotaGraceStorage','20','general');
select fn_db_add_config_value('QuotaGraceVdsGroup','20','general');
select fn_db_add_config_value('QuotaThresholdStorage','80','general');
select fn_db_add_config_value('QuotaThresholdVdsGroup','80','general');
--Handling Connect to RDP console with Fully Qualified User-Name (user@domain)
select fn_db_add_config_value('RedirectServletReportsPage','','general');
select fn_db_add_config_value('RhevhLocalFSPath','/data/images/','general');
select fn_db_add_config_value('SANWipeAfterDelete','false','general');
--Handling SASL QOP
select fn_db_add_config_value('SASL_QOP','auth-conf','general');
select fn_db_add_config_value('SearchResultsLimit','100','general');
select fn_db_add_config_value('SendSMPOnRunVm','true','general');
select fn_db_add_config_value('SendVmTicketUID','false','3.0');
select fn_db_add_config_value('SendVmTicketUID','true','3.1');
select fn_db_add_config_value('SendVmTicketUID','true','3.2');
select fn_db_add_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3;','3.0');
select fn_db_add_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3;','3.1');
select fn_db_add_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 7:Intel SandyBridge Family:vmx,nx,model_SandyBridge:SandyBridge; 8:Intel Haswell:vmx,nx,model_Haswell:Haswell; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3; 5:AMD Opteron G4:svm,nx,model_Opteron_G4:Opteron_G4; 6:AMD Opteron G5:smx,nx,model_Opteron_G5:Opteron_G5;','3.2');
select fn_db_add_config_value('ServerRebootTimeout','300','general');
select fn_db_add_config_value('SetupNetworksPollingTimeout','3','general');
-- Add shareable disk property in vdc_options to support only 3.1 version.
select fn_db_add_config_value('ShareableDiskEnabled','false','3.0');
select fn_db_add_config_value('ShareableDiskEnabled','true','3.1');
select fn_db_add_config_value('ShareableDiskEnabled','true','3.2');
select fn_db_add_config_value('SignCertTimeoutInSeconds','30','general');
select fn_db_add_config_value('SignLockFile','/var/lock/engine/.openssl.exclusivelock','general');
--Handling Script name for signing
select fn_db_add_config_value('SignScriptName','SignReq.sh','general');
select fn_db_add_config_value('SpiceDriverNameInGuest','RHEV-Spice','general');
select fn_db_add_config_value('SpiceReleaseCursorKeys','shift+f12','general');
select fn_db_add_config_value('SpiceToggleFullScreenKeys','shift+f11','general');
--Handling Enable USB devices sharing by default in SPICE
select fn_db_add_config_value('SpiceUsbAutoShare','true','general');
select fn_db_add_config_value('WANDisableEffects','animation','general');
select fn_db_add_config_value('WANColorDepth','16','general');
select fn_db_add_config_value('SpmCommandFailOverRetries','3','general');
select fn_db_add_config_value('SPMFailOverAttempts','3','general');
select fn_db_add_config_value('SpmVCpuConsumption','1','general');
select fn_db_add_config_value('SSHInactivityTimoutSeconds','300','general');
select fn_db_add_config_value('SSHInactivityHardTimoutSeconds','1800','general');
--Handling SPICE SSL Enabled
select fn_db_add_config_value('SSLEnabled','true','general');
select fn_db_add_config_value('StorageDomainFalureTimeoutInMinutes','5','general');
select fn_db_add_config_value('StorageDomainNameSizeLimit','50','general');
select fn_db_add_config_value('StoragePoolNameSizeLimit','40','general');
select fn_db_add_config_value('StoragePoolNonOperationalResetTimeoutInMin','3','general');
select fn_db_add_config_value('StoragePoolRefreshTimeInSeconds','10','general');
select fn_db_add_config_value('SucceededJobCleanupTimeInMinutes','10','general');
select fn_db_add_config_value('SupportedClusterLevels','3.0','general');
select fn_db_add_config_value('SupportedStorageFormats','0,2','3.0');
select fn_db_add_config_value('SupportedStorageFormats','0,2,3','3.1');
select fn_db_add_config_value('SupportedStorageFormats','0,2,3','3.2');
select fn_db_add_config_value('SupportedVDSMVersions','4.5,4.9','general');
select fn_db_add_config_value('SupportForceCreateVG','false','3.0');
select fn_db_add_config_value('SupportForceCreateVG','true','3.1');
select fn_db_add_config_value('SupportForceCreateVG','true','3.2');
select fn_db_add_config_value('SupportForceExtendVG','false','2.2');
select fn_db_add_config_value('SupportForceExtendVG','false','3.0');
select fn_db_add_config_value('SupportForceExtendVG','false','3.1');
select fn_db_add_config_value('SupportForceExtendVG','true','3.2');
select fn_db_add_config_value('SysPrep2K3Path','/etc/ovirt-engine/sysprep/sysprep.2k3','general');
select fn_db_add_config_value('SysPrep2K8Path','/etc/ovirt-engine/sysprep/sysprep.2k8x86','general');
select fn_db_add_config_value('SysPrep2K8R2Path','/etc/ovirt-engine/sysprep/sysprep.2k8','general');
select fn_db_add_config_value('SysPrep2K8x64Path','/etc/ovirt-engine/sysprep/sysprep.2k8','general');
select fn_db_add_config_value('SysPrepDefaultPassword','','general');
select fn_db_add_config_value('SysPrepDefaultUser','','general');
select fn_db_add_config_value('SysPrepWindows7Path','/etc/ovirt-engine/sysprep/sysprep.w7','general');
select fn_db_add_config_value('SysPrepWindows7x64Path','/etc/ovirt-engine/sysprep/sysprep.w7x64','general');
select fn_db_add_config_value('SysPrepWindows8Path','/etc/ovirt-engine/sysprep/sysprep.w8','general');
select fn_db_add_config_value('SysPrepWindows8x64Path','/etc/ovirt-engine/sysprep/sysprep.w8x64','general');
select fn_db_add_config_value('SysPrepWindows2012x64Path','/etc/ovirt-engine/sysprep/sysprep.2k12x64','general');
--Handling Path to an XP machine Sys-Prep file.
select fn_db_add_config_value('SysPrepXPPath','/etc/ovirt-engine/sysprep/sysprep.xp','general');
select fn_db_add_config_value('ThrottlerMaxWaitForVdsUpdateInMillis','10000','general');
select fn_db_add_config_value('TimeoutToResetVdsInSeconds','60','general');
select fn_db_add_config_value('TimeToReduceFailedRunOnVdsInMinutes','30','general');
select fn_db_add_config_value('TruststorePass','NoSoup4U','general');
--Handling Truststore URL
select fn_db_add_config_value('TruststoreUrl','.truststore','general');
select fn_db_add_config_value('UknownTaskPrePollingLapse','60000','general');
select fn_db_add_config_value('UserDefinedVMProperties','','3.0');
select fn_db_add_config_value('UserDefinedVMProperties','','3.1');
select fn_db_add_config_value('UserDefinedVMProperties','','3.2');
select fn_db_add_config_value('UserRefreshRate','3600','general');
select fn_db_add_config_value('UserSessionTimeOutInterval','30','general');
--Handling Use Secure Connection with Hosts
select fn_db_add_config_value('UseSecureConnectionWithServers','true','general');
select fn_db_add_config_value('UtilizationThresholdInPercent','80','general');
select fn_db_add_config_value('ValidNumOfMonitors','1,2,4','general');
select fn_db_add_config_value('VcpuConsumptionPercentage','10','general');
--Handling Host Installation Bootstrap Script URL
select fn_db_add_config_value('VdcVersion','3.0.0.0','general');
select fn_db_add_config_value('VDSAttemptsToResetCount','2','general');
select fn_db_add_config_value('VdsCertificateValidityInYears','5','general');
select fn_db_add_config_value('VdsFenceOptionMapping','alom:secure=secure,port=ipport;apc:secure=secure,port=ipport,slot=port;bladecenter:secure=secure,port=ipport,slot=port;cisco_ucs:secure=ssl,slot=port;drac5:secure=secure,slot=port;eps:slot=port;ilo:secure=ssl,port=ipport;ipmilan:;ilo2:secure=ssl,port=ipport;ipmilan:;ilo3:;ilo4:;rsa:secure=secure,port=ipport;rsb:;wti:secure=secure,port=ipport,slot=port','general');
select fn_db_add_config_value('VdsFenceOptions','','general');
select fn_db_add_config_value('VdsFenceOptionTypes','secure=bool,port=int,slot=int','general');
select fn_db_add_config_value('VdsFenceType','alom,apc,bladecenter,drac5,eps,ilo,ilo3,ipmilan,rsa,rsb,wti,cisco_ucs','3.0');
select fn_db_add_config_value('VdsFenceType','alom,apc,bladecenter,drac5,eps,ilo,ilo3,ipmilan,rsa,rsb,wti,cisco_ucs','3.1');
select fn_db_add_config_value('VdsFenceType','alom,apc,bladecenter,cisco_ucs,drac5,eps,ilo,ilo2,ilo3,ilo4,ipmilan,rsa,rsb,wti','3.2');
select fn_db_add_config_value('VdsLoadBalancingeIntervalInMinutes','1','general');
select fn_db_add_config_value('VdsLocalDisksCriticallyLowFreeSpace','100','general');
select fn_db_add_config_value('VdsLocalDisksLowFreeSpace','500','general');
select fn_db_add_config_value('VdsRecoveryTimeoutInMintues','3','general');
select fn_db_add_config_value('VdsRefreshRate','2','general');
--Handling Host Selection Algorithm default for cluster
select fn_db_add_config_value('VdsSelectionAlgorithm','None','general');
select fn_db_add_config_value('vdsTimeout','180','general');
--Handling Virtual Machine Domain Name
select fn_db_add_config_value('VM32BitMaxMemorySizeInMB','20480','general');
select fn_db_add_config_value('VM64BitMaxMemorySizeInMB','524288','3.0');
select fn_db_add_config_value('VM64BitMaxMemorySizeInMB','524288','3.1');
select fn_db_add_config_value('VM64BitMaxMemorySizeInMB','524288','3.2');
select fn_db_add_config_value('VmGracefulShutdownMessage','System Administrator has initiated shutdown of this Virtual Machine. Virtual Machine is shutting down.','general');
select fn_db_add_config_value('VmGracefulShutdownTimeout','30','general');
select fn_db_add_config_value('VMMinMemorySizeInMB','256','general');
--Number of subsequent failures in VM creation in a pool before giving up and stop creating new VMs
select fn_db_add_config_value('VmPoolMaxSubsequentFailures','3','general');
select fn_db_add_config_value('VmPoolMonitorBatchSize','5','general');
select fn_db_add_config_value('VmPoolMonitorIntervalInMinutes','5','general');
select fn_db_add_config_value('VmPoolMonitorMaxAttempts','3','general');
select fn_db_add_config_value('VmPriorityMaxValue','100','general');
--Handling Keyboard Layout configuration for VNC
select fn_db_add_config_value('VncKeyboardLayout','en-us','general');
select fn_db_add_config_value('WaitForVdsInitInSec','60','general');
--The default network connectivity check timeout
select fn_db_add_config_value('NetworkConnectivityCheckTimeoutInSeconds','120','general');
-- AutoRecoveryConfiguration
select fn_db_add_config_value('AutoRecoveryAllowedTypes','{\"storage domains\":\"true\",\"hosts\":\"true\"}','general');
-- Gluster refresh rates (in seconds)
select fn_db_add_config_value('GlusterRefreshRateLight', '5', 'general');
select fn_db_add_config_value('GlusterRefreshRateHeavy', '300', 'general');


------------------------------------------------------------------------------------
--                  Update with override section
------------------------------------------------------------------------------------

select fn_db_update_config_value('AutoRecoveryAllowedTypes','{\"storage domains\":\"true\",\"hosts\":\"true\"}','general');
select fn_db_update_config_value('BootstrapMinimalVdsmVersion','4.9','general');
select fn_db_update_config_value('CertAlias','1','general');
select fn_db_update_config_value('DBEngine','Postgres','general');
select fn_db_update_config_value('DefaultTimeZone','(GMT) GMT Standard Time','general');
select fn_db_update_config_value('FenceAgentDefaultParams','ilo3:lanplus,power_wait=4;ilo4:lanplus,power_wait=4','general');
select fn_db_update_config_value('FenceAgentMapping','ilo2=ilo,ilo3=ipmilan,ilo4=ipmilan','general');
select fn_db_update_config_value('IPTablesConfig','
# oVirt default firewall configuration. Automatically generated by vdsm bootstrap script.
*filter
:INPUT ACCEPT [0:0]
:FORWARD ACCEPT [0:0]
:OUTPUT ACCEPT [0:0]
-A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

-A INPUT -i lo -j ACCEPT
# vdsm
-A INPUT -p tcp --dport 54321 -j ACCEPT
# SSH
-A INPUT -p tcp --dport 22 -j ACCEPT
# snmp
-A INPUT -p udp --dport 161 -j ACCEPT

@CUSTOM_RULES@

# Reject any other input traffic
-A INPUT -j REJECT --reject-with icmp-host-prohibited
-A FORWARD -m physdev ! --physdev-is-bridged -j REJECT --reject-with icmp-host-prohibited
COMMIT
','general');
select fn_db_update_config_value('IsMultilevelAdministrationOn','true','general');
select fn_db_update_config_value('keystoreUrl','keys/engine.p12','general');
select fn_db_update_config_value('MaxNumOfVmCpus','64','3.0');
select fn_db_update_config_value('MaxNumOfVmCpus','160','3.1');
select fn_db_update_config_value('MaxNumOfVmCpus','160','3.2');
select fn_db_update_config_value('MinimalETLVersion','3.2.0','general');
select fn_db_update_config_value('OvirtInitialSupportedIsoVersion','2.5.5','general');
select fn_db_update_config_value('OvirtIsoPrefix','ovirt-node','general');
select fn_db_update_config_value('oVirtISOsRepositoryPath','/usr/share/ovirt-node-iso','general');
select fn_db_update_config_value('PostgresPagingSyntax','OFFSET (%1$s -1) LIMIT %2$s','general');
select fn_db_update_config_value('PostgresSearchTemplate','SELECT * FROM (%2$s) %1$s) as T1 %3$s','general');
select fn_db_update_config_value('RhevhLocalFSPath','/data/images/rhev','general');
select fn_db_update_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3;','3.0');
select fn_db_update_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 7:Intel SandyBridge Family:vmx,nx,model_SandyBridge:SandyBridge; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3; 5:AMD Opteron G4:svm,nx,model_Opteron_G4:Opteron_G4;','3.1');
select fn_db_update_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 7:Intel SandyBridge Family:vmx,nx,model_SandyBridge:SandyBridge; 8:Intel Haswell:vmx,nx,model_Haswell:Haswell; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3; 5:AMD Opteron G4:svm,nx,model_Opteron_G4:Opteron_G4; 6:AMD Opteron G5:smx,nx,model_Opteron_G5:Opteron_G5;','3.2');
select fn_db_update_config_value('SignLockFile','/var/lock/ovirt-engine/.openssl.exclusivelock','general');
select fn_db_update_config_value('SpiceDriverNameInGuest','{"windows": "RHEV-Spice", "linux" : "xorg-x11-drv-qxl" }','general');
select fn_db_update_config_value('SupportedClusterLevels','3.0,3.1,3.2','general');
select fn_db_update_config_value('SupportedStorageFormats','0,2,3','3.1');
select fn_db_update_config_value('SupportedVDSMVersions','4.9,4.10','general');
select fn_db_update_config_value('TruststoreUrl','.truststore','general');
select fn_db_update_config_value('VdcVersion','3.2.0.0','general');
select fn_db_update_config_value('ProductRPMVersion','3.2.0.0','general');
select fn_db_update_config_value('VdsFenceOptionMapping','alom:secure=secure,port=ipport;apc:secure=secure,port=ipport,slot=port;bladecenter:secure=secure,port=ipport,slot=port;cisco_ucs:secure=ssl,slot=port;drac5:secure=secure,slot=port;eps:slot=port;ilo:secure=ssl,port=ipport;ipmilan:;ilo2:secure=ssl,port=ipport;ipmilan:;ilo3:;ilo4:;rsa:secure=secure,port=ipport;rsb:;wti:secure=secure,port=ipport,slot=port','general');
select fn_db_update_config_value('VdsFenceType','alom,apc,bladecenter,drac5,eps,ilo,ilo3,ipmilan,rsa,rsb,wti,cisco_ucs','3.0');
select fn_db_update_config_value('VdsFenceType','alom,apc,bladecenter,drac5,eps,ilo,ilo3,ipmilan,rsa,rsb,wti,cisco_ucs','3.1');
select fn_db_update_config_value('VdsFenceType','alom,apc,bladecenter,cisco_ucs,drac5,eps,ilo,ilo2,ilo3,ilo4,ipmilan,rsa,rsb,wti','3.2');
select fn_db_update_config_value('VmGracefulShutdownMessage','System Administrator has initiated shutdown of this Virtual Machine. Virtual Machine is shutting down.','general');
select fn_db_update_config_value('DisconnectPoolOnReconstruct','0,2,3','general');
select fn_db_update_config_value('SupportBridgesReportByVDSM','false','3.1');


------------------------------------------------------------------------------------
--   Update only if default not changed section
------------------------------------------------------------------------------------

select fn_db_update_default_config_value('AdUserId','example.com:00000000-0000-0000-0000-000000000000','','general',false);
select fn_db_update_default_config_value('AdUserName','example.com:SampleUser','','general',false);
select fn_db_update_default_config_value('AdUserPassword','example.com:SamplePassword','','general',false);
-- Increase AsyncTaskZombieTaskLifeInMinutes to 50 hours if it's the default 5 hours.
select fn_db_update_default_config_value('AsyncTaskZombieTaskLifeInMinutes','300','3000','general',false);
select fn_db_update_default_config_value('DomainName','example.com','','general',false);
select fn_db_update_default_config_value('EmulatedMachine','rhel6.0.0', 'pc-0.14','3.0',false);
select fn_db_update_default_config_value('LDAPSecurityAuthentication','GSSAPI','default:GSSAPI','general',false);
select fn_db_update_default_config_value('LDAPSecurityAuthentication','SIMPLE','default:SIMPLE','general',false);
select fn_db_update_default_config_value('VdsLocalDisksCriticallyLowFreeSpace','100','500','general',false);
select fn_db_update_default_config_value('VdsLocalDisksLowFreeSpace','500', '1000','general',false);
------------------------------------------------------------------------------------
--              Cleanup deprecated configuration values section
------------------------------------------------------------------------------------

select fn_db_delete_config_value('AsyncPollingCyclesBeforeCallbackCleanup','general');
select fn_db_delete_config_value('AsyncPollingCyclesBeforeRefreshSuspend','general');
select fn_db_delete_config_value('AutoMode','general');
select fn_db_delete_config_value('AutoSuspendTimeInMinutes','general');
select fn_db_delete_config_value('CAEngineKey','general');
select fn_db_delete_config_value('CBCCertificateScriptName','general');
select fn_db_delete_config_value('CBCCloseCertificateScriptName','general');
select fn_db_delete_config_value('CbcCheckOnVdsChange','general');
select fn_db_delete_config_value('CertificateFingerPrint','general');
select fn_db_delete_config_value('CertificatePassword','general');
select fn_db_delete_config_value('CustomPublicConfig_AppsWebSite','general');
select fn_db_delete_config_value('DebugSearchLogging','general');
select fn_db_delete_config_value('DefaultWorkgroup','general');
select fn_db_delete_config_value('ENMailDomain','general');
select fn_db_delete_config_value('ENMailEnableSsl','general');
select fn_db_delete_config_value('ENMailHost','general');
select fn_db_delete_config_value('ENMailIsBodyHtml','general');
select fn_db_delete_config_value('ENMailPassword','general');
select fn_db_delete_config_value('ENMailPort','general');
select fn_db_delete_config_value('ENMailUseDefaultCredentials','general');
select fn_db_delete_config_value('ENMailUser','general');
select fn_db_delete_config_value('FreeSpaceCriticalLow','general');
select fn_db_delete_config_value('HotPlugSupportedOsList','general');
select fn_db_delete_config_value('ImagesSyncronizationTimeout','general');
select fn_db_delete_config_value('LdapServers','3.0');
select fn_db_delete_config_value('LicenseCertificateFingerPrint','general');
select fn_db_delete_config_value('LogDBCommands','general');
select fn_db_delete_config_value('LogVdsRegistration','general');
select fn_db_delete_config_value('LogXmlRpcData','general');
select fn_db_delete_config_value('NetConsolePort','general');
select fn_db_delete_config_value('PowerClientAllowUsingAsIRS','general');
select fn_db_delete_config_value('PowerClientGUI','general');
select fn_db_delete_config_value('PredefinedVMProperties','general');
select fn_db_delete_config_value('PublicURLPort','general');
select fn_db_delete_config_value('RDPLoginWithFQN','general');
select fn_db_delete_config_value('RedirectServletReportsPageError','general');
select fn_db_delete_config_value('RenewGuestIpOnVdsSubnetChange','general');
select fn_db_delete_config_value('RenewGuestIpOnVdsSubnetChangeOnParseError','general');
select fn_db_delete_config_value('RpmsRepositoryUrl','general');
select fn_db_delete_config_value('SQLServerI18NPrefix','general');
select fn_db_delete_config_value('SQLServerLikeSyntax','general');
select fn_db_delete_config_value('SQLServerPagingSyntax','general');
select fn_db_delete_config_value('SQLServerPagingType','general');
select fn_db_delete_config_value('SQLServerSearchTemplate','general');
select fn_db_delete_config_value('ScriptsPath','general');
select fn_db_delete_config_value('SearchesRefreshRateInSeconds','general');
select fn_db_delete_config_value('SelectCommandTimeout','general');
select fn_db_delete_config_value('SysPrep3.0Path','general');
select fn_db_delete_config_value('UseENGINERepositoryRPMs','general');
select fn_db_delete_config_value('UseVdsBrokerInProc','general');
select fn_db_delete_config_value('VM64BitMaxMemorySizeInMB','general');
select fn_db_delete_config_value('VdcBootStrapUrl','general');
select fn_db_delete_config_value('VdsErrorsFileName','general');
select fn_db_delete_config_value('VM64BitMaxMemorySizeInMB','general');
select fn_db_delete_config_value('LogVdsRegistration','general');
select fn_db_delete_config_for_version('2.2');
select fn_db_delete_config_value('IsNeedSupportForOldVgAPI', '2.2,3.0');
select fn_db_delete_config_value('LimitNumberOfNetworkInterfaces', '2.2,3.0,3.1,3.2');
select fn_db_delete_config_value('LocalStorageEnabled','2.2,3.0,3.1,3.2');
select fn_db_delete_config_value('SupportCustomProperties','2.2,3.0,3.1,3.2');
select fn_db_delete_config_value('SupportGetDevicesVisibility','2.2,3.0,3.1,3.2');
select fn_db_delete_config_value('SupportStorageFormat','2.2,3.0,3.1,3.2');
select fn_db_delete_config_value('UseRtl8139_pv','2.2,3.0,3.1,3.2');
select fn_db_delete_config_value('VdsFenceOptions','general');
select fn_db_delete_config_value('VirtualMachineDomainName','general');
------------------------------------------------------------------------------------
--                  Split config section
-- The purpose of this section is to treat config option that was once
-- general, and should now be version-specific.
-- To ease this the fn_db_split_config_value can be used, input is the
-- option_name, the old value and the new value. Result is creating one row for each old
-- cluster level with the original value if exists, or the input old value
-- and from the update version and beyond, the input value.
------------------------------------------------------------------------------------
select fn_db_split_config_value('SpiceSecureChannels','smain,sinputs','smain,sinputs,scursor,splayback,srecord,sdisplay,susbredir,ssmartcard', '3.1');

------------------------------------------------------------------------------------
--                  Simple direct updates section
------------------------------------------------------------------------------------

-- update keys from internal version 2.3 to official 3.0`
update vdc_options set version = '3.0' where version = '2.3';

------------------------------------------------------------------------------------
--                 complex updates using a temporary function section
--                 each temporary function name should start with __temp
------------------------------------------------------------------------------------

-- remove default security authentication

CREATE OR REPLACE FUNCTION __temp_upgrade_remove_default_security_auth(a_input VARCHAR(40))
  RETURNS void AS
$BODY$
   DECLARE
   v_entry VARCHAR(4000);
   v_pos integer;
BEGIN
    v_entry := option_value FROM vdc_options WHERE option_name='LDAPSecurityAuthentication';
    v_pos := strpos(lower(v_entry), ',' || lower(a_input) || ',');

    IF (v_pos = 0) THEN
                UPDATE vdc_options
                SET option_value = regexp_replace(option_value, ',?' || a_input || ',?' ,'','i')
                WHERE option_name = 'LDAPSecurityAuthentication';
    ELSE
                UPDATE vdc_options
                SET option_value = regexp_replace(option_value, ',' || a_input || ',' ,',','i')
                WHERE option_name = 'LDAPSecurityAuthentication';
    END IF;

END; $BODY$
LANGUAGE plpgsql;

SELECT __temp_upgrade_remove_default_security_auth('default:GSSAPI');
SELECT __temp_upgrade_remove_default_security_auth('default:SIMPLE');

DROP FUNCTION __temp_upgrade_remove_default_security_auth(VARCHAR);


--- upgrade domains to have a provider type

create or replace function __temp_update_ldap_provier_types()
RETURNS void
AS $procedure$
    DECLARE
    v_domains text;
    v_provider_types text;
    v_temp text;
    v_values record;
    boo smallint;

BEGIN

    v_temp := '';
    v_domains := (SELECT option_value FROM vdc_options where option_name = 'DomainName');
    v_provider_types := (SELECT option_value FROM vdc_options where option_name = 'LDAPProviderTypes');
    boo := (SELECT count(*) from regexp_matches(v_provider_types ,'[:]'));

    IF (boo = 0) THEN

        FOR v_values in select regexp_split_to_table(v_domains, ',') as val
        LOOP
            IF (length(v_values.val) > 0) THEN
                v_temp := v_temp || v_values.val || ':general,';
            END IF;
        END LOOP;

        v_temp = rtrim(v_temp,',');

        UPDATE vdc_options SET option_value = v_temp where option_name = 'LDAPProviderTypes';

    END IF;

END; $procedure$
LANGUAGE plpgsql;

SELECT  __temp_update_ldap_provier_types();
DROP FUNCTION __temp_update_ldap_provier_types();

