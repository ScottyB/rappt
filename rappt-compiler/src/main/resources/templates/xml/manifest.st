manifest(packageName, mainScreen, globalApplication, activities, callPermissions=false, mapKey=false, internetPermissions=false, locationPermission=false) ::= <<
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$packageName$">

$if(internetPermissions)$
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
$endif$

$if(callPermissions)$
<uses-permission android:name="android.permission.CALL_PHONE" />
$endif$

$if(locationPermission)$
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
$endif$

$if(mapKey)$
    <permission
       android:name="$packageName$.permission.MAPS_RECEIVE"
       android:protectionLevel="signature" />
    <uses-feature
       android:glEsVersion="0x00020000"
       android:required="true" />
    <uses-permission android:name="$packageName$.permission.MAPS_RECEIVE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="com.google.android.providers.gsf.permission.READ_GSERVICES"/>
$endif$
    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
$if(globalApplication)$
        android:name=".$globalApplication$_"
$endif$
        android:label="@string/app_name"
        android:theme="@style/CustomTheme">
        $activities :{activity |
        $if(activity.isLandingPage)$
        <activity
            android:label="@string/app_name"
            android:name=".activities.$activity.view.viewControllerName$_">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        $else$
        <activity
            $if(activity.stringTitleId)$android:label="@string/$activity.stringTitleId$"$endif$
            android:name=".activities.$activity.view.viewControllerName$_">
        </activity>
        $endif$
        }$
$if(mapKey)$
        <meta-data
            android:name="com.google.android.gms.version"
            android:value="@integer/google_play_services_version" />
        <meta-data
           android:name="com.google.android.maps.v2.API_KEY"
           android:value="$mapKey$" />
$endif$

    </application>
</manifest>
>>
