package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.action.BookmarksParametersBase;
import org.ovirt.engine.core.common.businessentities.Bookmark;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogField;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogFields;

@CustomLogFields({ @CustomLogField("BookmarkName"), @CustomLogField("BookmarkValue") })
public abstract class BookmarkCommandBase<T extends BookmarksParametersBase> extends AdminOperationCommandBase<T> {
    private Bookmark mBookmark;
    private String mBookmarkName;

    public BookmarkCommandBase(T parameters) {
        super(parameters);
    }

    public BookmarkCommandBase() {
    }

    protected Bookmark getBookmark() {
        if (mBookmark == null) {
            mBookmark = DbFacade.getInstance().getBookmarkDao()
                    .get(getBookmarkId());
        }
        return mBookmark;
    }

    public String getBookmarkValue() {
        return getBookmark() != null ? getBookmark().getbookmark_value() : null;
    }

    public String getBookmarkName() {
        if (mBookmarkName == null && getBookmark() != null) {
            mBookmarkName = getBookmark().getbookmark_name();
        }
        return mBookmarkName;
    }

    public void setBookmarkName(String value) {
        mBookmarkName = value;
    }

    public Guid getBookmarkId() {
        return getParameters().getBookmarkId();
    }

    protected void AddErrorMessages(VdcBllMessages messageActionTypeParameter, VdcBllMessages messageReason) {
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__BOOKMARK);
        addCanDoActionMessage(messageActionTypeParameter);
        addCanDoActionMessage(messageReason);
    }

    protected void AddInvalidIdErrorMessages(VdcBllMessages messageActionTypeParameter) {
        AddErrorMessages(messageActionTypeParameter, VdcBllMessages.ACTION_TYPE_FAILED_BOOKMARK_INVALID_ID);
    }
}
