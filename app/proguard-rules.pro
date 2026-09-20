# Room & WorkManager
# Room generates _Impl classes dynamically loaded via reflection in Room.getGeneratedImplementation()
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.InputMerger {
    public <init>();
}
-keep class * implements androidx.startup.Initializer {
    public <init>();
}

# Jetpack Glance
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}
-keep class * extends androidx.glance.appwidget.GlanceAppWidget {
    public <init>();
}
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver {
    public <init>();
}

# Koin
-keep class * implements org.koin.core.component.KoinComponent { *; }
