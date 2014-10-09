package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;

public enum VMStatus implements Identifiable {
    Unassigned(-1),
    Down(0),
    Up(1),
    PoweringUp(2),
    PoweredDown(3),
    Paused(4),
    MigratingFrom(5),
    MigratingTo(6),
    Unknown(7),
    NotResponding(8),
    WaitForLaunch(9),
    RebootInProgress(10),
    SavingState(11),
    RestoringState(12),
    Suspended(13),
    ImageIllegal(14),
    ImageLocked(15),
    PoweringDown(16);

    private int value;
    private static HashMap<Integer, VMStatus> valueToStatus = new HashMap<Integer, VMStatus>();

    static {
        for (VMStatus status : values()) {
            valueToStatus.put(status.getValue(), status);
        }
    }

    private VMStatus(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }

    public static VMStatus forValue(int value) {
        return valueToStatus.get(value);
    }

    /**
     * This method reflects whether the VM is surely not running in this status
     *
     * <p>Note: There might be cases in which the VM is not running and this method
     * returns false
     *
     * @return true if this status indicates that the VM is not running for sure, otherwise false
     */
    public boolean isNotRunning() {
        return this == Down || this == Suspended || this == ImageLocked || this == ImageIllegal;
    }

    /**
     * This method reflects whether the VM is qualify to migration in this status
     *
     * @return true if this status indicates that the VM is qualify to migration, otherwise false
     */
    public boolean isQualifyToMigrate() {
        return this == Up || this == PoweringUp || this == Paused || this == RebootInProgress;
    }

    /**
     * This method reflects whether the VM is surely running or paused in this status
     *
     * @see #isRunning()
     * @return true if this status indicates that the VM is paused or running for sure, otherwise false
     */
    public boolean isRunningOrPaused() {
        return this.isRunning() || this == Paused || this == SavingState || this == RestoringState;
    }

    /**
     * This method reflects whether the VM is surely running in this status
     *
     * <p>Note: There might be cases in which the VM is running and this method
     * returns false
     *
     * @return true if this status indicates that the VM is running for sure, otherwise false
     */
    public boolean isRunning() {
        return this == Up || this == PoweredDown || this == PoweringDown
                || this == PoweringUp || this == MigratingFrom || this == WaitForLaunch || this == RebootInProgress;
    }
}
