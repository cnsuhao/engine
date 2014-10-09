package org.ovirt.engine.core.vdsbroker.xmlrpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XmlRpcStruct {

    private final Map<String, Object> innerMap;

    public XmlRpcStruct(Map<String, Object> innerMap) {
        this.innerMap = innerMap;
    }

    public XmlRpcStruct() {
        this.innerMap = new HashMap<String, Object>();
    }

    public Object getItem(String key) {
        return innerMap.get(key);
    }

    /**
     * Adds a map to be passed to via the XmlRpc to the server.
     * The supported map values are listed on {@link http://ws.apache.org/xmlrpc/types.html}
     *
     * @param key
     *            the key which represents the map
     * @param map
     *            the map values to be sent
     */
    public void add(String key, Map<String, ?> map) {
        innerMap.put(key, map);
    }

    public void add(String key, List<String> map) {
        innerMap.put(key, map);
    }

    public void add(String key, XmlRpcStruct map) {
        innerMap.put(key, map.getInnerMap());
    }

    public void add(String key, String value) {
        innerMap.put(key, value);
    }

    public boolean containsKey(String name) {
        return innerMap.containsKey(name);
    }

    public boolean contains(String diskTotal) {
        return innerMap.containsKey(diskTotal);
    }

    public Set<String> getKeys() {
        return innerMap.keySet();
    }

    public void add(String key, Map[] drives) {
        innerMap.put(key, drives);
    }

    public void add(String key, XmlRpcStruct[] devices) {
        int i = 0;
        // TODO work with typed collection
        Map[] map = new Map[devices.length];
        for (XmlRpcStruct device : devices) {
            map[i++] = device.getInnerMap();
        }
        this.add(key, map);
    }

    public int getCount() {
        if (innerMap != null) {
            return innerMap.size();
        } else {
            return 0;
        }
    }

    public void add(String sysprepInf, byte[] binarySysPrep) {
        innerMap.put(sysprepInf, binarySysPrep);
    }

    public void add(String key, int value) {
        innerMap.put(key, value);
    }

    public Set<Map.Entry<String, Object>> getEntries() {
        return innerMap.entrySet();
    }

    public Map<String, Object> getInnerMap() {
        return innerMap;
    }

    @Override
    public String toString() {
        return innerMap.toString();
    }

}
