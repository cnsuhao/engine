package org.ovirt.engine.ui.uicommonweb.models.tags;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.EventDefinition;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LengthValidation;
import org.ovirt.engine.ui.uicommonweb.validation.I18NNameValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("unused")
public class TagModel extends Model
{

    public static EventDefinition SelectionChangedEventDefinition;
    private Event privateSelectionChangedEvent;

    public Event getSelectionChangedEvent()
    {
        return privateSelectionChangedEvent;
    }

    private void setSelectionChangedEvent(Event value)
    {
        privateSelectionChangedEvent = value;
    }

    public static void RecursiveEditAttachDetachLists(TagModel tagModel,
            Map<Guid, Boolean> attachedEntities,
            ArrayList<Guid> tagsToAttach,
            ArrayList<Guid> tagsToDetach)
    {
        if (tagModel.getSelection() != null && tagModel.getSelection().equals(true)
                && (!attachedEntities.containsKey(tagModel.getId()) || attachedEntities.get(tagModel.getId()) == false))
        {
            tagsToAttach.add(tagModel.getId());
        }
        else if (tagModel.getSelection() != null && tagModel.getSelection().equals(false)
                && attachedEntities.containsKey(tagModel.getId()))
        {
            tagsToDetach.add(tagModel.getId());
        }
        if (tagModel.getChildren() != null)
        {
            for (TagModel subModel : tagModel.getChildren())
            {
                RecursiveEditAttachDetachLists(subModel, attachedEntities, tagsToAttach, tagsToDetach);
            }
        }
    }

    private boolean privateIsNew;

    public boolean getIsNew()
    {
        return privateIsNew;
    }

    public void setIsNew(boolean value)
    {
        privateIsNew = value;
    }

    private Guid privateId = new Guid();

    public Guid getId()
    {
        return privateId;
    }

    public void setId(Guid value)
    {
        privateId = value;
    }

    private Guid privateParentId = new Guid();

    public Guid getParentId()
    {
        return privateParentId;
    }

    public void setParentId(Guid value)
    {
        privateParentId = value;
    }

    private ArrayList<TagModel> privateChildren;

    public ArrayList<TagModel> getChildren()
    {
        return privateChildren;
    }

    public void setChildren(ArrayList<TagModel> value)
    {
        privateChildren = value;
    }

    private EntityModel privateName;

    public EntityModel getName()
    {
        return privateName;
    }

    public void setName(EntityModel value)
    {
        privateName = value;
    }

    private EntityModel privateDescription;

    public EntityModel getDescription()
    {
        return privateDescription;
    }

    public void setDescription(EntityModel value)
    {
        privateDescription = value;
    }

    private Boolean selection;

    public Boolean getSelection()
    {
        return selection;
    }

    public void setSelection(Boolean value)
    {
        if (selection == null && value == null)
        {
            return;
        }
        if (selection == null || !selection.equals(value))
        {
            selection = value;
            getSelectionChangedEvent().raise(this, EventArgs.Empty);
            OnPropertyChanged(new PropertyChangedEventArgs("Selection")); //$NON-NLS-1$
        }
    }

    private TagModelType type = TagModelType.values()[0];

    public TagModelType getType()
    {
        return type;
    }

    public void setType(TagModelType value)
    {
        if (type != value)
        {
            type = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Type")); //$NON-NLS-1$
        }
    }

    static
    {
        SelectionChangedEventDefinition = new EventDefinition("SelectionChanged", TagModel.class); //$NON-NLS-1$
    }

    public TagModel()
    {
        setSelectionChangedEvent(new Event(SelectionChangedEventDefinition));

        setName(new EntityModel());
        setDescription(new EntityModel());
    }

    public boolean Validate()
    {
        LengthValidation tempVar = new LengthValidation();
        tempVar.setMaxLength(40);
        getName().ValidateEntity(new IValidation[] { new NotEmptyValidation(), tempVar, new I18NNameValidation() });

        return getName().getIsValid();
    }
}
