contentDrawerActivity(classModel) ::= <<

    $if(classModel.noSubHeader)$
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    $endif$

    private ActionBarDrawerToggle mDrawerToggle;
    private MenuItemAdapter mAdapter;

    @ViewById
    public DrawerLayout drawer_layout;

    @ViewById
    public ListView left_drawer;

    @ViewById
    ViewPager mViewPager;

    @AfterViews
    void afterViews() {
$if(classModel.noSubHeader)$mTitle = mDrawerTitle = getTitle();$endif$
        drawer_layout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mAdapter = new MenuItemAdapter(this, mViewPager);

            $classModel.tabs : {tab |
        mAdapter.addMenuItem($tab.className$_.class, R.string.$tab.stringId$, null);
            }$

        left_drawer.setAdapter(new ArrayAdapter<Object>(this, R.layout.drawer_list_item, mAdapter.mMenuItems.toArray()));
        left_drawer.setOnItemClickListener(new DrawerItemClickListener());

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawer_layout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
$if(classModel.noSubHeader)$getActionBar().setTitle(mTitle);$endif$
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
$if(classModel.noSubHeader)$getActionBar().setTitle(mDrawerTitle);$endif$
                invalidateOptionsMenu();
            }
        };
        drawer_layout.setDrawerListener(mDrawerToggle);
        left_drawer.setItemChecked(0, true);
$if(classModel.noSubHeader)$setTitle(mAdapter.mMenuItems.get(0).title);$endif$
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            left_drawer.setItemChecked(position, true);
            drawer_layout.closeDrawer(left_drawer);
            mAdapter.mViewPager.setCurrentItem(position);
$if(classModel.noSubHeader)$setTitle(mAdapter.mMenuItems.get(position).title);$endif$
        }
    }
$if(classModel.noSubHeader)$
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }
$endif$
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public class MenuItemAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener {
        private final Context mContext;

        final class MenuItemInfo {
            private final Bundle args;
            private final Class<?> aClass;
            private final String title;

            MenuItemInfo(Class<?> _class, String _title, Bundle _args) {
                aClass = _class;
                args = _args;
                title = _title;
            }

            @Override
            public String toString() {
                return title;
            }
        }

        private ViewPager mViewPager;
        private ArrayList<MenuItemInfo> mMenuItems = new ArrayList<MenuItemInfo>();

        public MenuItemAdapter(FragmentActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addMenuItem(Class<?> clss, int stringId, Bundle args) {
            MenuItemInfo info = new MenuItemInfo(clss, getResources().getString(stringId), args);
            mMenuItems.add(info);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mMenuItems.size();
        }

        @Override
        public Fragment getItem(int position) {
            MenuItemInfo info = mMenuItems.get(position);
            return Fragment.instantiate(mContext, info.aClass.getName(), info.args);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            left_drawer.setItemChecked(position, true);
$if(classModel.noSubHeader)$setTitle(mAdapter.mMenuItems.get(position).title);$endif$
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

    }
>>
