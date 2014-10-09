package org.ovirt.engine.core.compat;

import java.math.BigDecimal;

/**
 * @deprecated Use org.apache.commons.lang.math.NumberUtils instead.
 */
@Deprecated
public class DoubleCompat {

    public static BigDecimal tryParse(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            // eat it and return null
            return null;
        }
    }

    public static Double tryParseDouble(String value) {
        try {
            return new Double(value);
        } catch (NumberFormatException e) {
            // eat it and return null
            return null;
        }
    }

}
