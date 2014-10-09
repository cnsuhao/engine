package org.ovirt.engine.core.vdsbroker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;

public class MultipleServicesMonitoringStrategyTest {
    VirtMonitoringStrategy virtStrategy;
    GlusterMonitoringStrategy glusterStrategy;
    MultipleServicesMonitoringStrategy strategy;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    public MultipleServicesMonitoringStrategyTest() {
        virtStrategy = spy(new VirtMonitoringStrategy());
        glusterStrategy = spy(new GlusterMonitoringStrategy());
        doNothing().when(virtStrategy).vdsNonOperational(any(VDS.class));
        strategy = spy(new MultipleServicesMonitoringStrategy());
        strategy.addMonitoringStrategy(virtStrategy);
        strategy.addMonitoringStrategy(glusterStrategy);
    }

    @Test
    public void testCanMoveVdsToMaintenanceFalse() {
        VDS vds = new VDS();
        vds.setstatus(VDSStatus.PreparingForMaintenance);
        vds.setvm_count(1);
        assertFalse(strategy.canMoveToMaintenance(vds));
    }

    @Test
    public void testCanMoveVdsToMaintenanceTrue() {
        VDS vds = new VDS();
        vds.setstatus(VDSStatus.PreparingForMaintenance);
        vds.setvm_count(0);
        assertTrue(strategy.canMoveToMaintenance(vds));
    }

    @Test
    public void testIsmonitoringNeededTrue() {
        VDS vds = new VDS();
        vds.setstatus(VDSStatus.NonOperational);
        vds.setvm_count(1);
        assertTrue(strategy.isMonitoringNeeded(vds));
        vds.setstatus(VDSStatus.Up);
        assertTrue(strategy.isMonitoringNeeded(vds));
    }

    @Test
    public void testIsmonitoringNeededGlusterTrue() {
        VDS vds = new VDS();
        vds.setstatus(VDSStatus.NonOperational);
        vds.setvm_count(0);
        assertTrue(strategy.isMonitoringNeeded(vds));
    }

    @Test
    public void testProcessingSoftwareGluster() {
        doThrow(new RuntimeException()).when(glusterStrategy).processSoftwareCapabilities(any(VDS.class));
        exception.expect(RuntimeException.class);
        VDS vds = new VDS();
        strategy.processSoftwareCapabilities(vds);
    }

    @Test
    public void testProcessingHardwareVirt() {
        doThrow(new RuntimeException()).when(virtStrategy).processHardwareCapabilities(any(VDS.class));
        exception.expect(RuntimeException.class);
        VDS vds = new VDS();
        strategy.processHardwareCapabilities(vds);
    }
}
