package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.Map;
import java.util.concurrent.FutureTask;

public interface VdsServerConnector {
    public Map<String, Object> create(Map createInfo);

    public Map<String, Object> destroy(String vmId);

    public Map<String, Object> shutdown(String vmId, String timeout, String message);

    public Map<String, Object> shutdownHost(int reboot);

    public Map<String, Object> pause(String vmId);

    public Map<String, Object> hibernate(String vmId, String hiberVolHandle);

    public Map<String, Object> shutdown(String vmId);

    public Map<String, Object> reset(String vmId);

    public Map<String, Object> cont(String vmId);

    public Map<String, Object> list();

    public Map<String, Object> list(String isFull, String[] vmIds);

    public Map<String, Object> getVdsCapabilities();

    public Map<String, Object> getVdsHardwareInfo();

    public Map<String, Object> getVdsStats();

    public Map<String, Object> desktopLogin(String vmId, String domain, String user, String password);

    public Map<String, Object> desktopLogoff(String vmId, String force);

    public Map<String, Object> desktopLock(String vmId);

    public Map<String, Object> getVmStats(String vmId);

    public Map<String, Object> getAllVmStats();

    public Map<String, Object> migrate(Map<String, String> migrationInfo);

    public Map<String, Object> migrateStatus(String vmId);

    public Map<String, Object> migrateCancel(String vmId);

    public Map<String, Object> changeCD(String vmId, String imageLocation);

    public Map<String, Object> changeFloppy(String vmId, String imageLocation);

    public Map<String, Object> heartBeat();

    public Map<String, Object> monitorCommand(String vmId, String monitorCommand);

    public Map<String, Object> sendHcCmdToDesktop(String vmId, String hcCommand);

    public Map<String, Object> setVmTicket(String vmId, String otp64, String sec);

    public Map<String, Object> setVmTicket(String vmId, String otp64, String sec, String connectionAction, Map<String, String> params);

    public Map<String, Object> startSpice(String vdsIp, int port, String ticket);

    public Map<String, Object> addNetwork(String bridge, String vlan, String bond, String[] nics,
            Map<String, String> options);

    public Map<String, Object> delNetwork(String bridge, String vlan, String bond, String[] nics);

    public Map<String, Object> editNetwork(String oldBridge, String newBridge, String vlan, String bond, String[] nics,
            Map<String, String> options);

    public Map<String, Object> setupNetworks(Map networks, Map bonding, Map options);

    @FutureCall(delegeteTo="setupNetworks")
    public FutureTask<Map<String, Object>> futureSetupNetworks(Map networks, Map bonding, Map options);

    public Map<String, Object> setSafeNetworkConfig();

    public Map<String, Object> fenceNode(String ip, String port, String type, String user, String password,
            String action, String secured, String options);

    public Map<String, Object> connectStorageServer(int serverType, String spUUID, Map<String, String>[] args);

    public Map<String, Object> validateStorageServerConnection(int serverType, String spUUID, Map<String, String>[] args);

    public Map<String, Object> disconnectStorageServer(int serverType, String spUUID, Map<String, String>[] args);

    public Map<String, Object> getStorageConnectionsList(String spUUID);

    public Map<String, Object> validateStorageDomain(String sdUUID);

    public Map<String, Object> createStorageDomain(int domainType, String sdUUID, String domainName, String arg,
            int storageType, String storageFormatType);

    public Map<String, Object> formatStorageDomain(String sdUUID);

    public Map<String, Object> connectStoragePool(String spUUID, int hostSpmId, String SCSIKey, String masterdomainId,
            int masterVersion);

    public Map<String, Object> disconnectStoragePool(String spUUID, int hostSpmId, String SCSIKey);

    public Map<String, Object> createStoragePool(int poolType, String spUUID, String poolName, String msdUUID,
            String[] domList, int masterVersion, String lockPolicy, int lockRenewalIntervalSec, int leaseTimeSec,
            int ioOpTimeoutSec, int leaseRetries);

    public Map<String, Object> reconstructMaster(String spUUID, String poolName, String masterDom,
            Map<String, String> domDict, int masterVersion, String lockPolicy, int lockRenewalIntervalSec,
            int leaseTimeSec, int ioOpTimeoutSec, int leaseRetries, int hostSpmId);

    public Map<String, Object> getStorageDomainStats(String sdUUID);

    public Map<String, Object> getStorageDomainInfo(String sdUUID);

    public Map<String, Object> getStorageDomainsList(String spUUID, int domainType, int poolType, String path);

    public Map<String, Object> getIsoList(String spUUID);

    public Map<String, Object> createVG(String sdUUID, String[] deviceList);

    public Map<String, Object> createVG(String sdUUID, String[] deviceList, boolean force);

    public Map<String, Object> getVGList();

    public Map<String, Object> getVGInfo(String vgUUID);

    public Map<String, Object> getDeviceList(int storageType);

    public Map<String, Object> getDeviceInfo(String devGUID);

    public Map<String, Object> getDevicesVisibility(String[] devicesList);

    public Map<String, Object> discoverSendTargets(Map<String, String> args);

    public Map<String, Object> getSessionList();

    public Map<String, Object> spmStart(String spUUID,
            int prevID,
            String prevLVER,
            int recoveryMode,
            String SCSIFencing,
            int maxHostId,
            String storagePoolFormatType);

    public Map<String, Object> spmStop(String spUUID);

    public Map<String, Object> getSpmStatus(String spUUID);

    public Map<String, Object> fenceSpmStorage(String spUUID, int prevID, String prevLVER);

    public Map<String, Object> refreshStoragePool(String spUUID, String msdUUID, int masterVersion);

    public Map<String, Object> getTaskStatus(String taskUUID);

    public Map<String, Object> getAllTasksStatuses();

    public Map<String, Object> getTaskInfo(String taskUUID);

    public Map<String, Object> getAllTasksInfo();

    public Map<String, Object> stopTask(String taskUUID);

    public Map<String, Object> clearTask(String taskUUID);

    public Map<String, Object> revertTask(String taskUUID);

    Map<String, Object> hotplugDisk(Map<String, Object> info);

    Map<String, Object> hotunplugDisk(Map<String, Object> info);

    public Map<String, Object> hotplugNic(Map<String, Object> innerMap);

    public Map<String, Object> hotunplugNic(Map<String, Object> innerMap);

    public Map<String, Object> vmUpdateDevice(String vmId, Map<String, Object> device);

    Map<String, Object> snapshot(String vmId, Map<String, String>[] snapParams);

    // Gluster vdsm commands
    public Map<String, Object> glusterVolumeCreate(String volumeName,
            String[] brickList,
            int replicaCount,
            int stripeCount,
            String[] transportList);

    public Map<String, Object> glusterVolumeSet(String volumeName, String key, String value);

    public Map<String, Object> glusterVolumeStart(String volumeName, Boolean force);

    public Map<String, Object> glusterVolumeStop(String volumeName, Boolean force);

    public Map<String, Object> glusterVolumeDelete(String volumeName);

    public Map<String, Object> glusterVolumeReset(String volumeName, String volumeOption, Boolean force);

    public Map<String, Object> glusterVolumeSetOptionsList();

    public Map<String, Object> glusterVolumeRemoveBrickForce(String volumeName,
            String[] brickDirectories,
            int replicaCount);

    public Map<String, Object> glusterVolumeBrickAdd(String volumeName,
            String[] bricks,
            int replicaCount,
            int stripeCount);

    public Map<String, Object> glusterVolumeRebalanceStart(String volumeName, Boolean fixLayoutOnly, Boolean force);

    public Map<String, Object> replaceGlusterVolumeBrickStart(String volumeName, String existingBrickDir, String newBrickDir);

    public Map<String, Object> glusterHostRemove(String hostName, Boolean force);

    public Map<String, Object> glusterVolumeReplaceBrickStart(String volumeName, String existingBrickDir, String newBrickDir);

    public Map<String, Object> glusterHostAdd(String hostName);

    public Map<String, Object> glusterHostsList();

    public Map<String, Object> glusterVolumesList();

    public Map<String, Object> ping();

    @FutureCall(delegeteTo = "ping")
    public FutureTask<Map<String, Object>> futurePing();

    public Map<String, Object> diskReplicateStart(String vmUUID, Map srcDisk, Map dstDisk);

    public Map<String, Object> diskReplicateFinish(String vmUUID, Map srcDisk, Map dstDisk);

    public Map<String, Object> glusterVolumeProfileStart(String volumeName);

    public Map<String, Object> glusterVolumeProfileStop(String volumeName);

    public Map<String, Object> glusterVolumeStatus(String volumeName, String brickName, String volumeStatusOption);

    public Map<String, Object> glusterVolumeProfileInfo(String volumeName);
}
