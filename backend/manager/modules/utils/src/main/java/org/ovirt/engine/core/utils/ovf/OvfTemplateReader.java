package org.ovirt.engine.core.utils.ovf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VmOsType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.backendcompat.XmlDocument;
import org.ovirt.engine.core.compat.backendcompat.XmlNode;
import org.ovirt.engine.core.compat.backendcompat.XmlNodeList;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;

public class OvfTemplateReader extends OvfReader {
    protected VmTemplate _vmTemplate;

    public OvfTemplateReader(XmlDocument document,
            VmTemplate vmTemplate,
            ArrayList<DiskImage> images,
            ArrayList<VmNetworkInterface> interfaces) {
        super(document, images, interfaces, vmTemplate);
        _vmTemplate = vmTemplate;
    }

    @Override
    protected void readOsSection(XmlNode section) {
        _vmTemplate.setId(new Guid(section.Attributes.get("ovf:id").getValue()));
        XmlNode node = section.SelectSingleNode("Description");
        if (node != null) {
            _vmTemplate.setOs(VmOsType.valueOf(node.InnerText));
        } else {
            _vmTemplate.setOs(VmOsType.Unassigned);
        }
    }

    @Override
    protected void readHardwareSection(XmlNode section) {
        XmlNodeList list = section.SelectNodes("Item");
        for (XmlNode node : list) {
            int resourceType = Integer.parseInt(node.SelectSingleNode("rasd:ResourceType", _xmlNS).InnerText);

            switch (resourceType) {
            // CPU
            case 3:
                _vmTemplate
                        .setNumOfSockets(Integer.parseInt(node.SelectSingleNode("rasd:num_of_sockets", _xmlNS).InnerText));
                _vmTemplate
                        .setCpuPerSocket(Integer.parseInt(node.SelectSingleNode("rasd:cpu_per_socket", _xmlNS).InnerText));
                break;

            // Memory
            case 4:
                _vmTemplate
                        .setMemSizeMb(Integer.parseInt(node.SelectSingleNode("rasd:VirtualQuantity", _xmlNS).InnerText));
                break;

            // Image
            case 17:
                final Guid guid = new Guid(node.SelectSingleNode("rasd:InstanceId", _xmlNS).InnerText);

                DiskImage image = LinqUtils.firstOrNull(_images, new Predicate<DiskImage>() {
                    @Override
                    public boolean eval(DiskImage diskImage) {
                        return diskImage.getImageId().equals(guid);
                    }
                });
                image.setId(OvfParser.GetImageGrupIdFromImageFile(node.SelectSingleNode(
                        "rasd:HostResource", _xmlNS).InnerText));
                if (!StringHelper.isNullOrEmpty(node.SelectSingleNode("rasd:Parent", _xmlNS).InnerText)) {
                    image.setParentId(new Guid(node.SelectSingleNode("rasd:Parent", _xmlNS).InnerText));
                }
                if (!StringHelper.isNullOrEmpty(node.SelectSingleNode("rasd:Template", _xmlNS).InnerText)) {
                    image.setit_guid(new Guid(node.SelectSingleNode("rasd:Template", _xmlNS).InnerText));
                }
                image.setappList(node.SelectSingleNode("rasd:ApplicationList", _xmlNS).InnerText);
                if (!StringHelper.isNullOrEmpty(node.SelectSingleNode("rasd:StorageId", _xmlNS).InnerText)) {
                    image.setstorage_ids(new ArrayList<Guid>(Arrays.asList(new Guid(node.SelectSingleNode("rasd:StorageId",
                            _xmlNS).InnerText))));
                }
                if (!StringHelper.isNullOrEmpty(node.SelectSingleNode("rasd:StoragePoolId", _xmlNS).InnerText)) {
                    image.setstorage_pool_id(new Guid(node.SelectSingleNode("rasd:StoragePoolId", _xmlNS).InnerText));
                }
                final Date creationDate = OvfParser.UtcDateStringToLocaDate(
                        node.SelectSingleNode("rasd:CreationDate", _xmlNS).InnerText);
                if (creationDate != null) {
                    image.setcreation_date(creationDate);
                }
                final Date lastModified = OvfParser.UtcDateStringToLocaDate(
                        node.SelectSingleNode("rasd:LastModified", _xmlNS).InnerText);
                if (lastModified != null) {
                    image.setlastModified(lastModified);
                }
                readVmDevice(node, _vmTemplate, image.getId(), Boolean.TRUE);
                break;

            // Network
            case 10:
                VmNetworkInterface iface = getNetwotkInterface(node);
                if (!StringHelper.isNullOrEmpty(node.SelectSingleNode("rasd:ResourceSubType", _xmlNS).InnerText)) {
                    iface.setType(Integer.parseInt(node.SelectSingleNode("rasd:ResourceSubType", _xmlNS).InnerText));
                }

                String resourceSubNetworkName = node.SelectSingleNode(OvfProperties.VMD_CONNECTION, _xmlNS).InnerText;
                iface.setNetworkName(StringUtils.defaultIfEmpty(resourceSubNetworkName, null));

                XmlNode linkedNode = node.SelectSingleNode(OvfProperties.VMD_LINKED, _xmlNS);
                iface.setLinked(linkedNode == null ? true : Boolean.valueOf(linkedNode.InnerText));
                iface.setName(node.SelectSingleNode("rasd:Name", _xmlNS).InnerText);
                iface.setSpeed((node.SelectSingleNode("rasd:speed", _xmlNS) != null) ? Integer
                        .parseInt(node.SelectSingleNode("rasd:speed", _xmlNS).InnerText)
                        : VmInterfaceType.forValue(iface.getType()).getSpeed());
                _vmTemplate.getInterfaces().add(iface);
                readVmDevice(node, _vmTemplate, iface.getId(), Boolean.TRUE);
                break;
            // CDROM
            case 15:
                readVmDevice(node, _vmTemplate, Guid.NewGuid(), Boolean.TRUE);
                break;
            // USB
            case 23:
                _vmTemplate.setUsbPolicy(UsbPolicy.forStringValue(node.SelectSingleNode("rasd:UsbPolicy", _xmlNS).InnerText));
                break;

            // Monitor
            case 20:
                _vmTemplate
                        .setNumOfMonitors(Integer.parseInt(node.SelectSingleNode("rasd:VirtualQuantity", _xmlNS).InnerText));
                readVmDevice(node, _vmTemplate, Guid.NewGuid(), Boolean.TRUE);
                break;
            // OTHER
            case 0:
                readVmDevice(node, _vmTemplate, Guid.NewGuid(), Boolean.FALSE);
                break;

            }
        }
    }

    @Override
    protected void readGeneralData(XmlNode content) {
        // General Vm
        XmlNode node = content.SelectSingleNode("Name");
        if (node != null) {
            _vmTemplate.setname(node.InnerText);
            name = _vmTemplate.getname();
        }
        node = content.SelectSingleNode("TemplateId");
        if (node != null) {
            if (!StringHelper.isNullOrEmpty(node.InnerText)) {
                _vmTemplate.setId(new Guid(node.InnerText));
            }
        }

        node = content.SelectSingleNode("IsDisabled");
        if (node != null) {
            _vmTemplate.setDisabled(Boolean.parseBoolean(node.InnerText));
        }
    }

    @Override
    protected String getDefaultDisplayTypeStringRepresentation() {
        return "default_display_type";
    }
}
