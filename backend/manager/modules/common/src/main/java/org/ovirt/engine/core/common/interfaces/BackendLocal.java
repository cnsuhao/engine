package org.ovirt.engine.core.common.interfaces;

import org.ovirt.engine.core.common.action.LoginUserParameters;
import org.ovirt.engine.core.common.action.LogoutUserParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public interface BackendLocal {
    VdcReturnValueBase RunAction(VdcActionType actionType, VdcActionParametersBase parameters);

    VDSBrokerFrontend getResourceManager();

    VdcQueryReturnValue RunQuery(VdcQueryType actionType, VdcQueryParametersBase parameters);

    public VdcReturnValueBase EndAction(VdcActionType actionType, VdcActionParametersBase parameters);

    ErrorTranslator getErrorsTranslator();

    ErrorTranslator getVdsErrorsTranslator();

    java.util.ArrayList<VdcReturnValueBase> RunMultipleActions(VdcActionType actionType,
            java.util.ArrayList<VdcActionParametersBase> parameters,
            boolean isRunOnlyIfAllCanDoPass);

    void Initialize();

    VdcQueryReturnValue RunPublicQuery(VdcQueryType actionType, VdcQueryParametersBase parameters);

    VdcReturnValueBase Login(LoginUserParameters parameters);

    VdcReturnValueBase Logoff(LogoutUserParameters parameters);

    // for auto backend
    VdcReturnValueBase RunAutoAction(VdcActionType actionType, VdcActionParametersBase parameters);

    VdcQueryReturnValue RunAutoQuery(VdcQueryType actionType, VdcQueryParametersBase parameters);
}
