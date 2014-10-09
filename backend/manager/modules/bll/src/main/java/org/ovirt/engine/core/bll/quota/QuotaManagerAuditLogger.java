package org.ovirt.engine.core.bll.quota;


import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;

import java.text.DecimalFormat;

public class QuotaManagerAuditLogger {
    private static final DecimalFormat percentageFormatter = new DecimalFormat("#.##");

    protected void addCustomValuesStorage(AuditLogableBase auditLogableBase,
            String quotaName,
            double storageUsagePercentage,
            double storageRequestedPercentage) {
        auditLogableBase.AddCustomValue("QuotaName", quotaName);
        auditLogableBase.AddCustomValue("CurrentStorage", percentageFormatter.format(storageUsagePercentage));
        auditLogableBase.AddCustomValue("Requested", percentageFormatter.format(storageRequestedPercentage));
    }

    protected void addCustomValuesVdsGroup(AuditLogableBase auditLogableBase,
            String quotaName,
            double cpuCurrentPercentage,
            double cpuRequestPercentage,
            double memCurrentPercentage,
            double memRequestPercentage,
            boolean cpuOverLimit,
            boolean memOverLimit) {

        auditLogableBase.AddCustomValue("QuotaName", quotaName);

        StringBuilder currentUtilization = new StringBuilder();
        if (cpuOverLimit) {
            currentUtilization.append("vcpu:").append(percentageFormatter.format(cpuCurrentPercentage)).append("% ");
        }
        if (memOverLimit) {
            currentUtilization.append("mem:").append(percentageFormatter.format(memCurrentPercentage)).append("%");
        }

        StringBuilder request = new StringBuilder();
        if (cpuOverLimit) {
            request.append("vcpu:").append(percentageFormatter.format(cpuRequestPercentage)).append("% ");
        }
        if (memOverLimit) {
            request.append("mem:").append(percentageFormatter.format(memRequestPercentage)).append("%");
        }

        auditLogableBase.AddCustomValue("Utilization", currentUtilization.toString());
        auditLogableBase.AddCustomValue("Requested", request.toString());
    }

    public void auditLog(AuditLogType auditLogType, AuditLogableBase auditLogable) {
        if (auditLogType != null) {
            AuditLogDirector.log(auditLogable, auditLogType);
        }
    }
}
