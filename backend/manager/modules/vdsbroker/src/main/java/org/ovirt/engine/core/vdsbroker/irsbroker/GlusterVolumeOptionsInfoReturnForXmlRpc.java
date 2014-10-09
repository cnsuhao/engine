package org.ovirt.engine.core.vdsbroker.irsbroker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionInfo;

public final class GlusterVolumeOptionsInfoReturnForXmlRpc extends StatusReturnForXmlRpc {
    private static final String VOLUME_OPTIONS_DEFAULT = "volumeSetOptions";
    // We are ignoring missing fields after the status, because on failure it is
    // not sent.
    // [XmlRpcMissingMapping(MappingAction.Ignore), XmlRpcMember("volumeOptionsDefaults")]
    public Set<GlusterVolumeOptionInfo> optionsHelpSet = new HashSet<GlusterVolumeOptionInfo>();

    @SuppressWarnings("unchecked")
    public GlusterVolumeOptionsInfoReturnForXmlRpc(Map<String, Object> innerMap) {
        super(innerMap);
        Object[] temp = (Object[]) innerMap.get(VOLUME_OPTIONS_DEFAULT);
        if (temp != null) {
            for (int i = 0; i < temp.length; i++) {
                optionsHelpSet.add(prepareOptionHelpEntity((Map<String, Object>) temp[i]));
            }
        }
    }

    private GlusterVolumeOptionInfo prepareOptionHelpEntity(Map<String, Object> map) {
        GlusterVolumeOptionInfo entity = new GlusterVolumeOptionInfo();
        entity.setKey(map.get("name").toString());
        entity.setDefaultValue(map.get("defaultValue").toString());
        entity.setDescription(map.get("description").toString());
        return entity;
    }
}
