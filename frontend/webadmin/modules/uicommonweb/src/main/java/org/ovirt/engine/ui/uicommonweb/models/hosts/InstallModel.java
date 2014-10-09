package org.ovirt.engine.ui.uicommonweb.models.hosts;

import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;

@SuppressWarnings("unused")
public class InstallModel extends Model {

    private EntityModel privateRootPassword;

    public EntityModel getRootPassword() {
        return privateRootPassword;
    }

    private void setRootPassword(EntityModel value) {
        privateRootPassword = value;
    }

    private ListModel privateOVirtISO;

    public ListModel getOVirtISO() {
        return privateOVirtISO;
    }

    private void setOVirtISO(ListModel value) {
        privateOVirtISO = value;
    }

    private EntityModel privateOverrideIpTables;

    public EntityModel getOverrideIpTables() {
        return privateOverrideIpTables;
    }

    private void setOverrideIpTables(EntityModel value) {
        privateOverrideIpTables = value;
    }

    private EntityModel hostVersion;

    public EntityModel getHostVersion() {
        return hostVersion;
    }

    public void setHostVersion(EntityModel value) {
        hostVersion = value;
    }

    public InstallModel() {
        setRootPassword(new EntityModel());
        setOVirtISO(new ListModel());
        setHostVersion(new EntityModel());

        setOverrideIpTables(new EntityModel());
        getOverrideIpTables().setEntity(false);
    }

    public boolean Validate(boolean isOVirt) {
        getOVirtISO().setIsValid(true);
        getRootPassword().setIsValid(true);

        if (isOVirt) {
            getOVirtISO().ValidateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        } else {
            getRootPassword().ValidateEntity(new IValidation[] { new NotEmptyValidation() });
        }

        return getRootPassword().getIsValid() && getOVirtISO().getIsValid();
    }
}
