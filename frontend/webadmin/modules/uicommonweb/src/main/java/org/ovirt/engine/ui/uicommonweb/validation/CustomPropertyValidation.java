package org.ovirt.engine.ui.uicommonweb.validation;

import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class CustomPropertyValidation implements IValidation
{
    private ArrayList<String> privateCustomPropertiesKeysList;

    private ArrayList<String> getCustomPropertiesKeysList()
    {
        return privateCustomPropertiesKeysList;
    }

    private void setCustomPropertiesKeysList(ArrayList<String> value)
    {
        privateCustomPropertiesKeysList = value;
    }

    public CustomPropertyValidation(ArrayList<String> customPropertiesKeysList)
    {
        setCustomPropertiesKeysList(new ArrayList<String>());

        if (customPropertiesKeysList != null && customPropertiesKeysList.size() > 0)
        {
            for (String customPropertyKey : customPropertiesKeysList)
            {
                // make sure that only non-empty strings that contain '=' within them enter to the key list:
                if (!StringHelper.isNullOrEmpty(customPropertyKey) && customPropertyKey.contains("=")) //$NON-NLS-1$
                {
                    getCustomPropertiesKeysList().add(customPropertyKey);
                }
            }
        }
    }

    @Override
    public ValidationResult Validate(Object value)
    {
        // check regex first
        RegexValidation tempVar = new RegexValidation();
        tempVar.setExpression("(([a-zA-Z0-9_]+=[^;]+)+(;)?)(([a-zA-Z0-9_]+=[^;]+;+)*$)"); //$NON-NLS-1$
        tempVar.setMessage(ConstantsManager.getInstance().getConstants().fieldValueShouldFollowMsg());
        RegexValidation regexValidation = tempVar;

        ValidationResult validationResult = regexValidation.Validate(value);
        if (validationResult.getSuccess() == false)
        {
            return validationResult;
        }

        String[] split;

        if (value == null || StringHelper.stringsEqual(((String) (value)).trim(), "")) //$NON-NLS-1$
        {
            split = new String[0];
        }
        else
        {
            split = ((String) value).split("[;]", -1); //$NON-NLS-1$
        }
        if (getCustomPropertiesKeysList() == null || getCustomPropertiesKeysList().isEmpty()
                || StringHelper.isNullOrEmpty(getCustomPropertiesKeysList().get(0)))
        {
            return new ValidationResult();
        }

        for (String line : split)
        {
            if (StringHelper.isNullOrEmpty(line))
            {
                continue;
            }
            boolean contains = false;
            // TODO: GILAD: Validate values according to RegExps (keys will be available via drop-down in the new
            // design)
            // Also, put an appropriate validation messgae when necessary.
            for (String validKey : getCustomPropertiesKeysList())
            {
                if (line.substring(0, line.indexOf('=') < 0 ? line.length() : line.indexOf('='))
                        .equals(validKey.substring(0, validKey.indexOf('='))))
                {
                    contains = true;
                    break;
                }
            }
            if (!contains)
            {
                String parameters = ""; //$NON-NLS-1$

                for (String keyValue : getCustomPropertiesKeysList())
                {
                    parameters += keyValue.substring(0, keyValue.indexOf('=')) + ", "; //$NON-NLS-1$
                }
                parameters = parameters.substring(0, parameters.length() - 2);

                String reasonStr =
                        ConstantsManager.getInstance()
                                .getMessages()
                                .customPropertyOneOfTheParamsIsntSupported(parameters);

                ArrayList<String> reason = new ArrayList<String>();
                reason.add(reasonStr);
                ValidationResult tempVar2 = new ValidationResult();
                tempVar2.setSuccess(false);
                tempVar2.setReasons(reason);
                return tempVar2;
            }
        }
        String falseProperty = null;
        for (String line : split)
        {
            if (StringHelper.isNullOrEmpty(line))
            {
                continue;
            }
            for (String validKey : getCustomPropertiesKeysList())
            {
                if (line.substring(0, line.indexOf('=')).equals(validKey.substring(0, validKey.indexOf('='))))
                {
                    RegexValidation tempVar3 = new RegexValidation();
                    tempVar3.setExpression(validKey.substring(validKey.indexOf('=') + 1));
                    RegexValidation testValue = tempVar3;
                    if (testValue.Validate(line.substring(line.indexOf('=') + 1)).getSuccess() == false)
                    {
                        falseProperty = validKey;
                        continue;
                    }
                }
            }
        }
        if (falseProperty != null)
        {
            ArrayList<String> reason = new ArrayList<String>();
            reason.add(ConstantsManager.getInstance()
                    .getMessages()
                    .customPropertyValueShouldBeInFormatReason(falseProperty.substring(0, falseProperty.indexOf('=')),
                            falseProperty.substring(falseProperty.indexOf('=') + 1)));
            ValidationResult tempVar4 = new ValidationResult();
            tempVar4.setSuccess(false);
            tempVar4.setReasons(reason);
            return tempVar4;

        }
        return new ValidationResult();
    }

}
