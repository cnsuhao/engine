package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.permissions;
import org.ovirt.engine.core.common.queries.MultilevelAdministrationByAdElementIdParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.PermissionDAO;

/**
 * A test case for {@link GetPermissionsByAdElementIdQuery}.
 * This test mocks away all the DAOs, and just tests the flow of the query itself.
 */
public class GetPermissionsByAdElementIdQueryTest extends AbstractUserQueryTest<MultilevelAdministrationByAdElementIdParameters, GetPermissionsByAdElementIdQuery<MultilevelAdministrationByAdElementIdParameters>> {

    @Test
    public void testQueryExecution() {
        // Prepare the query parameters
        Guid adElementGuid = Guid.NewGuid();
        when(getQueryParameters().getAdElementId()).thenReturn(adElementGuid);

        // Create expected result
        permissions expected = new permissions();
        expected.setad_element_id(adElementGuid);

        // Mock the DAOs
        PermissionDAO permissionDAOMock = mock(PermissionDAO.class);
        when(permissionDAOMock.getAllForAdElement
                (adElementGuid, getUser().getUserId(), getQueryParameters().isFiltered())).
                thenReturn(Collections.singletonList(expected));
        when(getDbFacadeMockInstance().getPermissionDao()).thenReturn(permissionDAOMock);

        getQuery().executeQueryCommand();

        // Assert the query's results
        @SuppressWarnings("unchecked")
        List<permissions> actual = (List<permissions>) getQuery().getQueryReturnValue().getReturnValue();

        assertEquals("Wrong number of returned permissions", 1, actual.size());
        assertEquals("Wrong returned permissions", expected, actual.get(0));
    }
}
