package org.ovirt.engine.core.bll.network;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class MacPoolManager {

    private static final int HEX_RADIX = 16;
    private static final String INIT_ERROR_MSG = "Error in initializing MAC Addresses pool manager. ";
    private static final MacPoolManager INSTANCE = new MacPoolManager();

    private static Log log = LogFactory.getLog(MacPoolManager.class);

    private final Set<String> availableMacs = new HashSet<String>();
    private final Set<String> allocatedMacs = new HashSet<String>();
    private final Set<String> allocatedCustomMacs = new HashSet<String>();
    private final ReentrantReadWriteLock lockObj = new ReentrantReadWriteLock();
    private boolean initialized;

    private MacPoolManager() {
        // Empty ctor since this is singleton.
    }

    public static MacPoolManager getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        lockObj.writeLock().lock();
        try {
            String ranges = Config.<String> GetValue(ConfigValues.MacPoolRanges);
            if (!"".equals(ranges)) {
                try {
                    initRanges(ranges);
                } catch (MacPoolExceededMaxException e) {
                    log.errorFormat("MAC Pool range exceeded maximum number of mac pool addressed. Please check Mac Pool configuration.");
                }
            }

            List<VmNetworkInterface> interfaces = DbFacade.getInstance().getVmNetworkInterfaceDao().getAll();
            for (VmNetworkInterface iface: interfaces) {
                addMac(iface.getMacAddress());
            }
            initialized = true;
        } catch (Exception ex) {
            log.debug(INIT_ERROR_MSG, ex);
            log.error(INIT_ERROR_MSG + "Exception message is: " + ex.getMessage());
        } finally {
            lockObj.writeLock().unlock();
        }
    }

    private void initRanges(String ranges) {
        String[] rangesArray = ranges.split("[,]", -1);
        for (String range : rangesArray) {
            String[] startendArray = range.split("[-]", -1);
            if (startendArray.length == 2) {
                if (!initRange(startendArray[0], startendArray[1])) {
                    log.errorFormat("Failed to initialize Mac Pool range. Please fix Mac Pool range: {0}", range);
                }
            } else {
                log.errorFormat("Failed to initialize Mac Pool range. Please fix Mac Pool range: {0}", range);

            }
        }
        if (availableMacs.isEmpty()) {
            throw new VdcBLLException(VdcBllErrors.MAC_POOL_INITIALIZATION_FAILED);
        }
    }

    private String parseRangePart(String start) {
        StringBuilder builder = new StringBuilder();
        for (String part : start.split("[:]", -1)) {
            String tempPart = part.trim();
            if (tempPart.length() == 1) {
                builder.append('0');
            } else if (tempPart.length() > 2) {
                return null;
            }
            builder.append(tempPart);
        }
        return builder.toString();
    }

    private boolean initRange(String start, String end) {
        String parsedRangeStart = parseRangePart(start);
        String parsedRangeEnd = parseRangePart(end);
        if (parsedRangeEnd == null || parsedRangeStart == null) {
            return false;
        }
        long startNum = Long.parseLong(parseRangePart(start), HEX_RADIX);
        long endNum = Long.parseLong(parseRangePart(end), HEX_RADIX);
        if (startNum > endNum) {
            return false;
        }
        for (long i = startNum; i <= endNum; i++) {
            String value = String.format("%x", i);
            if (value.length() > 12) {
                return false;
            } else if (value.length() < 12) {
                value = StringUtils.leftPad(value, 12, '0');
            }
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < value.length(); j += 2) {
                builder.append(value.substring(j, j + 2));
                builder.append(":");
            }
            value = builder.toString();
            value = value.substring(0, value.length() - 1);
            if (!availableMacs.contains(value)) {
                availableMacs.add(value);
            }
            if (availableMacs.size() > Config.<Integer> GetValue(ConfigValues.MaxMacsCountInPool)) {
                throw new MacPoolExceededMaxException();
            }
        }
        return true;
    }

    public String allocateNewMac() {
        String mac = null;
        log.info("MacPoolManager::allocateNewMac entered");
        lockObj.writeLock().lock();
        try {
            if (!initialized) {
                logInitializationError("Failed to allocate new Mac address.");
                throw new VdcBLLException(VdcBllErrors.MAC_POOL_NOT_INITIALIZED);
            }
            if (availableMacs.isEmpty()) {
                throw new VdcBLLException(VdcBllErrors.MAC_POOL_NO_MACS_LEFT);
            }
            Iterator<String> my = availableMacs.iterator();
            mac = my.next();
            commitNewMac(mac);
        } finally {
            lockObj.writeLock().unlock();
        }
        log.infoFormat("MacPoolManager::allocateNewMac allocated mac = '{0}", mac);
        return mac;
    }

    private boolean commitNewMac(String mac) {
        availableMacs.remove(mac);
        allocatedMacs.add(mac);
        if (availableMacs.isEmpty()) {
            AuditLogableBase logable = new AuditLogableBase();
            AuditLogDirector.log(logable, AuditLogType.MAC_POOL_EMPTY);
            return false;
        }
        return true;
    }

    public int getavailableMacsCount() {
        return availableMacs.size();
    }

    public void freeMac(String mac) {
        log.infoFormat("MacPoolManager::freeMac(mac = '{0}') - entered", mac);
        lockObj.writeLock().lock();
        try {
            if (!initialized) {
                logInitializationError("Failed to free mac address " + mac + " .");
            } else {
                internalFreeMac(mac);
            }
        } finally {
            lockObj.writeLock().unlock();
        }
    }

    private void logInitializationError(String message) {
        log.error("The MAC addresses pool is not initialized");
        AuditLogableBase logable = new AuditLogableBase();
        logable.AddCustomValue("Message",message);
        AuditLogDirector.log(logable, AuditLogType.MAC_ADDRESSES_POOL_NOT_INITIALIZED);
    }

    private void internalFreeMac(String mac) {
        if (allocatedCustomMacs.contains(mac)) {
            allocatedCustomMacs.remove(mac);
        } else if (allocatedMacs.contains(mac)) {
            allocatedMacs.remove(mac);
            availableMacs.add(mac);
        }
    }

    /**
     * Add user define mac address Function return false if the mac is in use
     *
     * @param mac
     * @return
     */
    public boolean addMac(String mac) {
        boolean retVal = true;
        lockObj.writeLock().lock();
        try {
            if (allocatedMacs.contains(mac)) {
                retVal = false;
            } else {
                if (availableMacs.contains(mac)) {
                    retVal = commitNewMac(mac);
                } else if (allocatedCustomMacs.contains(mac)) {
                    retVal = false;
                } else {
                    allocatedCustomMacs.add(mac);
                }
            }
        } finally {
            lockObj.writeLock().unlock();
        }
        return retVal;
    }

    public boolean isMacInUse(String mac) {
        lockObj.readLock().lock();
        try {
            return allocatedMacs.contains(mac) || allocatedCustomMacs.contains(mac);
        } finally {
            lockObj.readLock().unlock();
        }
    }

    public void freeMacs(List<String> macs) {
        log.info("MacPoolManager::freeMacs - entered");
        lockObj.writeLock().lock();
        try {
            if (!initialized) {
                logInitializationError("Failed to free MAC addresses.");
            }
            for (String mac : macs) {
                internalFreeMac(mac);
            }

        } finally {
            lockObj.writeLock().unlock();
        }
    }

    @SuppressWarnings("serial")
    private class MacPoolExceededMaxException extends RuntimeException {
    }
}
