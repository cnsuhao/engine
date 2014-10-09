package org.ovirt.engine.core.bll;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.common.action.ImportVmTemplateParameters;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.StorageDomainDynamic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.queries.DiskImageList;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BusinessEntitySnapshotDAO;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.StorageDomainStaticDAO;
import org.ovirt.engine.core.dao.VmTemplateDAO;

public class TestHelperImportVmTemplateCommand extends ImportVmTemplateCommand {
    private static final long serialVersionUID = 1L;

    public TestHelperImportVmTemplateCommand(final ImportVmTemplateParameters p) {
        super(p);
    }

    @Override
    protected BusinessEntitySnapshotDAO getBusinessEntitySnapshotDAO() {
        return null;
    }

    @Override
    public StorageDomainDAO getStorageDomainDAO() {
        final storage_domains destination = new storage_domains();
        destination.setstorage_domain_type(StorageDomainType.Data);
        destination.setstatus(StorageDomainStatus.Active);

        final StorageDomainDAO d = mock(StorageDomainDAO.class);
        when(d.getForStoragePool(any(Guid.class), any(Guid.class))).thenReturn(destination);
        StorageDomainDynamic dy = new StorageDomainDynamic();
        dy.setavailable_disk_size(10);
        dy.setused_disk_size(0);
        destination.setStorageDynamicData(dy);
        return d;
    }

    @Override
    protected StorageDomainStaticDAO getStorageDomainStaticDAO() {
        StorageDomainStaticDAO d = mock(StorageDomainStaticDAO.class);
        when(d.get(any(Guid.class))).thenReturn(new StorageDomainStatic());
        return d;
    }

    @Override
    public VmTemplateDAO getVmTemplateDAO() {
        VmTemplateDAO d = mock(VmTemplateDAO.class);
        return d;
    }

    @Override
    public storage_pool getStoragePool() {
        return new storage_pool();
    }

    @Override
    public BackendInternal getBackend() {
        BackendInternal backend = mock(BackendInternal.class);
        when(backend.runInternalQuery(eq(VdcQueryType.GetTemplatesFromExportDomain), any(VdcQueryParametersBase.class))).thenReturn(createDiskImageQueryResult());
        return backend;
    }

    @Override
    protected storage_domains getSourceDomain() {
        storage_domains source = new storage_domains();
        source.setstorage_domain_type(StorageDomainType.ImportExport);
        source.setstatus(StorageDomainStatus.Active);
        return source;
    }

    @Override
    protected boolean isVmTemplateWithSameNameExist() {
        return false;
    }

    private static VdcQueryReturnValue createDiskImageQueryResult() {
        final VdcQueryReturnValue v = new VdcQueryReturnValue();
        Map<VmTemplate, DiskImageList> m = new HashMap<VmTemplate, DiskImageList>();
        VmTemplate t = new VmTemplate();
        DiskImage i = new DiskImage();
        i.setactual_size(2);
        ArrayList<DiskImage> ial = new ArrayList<DiskImage>();
        ial.add(i);
        DiskImageList il = new DiskImageList(ial);
        m.put(t, il);
        v.setReturnValue(m);
        v.setSucceeded(true);
        return v;
    }
}
