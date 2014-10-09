package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.businessentities.vds_spm_id_map;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.errors.VdcFault;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.linq.Function;
import org.ovirt.engine.core.utils.linq.LinqUtils;

@InternalCommandAttribute
@LockIdNameAttribute(isWait = true)
public class AddVdsSpmIdCommand<T extends VdsActionParameters> extends VdsCommand<T> {
    private List<vds_spm_id_map> vds_spm_id_mapList;

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    public AddVdsSpmIdCommand(Guid commandId) {
        super(commandId);
    }

    public AddVdsSpmIdCommand(T parametars) {
        super(parametars);
    }

    @Override
    protected boolean canDoAction() {
        // check if vds already has vds spm id and storage pool exists
        if (Guid.Empty.equals(getVds().getStoragePoolId())) {
            return false;
        }
        vds_spm_id_mapList = DbFacade.getInstance().getVdsSpmIdMapDao().getAll(
                getVds().getStoragePoolId());
        if (vds_spm_id_mapList.size() >= Config.<Integer> GetValue(ConfigValues.MaxNumberOfHostsInStoragePool)) {
            VdcFault fault = new VdcFault();
            fault.setError(VdcBllErrors.ReachedMaxNumberOfHostsInDC);
            fault.setMessage(Backend.getInstance()
                    .getVdsErrorsTranslator()
                    .TranslateErrorTextSingle(fault.getError().toString()));
            getReturnValue().setFault(fault);
            return false;
        }
        return true;
    }

    @Override
    protected void executeCommand() {
        // according to shaharf the first id is 1
        int selectedId = 1;
        List<Integer> list = LinqUtils.foreach(vds_spm_id_mapList, new Function<vds_spm_id_map, Integer>() {
            @Override
            public Integer eval(vds_spm_id_map vds_spm_id_map) {
                return vds_spm_id_map.getvds_spm_id();
            }
        });
        Collections.sort(list);
        for (int id : list) {
            if (selectedId == id) {
                selectedId++;
            } else {
                break;
            }
        }
        vds_spm_id_map newMap = new vds_spm_id_map(getVds().getStoragePoolId(), getVdsId(), selectedId);
        DbFacade.getInstance().getVdsSpmIdMapDao().save(newMap);
        if (getParameters().isCompensationEnabled()) {
            getCompensationContext().snapshotNewEntity(newMap);
            getCompensationContext().stateChanged();
        }

        setSucceeded(true);
    }

    @Override
    protected Map<String, String> getExclusiveLocks() {
        return Collections.singletonMap(getVds().getStoragePoolId().toString(), LockingGroup.REGISTER_VDS.name());
    }
}
