package org.ovirt.engine.core.common.queries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;
import org.ovirt.engine.core.common.interfaces.SearchType;

/** A test case for the {@link AdUsersSearchParameters} class */
public class AdUsersSearchParametersTest {

    @Test
    public void testOneArgConstructor() {
        String pattern = "pattern";
        AdUsersSearchParameters params = new AdUsersSearchParameters(pattern);

        assertTrue("Wrong pattern", params.getSearchPattern().endsWith(pattern));
        assertEquals("Wrong type", SearchType.AdUser, params.getSearchTypeValue());
    }

    @Test
    public void testTwoArgConstructor() {
        String pattern = "pattern";
        boolean caseSensitive = new Random().nextBoolean();
        AdUsersSearchParameters params = new AdUsersSearchParameters(pattern, caseSensitive);

        assertTrue("Wrong pattern", params.getSearchPattern().endsWith(pattern));
        assertEquals("Wrong type", SearchType.AdUser, params.getSearchTypeValue());
        assertEquals("Wrong case sensitivity", caseSensitive, params.getCaseSensitive());
    }
}
