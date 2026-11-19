# Futuristic Sounds — R8 keep rules

# AccessibilityService क्लास AndroidManifest में नाम से रेफ़र होती है, इसे पूरा रखें
-keep class com.futuristic.sounds.FuturisticService { *; }

# BroadcastReceiver (inner class) और इसके onReceive को भी रखें
-keep class com.futuristic.sounds.FuturisticService$* { *; }

# enum values()/valueOf() को R8 सही से हैंडल करे, इसके लिए सामान्य नियम
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Synth में फाइल-आधारित WAV जनरेशन है, कोई reflection नहीं — फिर भी data class जैसी चीज़ें सुरक्षित रखें
-keep class com.futuristic.sounds.SoundEvent { *; }

# javax.crypto (License.kt में इस्तेमाल) को लेकर R8 warnings न दे
-dontwarn javax.crypto.**

-dontwarn kotlin.**
