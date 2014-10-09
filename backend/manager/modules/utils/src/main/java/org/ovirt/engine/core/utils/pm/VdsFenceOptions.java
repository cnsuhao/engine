package org.ovirt.engine.core.utils.pm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.queries.ValueObjectMap;
import org.ovirt.engine.core.compat.DoubleCompat;
import org.ovirt.engine.core.compat.IntegerCompat;
import org.ovirt.engine.core.compat.LongCompat;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class VdsFenceOptions implements Serializable {

    private static final long serialVersionUID = -8832636627473217232L;
    private static final String COMMA = ",";
    private static final String EQUAL = "=";
    private static final String NEWLINE = "\n";
    private static final String SEMICOLON = ";";
    private static final String COLON = ":";
    private static final String TRUE_STRING = "true";
    private static final String FALSE_STRING = "false";
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final String AGENT_ERROR = "Cannot find fence agent named {0} in fence option mapping";
    private static final String MAPPING_FORMAT_ERROR = "Illegal fencing mapping format {0}";

    private static Log log = LogFactory.getLog(VdsFenceOptions.class);
    private static HashMap<String, HashMap<String, String>> fencingOptionMapping;
    private static HashMap<String, String> fencingOptionTypes;

    private String fenceAgent = "";
    private String fencingOptions;
    private static HashMap<String, String> fencingAgentInstanceOptions;
    private static HashSet<String> fencingSpecialParams;

    /**
     * Initializes a new instance of the <see cref="VdsFencingOptions"/> class.
     */
    public VdsFenceOptions() {
        InitCache();
        Init();
    }

    /**
     * Initializes a new instance of the <see cref="VdsFencingOptions"/> class.
     *
     * @param agent
     *            The agent.
     * @param fencingOptions
     *            The fencing options.
     */
    public VdsFenceOptions(String agent, String fencingOptions) {
        if (!StringHelper.isNullOrEmpty(agent)) {
            this.fenceAgent = agent;
            this.fencingOptions = fencingOptions;
        }
        InitCache();
        Init();
    }

    public ValueObjectMap getFencingAgentInstanceOptionsMap() {
        return new ValueObjectMap(fencingAgentInstanceOptions, false);
    }

    @SuppressWarnings("unchecked")
    public void setFencingAgentInstanceOptionsMap(ValueObjectMap value) {
        fencingAgentInstanceOptions = (value != null) ? new HashMap<String, String>(value.asMap()) : null;
    }

    public ValueObjectMap getFencingOptionMappingMap() {
        return new ValueObjectMap(fencingOptionMapping, true);
    }

    @SuppressWarnings("unchecked")
    public void setFencingOptionMappingMap(ValueObjectMap value) {
        fencingOptionMapping = (value != null) ? new HashMap<String, HashMap<String, String>>(
                value.asMap())
                : null;
    }

    public HashMap<String, HashMap<String, String>> getFencingOptionMappingMap2() {
        return fencingOptionMapping;
    }

    public void setFencingOptionMappingMap2(HashMap<String, HashMap<String, String>> value) {
        fencingOptionMapping = value;
    }

    public ValueObjectMap getFencingOptionTypesMap() {
        return new ValueObjectMap(fencingOptionTypes, false);
    }

    @SuppressWarnings("unchecked")
    public void setFencingOptionTypesMap(ValueObjectMap value) {
        fencingOptionTypes = (value != null) ? new HashMap<String, String>(value.asMap())
                : null;
    }

    /**
     * Caches the fencing agents options mapping. Mapping are stored in the following format <!--
     * <agent>:{var=value}{[,]var=value}*; --> for example :
     * alom:secure=secure,port=ipport;apc:secure=secure,port=ipport,slot=port
     */
    private void CacheFencingAgentsOptionMapping() {
        String localfencingOptionMapping = Config.<String> GetValue(ConfigValues.VdsFenceOptionMapping);
        String[] agentsOptionsStr = localfencingOptionMapping.split(StringHelper.quote(SEMICOLON), -1);
        for (String agentOptionsStr : agentsOptionsStr) {
            String[] parts = agentOptionsStr.split(StringHelper.quote(COLON), -1);
            if (parts.length == 2) {
                String agent = parts[0];
                HashMap<String, String> agentOptions = new HashMap<String, String>();
                // check for empty options
                if (!StringHelper.isNullOrEmpty(parts[1])) {
                    String[] options = parts[1].split(StringHelper.quote(COMMA), -1);
                    for (String option : options) {
                        String[] optionKeyVal = option.split(StringHelper.quote(EQUAL), -1);
                        agentOptions.put(optionKeyVal[0], optionKeyVal[1]);
                        // add mapped keys to special params
                        fencingSpecialParams.add(optionKeyVal[1]);
                    }
                }
                fencingOptionMapping.put(agent, agentOptions);
            } else {
                log.errorFormat(MAPPING_FORMAT_ERROR, agentOptionsStr);
                break;
            }
        }
    }

    /**
     * Caches the fencing agents option types. Types are stored in the following format <!-- [key=type][,][key=type]*-->
     * for example : secure=bool,port=int,slot=int
     */
    private void CacheFencingAgentsOptionTypes() {
        String localfencingOptionTypes = Config.<String> GetValue(ConfigValues.VdsFenceOptionTypes);
        String[] types = localfencingOptionTypes.split(StringHelper.quote(COMMA), -1);
        for (String entry : types) {
            String[] optionKeyVal = entry.split(StringHelper.quote(EQUAL), -1);
            fencingOptionTypes.put(optionKeyVal[0], optionKeyVal[1]);
        }
    }

    /**
     * Gets the real key given the displayed key.
     *
     * @param agent
     *            The agent.
     * @param displayedKey
     *            The displayed key.
     * @return
     */
    private String GetRealKey(String agent, String displayedKey) {
        String result = "";
        if (!StringHelper.isNullOrEmpty(agent) && !StringHelper.isNullOrEmpty(displayedKey)) {
            if (fencingOptionMapping.containsKey(agent)) {
                HashMap<String, String> agentOptions = fencingOptionMapping.get(agent);
                result = agentOptions.containsKey(displayedKey) ? agentOptions.get(displayedKey)
                        : displayedKey;
            } else {
                log.errorFormat(AGENT_ERROR, agent);
            }
        }
        return result;
    }

    /**
     * Gets the displayed key given the real key.
     *
     * @param agent
     *            The agent.
     * @param realKey
     *            The real key.
     * @return
     */
    private String GetDisplayedKey(String agent, String realKey) {
        String result = "";
        if (!StringHelper.isNullOrEmpty(agent) && !StringHelper.isNullOrEmpty(realKey)) {
            if (fencingOptionMapping.containsKey(agent)) {
                HashMap<String, String> agentOptions = fencingOptionMapping.get(agent);
                if (agentOptions.containsValue(realKey)) {
                    for (Map.Entry<String, String> pair : agentOptions.entrySet()) {
                        if (StringHelper.EqOp(pair.getValue(), realKey)) {
                            result = pair.getKey();
                            break;
                        }
                    }
                } else {
                    // assume that a legal flag that not exists in mapping was
                    // used
                    result = realKey;
                }
            } else {
                log.errorFormat(AGENT_ERROR, agent);
            }
        }
        return result;
    }

    /**
     * Gets the type of the key.
     *
     * @param key
     *            The key.
     * @return
     */
    private String GetOptionType(String key) {
        String result = "";
        if (!StringHelper.isNullOrEmpty(key) && fencingOptionTypes.containsKey(key)) {
            result = fencingOptionTypes.get(key);
        }
        return result;
    }

    /**
     * Translates the bool value to yes/no.
     *
     * @param value
     *            The value.
     * @return
     */
    private static String TranslateBoolValue(String value) {
        String result;
        if (value.equalsIgnoreCase(TRUE_STRING) || value.equalsIgnoreCase(FALSE_STRING)) {
            if (Boolean.parseBoolean(value)) {
                result = YES;
            }
            else {
                result = NO;
            }
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Inits this instance.
     */
    private void Init() {
        InitCache();
        CacheFencingAgentInstanceOptions();
    }

    /**
     * Cleans up.
     */
    private void CleanUp() {

        if (fencingAgentInstanceOptions != null && fencingOptionMapping != null
                && fencingOptionTypes != null) {
            fencingAgentInstanceOptions.clear();
            fencingOptionMapping.clear();
            fencingOptionTypes.clear();
            fencingSpecialParams.clear();
        }
        Init();
    }

    /**
     * Inits the cache.
     */
    private void InitCache() {
        if (fencingOptionMapping == null) {
            fencingAgentInstanceOptions = new HashMap<String, String>();
            fencingOptionMapping = new HashMap<String, HashMap<String, String>>();
            fencingOptionTypes = new HashMap<String, String>();
            fencingSpecialParams = new HashSet<String>();
            CacheFencingAgentsOptionMapping();
            CacheFencingAgentsOptionTypes();
        }
    }

    /**
     * Caches the fencing agent instance options.
     */
    private void CacheFencingAgentInstanceOptions() {
        if (!StringHelper.isNullOrEmpty(getAgent())
                && !StringHelper.isNullOrEmpty(getFencingOptions())) {
            String[] options = getFencingOptions().split(StringHelper.quote(COMMA), -1);
            fencingAgentInstanceOptions.clear();
            for (String option : options) {
                String[] optionKeyVal = option.split(StringHelper.quote(EQUAL), -1);
                if (optionKeyVal.length == 1) {
                    add(getAgent(), optionKeyVal[0], "");
                } else {
                    add(getAgent(), optionKeyVal[0], optionKeyVal[1]);
                }
            }
        }
    }

    /**
     * handles agent mapping, get the real agent for a given agent name
     *
     * @param agent
     *            the agent name
     * @return string , the agent real name to be used
     */
    public static String getRealAgent(String agent) {
        String agentMapping = Config.<String> GetValue(ConfigValues.FenceAgentMapping);
        String realAgent = agent;
        // result has the format [<agent>=<real agent>[,]]*
        String[] settings = agentMapping.split(StringHelper.quote(COMMA), -1);
        if (settings.length > 0) {
            for (String setting : settings) {
                // get the <agent>=<real agent> pair
                String[] pair = setting.split(StringHelper.quote(EQUAL), -1);
                if (pair.length == 2) {
                    if (agent.equalsIgnoreCase(pair[0])) {
                        realAgent = pair[1];
                        break;
                    }
                }
            }
        }
        return realAgent;
    }

    /**
     * handles agent default options
     *
     * @param agent
     * @param fenceOptions
     * @return String the options after adding default agent parameters
     */
    public static String getDefaultAgentOptions(String agent, String fenceOptions) {
        String agentdefaultParams = Config.<String> GetValue(ConfigValues.FenceAgentDefaultParams);
        StringBuilder realOptions = new StringBuilder(fenceOptions);
        // result has the format [<agent>:param=value[,]...;]*
        String[] params = agentdefaultParams.split(StringHelper.quote(SEMICOLON), -1);
        for (String agentOptionsStr : params) {
            String[] parts = agentOptionsStr.split(StringHelper.quote(COLON), -1);
            if (parts.length == 2) {
                if (agent.equalsIgnoreCase(parts[0])) {
                    // check for empty options
                    if (!StringHelper.isNullOrEmpty(parts[1])) {
                        String[] options = parts[1].split(StringHelper.quote(COMMA), -1);
                        for (String option : options) {
                            String[] optionKeyVal = option.split(StringHelper.quote(EQUAL), -1);
                            // if a value is set explicitly for a default param
                            // we respect that value and not use the default value
                            if (!fenceOptions.contains(optionKeyVal[0])) {
                                if (realOptions.length() > 0) {
                                    realOptions.append(COMMA);
                                }
                                realOptions.append(optionKeyVal[0]);
                                if (optionKeyVal.length == 2) {
                                    String val = (optionKeyVal[1] == null) ? "" : optionKeyVal[1];
                                    realOptions.append(EQUAL);
                                    realOptions.append(val);
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        return realOptions.toString();
    }

    public String getAgent() {
        return fenceAgent;
    }

    public void setAgent(String value) {
        fenceAgent = value;
        CleanUp();
    }

    public String getFencingOptions() {
        return fencingOptions;
    }

    public void setFencingOptions(String value) {
        fencingOptions = value;
        CleanUp();
    }

    /**
     * Adds the specified key.
     *
     * @param key
     *            The key.
     * @param value
     *            The value.
     */
    public void add(String key, String value) {
        add(getAgent(), key, value);
    }

    /**
     * Adds the specified key.
     *
     * @param agent
     *            The agent.
     * @param key
     *            The key.
     * @param value
     *            The value.
     */
    public void add(String agent, String key, String value) {
        key = GetRealKey(agent, key);
        fencingAgentInstanceOptions.put(key, value);
    }

    /**
     * Determines whether the specified current agent key is supported .
     *
     * @param key
     *            The key.
     * @return <c>true</c> if the specified key is supported; otherwise, <c>false</c>.
     */

    public boolean IsSupported(String key) {

        return IsSupported(getAgent(), key);
    }

    /**
     * Determines whether the specified agent key is supported.
     *
     * @param agent
     *            The agent.
     * @param key
     *            The key.
     * @return <c>true</c> if the specified agent is supported; otherwise, <c>false</c>.
     */

    public boolean IsSupported(String agent, String key) {
        boolean result = false;
        if (!StringHelper.isNullOrEmpty(agent) && !StringHelper.isNullOrEmpty(key)
                && fencingOptionMapping.containsKey(agent)) {
            HashMap<String, String> agentOptions = fencingOptionMapping.get(agent);
            result = (agentOptions == null) ? false : agentOptions.containsKey(key);
        } else {
            log.errorFormat(AGENT_ERROR, agent);
        }

        return result;
    }

    /**
     * Gets the current agent supported options.
     *
     * @return
     */

    public ArrayList<String> GetSupportedOptions() {
        return GetSupportedOptions(getAgent());

    }

    /**
     * Gets the agent supported options.
     *
     * @param agent
     *            The agent.
     * @return
     */

    public ArrayList<String> GetSupportedOptions(String agent) {
        ArrayList<String> agentOptions = new ArrayList<String>();
        if (fencingOptionMapping.containsKey(agent)) {
            HashMap<String, String> options = fencingOptionMapping.get(agent);
            for (Map.Entry<String, String> pair : options.entrySet()) {
                agentOptions.add(pair.getKey());
            }
        } else {
            log.errorFormat(AGENT_ERROR, agent);
        }
        return agentOptions;

    }

    /**
     * Gets the specified key.
     *
     * @param key
     *            The key.
     * @return The key value, null if key is not exist
     */

    public Object Get(String key) {
        final String BOOL = "bool";
        final String INT = "int";
        final String LONG = "long";
        final String DOUBLE = "double";
        Object result = null;
        if (!StringHelper.isNullOrEmpty(key)) {
            String type = GetOptionType(key);
            key = GetRealKey(getAgent(), key);
            if (fencingAgentInstanceOptions != null
                    && fencingAgentInstanceOptions.containsKey(key)) {
                if (!StringHelper.isNullOrEmpty(type)) {
                    // Convert to the suitable type according to metadata.
                    if (type.equalsIgnoreCase(BOOL)) {
                        result = Boolean.parseBoolean(fencingAgentInstanceOptions.get(key));
                    }
                    else if (type.equalsIgnoreCase(INT)) {
                        Integer intVal = IntegerCompat.tryParse(fencingAgentInstanceOptions
                                .get(key));
                        if (intVal != null) {
                            result = intVal;
                        }
                    }
                    else if (type.equalsIgnoreCase(LONG)) {
                        final Long longVal = LongCompat.tryParse(fencingAgentInstanceOptions
                                .get(key));
                        if (longVal != null) {
                            result = longVal;
                        }
                    }
                    else if (type.equalsIgnoreCase(DOUBLE)) {
                        final Double doubleVal = DoubleCompat.tryParseDouble(fencingAgentInstanceOptions
                                .get(key));
                        if (doubleVal != null) {
                            result = doubleVal;
                        }
                    } else // return as string
                    {
                        result = fencingAgentInstanceOptions.get(key);
                    }
                } else {
                    // return value as an object
                    result = fencingAgentInstanceOptions.get(key);
                }
            }
        }
        return result;
    }

    /**
     * Returns a <see cref="T:System.String"/> that represents the current <see cref="T:System.Object"/>.
     *
     * @return A <see cref="T:System.String"/> that represents the current <see cref="T:System.Object"/>.
     */

    @Override
    public String toString() {
        StringBuilder value = new StringBuilder();
        String delimiter = "";
        for (Map.Entry<String, String> pair : fencingAgentInstanceOptions.entrySet()) {
            value.append(delimiter)
                    .append(GetDisplayedKey(getAgent(), pair.getKey()))
                    .append(pair.getValue().length() > 0 ? EQUAL + pair.getValue() : "");
            delimiter = COMMA;
        }
        return value.toString();
    }

    /**
     * Gets the unsupported options string.
     *
     * @return
     */
    public String ToUnsupportedOptionsString() {
        String delimiter = "";
        StringBuilder value = new StringBuilder();
        for (Map.Entry<String, String> pair : fencingAgentInstanceOptions.entrySet()) {
            String displayedKey = GetDisplayedKey(getAgent(), pair.getKey());
            if (!IsSupported(displayedKey)) {
                value.append(delimiter)
                        .append(displayedKey)
                        .append((pair.getValue().length() > 0 ? EQUAL + pair.getValue() : ""));
                delimiter = COMMA;
            }
        }
        return value.toString();
    }

    /**
     * Gets the internal representation of the options.
     *
     * @return
     */

    public String ToInternalString() {
        StringBuilder value = new StringBuilder();
        String delimiter = "";
        for (Map.Entry<String, String> pair : fencingAgentInstanceOptions.entrySet()) {
            if (pair.getValue().trim().length() > 0) {
                value.append(delimiter).append(pair.getKey()).append(EQUAL).append(TranslateBoolValue(pair.getValue()));
                // special params should not be sent if value is empty
            } else if (!fencingSpecialParams.contains(pair.getKey())) {
                value.append(delimiter).append(pair.getKey());
            }
            delimiter = NEWLINE;
        }
        return value.toString();

    }

}
