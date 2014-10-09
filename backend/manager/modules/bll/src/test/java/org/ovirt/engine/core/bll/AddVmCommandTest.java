package org.ovirt.engine.core.bll;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.common.action.AddVmFromSnapshotParameters;
import org.ovirt.engine.core.common.action.AddVmFromTemplateParameters;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskImageBase;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBaseMockUtils;
import org.ovirt.engine.core.dao.DiskImageDAO;
import org.ovirt.engine.core.dao.SnapshotDao;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.VdsGroupDAO;
import org.ovirt.engine.core.dao.VmDAO;
import org.ovirt.engine.core.dao.VmTemplateDAO;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("serial")
public class AddVmCommandTest {

    private static final int REQUIRED_DISK_SIZE_GB = 10;
    private static final int AVAILABLE_SPACE_GB = 11;
    private static final int USED_SPACE_GB = 4;
    private static final Guid STORAGE_POOL_ID = Guid.NewGuid();
    private static final Guid STORAGE_DOMAIN_ID = Guid.NewGuid();
    private VmTemplate vmTemplate = null;

    @Rule
    public MockConfigRule mcr = new MockConfigRule();

    @Mock
    StorageDomainDAO sdDAO;

    @Mock
    VmTemplateDAO vmTemplateDAO;

    @Mock
    VmDAO vmDAO;

    @Mock
    DiskImageDAO diskImageDAO;

    @Mock
    VdsGroupDAO vdsGroupDao;

    @Mock
    BackendInternal backend;

    @Mock
    VDSBrokerFrontend vdsBrokerFrontend;

    @Mock
    SnapshotDao snapshotDao;

    @Test
    public void create10GBVmWith11GbAvailableAndA5GbBuffer() throws Exception {
        VM vm = createVm();
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd = createVmFromTemplateCommand(vm);

        mockStorageDomainDAOGetForStoragePool();
        mockVmTemplateDAOReturnVmTemplate();
        mockDiskImageDAOGetSnapshotById();
        mockGetImageDomainsListVdsCommand();
        mockVerifyAddVM(cmd);
        mockConfig();
        mockConfigSizeDefaults();

        mockStorageDomainDaoGetAllStoragesForPool(AVAILABLE_SPACE_GB);
        mockUninterestingMethods(cmd);
        assertFalse("If the disk is too big, canDoAction should fail", cmd.canDoAction());
        assertTrue("canDoAction failed for the wrong reason",
                cmd.getReturnValue()
                        .getCanDoActionMessages()
                        .contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW.toString()));
    }

    @Test
    public void canAddVm() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 20;
        final int sizeRequired = 5;
        final int pctRequired = 10;
        AddVmCommand<VmManagementParametersBase> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired, pctRequired);
        doReturn(Collections.emptyList()).when(cmd).validateCustomProperties(any(VmStatic.class));
        assertTrue("vm could not be added", cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
    }

    @Test
    public void canAddVmFailSpaceThreshold() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int sizeRequired = 10;
        final int pctRequired = 0;
        final int domainSizeGB = 4;
        AddVmCommand<VmManagementParametersBase> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired, pctRequired);
        doReturn(Collections.emptyList()).when(cmd).validateCustomProperties(any(VmStatic.class));
        assertFalse("vm could not be added", cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
        assertTrue("canDoAction failed for the wrong reason",
                reasons.contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW.toString()));
    }

    @Test
    public void canAddVmFailPctThreshold() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int sizeRequired = 0;
        final int pctRequired = 95;
        final int domainSizeGB = 10;
        AddVmCommand<VmManagementParametersBase> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired, pctRequired);
        doReturn(Collections.emptyList()).when(cmd).validateCustomProperties(any(VmStatic.class));
        assertFalse("vm could not be added", cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
        assertTrue("canDoAction failed for the wrong reason",
                reasons.contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW.toString()));
    }

    @Test
    public void canAddThinVmFromTemplateWithManyDisks() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 20;
        final int sizeRequired = 10;
        final int pctRequired = 10;
        AddVmCommand<VmManagementParametersBase> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired, pctRequired);
        doReturn(Collections.emptyList()).when(cmd).validateCustomProperties(any(VmStatic.class));
        // Adding 10 disks, which each one should consume the default sparse size (which is 1GB).
        setNewDisksForTemplate(10, cmd.getVmTemplate().getDiskMap());
        doReturn(createVmTemplate()).when(cmd).getVmTemplate();
        assertFalse("Thin vm could not be added due to storage sufficient",
                cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
        assertTrue("canDoAction failed for insufficient disk size",
                reasons.contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW.toString()));
    }

    @Test
    public void canAddCloneVmFromTemplate() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 15;
        final int sizeRequired = 4;
        final int pctRequired = 10;
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd =
                setupCanAddVmFromTemplateTests(domainSizeGB, sizeRequired, pctRequired);

        // Set new Disk Image map with 3GB.
        setNewImageDiskMapForTemplate(cmd, 3000000000L, cmd.getVmTemplate().getDiskImageMap());
        assertFalse("Clone vm could not be added due to storage sufficient",
                cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
        assertTrue("canDoAction failed for insufficient disk size",
                reasons.contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW.toString()));
    }

    @Test
    public void canAddCloneVmFromTemplateInvalidPercentage() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 30;
        final int sizeRequired = 6;
        final int pctRequired = 53;
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd =
                setupCanAddVmFromTemplateTests(domainSizeGB, sizeRequired, pctRequired);
        setNewImageDiskMapForTemplate(cmd, 3000000000L, cmd.getVmTemplate().getDiskImageMap());
        assertFalse("Thin vm could not be added due to storage sufficient",
                cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
        assertTrue("canDoAction failed for insufficient disk size",
                reasons.contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW.toString()));
    }

    @Test
    public void canAddCloneVmFromSnapshotSnapshotDoesNotExist() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 15;
        final int sizeRequired = 4;
        final int pctRequired = 10;
        final Guid sourceSnapshotId = Guid.NewGuid();
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd =
                setupCanAddVmFromSnapshotTests(domainSizeGB, sizeRequired, pctRequired, sourceSnapshotId);
        cmd.getVm().setVmName("vm1");
        mockNonInterestingMethodsForCloneVmFromSnapshot(cmd);
        assertFalse("Clone vm should have failed due to non existing snapshot id", cmd.canDoAction());
        reasons = cmd.getReturnValue().getCanDoActionMessages();
        assertTrue("Clone vm should have failed due to non existing snapshot id",
                reasons.contains(VdcBllMessages.ACTION_TYPE_FAILED_VM_SNAPSHOT_DOES_NOT_EXIST.toString()));
    }

    @Test
    public void canAddCloneVmFromSnapshotNoConfiguration() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 15;
        final int sizeRequired = 4;
        final int pctRequired = 10;
        final Guid sourceSnapshotId = Guid.NewGuid();
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd =
                setupCanAddVmFromSnapshotTests(domainSizeGB, sizeRequired, pctRequired, sourceSnapshotId);
        cmd.getVm().setVmName("vm1");
        mockNonInterestingMethodsForCloneVmFromSnapshot(cmd);
        when(snapshotDao.get(sourceSnapshotId)).thenReturn(new Snapshot());
        assertFalse("Clone vm should have failed due to non existing vm configuration", cmd.canDoAction());
        reasons = cmd.getReturnValue().getCanDoActionMessages();
        assertTrue("Clone vm should have failed due to no configuration id",
                reasons.contains(VdcBllMessages.ACTION_TYPE_FAILED_VM_SNAPSHOT_HAS_NO_CONFIGURATION.toString()));

    }

    protected void mockNonInterestingMethodsForCloneVmFromSnapshot(AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd) {
        mockUninterestingMethods(cmd);
        doReturn(true).when(cmd).checkCpuSockets();
        doReturn(null).when(cmd).getVmFromConfiguration();
    }

    private AddVmFromTemplateCommand<AddVmFromTemplateParameters> createVmFromTemplateCommand(VM vm) {
        AddVmFromTemplateParameters param = new AddVmFromTemplateParameters();
        param.setVm(vm);
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> concrete =
                new AddVmFromTemplateCommand<AddVmFromTemplateParameters>(param) {
                    @Override
                    protected void initTemplateDisks() {
                        // Stub for testing
                    }

                    @Override
                    protected void initStoragePoolId() {
                        // Stub for testing
                    }
                };
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> result = spy(concrete);
        doReturn(true).when(result).checkNumberOfMonitors();
        doReturn(createVmTemplate()).when(result).getVmTemplate();
        doReturn(Collections.emptyList()).when(result).validateCustomProperties(any(VmStatic.class));
        mockDAOs(result);
        mockBackend(result);
        return result;
    }

    private AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> createVmFromSnapshotCommand(VM vm,
            Guid sourceSnapshotId) {
        AddVmFromSnapshotParameters param = new AddVmFromSnapshotParameters();
        param.setVm(vm);
        param.setSourceSnapshotId(sourceSnapshotId);
        param.setStorageDomainId(Guid.NewGuid());
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd =
                new AddVmFromSnapshotCommand<AddVmFromSnapshotParameters>(param) {
                    @Override
                    protected void initTemplateDisks() {
                        // Stub for testing
                    }

                    @Override
                    protected void initStoragePoolId() {
                        // Stub for testing
                    }
                };
        cmd = spy(cmd);
        doReturn(vm).when(cmd).getVm();
        mockDAOs(cmd);
        doReturn(snapshotDao).when(cmd).getSnapshotDao();
        mockBackend(cmd);
        return cmd;
    }

    private AddVmFromTemplateCommand<AddVmFromTemplateParameters> setupCanAddVmFromTemplateTests(final int domainSizeGB,
            final int sizeRequired,
            final int pctRequired) {
        VM vm = initializeMock(domainSizeGB, sizeRequired, pctRequired);
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd = createVmFromTemplateCommand(vm);
        initCommandMethods(cmd);
        return cmd;
    }

    private AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> setupCanAddVmFromSnapshotTests(final int domainSizeGB,
            final int sizeRequired,
            final int pctRequired,
            Guid sourceSnapshotId) {
        VM vm = initializeMock(domainSizeGB, sizeRequired, pctRequired);
        initializeVmDAOMock(vm);
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd = createVmFromSnapshotCommand(vm, sourceSnapshotId);
        initCommandMethods(cmd);
        return cmd;
    }

    private void initializeVmDAOMock(VM vm) {
        when(vmDAO.get(Matchers.<Guid> any(Guid.class))).thenReturn(vm);
    }

    private AddVmCommand<VmManagementParametersBase> setupCanAddVmTests(final int domainSizeGB,
            final int sizeRequired,
            final int pctRequired) {
        VM vm = initializeMock(domainSizeGB, sizeRequired, pctRequired);
        AddVmCommand<VmManagementParametersBase> cmd = createCommand(vm);
        initCommandMethods(cmd);
        doReturn(createVmTemplate()).when(cmd).getVmTemplate();
        return cmd;
    }

    private static <T extends VmManagementParametersBase> void initCommandMethods(AddVmCommand<T> cmd) {
        doReturn(Guid.NewGuid()).when(cmd).getStoragePoolId();
        doReturn(true).when(cmd).canAddVm(anyListOf(String.class), anyString(), any(Guid.class), anyInt());
        doReturn(STORAGE_POOL_ID).when(cmd).getStoragePoolId();
    }

    private VM initializeMock(final int domainSizeGB, final int sizeRequired, final int pctRequired) {
        mockVmTemplateDAOReturnVmTemplate();
        mockDiskImageDAOGetSnapshotById();
        mockStorageDomainDAOGetForStoragePool(domainSizeGB);
        mockStorageDomainDAOGet(domainSizeGB);
        mockGetImageDomainsListVdsCommand();
        mockConfig();
        mockConfigSizeRequirements(sizeRequired, pctRequired);
        VM vm = createVm();
        return vm;
    }

    private static void setNewDisksForTemplate(int numberOfNewDisks, Map<Guid, DiskImage> disksMap) {
        for (int newDiskInd = 0; newDiskInd < numberOfNewDisks; newDiskInd++) {
            DiskImage diskImageTempalte = new DiskImage();
            diskImageTempalte.setImageId(Guid.NewGuid());
            disksMap.put(Guid.NewGuid(), diskImageTempalte);
        }
    }

    private static void setNewImageDiskMapForTemplate(AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd,
            long diskSize,
            Map<Guid, DiskImage> diskImageMap) {
        DiskImage diskImage = new DiskImage();
        diskImage.setactual_size(diskSize);
        diskImage.setstorage_ids(new ArrayList<Guid>(Arrays.asList(STORAGE_DOMAIN_ID)));
        diskImageMap.put(Guid.NewGuid(), diskImage);
        cmd.storageToDisksMap = new HashMap<Guid, List<DiskImage>>();
        cmd.storageToDisksMap.put(STORAGE_DOMAIN_ID,
                new ArrayList<DiskImage>(diskImageMap.values()));
    }

    private void mockBackend(AddVmCommand<?> cmd) {
        VdcQueryReturnValue returnValue = new VdcQueryReturnValue();
        returnValue.setReturnValue(Boolean.FALSE);
        when(backend.runInternalQuery(Matchers.<VdcQueryType> any(VdcQueryType.class),
                Matchers.any(VdcQueryParametersBase.class))).thenReturn(returnValue);
        when(backend.getResourceManager()).thenReturn(vdsBrokerFrontend);
        doReturn(backend).when(cmd).getBackend();
    }

    private void mockDAOs(AddVmCommand<?> cmd) {
        AuditLogableBaseMockUtils.mockVmDao(cmd, vmDAO);
        doReturn(sdDAO).when(cmd).getStorageDomainDAO();
        doReturn(vmTemplateDAO).when(cmd).getVmTemplateDAO();
        doReturn(vdsGroupDao).when(cmd).getVdsGroupDAO();
    }

    private void mockStorageDomainDAOGetForStoragePool(int domainSpaceGB) {
        when(sdDAO.getForStoragePool(Matchers.<Guid> any(Guid.class), Matchers.<NGuid> any(NGuid.class))).thenReturn(createStorageDomain(domainSpaceGB));
    }

    private void mockStorageDomainDAOGet(final int domainSpaceGB) {
        doAnswer(new Answer<storage_domains>() {

            @Override
            public storage_domains answer(InvocationOnMock invocation) throws Throwable {
                storage_domains result = createStorageDomain(domainSpaceGB);
                result.setId((Guid) invocation.getArguments()[0]);
                return result;
            }

        }).when(sdDAO).get(any(Guid.class));
    }

    private void mockStorageDomainDaoGetAllStoragesForPool(int domainSpaceGB) {
        when(sdDAO.getAllForStoragePool(any(Guid.class))).thenReturn(Arrays.asList(createStorageDomain(domainSpaceGB)));
    }

    private void mockStorageDomainDAOGetForStoragePool() {
        mockStorageDomainDAOGetForStoragePool(AVAILABLE_SPACE_GB);
    }

    private void mockVmTemplateDAOReturnVmTemplate() {
        when(vmTemplateDAO.get(Matchers.<Guid> any(Guid.class))).thenReturn(createVmTemplate());
    }

    private VmTemplate createVmTemplate() {
        if (vmTemplate == null) {
            vmTemplate = new VmTemplate();
            vmTemplate.setstorage_pool_id(STORAGE_POOL_ID);
            DiskImage image = createDiskImageTemplate();
            vmTemplate.getDiskMap().put(image.getImageId(), image);
            Map<Guid, DiskImage> diskImageMap = new HashMap<Guid, DiskImage>();
            DiskImage diskImage = createDiskImage(REQUIRED_DISK_SIZE_GB);
            diskImageMap.put(diskImage.getId(), diskImage);
            vmTemplate.setDiskImageMap(diskImageMap);
        }
        return vmTemplate;
    }

    private static DiskImage createDiskImageTemplate() {
        DiskImage i = new DiskImage();
        i.setSizeInGigabytes(USED_SPACE_GB + AVAILABLE_SPACE_GB);
        i.setactual_size(REQUIRED_DISK_SIZE_GB * 1024L * 1024L * 1024L);
        i.setImageId(Guid.NewGuid());
        i.setstorage_ids(new ArrayList<Guid>(Arrays.asList(STORAGE_DOMAIN_ID)));
        return i;
    }

    private void mockDiskImageDAOGetSnapshotById() {
        when(diskImageDAO.getSnapshotById(Matchers.<Guid> any(Guid.class))).thenReturn(createDiskImage(REQUIRED_DISK_SIZE_GB));
    }

    private static DiskImage createDiskImage(int size) {
        DiskImage img = new DiskImage();
        img.setSizeInGigabytes(size);
        img.setActualSize(size);
        img.setId(Guid.NewGuid());
        img.setstorage_ids(new ArrayList<Guid>(Arrays.asList(STORAGE_DOMAIN_ID)));
        return img;
    }

    private void mockGetImageDomainsListVdsCommand() {
        ArrayList<Guid> guids = new ArrayList<Guid>(1);
        guids.add(Guid.NewGuid());
        VDSReturnValue returnValue = new VDSReturnValue();
        returnValue.setReturnValue(guids);
        when(vdsBrokerFrontend.RunVdsCommand(eq(VDSCommandType.GetImageDomainsList),
                Matchers.<VDSParametersBase> any(VDSParametersBase.class))).thenReturn(returnValue);
    }

    protected storage_domains createStorageDomain(int availableSpace) {
        storage_domains sd = new storage_domains();
        sd.setstorage_domain_type(StorageDomainType.Master);
        sd.setstatus(StorageDomainStatus.Active);
        sd.setavailable_disk_size(availableSpace);
        sd.setused_disk_size(USED_SPACE_GB);
        sd.setId(STORAGE_DOMAIN_ID);
        return sd;
    }

    private static void mockVerifyAddVM(AddVmCommand<?> cmd) {
        doReturn(true).when(cmd).verifyAddVM(anyListOf(String.class), anyInt());
    }

    private void mockConfig() {
        mcr.mockConfigValue(ConfigValues.PredefinedVMProperties, "3.0", "");
        mcr.mockConfigValue(ConfigValues.UserDefinedVMProperties, "3.0", "");
        mcr.mockConfigValue(ConfigValues.InitStorageSparseSizeInGB, 1);
    }

    private void mockConfigSizeRequirements(int requiredSpaceBufferInGB, int requiredSpacePercent) {
        mcr.mockConfigValue(ConfigValues.FreeSpaceCriticalLowInGB, requiredSpaceBufferInGB);
        mcr.mockConfigValue(ConfigValues.FreeSpaceLow, requiredSpacePercent);
    }

    private void mockConfigSizeDefaults() {
        int requiredSpaceBufferInGB = 5;
        int requiredSpacePercent = 0;
        mockConfigSizeRequirements(requiredSpaceBufferInGB, requiredSpacePercent);
    }

    private static VM createVm() {
        VM vm = new VM();
        VmDynamic dynamic = new VmDynamic();
        VmStatic stat = new VmStatic();
        stat.setVmtGuid(Guid.NewGuid());
        stat.setVmName("testVm");
        stat.setPriority(1);
        vm.setStaticData(stat);
        vm.setDynamicData(dynamic);
        return vm;
    }

    private AddVmCommand<VmManagementParametersBase> createCommand(VM vm) {
        VmManagementParametersBase param = new VmManagementParametersBase(vm);
        AddVmCommand<VmManagementParametersBase> cmd = new AddVmCommand<VmManagementParametersBase>(param) {
            @Override
            protected void initTemplateDisks() {
                // Stub for testing
            }

            @Override
            protected void initStoragePoolId() {
                // stub for testing
            }

            @Override
            protected int getNeededDiskSize(Guid domainId) {
                return getBlockSparseInitSizeInGb() * getVmTemplate().getDiskMap().size();
            }
        };
        cmd = spy(cmd);
        mockDAOs(cmd);
        mockBackend(cmd);
        return cmd;
    }

    private <T extends VmManagementParametersBase> void mockUninterestingMethods(AddVmCommand<T> spy) {
        doReturn(true).when(spy).isVmNameValidLength(Matchers.<VM> any(VM.class));
        doReturn(STORAGE_POOL_ID).when(spy).getStoragePoolId();
        doReturn(createVmTemplate()).when(spy).getVmTemplate();
        doReturn(true).when(spy).areParametersLegal(anyListOf(String.class));
        doReturn(Collections.<VmNetworkInterface> emptyList()).when(spy).getVmInterfaces();
        doReturn(Collections.<DiskImageBase> emptyList()).when(spy).getVmDisks();
        spy.setVmTemplateId(Guid.NewGuid());
    }
}
