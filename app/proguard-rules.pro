# Glance instantiates ActionCallbacks by class name
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# WorkManager instantiates workers by class name
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }

# kotlinx.serialization: keep generated serializers of our @Serializable classes
-keepclassmembers @kotlinx.serialization.Serializable class flashman.einundzwanzig.widget.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class flashman.einundzwanzig.widget.**$$serializer { *; }
