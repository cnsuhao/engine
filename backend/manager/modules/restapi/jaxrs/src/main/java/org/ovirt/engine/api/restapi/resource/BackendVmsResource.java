package org.ovirt.engine.api.restapi.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.common.util.DetailHelper;
import org.ovirt.engine.api.common.util.DetailHelper.Detail;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Certificate;
import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.Disks;
import org.ovirt.engine.api.model.Display;
import org.ovirt.engine.api.model.MemoryPolicy;
import org.ovirt.engine.api.model.Nics;
import org.ovirt.engine.api.model.Payload;
import org.ovirt.engine.api.model.Payloads;
import org.ovirt.engine.api.model.Snapshots;
import org.ovirt.engine.api.model.Statistics;
import org.ovirt.engine.api.model.Tags;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.model.VMs;
import org.ovirt.engine.api.resource.VmResource;
import org.ovirt.engine.api.resource.VmsResource;
import org.ovirt.engine.api.restapi.types.DiskMapper;
import org.ovirt.engine.api.restapi.types.VmMapper;
import org.ovirt.engine.core.common.action.AddVmFromScratchParameters;
import org.ovirt.engine.core.common.action.AddVmFromSnapshotParameters;
import org.ovirt.engine.core.common.action.AddVmFromTemplateParameters;
import org.ovirt.engine.core.common.action.RemoveVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmPayload;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.GetAllDisksByVmIdParameters;
import org.ovirt.engine.core.common.queries.GetVdsGroupByNameParameters;
import org.ovirt.engine.core.common.queries.GetVdsGroupByVdsGroupIdParameters;
import org.ovirt.engine.core.common.queries.GetVmByVmIdParameters;
import org.ovirt.engine.core.common.queries.GetVmConfigurationBySnapshotQueryParams;
import org.ovirt.engine.core.common.queries.GetVmTemplateParameters;
import org.ovirt.engine.core.common.queries.GetVmTemplatesDisksParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendVmsResource extends
        AbstractBackendCollectionResource<VM, org.ovirt.engine.core.common.businessentities.VM>
        implements VmsResource {

    static final String[] SUB_COLLECTIONS = { "disks", "nics", "cdroms", "snapshots", "tags", "permissions",
            "statistics", "reporteddevices" };

    public BackendVmsResource() {
        super(VM.class, org.ovirt.engine.core.common.businessentities.VM.class, SUB_COLLECTIONS);
    }

    @Override
    public VMs list() {
        if (isFiltered())
            return mapCollection(getBackendCollection(VdcQueryType.GetAllVms, new VdcQueryParametersBase()), true);
        else
         return mapCollection(getBackendCollection(SearchType.VM), false);
    }

    @Override
    @SingleEntityResource
    public VmResource getVmSubResource(String id) {
        return inject(new BackendVmResource(id, this));
    }

    @Override
    public Response add(VM vm) {
        validateParameters(vm, "name", "cluster.id|name");
        validateEnums(VM.class, vm);
        Response response = null;
        if (isCreateFromSnapshot(vm)) {
            response = createVmFromSnapshot(vm);
        } else {
            validateParameters(vm, "template.id|name");
            Guid templateId = getTemplateId(vm);
            VmStatic staticVm = getMapper(VM.class, VmStatic.class).map(vm,
                    getMapper(VmTemplate.class, VmStatic.class).map(lookupTemplate(templateId), null));
            if (namedCluster(vm)) {
                staticVm.setVdsGroupId(getClusterId(vm));
            }

            staticVm.setUsbPolicy(VmMapper.getUsbPolicyOnCreate(vm.getUsb(), lookupCluster(staticVm.getVdsGroupId())));

            if (!isFiltered()) {
                // if the user set the host-name within placement-policy, rather than the host-id (legal) -
                // resolve the host's ID, because it will be needed down the line
                if (vm.isSetPlacementPolicy() && vm.getPlacementPolicy().isSetHost()
                        && vm.getPlacementPolicy().getHost().isSetName()
                        && !vm.getPlacementPolicy().getHost().isSetId()) {
                    staticVm.setDedicatedVmForVds(asGuid(getHostId(vm.getPlacementPolicy().getHost().getName())));
                }
            } else {
                vm.setPlacementPolicy(null);
            }
            Guid storageDomainId =
                    (vm.isSetStorageDomain() && vm.getStorageDomain().isSetId()) ? asGuid(vm.getStorageDomain().getId())
                            : Guid.Empty;
            if (vm.isSetDisks() && vm.getDisks().isSetClone() && vm.getDisks().isClone()) {
                response = cloneVmFromTemplate(staticVm, vm, templateId);
            } else if (Guid.Empty.equals(templateId)) {
                response = addVmFromScratch(staticVm, vm, storageDomainId);
            } else {
                response = addVm(staticVm, vm, storageDomainId, templateId);
            }
        }
        return removeRestrictedInfoFromResponse(response);
    }

    private boolean shouldMakeCreatorExplicitOwner() {
        // In the user level API we should make the creator the owner of the new created machine
        return isFiltered();
    }

    private boolean isCreateFromSnapshot(VM vm) {
        return vm.isSetSnapshots() && vm.getSnapshots().getSnapshots() != null
                && !vm.getSnapshots().getSnapshots().isEmpty();
    }

    private Response createVmFromSnapshot(VM vm) {
        // If Vm has snapshots collection - this is a clone vm from snapshot operation
        String snapshotId = getSnapshotId(vm.getSnapshots());
        org.ovirt.engine.core.common.businessentities.VM vmConfiguration = getVmConfiguration(snapshotId);
        getMapper(VM.class, VmStatic.class).map(vm, vmConfiguration.getStaticData());
        // If vm passed in the call has disks attached on them,
        // merge their data with the data of the disks on the configuration
        // The parameters to AddVmFromSnapshot hold an array list of Disks
        // and not List of Disks, as this is a GWT serialization limitation,
        // and this parameter class serves GWT clients as well.
        HashMap<Guid, DiskImage> diskImagesByImageId = getDiskImagesByIdMap(vmConfiguration.getDiskMap().values());
        if (vm.isSetDisks()) {
            prepareImagesForCloneFromSnapshotParams(vm.getDisks(), diskImagesByImageId);
        }
        return cloneVmFromSnapshot(vmConfiguration.getStaticData(),
                        snapshotId,
                        diskImagesByImageId);
    }

    private Response removeRestrictedInfoFromResponse(Response response) {
        if (isFiltered()) {
            VM vm = (VM) response.getEntity();
            removeRestrictedInfoFromVM(vm);
        }
        return response;
    }

    private VM removeRestrictedInfoFromVM(VM vm) {
        if (vm != null) {
            vm.setHost(null);
            vm.setPlacementPolicy(null);
        }
        return vm;
    }

    protected VmPayload getPayload(VM vm) {
        VmPayload payload = null;
        if (vm.isSetPayloads() && vm.getPayloads().isSetPayload()) {
            payload = getMapper(Payload.class, VmPayload.class).map(vm.getPayloads().getPayload().get(0), new VmPayload());
        }
        return payload;
    }

    protected org.ovirt.engine.core.common.businessentities.VM getVmConfiguration(String snapshotId) {
        org.ovirt.engine.core.common.businessentities.VM vmConfiguration =
                getEntity(org.ovirt.engine.core.common.businessentities.VM.class,
                        VdcQueryType.GetVmConfigurationBySnapshot,
                        new GetVmConfigurationBySnapshotQueryParams(asGuid(snapshotId)),
                        "");
        return vmConfiguration;
    }

    private void prepareImagesForCloneFromSnapshotParams(Disks disks,
            Map<Guid, DiskImage> imagesFromConfiguration) {
        if (disks.getDisks() != null) {
            for (Disk disk : disks.getDisks()) {
                DiskImage diskImageFromConfig = imagesFromConfiguration.get(asGuid(disk.getImageId()));
                DiskImage diskImage = (DiskImage)getMapper(Disk.class, org.ovirt.engine.core.common.businessentities.Disk.class).map(disk, diskImageFromConfig);
                imagesFromConfiguration.put(diskImage.getId(), diskImage);
            }
        }
    }

    private HashMap<Guid, DiskImage> getDiskImagesByIdMap(Collection<org.ovirt.engine.core.common.businessentities.Disk> values) {
        HashMap<Guid, DiskImage> result = new HashMap<Guid, DiskImage>();
        for (org.ovirt.engine.core.common.businessentities.Disk diskImage : values) {
            result.put(((DiskImage) diskImage).getId(), (DiskImage) diskImage);
        }
        return result;
    }

    private String getSnapshotId(Snapshots snapshots) {
        return (snapshots.getSnapshots() != null && !snapshots.getSnapshots().isEmpty()) ? snapshots.getSnapshots()
                .get(0)
                .getId() : Guid.Empty.toString();
    }

    private String getHostId(String hostName) {
        return getEntity(VDS.class, SearchType.VDS, "Hosts: name=" + hostName).getId().toString();
    }

    private Response cloneVmFromSnapshot(VmStatic staticVm,
            String snapshotId,
            HashMap<Guid, DiskImage> images) {
        Guid sourceSnapshotId = asGuid(snapshotId);
        AddVmFromSnapshotParameters params =
                new AddVmFromSnapshotParameters(staticVm, sourceSnapshotId);
        params.setDiskInfoDestinationMap(images);
        params.setMakeCreatorExplicitOwner(shouldMakeCreatorExplicitOwner());
        return performCreate(VdcActionType.AddVmFromSnapshot,
                                params,
                                new QueryIdResolver<Guid>(VdcQueryType.GetVmByVmId, GetVmByVmIdParameters.class));
    }

    private Response cloneVmFromTemplate(VmStatic staticVm, VM vm, Guid templateId) {
        AddVmFromTemplateParameters params = new AddVmFromTemplateParameters(staticVm, getDisksToClone(vm.getDisks(), templateId), Guid.Empty);
        params.setVmPayload(getPayload(vm));
        if (vm.isSetMemoryPolicy() && vm.getMemoryPolicy().isSetBallooning()) {
            params.setBalloonEnabled(vm.getMemoryPolicy().isBallooning());
        }
        params.setMakeCreatorExplicitOwner(shouldMakeCreatorExplicitOwner());
        return performCreate(VdcActionType.AddVmFromTemplate,
                               params,
                               new QueryIdResolver<Guid>(VdcQueryType.GetVmByVmId, GetVmByVmIdParameters.class));
    }

    private HashMap<Guid, DiskImage> getDisksToClone(Disks disks, Guid templateId) {
        HashMap<Guid, DiskImage> disksMap = new HashMap<Guid, DiskImage>();

        if (disks != null && disks.isSetDisks() && disks.getDisks().size() > 0){
            HashMap<Guid, DiskImage> templatesDisksMap = getTemplateDisks(templateId);
            for (Disk disk : disks.getDisks()) {
                DiskImage templateDisk = templatesDisksMap.get(asGuid(disk.getId()));
                if( templateDisk != null ) {
                    disksMap.put(templateDisk.getId(), map(disk, templateDisk));
                } else {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
            }
        }
        return disksMap;
    }

    @SuppressWarnings("unchecked")
    private HashMap<Guid, DiskImage> getTemplateDisks(Guid templateId) {
        HashMap<Guid, DiskImage> templatesDisksMap = new HashMap<Guid, DiskImage>();
        for (DiskImage di : (List<DiskImage>) getEntity(List.class,
                                                      VdcQueryType.GetVmTemplatesDisks,
                                                      new GetVmTemplatesDisksParameters(templateId),
                                                      "Disks")) {
            templatesDisksMap.put(di.getId(), di);
        }
        return templatesDisksMap;
    }

    private DiskImage map(Disk entity, DiskImage template) {
        return (DiskImage)getMapper(Disk.class, org.ovirt.engine.core.common.businessentities.Disk.class).map(entity, template);
    }

    protected Response addVm(VmStatic staticVm, VM vm, Guid storageDomainId, Guid templateId) {
        VmManagementParametersBase params = new VmManagementParametersBase(staticVm);
        params.setVmPayload(getPayload(vm));
        if (vm.isSetMemoryPolicy() && vm.getMemoryPolicy().isSetBallooning()) {
            params.setBalloonEnabled(vm.getMemoryPolicy().isBallooning());
        }
        params.setStorageDomainId(storageDomainId);
        params.setDiskInfoDestinationMap(getDisksToClone(vm.getDisks(), templateId));
        params.setMakeCreatorExplicitOwner(shouldMakeCreatorExplicitOwner());
        return performCreate(VdcActionType.AddVm,
                               params,
                               new QueryIdResolver<Guid>(VdcQueryType.GetVmByVmId, GetVmByVmIdParameters.class));
    }

    protected Response addVmFromScratch(VmStatic staticVm, VM vm, Guid storageDomainId) {
        AddVmFromScratchParameters params = new AddVmFromScratchParameters(staticVm, mapDisks(vm.getDisks()), Guid.Empty);
        params.setVmPayload(getPayload(vm));
        if (vm.isSetMemoryPolicy() && vm.getMemoryPolicy().isSetBallooning()) {
            params.setBalloonEnabled(vm.getMemoryPolicy().isBallooning());
        }
        params.setMakeCreatorExplicitOwner(shouldMakeCreatorExplicitOwner());
        params.setStorageDomainId(storageDomainId);
        return performCreate(VdcActionType.AddVmFromScratch,
                               params,
                               new QueryIdResolver<Guid>(VdcQueryType.GetVmByVmId, GetVmByVmIdParameters.class));
    }

    private ArrayList<DiskImage> mapDisks(Disks disks) {
        ArrayList<DiskImage> diskImages = null;
        if (disks!=null && disks.isSetDisks()) {
            diskImages = new ArrayList<DiskImage>();
            for (Disk disk : disks.getDisks()) {
                DiskImage diskImage = (DiskImage)DiskMapper.map(disk, null);
                diskImages.add(diskImage);
            }
        }
        return diskImages;
    }

    protected VM addInlineDetails(Set<Detail> details, VM vm) {
        if (details.contains(Detail.DISKS)) {
            addInlineDisks(vm);
        }
        if (details.contains(Detail.NICS)) {
            addInlineNics(vm);
        }
        if (details.contains(Detail.TAGS)) {
            addInlineTags(vm);
        }
        return vm;
    }

    private void addInlineStatistics(VM vm) {
        EntityIdResolver<Guid> resolver = new QueryIdResolver<Guid>(VdcQueryType.GetVmByVmId, GetVmByVmIdParameters.class);
        VmStatisticalQuery query = new VmStatisticalQuery(resolver, newModel(vm.getId()));
        BackendStatisticsResource<VM, org.ovirt.engine.core.common.businessentities.VM> statisticsResource = inject(new BackendStatisticsResource<VM, org.ovirt.engine.core.common.businessentities.VM>(entityType, Guid.createGuidFromString(vm.getId()), query));
        Statistics statistics = statisticsResource.list();
        vm.setStatistics(statistics);
    }

    private void addInlineTags(VM vm) {
        BackendVmTagsResource tagsResource = inject(new BackendVmTagsResource(vm.getId()));
        Tags tags = tagsResource.list();
        vm.setTags(tags);
    }

    private void addInlineNics(VM vm) {
        BackendVmNicsResource nicsResource = inject(new BackendVmNicsResource(asGuid(vm.getId())));
        Nics nics = nicsResource.list();
        vm.setNics(nics);
    }

    private void addInlineDisks(VM vm) {
        BackendVmDisksResource disksResource = inject(new BackendVmDisksResource(Guid.createGuidFromString(vm.getId()),
                VdcQueryType.GetAllDisksByVmId,
                new GetAllDisksByVmIdParameters(Guid.createGuidFromString(vm.getId()))));
        Disks disks = disksResource.list();
        vm.setDisks(disks);
    }

    @Override
    public Response performRemove(String id) {
        return performAction(VdcActionType.RemoveVm, new RemoveVmParameters(asGuid(id), false));
    }

    @Override
    public Response remove(String id, Action action) {
        getEntity(id);
        boolean forceRemove = action != null && action.isSetForce() ? action.isForce() : false;
        RemoveVmParameters params = new RemoveVmParameters(asGuid(id), forceRemove);
        // If detach only is set we do not remove the VM disks
        if (action.isSetVm() && action.getVm().isSetDisks() && action.getVm().getDisks().isSetDetachOnly()) {
            params.setRemoveDisks(false);
        }
        return performAction(VdcActionType.RemoveVm, params);
    }

    protected VMs mapCollection(List<org.ovirt.engine.core.common.businessentities.VM> entities, boolean isFiltered) {
        VMs collection = new VMs();
        for (org.ovirt.engine.core.common.businessentities.VM entity : entities) {
            VM vm = map(entity);
         // Filtered users are not allowed to view host related information
            if (isFiltered) {
                removeRestrictedInfoFromVM(vm);
            }
            collection.getVMs().add(addLinks(populate(vm, entity)));
        }
        return collection;
    }

    protected void setPayload(VM vm) {
        try {
            VmPayload payload = getEntity(VmPayload.class,
                    VdcQueryType.GetVmPayload,
                    new GetVmByVmIdParameters(new Guid(vm.getId())),
                    null,
                    true);

            if (payload != null) {
                Payload p = getMapper(VmPayload.class, Payload.class).map(payload, null);
                Payloads payloads = new Payloads();
                payloads.getPayload().add(p);
                vm.setPayloads(payloads);
            }
        }
        catch (WebApplicationException ex) {
            if (ex.getResponse().getStatus()==Response.Status.NOT_FOUND.getStatusCode()) {
                //It's legal to not receive a payload for this VM, so the exception is caught and ignored.
                //(TODO: 'getEntity()' should be refactored to make it the programmer's decision,
                //whether to throw an exception or not in case the entity is not found.) Then
                //this try-catch won't be necessary.
            } else{
                throw ex;
            }
        }
    }

    protected boolean templated(VM vm) {
        return vm.isSetTemplate() && (vm.getTemplate().isSetId() || vm.getTemplate().isSetName());
    }

    protected Guid getTemplateId(VM vm) {
        return vm.getTemplate().isSetId() ? asGuid(vm.getTemplate().getId()) : getTemplateByName(vm).getId();
    }

    private VmTemplate getTemplateByName(VM vm) {
        return isFiltered() ? lookupTemplateByName(vm.getTemplate().getName()) : getEntity(
                VmTemplate.class, SearchType.VmTemplate,
                "Template: name=" + vm.getTemplate().getName());
    }

    public VmTemplate lookupTemplateByName(String name) {
        return getEntity(VmTemplate.class, VdcQueryType.GetVmTemplate, new GetVmTemplateParameters(name), "GetVmTemplate");
    }

    public VmTemplate lookupTemplate(Guid id) {
        return getEntity(VmTemplate.class, VdcQueryType.GetVmTemplate, new GetVmTemplateParameters(id), "GetVmTemplate");
    }

    private VDSGroup lookupCluster(Guid id) {
        return getEntity(VDSGroup.class, VdcQueryType.GetVdsGroupByVdsGroupId, new GetVdsGroupByVdsGroupIdParameters(id), "GetVdsGroupByVdsGroupId");
    }

    protected boolean namedCluster(VM vm) {
        return vm.isSetCluster() && vm.getCluster().isSetName() && !vm.getCluster().isSetId();
    }

    protected Guid getClusterId(VM vm) {
        return isFiltered() ? lookupClusterByName(vm.getCluster().getName()).getId() : getEntity(
                VDSGroup.class, SearchType.Cluster,
                "Cluster: name=" + vm.getCluster().getName()).getId();
    }

    public VDSGroup lookupClusterByName(String name) {
        return getEntity(VDSGroup.class, VdcQueryType.GetVdsGroupByName, new GetVdsGroupByNameParameters(name), "GetVdsGroupByName");
    }

    protected void setBallooning(VM vm) {
        Boolean balloonEnabled = getEntity(Boolean.class,
                VdcQueryType.IsBalloonEnabled,
                new GetVmByVmIdParameters(new Guid(vm.getId())),
                null,
                true);
        if (!vm.isSetMemoryPolicy()) {
            vm.setMemoryPolicy(new MemoryPolicy());
        }
        vm.getMemoryPolicy().setBallooning(balloonEnabled);
    }

    public void setCertificateInfo(VM model) {
        VdcQueryReturnValue result =
                runQuery(VdcQueryType.GetVdsCertificateSubjectByVmId,
                        new GetVmByVmIdParameters(asGuid(model.getId())));

        if (result != null && result.getSucceeded() && result.getReturnValue() != null) {
            if (!model.isSetDisplay()) {
                model.setDisplay(new Display());
            }
            model.getDisplay().setCertificate(new Certificate());
            model.getDisplay().getCertificate().setSubject(result.getReturnValue().toString());
        }
    }

    @Override
    protected VM deprecatedPopulate(VM model, org.ovirt.engine.core.common.businessentities.VM entity) {
        Set<Detail> details = DetailHelper.getDetails(getHttpHeaders());
        model = addInlineDetails(details, model);
        if (details.contains(Detail.STATISTICS)) {
            addInlineStatistics(model);
        }
        return model;
    }

    @Override
    protected VM doPopulate(VM model, org.ovirt.engine.core.common.businessentities.VM entity) {
        setPayload(model);
        setBallooning(model);
        setCertificateInfo(model);
        return model;
    }
}
