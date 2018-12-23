contentCollection(collection) ::= <<

    @AfterViews
    void bindAdapter() {
        if (adapter.hasNoData()) {
            $if(collection.hasPullToRefresh)$setupActionBarPullToRefresh();
            loadingStarted();
            $else$
            message.setText(R.string.$collection.messageLoading$);
            $endif$
            fetchData();
        } else {
            message.setVisibility(View.GONE);
        }
         $collection.listId$.setAdapter(adapter);
    }


$if(collection.hasPullToRefresh)$
   @ViewById
    PullToRefreshLayout pullToRefreshLayout;

    @UiThread
    void loadingCompleted() {
        pullToRefreshLayout.setRefreshComplete();
    }

    void loadingStarted() {
        pullToRefreshLayout.setRefreshing(true);
    }

    @Override
    public void onRefreshStarted(View view) {
        fetchData();
    }

    public void setupActionBarPullToRefresh() {
        ActionBarPullToRefresh.from($if(collection.isFragment)$getActivity()$else$this$endif$)
                .allChildrenArePullable()
                .listener(this)
                .setup(pullToRefreshLayout);
    }
$endif$
>>
