package org.ovirt.engine.ui.uicommonweb.models.storage;

import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;

@SuppressWarnings("unused")
public class PosixStorageModel extends Model implements IStorageModel {

    private UICommand updateCommand;

    @Override
    public UICommand getUpdateCommand() {
        return updateCommand;
    }

    private void setUpdateCommand(UICommand value) {
        updateCommand = value;
    }

    private StorageModel container;

    @Override
    public StorageModel getContainer() {
        return container;
    }

    @Override
    public void setContainer(StorageModel value) {
        container = value;
    }

    private StorageDomainType privateRole = StorageDomainType.values()[0];

    @Override
    public StorageDomainType getRole() {
        return privateRole;
    }

    @Override
    public void setRole(StorageDomainType value) {
        privateRole = value;
    }

    private EntityModel path;

    public EntityModel getPath() {
        return path;
    }

    private void setPath(EntityModel value) {
        path = value;
    }

    private EntityModel vfsType;

    public EntityModel getVfsType() {
        return vfsType;
    }

    private void setVfsType(EntityModel value) {
        vfsType = value;
    }

    private EntityModel mountOptions;

    public EntityModel getMountOptions() {
        return mountOptions;
    }

    private void setMountOptions(EntityModel value) {
        mountOptions = value;
    }


    public PosixStorageModel() {

        setUpdateCommand(new UICommand("Update", this)); //$NON-NLS-1$

        setPath(new EntityModel());
        setVfsType(new EntityModel());
        setMountOptions(new EntityModel());
    }

    @Override
    public boolean Validate() {

        getPath().ValidateEntity(
            new IValidation[] {
                new NotEmptyValidation(),
            }
        );

        getVfsType().ValidateEntity(
            new IValidation[] {
                new NotEmptyValidation(),
            }
        );


        return getPath().getIsValid()
            && getVfsType().getIsValid();
    }

    @Override
    public StorageType getType() {
        return StorageType.POSIXFS;
    }
}
