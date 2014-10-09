package org.ovirt.engine.core.utils.ovf;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.utils.VmDeviceCommonUtils;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.compat.Formatting;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.backendcompat.Path;
import org.ovirt.engine.core.compat.backendcompat.XmlDocument;
import org.ovirt.engine.core.compat.backendcompat.XmlTextWriter;

public abstract class OvfWriter implements IOvfBuilder {
    protected String _fileName;
    protected int _instanceId;
    protected List<DiskImage> _images;
    protected XmlTextWriter _writer;
    protected XmlDocument _document;
    protected VM _vm;
    protected VmBase vmBase;

    public OvfWriter(XmlDocument document, VmBase vmBase, List<DiskImage> images) {
        _fileName = Path.GetTempFileName();
        _document = document;
        _images = images;
        _writer = new XmlTextWriter(_fileName);
        this.vmBase = vmBase;
        WriteHeader();
    }

    private void WriteHeader() {
        _instanceId = 0;
        _writer.Formatting = Formatting.Indented;
        _writer.Indentation = 4;
        _writer.WriteStartDocument(false);

        _writer.SetPrefix(OVF_PREFIX, OVF_URI);
        _writer.SetPrefix(RASD_PREFIX, RASD_URI);
        _writer.SetPrefix(VSSD_PREFIX, VSSD_URI);
        _writer.SetPrefix(XSI_PREFIX, XSI_URI);

        _writer.WriteStartElement(OVF_URI, "Envelope");
        _writer.WriteNamespace(OVF_PREFIX, OVF_URI);
        _writer.WriteNamespace(RASD_PREFIX, RASD_URI);
        _writer.WriteNamespace(VSSD_PREFIX, VSSD_URI);
        _writer.WriteNamespace(XSI_PREFIX, XSI_URI);

        // Setting the OVF version according to ENGINE (in 2.2 , version was set to "0.9")
        _writer.WriteAttributeString(OVF_URI, "version", Config.<String> GetValue(ConfigValues.VdcVersion));
    }

    private void CloseElements() {
        _writer.WriteEndElement();
    }

    protected long BytesToGigabyte(long bytes) {
        return bytes / 1024 / 1024 / 1024;
    }

    @Override
    public void buildReference() {
        _writer.WriteStartElement("References");
        for (DiskImage image : _images) {
            _writer.WriteStartElement("File");
            _writer.WriteAttributeString(OVF_URI, "href", OvfParser.CreateImageFile(image));
            _writer.WriteAttributeString(OVF_URI, "id", image.getImageId().toString());
            _writer.WriteAttributeString(OVF_URI, "size", String.valueOf(image.getsize()));
            _writer.WriteAttributeString(OVF_URI, "description", StringUtils.defaultString(image.getdescription()));
            _writer.WriteEndElement();

        }
        for (VmNetworkInterface iface : vmBase.getInterfaces()) {
            _writer.WriteStartElement("Nic");
            _writer.WriteAttributeString(OVF_URI, "id", iface.getId().toString());
            _writer.WriteEndElement();
        }
        _writer.WriteEndElement();
    }

    @Override
    public void buildNetwork() {
        _writer.WriteStartElement("Section");
        _writer.WriteAttributeString(XSI_URI, "type", OVF_PREFIX + ":NetworkSection_Type");
        _writer.WriteStartElement("Info");
        _writer.WriteRaw("List of networks");
        _writer.WriteEndElement();
        _writer.WriteStartElement("Network");
        _writer.WriteAttributeString(OVF_URI, "name", "Network 1");
        _writer.WriteEndElement();
        _writer.WriteEndElement();
    }

    @Override
    public void buildDisk() {
        _writer.WriteStartElement("Section");
        _writer.WriteAttributeString(XSI_URI, "type", OVF_PREFIX + ":DiskSection_Type");
        _writer.WriteStartElement("Info");
        _writer.WriteRaw("List of Virtual Disks");
        _writer.WriteEndElement();
        for (DiskImage image : _images) {
            _writer.WriteStartElement("Disk");
            _writer.WriteAttributeString(OVF_URI, "diskId", image.getImageId().toString());
            _writer.WriteAttributeString(OVF_URI, "size", String.valueOf(BytesToGigabyte(image.getsize())));
            _writer.WriteAttributeString(OVF_URI, "actual_size", String.valueOf(BytesToGigabyte(image.getactual_size())));
            _writer.WriteAttributeString(OVF_URI, "vm_snapshot_id", (image.getvm_snapshot_id() != null) ? image
                    .getvm_snapshot_id().getValue().toString() : "");

            if (image.getParentId().equals(Guid.Empty)) {
                _writer.WriteAttributeString(OVF_URI, "parentRef", "");
            } else {
                int i = 0;
                while (_images.get(i).getImageId().equals(image.getParentId()))
                    i++;
                List<DiskImage> res = _images.subList(i, _images.size() - 1);

                if (res.size() > 0) {
                    _writer.WriteAttributeString(OVF_URI, "parentRef", OvfParser.CreateImageFile(res.get(0)));
                } else {
                    _writer.WriteAttributeString(OVF_URI, "parentRef", "");
                }
            }

            _writer.WriteAttributeString(OVF_URI, "fileRef", OvfParser.CreateImageFile(image));

            String format = "";
            switch (image.getvolume_format()) {
            case RAW:
                format = "http://www.vmware.com/specifications/vmdk.html#sparse";
                break;

            case COW:
                format = "http://www.gnome.org/~markmc/qcow-image-format.html";
                break;

            case Unassigned:
                break;
            }
            _writer.WriteAttributeString(OVF_URI, "format", format);
            _writer.WriteAttributeString(OVF_URI, "volume-format", image.getvolume_format().toString());
            _writer.WriteAttributeString(OVF_URI, "volume-type", image.getvolume_type().toString());
            _writer.WriteAttributeString(OVF_URI, "disk-interface", image.getDiskInterface().toString());
            _writer.WriteAttributeString(OVF_URI, "boot", String.valueOf(image.isBoot()));
            if (image.getDiskAlias() != null) {
                _writer.WriteAttributeString(OVF_URI, "disk-alias", image.getDiskAlias());
            }
            if (image.getDiskDescription() != null) {
                _writer.WriteAttributeString(OVF_URI, "disk-description", image.getDiskDescription());
            }
            _writer.WriteAttributeString(OVF_URI, "wipe-after-delete",
                    (new Boolean(image.isWipeAfterDelete())).toString());
            _writer.WriteEndElement();
        }
        _writer.WriteEndElement();
    }

    @Override
    public void buildVirtualSystem() {
        // General Vm
        _writer.WriteStartElement("Content");
        _writer.WriteAttributeString(OVF_URI, "id", "out");
        _writer.WriteAttributeString(XSI_URI, "type", OVF_PREFIX + ":VirtualSystem_Type");

        // General Data
        writeGeneralData();

        // Application List
        WriteAppList();

        // Content Items
        WriteContentItems();

        _writer.WriteEndElement(); // End Content tag
    }

    protected void writeGeneralData() {
        _writer.WriteStartElement("Description");
        _writer.WriteRaw(vmBase.getDescription());
        _writer.WriteEndElement();

        _writer.WriteStartElement("Domain");
        _writer.WriteRaw(vmBase.getDomain());
        _writer.WriteEndElement();

        _writer.WriteStartElement("CreationDate");
        _writer.WriteRaw(OvfParser.LocalDateToUtcDateString(vmBase.getCreationDate()));
        _writer.WriteEndElement();

        _writer.WriteStartElement("ExportDate");
        _writer.WriteRaw(OvfParser.LocalDateToUtcDateString(new Date()));
        _writer.WriteEndElement();

        _writer.WriteStartElement("IsAutoSuspend");
        _writer.WriteRaw(String.valueOf(vmBase.isAutoSuspend()));
        _writer.WriteEndElement();

        _writer.WriteStartElement("DeleteProtected");
        _writer.WriteRaw(String.valueOf(vmBase.isDeleteProtected()));
        _writer.WriteEndElement();

        _writer.WriteStartElement("IsSmartcardEnabled");
        _writer.WriteRaw(String.valueOf(vmBase.isSmartcardEnabled()));
        _writer.WriteEndElement();

        _writer.WriteStartElement("TimeZone");
        _writer.WriteRaw(vmBase.getTimeZone());
        _writer.WriteEndElement();

        _writer.WriteStartElement("default_boot_sequence");
        _writer.WriteRaw(String.valueOf(vmBase.getDefaultBootSequence().getValue()));
        _writer.WriteEndElement();

        if (!StringHelper.isNullOrEmpty(vmBase.getInitrdUrl())) {
            _writer.WriteStartElement("initrd_url");
            _writer.WriteRaw(vmBase.getInitrdUrl());
            _writer.WriteEndElement();
        }
        if (!StringHelper.isNullOrEmpty(vmBase.getKernelUrl())) {
            _writer.WriteStartElement("kernel_url");
            _writer.WriteRaw(vmBase.getKernelUrl());
            _writer.WriteEndElement();
        }
        if (!StringHelper.isNullOrEmpty(vmBase.getKernelParams())) {
            _writer.WriteStartElement("kernel_params");
            _writer.WriteRaw(vmBase.getKernelParams());
            _writer.WriteEndElement();
        }

        _writer.WriteStartElement("Generation");
        _writer.WriteRaw(String.valueOf(vmBase.getDbGeneration()));
        _writer.WriteEndElement();

        _writer.WriteStartElement("VmType");
        _writer.WriteRaw(String.valueOf(vmBase.getVmType().getValue()));
        _writer.WriteEndElement();
    }

    protected abstract void WriteAppList();

    protected abstract void WriteContentItems();

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    protected void writeManagedDeviceInfo(VmBase vmBase, XmlTextWriter writer, Guid deviceId) {
        VmDevice vmDevice = vmBase.getManagedDeviceMap().get(deviceId);
        if (deviceId != null && vmDevice != null && vmDevice.getAddress() != null) {
            writeVmDeviceInfo(vmDevice);
        }
    }

    protected void writeOtherDevices(VmBase vmBase, XmlTextWriter write) {
        List<VmDevice> devices = vmBase.getUnmanagedDeviceList();

        Collection<VmDevice> managedDevices = vmBase.getManagedDeviceMap().values();
        for (VmDevice device : managedDevices) {
            if (VmDeviceCommonUtils.isSpecialDevice(device.getDevice(), device.getType())) {
                devices.add(device);
            }
        }

        for (VmDevice vmDevice : devices) {
            _writer.WriteStartElement("Item");
            _writer.WriteStartElement(RASD_URI, "ResourceType");
            _writer.WriteRaw(OvfHardware.OTHER);
            _writer.WriteEndElement();
            _writer.WriteStartElement(RASD_URI, "InstanceId");
            _writer.WriteRaw(vmDevice.getId().getDeviceId().toString());
            _writer.WriteEndElement();
            writeVmDeviceInfo(vmDevice);
            _writer.WriteEndElement(); // item
        }
    }

    protected void writeMonitors(VmBase vmBase) {
        Collection<VmDevice> devices = vmBase.getManagedDeviceMap().values();
        int numOfMonitors = vmBase.getNumOfMonitors();
        int i = 0;
        for (VmDevice vmDevice : devices) {
            if (vmDevice.getType().equals(VmDeviceType.VIDEO.getName())) {
                _writer.WriteStartElement("Item");
                _writer.WriteStartElement(RASD_URI, "Caption");
                _writer.WriteRaw("Graphical Controller");
                _writer.WriteEndElement();
                _writer.WriteStartElement(RASD_URI, "InstanceId");
                _writer.WriteRaw(vmDevice.getId().getDeviceId().toString());
                _writer.WriteEndElement();
                _writer.WriteStartElement(RASD_URI, "ResourceType");
                _writer.WriteRaw(OvfHardware.Monitor);
                _writer.WriteEndElement();
                _writer.WriteStartElement(RASD_URI, "VirtualQuantity");
                // we should write number of monitors for each entry for backward compatibility
                _writer.WriteRaw(String.valueOf(numOfMonitors));
                _writer.WriteEndElement();
                writeVmDeviceInfo(vmDevice);
                _writer.WriteEndElement(); // item
                if (i++ == numOfMonitors) {
                    break;
                }
            }
        }
    }

    protected void writeCd(VmBase vmBase) {
        Collection<VmDevice> devices = vmBase.getManagedDeviceMap().values();
        for (VmDevice vmDevice : devices) {
            if (vmDevice.getType().equals(VmDeviceType.CDROM.getName())) {
                _writer.WriteStartElement("Item");
                _writer.WriteStartElement(RASD_URI, "Caption");
                _writer.WriteRaw("CDROM");
                _writer.WriteEndElement();
                _writer.WriteStartElement(RASD_URI, "InstanceId");
                _writer.WriteRaw(vmDevice.getId().getDeviceId().toString());
                _writer.WriteEndElement();
                _writer.WriteStartElement(RASD_URI, "ResourceType");
                _writer.WriteRaw(OvfHardware.CD);
                _writer.WriteEndElement();
                writeVmDeviceInfo(vmDevice);
                _writer.WriteEndElement(); // item
                break; // only one CD is currently supported
            }
        }
    }

    public void deleteTmpFile() {
        try {
            File tmpFile = new File(_fileName);
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        } catch (Exception e) {
        }
    }

    public void dispose() {
        if (_writer != null) {
            CloseElements();
            _writer.close();
            _document.Load(_fileName);
        }
        deleteTmpFile();
    }

    private void writeVmDeviceInfo(VmDevice vmDevice) {
        _writer.WriteStartElement(OvfProperties.VMD_TYPE);
        _writer.WriteRaw(String.valueOf(vmDevice.getType()));
        _writer.WriteEndElement();
        _writer.WriteStartElement(OvfProperties.VMD_DEVICE);
        _writer.WriteRaw(String.valueOf(vmDevice.getDevice()));
        _writer.WriteEndElement();
        _writer.WriteStartElement(OvfProperties.VMD_ADDRESS);
        _writer.WriteRaw(vmDevice.getAddress());
        _writer.WriteEndElement();
        _writer.WriteStartElement(OvfProperties.VMD_BOOT_ORDER);
        _writer.WriteRaw(String.valueOf(vmDevice.getBootOrder()));
        _writer.WriteEndElement();
        _writer.WriteStartElement(OvfProperties.VMD_IS_PLUGGED);
        _writer.WriteRaw(String.valueOf(vmDevice.getIsPlugged()));
        _writer.WriteEndElement();
        _writer.WriteStartElement(OvfProperties.VMD_IS_READONLY);
        _writer.WriteRaw(String.valueOf(vmDevice.getIsReadOnly()));
        _writer.WriteEndElement();
        _writer.WriteStartElement(OvfProperties.VMD_ALIAS);
        _writer.WriteRaw(String.valueOf(vmDevice.getAlias()));
        _writer.WriteEndElement();
        if (vmDevice.getSpecParams() != null && vmDevice.getSpecParams().size() != 0) {
            _writer.WriteStartElement(OvfProperties.VMD_SPEC_PARAMS);
            _writer.WriteMap(vmDevice.getSpecParams());
            _writer.WriteEndElement();
        }
    }
}
