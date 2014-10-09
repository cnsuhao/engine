package org.ovirt.engine.core.searchbackend;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.ovirt.engine.core.compat.StringBuilderCompat;
import org.ovirt.engine.core.compat.StringFormat;
import org.ovirt.engine.core.compat.StringHelper;

public class SyntaxContainer implements Iterable<SyntaxObject> {

    private final String mOrigText;
    private final LinkedList<SyntaxObject> mObjList = new LinkedList<SyntaxObject>();
    private final List<String> mCurrentCompletions = new ArrayList<String>();

    private boolean mValid = false;
    private SyntaxError mError = SyntaxError.NO_ERROR;
    private final int[] mErrorPos = new int[2];
    private int privateMaxCount;
    private long searchFrom = 0;
    private boolean caseSensitive=true;

    public int getMaxCount() {
        return privateMaxCount;
    }

    public void setMaxCount(int value) {
        privateMaxCount = value;
    }

    public long getSearchFrom() {
        return searchFrom;
    }

    public void setSearchFrom(long value) {
        searchFrom = value;
    }

    public boolean getvalid() {
        return mValid;
    }

    public void setvalid(boolean value) {
        mValid = value;
    }

    public SyntaxError getError() {
        return mError;
    }

    public boolean getCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean value) {
        caseSensitive = value;
    }
    public int getErrorStartPos() {
        return mErrorPos[0];
    }

    public int getErrorEndPos() {
        return mErrorPos[1];
    }

    public SyntaxObject getFirst() {
        return mObjList.getFirst();
    }

    public String getSearchObjectStr() {
        if (mObjList.getFirst() != null) {
            return getObjSingularName(mObjList.getFirst().getBody());
        }
        return null;
    }

    public SyntaxContainer(final String origText) {
        mOrigText = origText;
        mValid = false;
    }

    public void setErr(SyntaxError errCode, int startPos, int endPos) {
        mErrorPos[0] = startPos;
        mErrorPos[1] = endPos;
        mError = errCode;
        mValid = false;
    }

    public void addSyntaxObject(SyntaxObjectType type, String body, int startPos, int endPos) {
        SyntaxObject newObj = new SyntaxObject(type, body, startPos, endPos);
        mObjList.addLast(newObj);
    }

    public SyntaxObjectType getState() {
        SyntaxObjectType retval = SyntaxObjectType.BEGIN;
        if (mObjList.size() > 0) {
            retval = mObjList.getLast().getType();
        }
        return retval;
    }

    public int getLastHandledIndex() {
        int retval = 0;
        if (mObjList.size() > 0) {
            retval = mObjList.getLast().getPos()[1];
        }
        return retval;
    }

    public String getPreviousSyntaxObject(int steps, SyntaxObjectType type) {
        String retval = "";
        if (mObjList.size() > steps) {
            SyntaxObject obj = mObjList.get(mObjList.size() - 1 - steps);
            if (obj.getType() == type) {
                retval = obj.getBody();
            }
        }
        if ((StringHelper.EqOp(retval, ""))
                && ((type == SyntaxObjectType.CROSS_REF_OBJ) || (type == SyntaxObjectType.SEARCH_OBJECT))) {
            retval = mObjList.getFirst().getBody();
        }
        return retval;
    }

    public SyntaxObjectType getPreviousSyntaxObjectType(int steps) {
        SyntaxObjectType retval = SyntaxObjectType.END;
        if (mObjList.size() > steps) {
            SyntaxObject obj = mObjList.get(mObjList.size() - 1 - steps);
            retval = obj.getType();
        }
        return retval;
    }

    public void addToACList(String[] acArr) {
        for (int idx = 0; idx < acArr.length; idx++) {
            mCurrentCompletions.add(acArr[idx]);
        }
    }

    public String[] getCompletionArray() {
        String[] retval = new String[mCurrentCompletions.size()];
        for (int idx = 0; idx < mCurrentCompletions.size(); idx++) {
            retval[idx] = mCurrentCompletions.get(idx);
        }
        return retval;
    }

    public java.util.ArrayList<String> getCrossRefObjList() {
        java.util.ArrayList<String> retval = new java.util.ArrayList<String>();
        String searchObj = getObjSingularName(getSearchObjectStr());
        for (SyntaxObject obj : mObjList) {
            if (obj.getType() == SyntaxObjectType.CROSS_REF_OBJ) {
                String objSingularName = getObjSingularName(obj.getBody());
                if ((!retval.contains(objSingularName)) && (!StringHelper.EqOp(searchObj, objSingularName))) {
                    retval.add(objSingularName);
                }
            }
        }
        return retval;
    }

    public String getObjSingularName(String obj) {
        String retval = obj;

        if (obj == null) {
            return null;
        }
        if (StringHelper.EqOp(obj, SearchObjects.AD_USER_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.AD_USER_PLU_OBJ_NAME)) {
            retval = SearchObjects.AD_USER_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.AUDIT_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.AUDIT_PLU_OBJ_NAME)) {
            retval = SearchObjects.AUDIT_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.TEMPLATE_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.TEMPLATE_PLU_OBJ_NAME)) {
            retval = SearchObjects.TEMPLATE_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.VDC_USER_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.VDC_USER_PLU_OBJ_NAME)) {
            retval = SearchObjects.VDC_USER_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.VDS_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.VDS_PLU_OBJ_NAME)) {
            retval = SearchObjects.VDS_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.VM_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.VM_PLU_OBJ_NAME)) {
            retval = SearchObjects.VM_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.DISK_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.DISK_PLU_OBJ_NAME)) {
            retval = SearchObjects.DISK_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.QUOTA_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.QUOTA_PLU_OBJ_NAME)) {
            retval = SearchObjects.QUOTA_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.VDC_POOL_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.VDC_POOL_PLU_OBJ_NAME)) {
            retval = SearchObjects.VDC_POOL_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.VDC_CLUSTER_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.VDC_CLUSTER_PLU_OBJ_NAME)) {
            retval = SearchObjects.VDC_CLUSTER_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.VDC_STORAGE_DOMAIN_PLU_OBJ_NAME)) {
            retval = SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.GLUSTER_VOLUME_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.GLUSTER_VOLUME_PLU_OBJ_NAME)) {
            retval = SearchObjects.GLUSTER_VOLUME_OBJ_NAME;
        }
        else if (StringHelper.EqOp(obj, SearchObjects.NETWORK_OBJ_NAME)
                || StringHelper.EqOp(obj, SearchObjects.NETWORK_PLU_OBJ_NAME)) {
            retval = SearchObjects.NETWORK_OBJ_NAME;
        } else {
            retval = obj;

        }
        return retval;
    }

    @Override
    public String toString() {
        StringBuilderCompat sb = new StringBuilderCompat("---------------- SyntaxContainer ---------------------");
        sb.append("\n");
        sb.append("mOrigText       = ");
        sb.AppendLine(mOrigText);
        sb.append("Valid           = ");
        sb.AppendLine(Boolean.toString(mValid));
        sb.append("Error           = ");
        sb.AppendLine(mError.toString());
        sb.append("CrossRefObjlist = ");
        for (String cro : getCrossRefObjList()) {
            sb.append(StringFormat.format("%1$s, ", cro));
        }
        sb.append("Syntax object list:");

        for (SyntaxObject obj : mObjList) {
            sb.AppendLine("    ");
            sb.append(obj.toString());
        }
        return sb.toString();
    }

    public String ToStringBr() {
        StringBuilderCompat sb = new StringBuilderCompat("---------------- SyntaxContainer ---------------------");
        sb.append("<BR>mOrigText       = ");
        sb.append(mOrigText);
        sb.append("<BR>Valid           = ");
        sb.append(mValid);
        sb.append("<BR>Error           = ");
        sb.append(mError);
        sb.append("<BR>Syntax object list:");
        sb.append("<BR>CrossRefObjlist = ");
        for (String cro : getCrossRefObjList()) {
            sb.append(StringFormat.format("%1$s, ", cro));
        }
        for (SyntaxObject obj : mObjList) {
            sb.append("<BR>    ");
            sb.append(obj.toString());
        }
        return sb.toString();
    }

    public boolean contains(SyntaxObjectType type, String val) {
        boolean retval = false;
        for (SyntaxObject obj : mObjList) {
            if ((obj.getType() == type) && (StringHelper.EqOp(obj.getBody().toUpperCase(), val.toUpperCase()))) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    public java.util.ListIterator<SyntaxObject> listIterator(int index) {
        return mObjList.listIterator(index);
    }

    @Override
    public java.util.Iterator<SyntaxObject> iterator() {
        return mObjList.iterator();
    }
}
