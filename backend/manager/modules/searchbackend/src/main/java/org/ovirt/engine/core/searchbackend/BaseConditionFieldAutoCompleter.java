package org.ovirt.engine.core.searchbackend;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.DateEnumForSearch;
import org.ovirt.engine.core.common.businessentities.tags;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.ITagsHandler;
import org.ovirt.engine.core.compat.DateFormatCompat;
import org.ovirt.engine.core.compat.DateTime;
import org.ovirt.engine.core.compat.DayOfWeek;
import org.ovirt.engine.core.compat.DoubleCompat;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.IntegerCompat;
import org.ovirt.engine.core.compat.RefObject;
import org.ovirt.engine.core.compat.Regex;
import org.ovirt.engine.core.compat.StringFormat;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.TimeSpan;

/**
 * A base class for all condition field auto completers
 */
public class BaseConditionFieldAutoCompleter extends BaseAutoCompleter implements IConditionFieldAutoCompleter {

    public static final int DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

    protected final Map<String, List<ValueValidationFunction>> mValidationDict =
            new HashMap<String, List<ValueValidationFunction>>();
    private final Map<String, Class<?>> mTypeDict = new HashMap<String, Class<?>>();
    protected final Map<String, String> mColumnNameDict = new HashMap<String, String>();
    protected final List<String> mNotFreeTextSearchableFieldsList = new ArrayList<String>();

    /**
     * Gets the LIKE clause syntax for non case-sensitive search
     * @return the LIKE syntax according to current DBEngine.
     */
    public static String getLikeSyntax(boolean caseSensitive) {
        // for tests we don't have the Config class initialized
        // also if caseSensitive flag is set we will use LIKE
        if (Config.getConfigUtils() == null || caseSensitive)
            return "LIKE";
        else
            return Config.<String> GetValue(ConfigValues.DBLikeSyntax);
    }

    /**
     * Gets the I18N prefix used for value compare.
     * @return
     */
    public static String getI18NPrefix() {
        // for tests we don't have the Config class initialized
        if (Config.getConfigUtils() == null)
            return "";
        else
            return Config.<String> GetValue(ConfigValues.DBI18NPrefix);

    }

    public static ITagsHandler TagsHandler;

    public Map<String, Class<?>> getTypeDictionary() {
        return mTypeDict;
    }

    protected void buildBasicValidationTable() {
        for (String key : mVerbs.keySet()) {
            final List<ValueValidationFunction> curList = new ArrayList<ValueValidationFunction>();
            final Class<?> curType = mTypeDict.get(key);
            if (curType == java.math.BigDecimal.class) {
                curList.add(validDecimal);
            } else if (curType == Integer.class) {
                curList.add(validInteger);
            } else if (curType == Date.class) {
                curList.add(validDateTime);
            } else if (curType == TimeSpan.class) {
                curList.add(validTimeSpan);
            } else {
                curList.add(validCharacters);
            }
            final IConditionValueAutoCompleter tmp = getFieldValueAutoCompleter(key);
            if (tmp != null) {
                if (tmp.getClass() == DateEnumValueAutoCompleter.class) {
                    curList.add(validateDateEnumValueByValueAC);
                } else {
                    curList.add(validateFieldValueByValueAC);
                }
            }
            mValidationDict.put(key, curList);
        }
    }

    public boolean validateFieldValue(String fieldName, String fieldValue) {
        if (mValidationDict.containsKey(fieldName)) {
            final List<ValueValidationFunction> validationList = mValidationDict.get(fieldName);
            for (ValueValidationFunction curValidationFunc : validationList) {
                if (!curValidationFunc.isValid(fieldName, fieldValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getDbFieldName(String fieldName) {
        String retval = null;
        if (mColumnNameDict.containsKey(fieldName)) {
            retval = mColumnNameDict.get(fieldName);
        }
        return retval;
    }

    public Class<?> getDbFieldType(String fieldName) {
        Class<?> retval = null;
        if (mTypeDict.containsKey(fieldName)) {
            retval = mTypeDict.get(fieldName);
        }
        return retval;

    }

    // FIXME Probably Not Hibernate Friendly
    public final String buildFreeTextConditionSql(String tableName, String relations, String value, boolean caseSensitive) {
        StringBuilder sb = new StringBuilder(" ( ");
        boolean firstTime = true;
        if (!StringHelper.isNullOrEmpty(value) && !"''".equals(value)) {
            value = StringFormat.format(getI18NPrefix() + "'%%%1$s%%'", StringHelper.trim(value, '\''));

        }
        if ("=".equals(relations)) {
            relations = getLikeSyntax(caseSensitive);
        } else if ("!=".equals(relations)) {
            relations = "NOT " + getLikeSyntax(caseSensitive);
        }
        for (String field : mColumnNameDict.keySet()) {
            if (mTypeDict.get(field) == String.class && !mNotFreeTextSearchableFieldsList.contains(field)) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    sb.append(" OR ");
                }
                sb.append(StringFormat.format(" %1$s.%2$s %3$s %4$s",
                        tableName,
                        mColumnNameDict.get(field),
                        relations,
                        value));
            }
        }
        sb.append(" ) ");
        return sb.toString();
    }

    final static Regex validChar = new Regex("^[^\\<\\>&^#!']*$");

    public final static ValueValidationFunction validCharacters = new ValueValidationFunction() {
        public boolean isValid(String field, String value) {
            return validChar.IsMatch(value);
        }
    };

    public final static ValueValidationFunction validDateTime = new ValueValidationFunction() {
        public boolean isValid(String field, String value) {
            Date test = DateUtils.parse(value);
            if (test != null) {
                return true;
            } else { // check for enum
                for (DateEnumForSearch val : DateEnumForSearch.values()) {
                    if (StringHelper.EqOp(value.toUpperCase(), val.name().toUpperCase())) {
                        return true;
                    }
                }
                // check for week before
                for (DayOfWeek day : DayOfWeek.class.getEnumConstants()) {
                    if (StringHelper.EqOp(value.toUpperCase(), day.toString().toUpperCase())) {
                        return true;
                    }
                }
            }
            return false;
        }
    };

    public final static ValueValidationFunction validTimeSpan = new ValueValidationFunction() {
        public boolean isValid(String field, String value) {
            return TimeSpan.tryParse(value) != null;
        }
    };

    public final static ValueValidationFunction validInteger = new ValueValidationFunction() {
        public boolean isValid(String field, String value) {
            return IntegerCompat.tryParse(value) != null;
        }
    };

    public final static ValueValidationFunction validDecimal = new ValueValidationFunction() {
        public boolean isValid(String field, String value) {
            return DoubleCompat.tryParse(value) != null;
        }
    };

    public final ValueValidationFunction validateDateEnumValueByValueAC = new ValueValidationFunction() {
        public boolean isValid(String field, String value) {
            boolean retval = true;
            IConditionValueAutoCompleter vlaueAc = getFieldValueAutoCompleter(field);
            if (vlaueAc != null) // check if this enum first
            {
                retval = vlaueAc.validate(value);
            }
            if (!retval) // check for week before
            {
                for (DayOfWeek day : DayOfWeek.values()) {
                    if (StringHelper.EqOp(value.toUpperCase(), day.toString().toUpperCase())) // Enum.GetName(typeof(DayOfWeek),
                                                                                              // day).ToUpper()
                    {
                        return true;
                    }
                }
            }
            if (!retval) // check for free date
            {
                retval = DateUtils.parse(StringHelper.trim(value, '\'')) != null;
            }

            return retval;
        }
    };

    public final ValueValidationFunction validateFieldValueByValueAC = new ValueValidationFunction() {
        public boolean isValid(String field, String value) {
            boolean retval = true;
            IConditionValueAutoCompleter vlaueAc = getFieldValueAutoCompleter(field);
            if (vlaueAc != null) {
                retval = vlaueAc.validate(value);
            }
            return retval;
        }
    };

    public IAutoCompleter getFieldRelationshipAutoCompleter(String fieldName) {
        return null;
    }

    public IConditionValueAutoCompleter getFieldValueAutoCompleter(String fieldName) {
        return null;
    }

    public void formatValue(String fieldName,
            RefObject<String> relations,
            RefObject<String> value,
            boolean caseSensitive) {
        if (fieldName == null) {
            return;
        }

        if ("TIME".equals(fieldName) || "CREATIONDATE".equals(fieldName)) {
            Date temp = DateUtils.parse(StringHelper.trim(value.argvalue, '\''));

            DateTime result;
            if (temp == null) {
                result = DealWithDateEnum(value.argvalue);
            } else {
                result = new DateTime(temp);
            }

            if (relations.argvalue != null && relations.argvalue.equals("=")) {
                relations.argvalue = "between";
                DateTime nextDay = result.AddDays(1);
                value.argvalue = StringFormat.format("'%1$s' and '%2$s'",
                        result.toString(DateUtils.getFormat(DateFormatCompat.DEFAULT, DateFormatCompat.SHORT)),
                        nextDay.toString(DateUtils.getFormat(DateFormatCompat.DEFAULT, DateFormatCompat.SHORT)));
            } else { // ">" or "<"
                     // value.argvalue = String.format("'%1$s'", result);
                value.argvalue = StringFormat.format("'%1$s'",
                        result.toString(DateUtils.getFormat(DateFormatCompat.DEFAULT, DateFormatCompat.SHORT)));
            }

        }
        else if ("TAG".equals(fieldName)) {
            value.argvalue = value.argvalue.startsWith("N'") ? value.argvalue.substring(2) : value.argvalue;
            if (relations.argvalue != null && relations.argvalue.equals("=")) {
                relations.argvalue = "IN";
                value.argvalue = StringHelper.trim(value.argvalue, '\'');
                tags tag = TagsHandler.GetTagByTagName(value.argvalue);
                if (tag != null) {
                    value.argvalue =
                            StringFormat.format("(%1$s)", TagsHandler.GetTagNameAndChildrenNames(tag.gettag_id()));
                } else {
                    value.argvalue = StringFormat.format("('%1$s')", Guid.Empty);
                }
            } else if (relations.argvalue != null && relations.argvalue.equals("LIKE")) {
                relations.argvalue = "IN";
                value.argvalue = StringHelper.trim(value.argvalue, '\'').replace("%", "*");

                String IDs = TagsHandler.GetTagNamesAndChildrenNamesByRegExp(value.argvalue);
                if (StringHelper.isNullOrEmpty(IDs)) {
                    value.argvalue = StringFormat.format("('%1$s')", Guid.Empty);
                } else {
                    value.argvalue = StringFormat.format("(%1$s)", IDs);
                }
            }
        }
    }
    // private static final String DATE_FORMAT = "MMM dd,yyyy";
    private static DateTime DealWithDateEnum(String value) {
        DateTime formatedValue = new DateTime();
        final Integer result = IntegerCompat.tryParse(StringHelper.trim(value, '\''));
        if (result != null) {
            DateEnumForSearch dateEnumVal = DateEnumForSearch.forValue(result);
            switch (dateEnumVal) {
            case Today:
                formatedValue = DateTime.getNow();
                break;
            case Yesterday:
                formatedValue = DateTime.getNow().AddDays(-1);
                break;

            default:
                break;
            }
        } else {
            for (int i = -2; i > -8; i--) {
                if (StringHelper.EqOp(DateUtils.getDayOfWeek(i), StringHelper.trim(value, '\'').toUpperCase())) {
                    formatedValue = DateTime.getNow();
                    return formatedValue.resetToMidnight();
                }
            }
        }
        return formatedValue.resetToMidnight();
    }

    public final String buildConditionSql(String fieldName, String customizedValue, String customizedRelation,
            String tableName, boolean caseSensitive) {
        RefObject<String> tempRefObject = new RefObject<String>(customizedRelation);
        RefObject<String> tempRefObject2 = new RefObject<String>(customizedValue);
        formatValue(fieldName, tempRefObject, tempRefObject2, caseSensitive);
        customizedRelation = tempRefObject.argvalue;
        customizedValue = tempRefObject2.argvalue;
        if (("''".equals(customizedValue) || "'null'".equalsIgnoreCase(customizedValue))
                && (("=".equals(customizedRelation)) || ("!=".equals(customizedRelation)))) {
            String nullRelation = ("=".equals(customizedRelation)) ? "IS" : "IS NOT";
            return StringFormat.format("(%1$s.%2$s %3$s  NULL)", tableName,
                    getDbFieldName(fieldName), nullRelation);
        } else {
            return StringFormat.format(" %1$s.%2$s %3$s %4$s ", tableName, getDbFieldName(fieldName),
                    customizedRelation, customizedValue);
        }
    }
}
