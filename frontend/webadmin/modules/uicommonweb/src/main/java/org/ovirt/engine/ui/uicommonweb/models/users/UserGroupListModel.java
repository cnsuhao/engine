package org.ovirt.engine.ui.uicommonweb.models.users;

import org.ovirt.engine.core.common.businessentities.DbUser;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class UserGroupListModel extends SearchableListModel
{

    @Override
    public DbUser getEntity()
    {
        return (DbUser) ((super.getEntity() instanceof DbUser) ? super.getEntity() : null);
    }

    public void setEntity(DbUser value)
    {
        super.setEntity(value);
    }

    public UserGroupListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().directoryGroupsTitle());
        setHashName("directory_groups"); // $//$NON-NLS-1$
    }

    @Override
    protected void OnEntityChanged()
    {
        super.OnEntityChanged();

        if (getEntity() != null)
        {
            ArrayList<UserGroup> items = new ArrayList<UserGroup>();
            for (String groupFullName : getEntity().getgroups().split("[,]", -1)) //$NON-NLS-1$
            {
                items.add(CreateUserGroup(groupFullName));
            }

            setItems(items);
        }
        else
        {
            setItems(null);
        }
    }

    private static UserGroup CreateUserGroup(String groupFullName)
    {
        // Parse 'groupFullName' (representation: Domain/OrganizationalUnit/Group)
        int firstIndexOfSlash = groupFullName.indexOf('/');
        int lastIndexOfSlash = groupFullName.lastIndexOf('/');
        String domain = firstIndexOfSlash >= 0 ? groupFullName.substring(0, firstIndexOfSlash) : ""; //$NON-NLS-1$
        String groupName = lastIndexOfSlash >= 0 ? groupFullName.substring(lastIndexOfSlash + 1) : ""; //$NON-NLS-1$
        String organizationalUnit =
                lastIndexOfSlash > firstIndexOfSlash ? groupFullName.substring(0, lastIndexOfSlash)
                        .substring(firstIndexOfSlash + 1) : ""; //$NON-NLS-1$

        UserGroup tempVar = new UserGroup();
        tempVar.setGroupName(groupName);
        tempVar.setOrganizationalUnit(organizationalUnit);
        tempVar.setDomain(domain);
        return tempVar;
    }

    @Override
    protected String getListName() {
        return "UserGroupListModel"; //$NON-NLS-1$
    }
}
