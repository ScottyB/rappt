menuLayout(actionItems) ::= <<
<menu xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:app="http://schemas.android.com/apk/res-auto">
$actionItems :{ item |
  <item
      android:id="@+id/$item.functionName$"
      android:orderInCategory="100"
      app:showAsAction="always"
      $if(item.iconDrawable)$android:icon="@drawable/$item.iconDrawable$"$endif$
      android:title="@string/$item.stringId$"/>
}$
</menu>
>>
