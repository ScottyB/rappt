adb shell am broadcast -a "com.google.android.gms.gcm.ACTION_TRIGGER_TASK" -e component "com.rappt.architectural.app/.logic.SyncService_" -e tag "sync_tag"
