package org.ovirt.engine.core.searchbackend;

import java.util.Collections;

import org.ovirt.engine.core.compat.StringHelper;

public class AdUserConditionFieldAutoCompleter extends BaseConditionFieldAutoCompleter {
    public AdUserConditionFieldAutoCompleter() {
        // Building the basic vervs Dict
        mVerbs.put("NAME", "NAME");
        mVerbs.put("USERNAME", "USERNAME");
        if (StringHelper.EqOp(SyntaxCheckerFactory.getConfigAuthenticationMethod().toUpperCase(), "LDAP")) {
            mVerbs.put("LASTNAME", "LASTNAME");
            mVerbs.put("DEPARTMENT", "DEPARTMENT");
            mVerbs.put("TITLE", "TITLE");
        }
        mVerbs.put("ALLNAMES", "ALLNAMES");

        // Building the autoCompletion Dict
        buildCompletions();
        // Building the types dict
        getTypeDictionary().put("NAME", String.class);
        getTypeDictionary().put("USERNAME", String.class);
        if (StringHelper.EqOp(SyntaxCheckerFactory.getConfigAuthenticationMethod().toUpperCase(), "LDAP")) {
            getTypeDictionary().put("LASTNAME", String.class);
            getTypeDictionary().put("DEPARTMENT", String.class);
            getTypeDictionary().put("TITLE", String.class);
        }
        // building the ColumnName Dict
        mColumnNameDict.put("NAME", "$GIVENNAME");
        mColumnNameDict.put("USERNAME", "$SAMACCOUNTNAME");
        if (StringHelper.EqOp(SyntaxCheckerFactory.getConfigAuthenticationMethod().toUpperCase(), "LDAP")) {
            mColumnNameDict.put("LASTNAME", "$SN");
            mColumnNameDict.put("DEPARTMENT", "$DEPARTMENT");
            mColumnNameDict.put("TITLE", "$TITLE");
        }
        // Building the validation dict
        for (String key : mVerbs.keySet()) {
            mValidationDict.put(key, Collections.singletonList(validCharacters));
        }
    }

    @Override
    public IAutoCompleter getFieldRelationshipAutoCompleter(String fieldName) {
        return StringConditionRelationAutoCompleter.INSTANCE;
    }
}
