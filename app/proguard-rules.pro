# ProGuard rules for the Solitaire Klondike app

# Keep the application class
-keep class com.example.klondike.GameApplication { *; }

# Keep the main activity
-keep class com.example.klondike.MainActivity { *; }

# Keep all model classes
-keep class com.example.klondike.game.model.** { *; }

# Keep all engine classes
-keep class com.example.klondike.game.engine.** { *; }

# Keep all UI classes
-keep class com.example.klondike.game.ui.** { *; }

# Keep all utility classes
-keep class com.example.klondike.game.util.** { *; }

# Keep all resources
-keep class * extends android.content.res.Resources { *; }

# Keep all Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}