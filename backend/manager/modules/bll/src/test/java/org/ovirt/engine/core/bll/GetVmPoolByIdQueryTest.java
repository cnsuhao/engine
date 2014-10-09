package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.vm_pools;
import org.ovirt.engine.core.common.queries.GetVmPoolByIdParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VmPoolDAO;

/**
 * A test case for {@link GetVmPoolByIdQuery}.
 * It does not test database implementation, but rather tests that the right delegations to the DAO occur.
 */
public class GetVmPoolByIdQueryTest extends AbstractUserQueryTest<GetVmPoolByIdParameters, GetVmPoolByIdQuery<GetVmPoolByIdParameters>> {
    @Test
    public void testExecuteQuery() {
        Guid vmPoolID = Guid.NewGuid();
        vm_pools expectedResult = new vm_pools();
        expectedResult.setvm_pool_id(vmPoolID);

        GetVmPoolByIdParameters paramsMock = getQueryParameters();
        when(paramsMock.getPoolId()).thenReturn(vmPoolID);

        VmPoolDAO vmPoolDAOMock = mock(VmPoolDAO.class);
        when(vmPoolDAOMock.get(vmPoolID, getUser().getUserId(), paramsMock.isFiltered())).thenReturn(expectedResult);

        DbFacade dbFacadeMock = getDbFacadeMockInstance();
        when(dbFacadeMock.getVmPoolDao()).thenReturn(vmPoolDAOMock);

        getQuery().executeQueryCommand();

        vm_pools result = (vm_pools) getQuery().getQueryReturnValue().getReturnValue();

        assertEquals("Wrong VM pool returned", expectedResult, result);
    }
}
