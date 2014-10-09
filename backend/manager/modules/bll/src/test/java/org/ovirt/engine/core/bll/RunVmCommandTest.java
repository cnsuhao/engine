package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.common.action.RunVmParams;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.IVdsAsyncCommand;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsAndVmIDVDSParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBaseMockUtils;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.VmDAO;
import org.ovirt.engine.core.dao.VmDeviceDAO;
import org.ovirt.engine.core.utils.MockConfigRule;
import org.ovirt.engine.core.utils.vmproperties.VmPropertiesUtils;

@RunWith(MockitoJUnitRunner.class)
public class RunVmCommandTest {

    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.VdsSelectionAlgorithm, "General", "0"),
            mockConfig(ConfigValues.PredefinedVMProperties, "3.0", "0"),
            mockConfig(ConfigValues.UserDefinedVMProperties, "3.0", "0")
            );

    /**
     * The command under test.
     */
    private RunVmCommand<RunVmParams> command;

    @Mock
    private VDSBrokerFrontend vdsBrokerFrontend;

    @Mock
    private VmDAO vmDAO;

    @Spy
    private final VmRunHandler vmRunHandler = VmRunHandler.getInstance();

    @Mock
    private BackendInternal backend;

    private static final String ISO_PREFIX = "iso://";
    private static final String ACTIVE_ISO_PREFIX =
            "/rhev/data-center/mnt/some_computer/f6bccab4-e2f5-4e02-bba0-5748a7bc07b6/images/11111111-1111-1111-1111-111111111111";
    private static final String INACTIVE_ISO_PREFIX = "";

    public void mockBackend() {
        doReturn(backend).when(command).getBackend();
        doReturn(backend).when(vmRunHandler).getBackend();

        VDSReturnValue vdsReturnValue = new VDSReturnValue();
        vdsReturnValue.setReturnValue(true);
        when(vdsBrokerFrontend.RunVdsCommand(any(VDSCommandType.class), any(VDSParametersBase.class))).thenReturn(vdsReturnValue);
        when(backend.getResourceManager()).thenReturn(vdsBrokerFrontend);

        // Set Valid Iso Prefix
        setIsoPrefixVDSMethod(ACTIVE_ISO_PREFIX);

        // Set create Vm.
        setCreateVmVDSMethod();
    }

    /**
     * Set create VM to return VM with status Up.
     */
    private void setCreateVmVDSMethod() {
        VDSReturnValue returnValue = new VDSReturnValue();
        returnValue.setReturnValue(VMStatus.Up);
        when(backend.getResourceManager().RunAsyncVdsCommand(eq(VDSCommandType.CreateVm),
                any(VdsAndVmIDVDSParametersBase.class),
                any(IVdsAsyncCommand.class))).thenReturn(returnValue);
    }

    private static DiskImage createImage() {
        final DiskImage diskImage = new DiskImage();
        diskImage.setId(Guid.NewGuid());
        diskImage.setstorage_ids(new ArrayList<Guid>(Arrays.asList(new Guid())));
        return diskImage;
    }

    private static VmDevice createDiskVmDevice(final DiskImage diskImage) {
        final VmDevice vmDevice = new VmDevice();
        vmDevice.setIsPlugged(true);
        vmDevice.setId(new VmDeviceId(diskImage.getId(), Guid.NewGuid()));
        return vmDevice;
    }

    /**
     * Set the Iso prefix.
     *
     * @param isoPrefix
     *            - Valid Iso patch or blank (when the Iso is not active.
     */
    private void setIsoPrefixVDSMethod(final String isoPrefix) {
        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return ImagesHandler.cdPathWindowsToLinux(invocation.getArguments()[0].toString(), isoPrefix);
            }

        }).when(command).cdPathWindowsToLinux(anyString());
    }

    @Test
    public void validateSimpleInitrdAndKernelName() throws Exception {
        String Initrd = "/boot/initrd.initrd";
        String Kernel = "/boot/kernel.image";
        VM vm = createVmForTesting(Initrd, Kernel);
        assertEquals(vm.getInitrdUrl(), Initrd);
        assertEquals(vm.getKernelUrl(), Kernel);
    }

    @Test
    public void validateIsoPrefix() throws Exception {
        String initrd = "initrd";
        String kernel = "kernel";
        VM vm = createVmForTesting(ISO_PREFIX + initrd, ISO_PREFIX + kernel);
        assertEquals(vm.getInitrdUrl(), ACTIVE_ISO_PREFIX + "/" + initrd);
        assertEquals(vm.getKernelUrl(), ACTIVE_ISO_PREFIX + "/" + kernel);
    }

    @Test
    public void validateIsoPrefixForKernelAndNoPrefixForInitrd() throws Exception {
        String initrd = "initrd";
        String kernel = "kernel";
        VM vm = createVmForTesting(initrd, ISO_PREFIX + kernel);
        assertEquals(vm.getInitrdUrl(), initrd);
        assertEquals(vm.getKernelUrl(), ACTIVE_ISO_PREFIX + "/" + kernel);
    }

    @Test
    public void validateIsoPrefixForInitrdAndNoPrefixForKernel() throws Exception {
        String initrd = "initrd";
        String kernel = "kernel";
        VM vm = createVmForTesting(ISO_PREFIX + initrd, kernel);
        assertEquals(vm.getInitrdUrl(), ACTIVE_ISO_PREFIX + "/" + initrd);
        assertEquals(vm.getKernelUrl(), kernel);
    }

    @Test
    public void validateIsoPrefixNameForKernelAndNullForInitrd() throws Exception {
        String kernel = "kernel";
        VM vm = createVmForTesting(null, ISO_PREFIX + kernel);
        assertEquals(vm.getInitrdUrl(), null);
        assertEquals(vm.getKernelUrl(), ACTIVE_ISO_PREFIX + "/" + kernel);
    }

    @Test
    public void validateIsoPrefixCaseSensitive() throws Exception {
        String initrd = "ISO://";
        VM vm = createVmForTesting(initrd, null);
        assertEquals(vm.getInitrdUrl(), "");
    }

    @Test
    public void validateIsoPrefixForOnlyIsoPrefixInKernelAndInitrd() throws Exception {
        String initrd = ISO_PREFIX;
        String kernelUrl = ISO_PREFIX;
        VM vm = createVmForTesting(initrd, kernelUrl);
        assertEquals(vm.getInitrdUrl(), "");
        assertEquals(vm.getKernelUrl(), "");
    }

    @Test
    public void checkIsoPrefixForNastyCharacters() throws Exception {
        String initrd = "@#$!";
        String kernelUrl = "    ";
        VM vm = createVmForTesting(initrd, kernelUrl);
        assertEquals(vm.getInitrdUrl(), initrd);
        assertEquals(vm.getKernelUrl(), kernelUrl);
    }

    @Test
    public void validateIsoPrefixNameForInitrdAndNullForKernel() throws Exception {
        String initrd = "initrd";
        VM vm = createVmForTesting(ISO_PREFIX + initrd, null);
        assertEquals(vm.getInitrdUrl(), ACTIVE_ISO_PREFIX + "/" + initrd);
        assertEquals(vm.getKernelUrl(), null);
    }

    @Test
    public void validateIsoPrefixWhenNoActiveIso() throws Exception {
        // Set Valid Iso Prefix
        setIsoPrefixVDSMethod(INACTIVE_ISO_PREFIX);

        String initrd = "initrd";
        VM vm = createVmForTesting(ISO_PREFIX + initrd, null);
        assertEquals(vm.getInitrdUrl(), INACTIVE_ISO_PREFIX + "/" + initrd);
    }

    @Test
    public void validateIsoPrefixWithTrippleSlash() throws Exception {
        String initrd = ISO_PREFIX + "/";
        VM vm = createVmForTesting(initrd, null);
        assertEquals(vm.getInitrdUrl(), ACTIVE_ISO_PREFIX + "/");
    }

    @Test
    public void validateIsoPrefixInTheMiddleOfTheInitrdAndKerenelName() throws Exception {
        String initrd = "initrd " + ISO_PREFIX;
        String kernelUrl = "kernelUrl " + ISO_PREFIX;
        VM vm = createVmForTesting(initrd, kernelUrl);
        assertEquals(vm.getInitrdUrl(), initrd);
        assertEquals(vm.getKernelUrl(), kernelUrl);
    }

    @Test
    public void validateInitrdWithSlashOnly() throws Exception {
        String initrd = "/";
        VM vm = createVmForTesting(initrd, null);
        assertEquals(vm.getInitrdUrl(), "/");
    }

    @Test
    public void validateIsoPrefixWithBackSlash() throws Exception {
        String initrd = "iso:\\";
        VM vm = createVmForTesting(initrd, null);
        assertEquals(vm.getInitrdUrl(), "iso:\\");
    }

    @Test
    public void validateBootPrefixForInitrdAndKernelImage() throws Exception {
        String initrd = "/boot";
        String kernelImage = "/boot";
        VM vm = createVmForTesting(initrd, kernelImage);
        assertEquals(vm.getInitrdUrl(), initrd);
        assertEquals(vm.getKernelUrl(), kernelImage);
    }

    @Test
    public void validateInitrdAndKernelImageWithOneCharacter() throws Exception {
        String initrd = "i";
        String kernelImage = "k";
        VM vm = createVmForTesting(initrd, kernelImage);
        assertEquals(vm.getInitrdUrl(), "i");
        assertEquals(vm.getKernelUrl(), "k");
    }

    private VM createVmForTesting(String initrd, String kernel) {
        mockVm(command);

        // Set parameter
        command.getVm().setInitrdUrl(initrd);
        command.getVm().setKernelUrl(kernel);
        command.createVm();

        // Check Vm
        VM vm = vmDAO.get(command.getParameters().getVmId());
        return vm;
    }

    /**
     * Mock a VM.
     */
    private VM mockVm(RunVmCommand<RunVmParams> spyVmCommand) {
        VM vm = new VM();
        vm.setStatus(VMStatus.Down);
        AuditLogableBaseMockUtils.mockVmDao(spyVmCommand, vmDAO);
        when(vmDAO.get(command.getParameters().getVmId())).thenReturn(vm);
        return vm;
    }

    @Before
    public void createCommand() {
        RunVmParams param = new RunVmParams(Guid.NewGuid());
        command = spy(new RunVmCommand<RunVmParams>(param));

        mockVmRunHandler();
        mockSuccessfulSnapshotValidator();
        mockVmPropertiesUtils();
        mockBackend();
    }

    protected void mockVmRunHandler() {
        doReturn(vmRunHandler).when(command).getVmRunHandler();

        doReturn(true).when(vmRunHandler).performImageChecksForRunningVm(any(VM.class),
                anyListOf(String.class),
                any(RunVmParams.class),
                anyListOf(Disk.class));
        doReturn(false).when(vmRunHandler).isVmInPreview(any(VM.class));
    }

    @Test
    public void canRunVmFailNodisk() {
        initDAOMocks(Collections.<Disk> emptyList(), Collections.<VmDevice> emptyList());

        final VM vm = new VM();
        doReturn(vm).when(command).getVm();
        doReturn(new VdsSelector(vm, new Guid(), true, new VdsFreeMemoryChecker(command))).when(command)
                .getVdsSelector();

        assertFalse(command.canRunVm());
        assertTrue(command.getReturnValue().getCanDoActionMessages().contains("VM_CANNOT_RUN_FROM_DISK_WITHOUT_DISK"));
    }

    @Test
    public void canRunVmFailVmRunning() {
        final ArrayList<Disk> disks = new ArrayList<Disk>();
        final DiskImage diskImage = createImage();
        disks.add(diskImage);
        final VmDevice vmDevice = createDiskVmDevice(diskImage);
        initDAOMocks(disks, Collections.singletonList(vmDevice));
        final VM vm = new VM();
        vm.setStatus(VMStatus.Up);
        doReturn(vm).when(command).getVm();
        doReturn(new VdsSelector(vm, new NGuid(), true, new VdsFreeMemoryChecker(command))).when(command)
                .getVdsSelector();

        assertFalse(command.canRunVm());
        assertTrue(command.getReturnValue().getCanDoActionMessages().contains("ACTION_TYPE_FAILED_VM_IS_RUNNING"));
    }

    @Test
    public void canRunVmFailVmDuringSnapshot() {
        final ArrayList<Disk> disks = new ArrayList<Disk>();
        final DiskImage diskImage = createImage();
        disks.add(diskImage);
        final VmDevice vmDevice = createDiskVmDevice(diskImage);
        initDAOMocks(disks, Collections.singletonList(vmDevice));
        final VM vm = new VM();
        SnapshotsValidator snapshotsValidator = mock(SnapshotsValidator.class);
        when(snapshotsValidator.vmNotDuringSnapshot(vm.getId()))
                .thenReturn(new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_DURING_SNAPSHOT));
        doReturn(snapshotsValidator).when(command).getSnapshotsValidator();
        doReturn(vm).when(command).getVm();

        assertFalse(command.canRunVm());
        assertTrue(command.getReturnValue()
                .getCanDoActionMessages()
                .contains(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_DURING_SNAPSHOT.name()));
    }

    private void canRunStatelessVmTest(boolean autoStartUp,
            boolean isVmStateless,
            Boolean isStatelessParam,
            boolean shouldPass) {
        final ArrayList<Disk> disks = new ArrayList<Disk>();
        final DiskImage diskImage = createImage();
        disks.add(diskImage);
        final VmDevice vmDevice = createDiskVmDevice(diskImage);

        final VdsSelector vdsSelector = mock(VdsSelector.class);
        when(vdsSelector.canFindVdsToRunOn(anyListOf(String.class), anyBoolean())).thenReturn(true);
        doReturn(vdsSelector).when(command).getVdsSelector();

        VDSReturnValue vdsReturnValue = new VDSReturnValue();
        vdsReturnValue.setReturnValue(false);
        when(vdsBrokerFrontend.RunVdsCommand(eq(VDSCommandType.IsVmDuringInitiating), any(VDSParametersBase.class))).thenReturn(vdsReturnValue);
        initDAOMocks(disks, Collections.singletonList(vmDevice));

        final VM vm = new VM();
        // set stateless and HA
        vm.setStateless(isVmStateless);
        vm.setAutoStartup(autoStartUp);
        doReturn(vm).when(command).getVm();

        command.getParameters().setRunAsStateless(isStatelessParam);
        boolean canRunVm = command.canRunVm();

        final List<String> messages = command.getReturnValue().getCanDoActionMessages();
        assertEquals(shouldPass, canRunVm);
        assertEquals(shouldPass, !messages.contains("VM_CANNOT_RUN_STATELESS_HA"));
    }

    private VmPropertiesUtils mockVmPropertiesUtils() {
        // Mocks vm properties utils (mocks a successful validation)
        VmPropertiesUtils utils = spy(new VmPropertiesUtils());
        doReturn(Collections.singletonMap("agent", "true")).when(utils).getPredefinedProperties(any(Version.class),
                any(VmStatic.class));
        doReturn(Collections.singletonMap("buff", "123")).when(utils).getUserDefinedProperties(any(Version.class),
                any(VmStatic.class));
        doReturn(new HashSet<Version>(Arrays.asList(Version.v3_0, Version.v3_1))).when(utils)
                .getSupportedClusterLevels();
        doReturn(Collections.emptyList()).when(utils).validateVMProperties(any(Version.class), any(VmStatic.class));
        doReturn(utils).when(command).getVmPropertiesUtils();
        return utils;
    }

    @Test
    public void canRunVmFailStatelessWhenVmHA() {
        canRunStatelessVmTest(true, false, Boolean.TRUE, false);
    }

    @Test
    public void canRunVmPassStatelessWhenVmHAandStatelessFalse() {
        canRunStatelessVmTest(true, true, Boolean.FALSE, true);
    }

    @Test
    public void canRunVmFailStatelessWhenVmHAwithNullStatelessParam() {
        canRunStatelessVmTest(true, true, null, false);
    }

    @Test
    public void canRunVmPassStatelessWhenVmHAwithNullStatelessParam() {
        canRunStatelessVmTest(true, false, null, true);
    }

    @Test
    public void canRunVmPassStatelessWhenVmHAwithNegativeStatelessParam() {
        canRunStatelessVmTest(true, false, Boolean.FALSE, true);
    }

    @Test
    public void canRunVmPassStatelessWhenVmNotHAwithNegativeStatelessParam() {
        canRunStatelessVmTest(false, false, Boolean.TRUE, true);
    }

    /**
     * @param disks
     * @param vmDevices
     * @param guid
     */
    protected void initDAOMocks(final List<Disk> disks, final List<VmDevice> vmDevices) {
        final DiskDao diskDao = mock(DiskDao.class);
        when(diskDao.getAllForVm(Guid.Empty)).thenReturn(disks);
        doReturn(diskDao).when(command).getDiskDao();
        doReturn(diskDao).when(vmRunHandler).getDiskDao();

        final StorageDomainDAO storageDomainDAO = mock(StorageDomainDAO.class);
        when(storageDomainDAO.getAllForStoragePool(Guid.Empty))
                .thenReturn(new ArrayList<storage_domains>());
        doReturn(storageDomainDAO).when(command).getStorageDomainDAO();
        doReturn(storageDomainDAO).when(vmRunHandler).getStorageDomainDAO();

        final VmDeviceDAO vmDeviceDao = mock(VmDeviceDAO.class);
        when(vmDeviceDao.getVmDeviceByVmIdTypeAndDevice(Guid.Empty,
                VmDeviceType.DISK.getName(),
                VmDeviceType.DISK.getName())).thenReturn(vmDevices);
        doReturn(vmDeviceDao).when(command).getVmDeviceDao();
        doReturn(vmDeviceDao).when(vmRunHandler).getVmDeviceDAO();
    }

    private SnapshotsValidator mockSuccessfulSnapshotValidator() {
        SnapshotsValidator snapshotsValidator = mock(SnapshotsValidator.class);
        when(snapshotsValidator.vmNotDuringSnapshot(any(Guid.class))).thenReturn(ValidationResult.VALID);
        doReturn(snapshotsValidator).when(command).getSnapshotsValidator();
        return snapshotsValidator;
    }
}
