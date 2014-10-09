package org.ovirt.engine.ui.uicommonweb.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.HasStoragePool;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IntegerCompat;
import org.ovirt.engine.core.compat.Match;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.compat.NotifyCollectionChangedEventArgs;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.core.compat.Regex;
import org.ovirt.engine.core.compat.RegexOptions;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.frontend.RegistrationResult;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.ProvideTickEvent;
import org.ovirt.engine.ui.uicommonweb.ReportCommand;
import org.ovirt.engine.ui.uicommonweb.ReportInit;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.reports.ReportModel;
import org.ovirt.engine.ui.uicompat.IteratorUtils;

/**
 * Represents a list model with ability to fetch items both sync and async.
 */
@SuppressWarnings("unused")
public abstract class SearchableListModel extends ListModel implements GridController
{
    private static final int UnknownInteger = -1;
    private static Logger logger = Logger.getLogger(SearchableListModel.class.getName());
    private static final String PAGE_STRING_REGEX = "[\\s]+page[\\s]+[1-9]+[0-9]*[\\s]*$"; //$NON-NLS-1$
    private static final String PAGE_NUMBER_REGEX = "[1-9]+[0-9]*$"; //$NON-NLS-1$

    private UICommand privateSearchCommand;

    public UICommand getSearchCommand()
    {
        return privateSearchCommand;
    }

    private void setSearchCommand(UICommand value)
    {
        privateSearchCommand = value;
    }

    private UICommand privateSearchNextPageCommand;

    public UICommand getSearchNextPageCommand()
    {
        return privateSearchNextPageCommand;
    }

    private void setSearchNextPageCommand(UICommand value)
    {
        privateSearchNextPageCommand = value;
    }

    private UICommand privateSearchPreviousPageCommand;

    public UICommand getSearchPreviousPageCommand()
    {
        return privateSearchPreviousPageCommand;
    }

    private void setSearchPreviousPageCommand(UICommand value)
    {
        privateSearchPreviousPageCommand = value;
    }

    private UICommand privateForceRefreshCommand;

    public UICommand getForceRefreshCommand()
    {
        return privateForceRefreshCommand;
    }

    private void setForceRefreshCommand(UICommand value)
    {
        privateForceRefreshCommand = value;
    }

    private final List<ReportCommand> openReportCommands = new LinkedList<ReportCommand>();

    public ReportCommand addOpenReportCommand(String idParamName, boolean isMultiple, String uriId) {
        return addOpenReportCommand(new ReportCommand("OpenReport", idParamName, isMultiple, uriId, this)); //$NON-NLS-1$
    }

    private ReportCommand addOpenReportCommand(ReportCommand reportCommand)
    {
        if (openReportCommands.add(reportCommand))
        {
            ArrayList<IVdcQueryable> items =
                    getSelectedItems() != null ? Linq.<IVdcQueryable> Cast(getSelectedItems())
                            : new ArrayList<IVdcQueryable>();
            UpdateReportCommandAvailability(reportCommand, items);

            return reportCommand;
        } else {
            return null;
        }
    }

    private boolean privateIsQueryFirstTime;

    public boolean getIsQueryFirstTime()
    {
        return privateIsQueryFirstTime;
    }

    public void setIsQueryFirstTime(boolean value)
    {
        privateIsQueryFirstTime = value;
    }

    private boolean privateIsTimerDisabled;

    public boolean getIsTimerDisabled()
    {
        return privateIsTimerDisabled;
    }

    public void setIsTimerDisabled(boolean value)
    {
        privateIsTimerDisabled = value;
    }

    // Update IsAsync wisely! Set it once after initializing the SearchableListModel object.
    private boolean privateIsAsync;

    public boolean getIsAsync()
    {
        return privateIsAsync;
    }

    public void setIsAsync(boolean value)
    {
        privateIsAsync = value;
    }

    private String privateDefaultSearchString;

    public String getDefaultSearchString()
    {
        return privateDefaultSearchString;
    }

    public void setDefaultSearchString(String value)
    {
        privateDefaultSearchString = value;
    }

    private String[] searchObjects;

    public String[] getSearchObjects()
    {
        return searchObjects;
    }

    public void setSearchObjects(String[] value)
    {
        searchObjects = value;
    }

    private int privateSearchPageSize;

    public int getSearchPageSize()
    {
        return privateSearchPageSize;
    }

    public void setSearchPageSize(int value)
    {
        privateSearchPageSize = value;
    }

    private RegistrationResult asyncResult;

    public RegistrationResult getAsyncResult()
    {
        return asyncResult;
    }

    public void setAsyncResult(RegistrationResult value)
    {
        if (asyncResult != value)
        {
            AsyncResultChanging(value, asyncResult);
            asyncResult = value;
            OnPropertyChanged(new PropertyChangedEventArgs("AsyncResult")); //$NON-NLS-1$
        }
    }

    private String searchString;

    public String getSearchString()
    {
        return searchString;
    }

    public void setSearchString(String value)
    {
        if (!StringHelper.stringsEqual(searchString, value))
        {
            searchString = value;
            SearchStringChanged();
            OnPropertyChanged(new PropertyChangedEventArgs("SearchString")); //$NON-NLS-1$
        }
    }

    public int getSearchPageNumber()
    {
        if (StringHelper.isNullOrEmpty(getSearchString()))
        {
            return 1;
        }

        // try getting the end of SearchString in the form of "page <n>"
        String pageStringRegex = PAGE_STRING_REGEX;

        Match match = Regex.Match(getSearchString(), pageStringRegex, RegexOptions.IgnoreCase);
        if (match.Success())
        {
            // retrieve the page number itself:
            String pageString = match.getValue(); // == "page <n>"
            String pageNumberRegex = PAGE_NUMBER_REGEX;
            match = Regex.Match(pageString, pageNumberRegex);
            if (match.Success())
            {
                final Integer retValue = IntegerCompat.tryParse(match.getValue());
                if (retValue != null)
                {
                    return retValue;
                }
            }
        }

        return 1;
    }

    public String getItemsCountString() {
        if (getItems() == null) {
            return ""; //$NON-NLS-1$
        }
        int fromItemCount = getSearchPageSize() * (getSearchPageNumber() - 1) + 1;
        int toItemCount = (fromItemCount - 1) + ((List) getItems()).size();

        if (toItemCount == 0 || fromItemCount > toItemCount) {
            return ""; //$NON-NLS-1$
        }

        return fromItemCount + "-" + toItemCount; //$NON-NLS-1$
    }

    public int getNextSearchPageNumber()
    {
        return getSearchPageNumber() + 1;
    }

    public int getPreviousSearchPageNumber()
    {
        return getSearchPageNumber() == 1 ? 1 : getSearchPageNumber() - 1;
    }

    private final PrivateAsyncCallback asyncCallback;

    protected SearchableListModel()
    {
        // Configure this instance.
        getConfigurator().Configure(this);

        setSearchCommand(new UICommand("Search", this)); //$NON-NLS-1$
        setSearchNextPageCommand(new UICommand("SearchNextPage", this)); //$NON-NLS-1$
        setSearchPreviousPageCommand(new UICommand("SearchPreviousPage", this)); //$NON-NLS-1$
        setForceRefreshCommand(new UICommand("ForceRefresh", this)); //$NON-NLS-1$
        setSearchPageSize(UnknownInteger);
        asyncCallback = new PrivateAsyncCallback(this);

        UpdateActionAvailability();

        // Most of SearchableListModels will not have paging. The ones that
        // should have paging will set it explicitly in their constructors.
        getSearchNextPageCommand().setIsAvailable(false);
        getSearchPreviousPageCommand().setIsAvailable(false);
    }

    /**
     * Returns value indicating whether the specified search string is matching this list model.
     */
    public boolean IsSearchStringMatch(String searchString)
    {
        return true;
    }

    private GridTimer privatetimer;

    private boolean rapidTimerRunning;

    public GridTimer gettimer()
    {
        return privatetimer;
    }

    public void settimer(GridTimer value)
    {
        privatetimer = value;
    }

    @Override
    public GridTimer getTimer()
    {
        if (gettimer() == null)
        {
            settimer(new GridTimer(getListName()) {

                @Override
                public void execute() {
                    logger.fine(SearchableListModel.this.getClass().getName() + ": Executing search"); //$NON-NLS-1$
                    if (getIsAsync())
                    {
                        AsyncSearch();
                    } else {
                        SyncSearch();
                    }
                }

            });
            gettimer().setRefreshRate(getConfigurator().getPollingTimerInterval());
        }
        return gettimer();
    }

    @Override
    public void refresh() {
        getForceRefreshCommand().Execute();
    }

    @Override
    public void setSelectedItem(Object value) {
        setIsQueryFirstTime(true);
        super.setSelectedItem(value);
        setIsQueryFirstTime(false);
    }

    @Override
    public void setEntity(Object value) {
        if (getEntity() == null) {
            super.setEntity(value);
            return;
        }
        // Equals doesn't always has the same outcome as checking the ids of the elements.
        if (getEntity() instanceof IVdcQueryable) {
            if (value != null) {
                IVdcQueryable ivdcq_value = (IVdcQueryable) value;
                IVdcQueryable ivdcq_entity = (IVdcQueryable) getEntity();
                if (!ivdcq_value.getQueryableId().equals(ivdcq_entity.getQueryableId())) {
                    super.setEntity(value);
                    return;
                }
            }
        }
        if (!getEntity().equals(value)) {
            super.setEntity(value);
            return;
        }

        setEntity(value, false);
    }

    protected abstract String getListName();

    protected void SearchStringChanged()
    {
    }

    public void Search()
    {
        // Defer search if there max result limit was not yet retrieved.
        if (getSearchPageSize() == UnknownInteger)
        {
            asyncCallback.RequestSearch();
        }
        else
        {
            EnsureAsyncSearchStopped();

            if (getIsQueryFirstTime())
            {
                setSelectedItem(null);
                setSelectedItems(null);
            }

            if (getIsAsync())
            {
                AsyncSearch();
            }
            else
            {
                if (getIsTimerDisabled() == false)
                {
                    setIsQueryFirstTime(true);
                    SyncSearch();
                    setIsQueryFirstTime(false);
                    getTimer().start();
                }
                else
                {
                    SyncSearch();
                }
            }
        }
    }

    public void ForceRefresh()
    {
        getTimer().stop();
        setIsQueryFirstTime(true);
        SyncSearch();

        if (!getIsTimerDisabled())
        {
            getTimer().start();
        }
    }

    protected void setReportModelResourceId(ReportModel reportModel, String idParamName, boolean isMultiple) {

    }

    protected void OpenReport()
    {
        setWidgetModel(createReportModel());
    }

    protected ReportModel createReportModel() {
        ReportCommand reportCommand = (ReportCommand) getLastExecutedCommand();
        ReportModel reportModel = new ReportModel(ReportInit.getInstance().getReportBaseUrl());

        reportModel.setReportUnit(reportCommand.getUriValue());

        if (reportCommand.getIdParamName() != null) {
            for (Object item : getSelectedItems()) {
                if (((ReportCommand) getLastExecutedCommand()).isMultiple) {
                    reportModel.addResourceId(reportCommand.getIdParamName(), ((BusinessEntity<?>) item).getId()
                            .toString());
                } else {
                    reportModel.setResourceId(reportCommand.getIdParamName(), ((BusinessEntity<?>) item).getId()
                            .toString());
                }
            }
        }

        boolean isFromSameDc = true;
        boolean firstItem = true;
        String dcId = ""; //$NON-NLS-1$
        for (Object item : getSelectedItems()) {
            if (item instanceof HasStoragePool) {
                if (firstItem) {
                    dcId = ((HasStoragePool<?>) item).getStoragePoolId().toString();
                    firstItem = false;
                } else if (!(((HasStoragePool<?>) item).getStoragePoolId().toString().equals(dcId))) {
                    isFromSameDc = false;
                    reportModel.setDifferntDcError(true);
                    continue;
                }
            }
        }

        if (!dcId.equals("")) { //$NON-NLS-1$
            reportModel.setDataCenterID(dcId);
        }

        return reportModel;
    }

    private String dateStr(Date date) {
        return date.getYear() + "-" + date.getMonth() + "-" + date.getDate(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void AsyncResultChanging(RegistrationResult newValue, RegistrationResult oldValue)
    {
        if (oldValue != null)
        {
            oldValue.getRetrievedEvent().removeListener(this);
        }

        if (newValue != null)
        {
            newValue.getRetrievedEvent().addListener(this);
        }
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.equals(RegistrationResult.RetrievedEventDefinition))
        {
            AsyncResult_Retrieved();
        }
        if (ev.equals(ProvideTickEvent.Definition))
        {
            SyncSearch();
        }
    }

    private void AsyncResult_Retrieved()
    {
        // Update IsEmpty flag.

        // Note: Do NOT use IList. 'Items' is not necissarily IList
        // (e.g in Monitor models, the different ListModels' Items are
        // of type 'valueObjectEnumerableList', which is not IList).
        if (getItems() != null)
        {
            Iterator enumerator = getItems().iterator();
            setIsEmpty(enumerator.hasNext() ? false : true);
        }
        else
        {
            setIsEmpty(true);
        }
    }

    private void ResetIsEmpty()
    {
        // Note: Do NOT use IList: 'Items' is not necissarily IList
        // (e.g in Monitor models, the different ListModels' Items are
        // of type 'valueObjectEnumerableList', which is not IList).
        if (getItems() != null)
        {
            Iterator enumerator = getItems().iterator();
            if (enumerator.hasNext())
            {
                setIsEmpty(false);
            }
        }
    }

    @Override
    protected void ItemsChanged()
    {
        super.ItemsChanged();

        ResetIsEmpty();
        UpdatePagingAvailability();
    }

    @Override
    protected void ItemsCollectionChanged(Object sender, NotifyCollectionChangedEventArgs e)
    {
        super.ItemsCollectionChanged(sender, e);

        ResetIsEmpty();
        UpdatePagingAvailability();
    }

    @Override
    protected void OnSelectedItemChanged() {
        super.OnSelectedItemChanged();
        UpdateActionAvailability();
    }

    @Override
    protected void SelectedItemsChanged() {
        super.SelectedItemsChanged();
        UpdateActionAvailability();
    }

    private void UpdateReportCommandAvailability(ReportCommand reportCommand, List<?> selectedItems) {
        reportCommand.setIsExecutionAllowed((!reportCommand.isMultiple() && (selectedItems.size() == 1))
                || (reportCommand.isMultiple() && (selectedItems.size() > 1)));
    }

    private void UpdateActionAvailability() {
        List<?> items =
                getSelectedItems() != null ? getSelectedItems()
                        : Collections.emptyList();

        for (ReportCommand reportCommand : openReportCommands)
        {
            UpdateReportCommandAvailability(reportCommand, items);
        }
    }

    protected void UpdatePagingAvailability()
    {
        getSearchNextPageCommand().setIsExecutionAllowed(getSearchNextPageCommand().getIsAvailable()
                && getNextSearchPageAllowed());
        getSearchPreviousPageCommand().setIsExecutionAllowed(getSearchPreviousPageCommand().getIsAvailable()
                && getPreviousSearchPageAllowed());
    }

    private void SetSearchStringPage(int newSearchPageNumber)
    {
        if (Regex.IsMatch(getSearchString(), PAGE_STRING_REGEX, RegexOptions.IgnoreCase))
        {
            setSearchString(Regex.replace(getSearchString(),
                    PAGE_STRING_REGEX,
                    " page " + newSearchPageNumber)); //$NON-NLS-1$
        }
        else
        {
            setSearchString(getSearchString() + " page " + newSearchPageNumber); //$NON-NLS-1$
        }
    }

    protected void SearchNextPage()
    {
        SetSearchStringPage(getNextSearchPageNumber());
        getSearchCommand().Execute();
    }

    protected void SearchPreviousPage()
    {
        SetSearchStringPage(getPreviousSearchPageNumber());
        getSearchCommand().Execute();
    }

    protected boolean getNextSearchPageAllowed()
    {
        if (!getSearchNextPageCommand().getIsAvailable() || getItems() == null
                || IteratorUtils.moveNext(getItems().iterator()) == false)
        {
            return false;
        }

        boolean retValue = true;

        // ** TODO: Inefficient performance-wise! If 'Items' was ICollection or IList
        // ** it would be better, since we could simply check its 'Count' property.

        int pageSize = getSearchPageSize();

        if (pageSize > 0)
        {
            Iterator e = getItems().iterator();
            int itemsCountInCurrentPage = 0;
            while (IteratorUtils.moveNext(e))
            {
                itemsCountInCurrentPage++;
            }

            if (itemsCountInCurrentPage < pageSize)
            {
                // current page contains results quantity smaller than
                // the pageSize -> there is no next page:
                retValue = false;
            }
        }

        return retValue;
    }

    protected boolean getPreviousSearchPageAllowed()
    {
        return getSearchPreviousPageCommand().getIsAvailable() && getSearchPageNumber() > 1;
    }

    /**
     * Override this method to take care on sync fetching.
     */
    protected void SyncSearch()
    {
    }

    @Override
    public Iterable getItems()
    {
        return items;
    }

    @Override
    public void setItems(Iterable value)
    {
        if (items != value)
        {
            IVdcQueryable lastSelectedItem = (IVdcQueryable) getSelectedItem();
            ArrayList<IVdcQueryable> lastSelectedItems = new ArrayList<IVdcQueryable>();

            if (getSelectedItems() != null)
            {
                if (getSelectedItems() instanceof ArrayList)
                {
                    for (Object item : getSelectedItems())
                    {
                        lastSelectedItems.add((IVdcQueryable) item);
                    }
                }
                else
                {
                    Iterator iterator = getSelectedItems().iterator();
                    while (iterator.hasNext())
                    {
                        lastSelectedItems.add((IVdcQueryable) iterator.next());
                    }
                }
            }

            ItemsChanging(value, items);
            items = value;
            UpdatePagingAvailability();
            getItemsChangedEvent().raise(this, EventArgs.Empty);
            OnPropertyChanged(new PropertyChangedEventArgs("Items")); //$NON-NLS-1$

            selectedItem = null;

            if (getSelectedItems() != null)
            {
                getSelectedItems().clear();
            }

            if (lastSelectedItem != null && value != null)
            {
                IVdcQueryable newSelectedItem = null;
                ArrayList<IVdcQueryable> newItems = new ArrayList<IVdcQueryable>();

                if (value instanceof ArrayList)
                {
                    for (Object item : value)
                    {
                        newItems.add((IVdcQueryable) item);
                    }
                }
                else
                {
                    Iterator iterator = value.iterator();
                    while (iterator.hasNext())
                    {
                        newItems.add((IVdcQueryable) iterator.next());
                    }
                }

                if (newItems != null)
                {
                    for (IVdcQueryable newItem : newItems)
                    {
                        // Search for selected item
                        if (newItem.getQueryableId().equals(lastSelectedItem.getQueryableId()))
                        {
                            newSelectedItem = newItem;
                        }
                        else
                        {
                            // Search for selected items
                            for (IVdcQueryable item : lastSelectedItems)
                            {
                                if (newItem.getQueryableId().equals(item.getQueryableId()))
                                {
                                    selectedItems.add(newItem);
                                }
                            }
                        }
                    }
                }
                if (newSelectedItem != null)
                {
                    selectedItem = newSelectedItem;

                    if (selectedItems != null)
                    {
                        selectedItems.add(newSelectedItem);
                    }
                }
            }
            OnSelectedItemChanged();
        }
    }

    public void SyncSearch(VdcQueryType vdcQueryType, VdcQueryParametersBase vdcQueryParametersBase)
    {
        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object ReturnValue)
            {
                SearchableListModel searchableListModel = (SearchableListModel) model;
                searchableListModel.setItems((Iterable) ((VdcQueryReturnValue) ReturnValue).getReturnValue());
            }
        };

        vdcQueryParametersBase.setRefresh(getIsQueryFirstTime());

        Frontend.RunQuery(vdcQueryType, vdcQueryParametersBase, _asyncQuery);

        setIsQueryFirstTime(false);
    }

    /**
     * Override this method to take care on async fetching.
     */
    protected void AsyncSearch()
    {
    }

    public void EnsureAsyncSearchStopped()
    {
        getTimer().stop();
        if (getAsyncResult() != null && !getAsyncResult().getId().equals(NGuid.Empty))
        {
            Frontend.UnregisterQuery(getAsyncResult().getId());
            setAsyncResult(null);
        }
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);

        if (command == getSearchCommand())
        {
            Search();
        }
        else if (command == getSearchNextPageCommand())
        {
            SearchNextPage();
        }
        else if (command == getSearchPreviousPageCommand())
        {
            SearchPreviousPage();
        }
        else if (command == getForceRefreshCommand())
        {
            ForceRefresh();
        } else if (command instanceof ReportCommand) {
            ReportCommand reportCommand = (ReportCommand) command;
            OpenReport();
        }

        if (command != null && command.isAutoRefresh()) {
            getTimer().fastForward();
        }
    }

    public final static class PrivateAsyncCallback
    {
        private final SearchableListModel model;
        private boolean searchRequested;

        public PrivateAsyncCallback(SearchableListModel model)
        {
            this.model = model;
            AsyncQuery _asyncQuery1 = new AsyncQuery();
            _asyncQuery1.setModel(this);
            _asyncQuery1.asyncCallback = new INewAsyncCallback() {
                @Override
                public void OnSuccess(Object model1, Object result1)
                {
                    PrivateAsyncCallback privateAsyncCallback1 = (PrivateAsyncCallback) model1;
                    privateAsyncCallback1.ApplySearchPageSize((Integer) result1);
                }
            };
            AsyncDataProvider.GetSearchResultsLimit(_asyncQuery1);
        }

        public void RequestSearch()
        {
            searchRequested = true;
            model.setItems(new ArrayList());
            model.getSelectedItemChangedEvent().raise(this, new EventArgs());
            model.getSelectedItemsChangedEvent().raise(this, new EventArgs());
        }

        private void ApplySearchPageSize(int value)
        {
            model.setSearchPageSize(value);

            // If there search was requested before max result limit was retrieved, do it now.
            if (searchRequested && !model.getTimer().isActive())
            {
                model.getSearchCommand().Execute();
            }

            // Sure paging functionality.
            model.UpdatePagingAvailability();
        }
    }

    // ////////////////////////////
    // GridController methods
    // ///////////////////////////

    @Override
    public String getId() {
        return getListName();
    }

}
