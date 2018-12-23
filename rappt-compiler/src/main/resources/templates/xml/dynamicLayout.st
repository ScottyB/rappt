dynamicLayout(showFields, views, hasMessage, cardLayout, hasHeader, pullToRefresh, enableScroll, elements) ::= <<
<?xml version="1.0" encoding="utf-8"?>
$if(enableScroll)$
<ScrollView
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent"
        >$endif$

<LinearLayout
    $if(!enableScroll)$xmlns:android="http://schemas.android.com/apk/res/android"$endif$
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="10dp"
$if(cardLayout)$
    android:background="@drawable/$cardLayout$"
    android:layout_marginLeft="10dp"
    android:layout_marginRight="10dp"
    android:layout_marginTop="10dp"
    android:paddingTop="10dp"
    android:paddingLeft="10dp"
    android:paddingRight="10dp"
    android:paddingBottom="20dp"
$endif$
    >
$if(pullToRefresh)$
    <uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout
            android:id="@+id/pullToRefreshLayout"
            android:layout_width="match_parent"
            android:layout_height="match_parent">
$endif$
$if(hasHeader)$<include layout="@layout/header"/>$endif$
$if(hasMessage)$<include layout="@layout/message"/>$endif$

$showFields.fieldPaths:{ f |
$if(f.isImage)$
  <ImageView
      android:layout_height="wrap_content"
      android:layout_width="wrap_content"
      android:layout_marginRight="10dp"
      android:id="@+id/$f.id$"/>
$else$
  <TextView
      android:id="@+id/$f.id$"
      android:layout_width="fill_parent"
      android:layout_height="wrap_content"
      android:textSize="18dp"
      android:textColor="@android:color/background_dark"
      />
$endif$
}$

$views: {v |

<ListView
    	android:id="@+id/$v.id$"
    	android:layout_width="fill_parent"
    	android:layout_height="wrap_content" />
}$

$elements: { e |
$templateType(e)$
}$

$if(pullToRefresh)$</uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout>$endif$

</LinearLayout>
$if(enableScroll)$</ScrollView>$endif$

>>
