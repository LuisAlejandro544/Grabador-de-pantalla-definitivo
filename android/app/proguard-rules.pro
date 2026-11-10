# ===================================================================
# Reglas de Optimización y ProGuard para VeloRec Studio (Release)
# ===================================================================

# 1. Erradicar llamadas a android.util.Log en compilaciones Release (Cero overhead de logcat)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 2. Preservar firmas y métodos nativos JNI (C++23 y Rust Edition 2024)
# Es VITAL no ofuscar ni eliminar métodos 'native' ni clases que interactúan con ANativeWindow / AMediaCodec
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.velorec.recorder.ScreenRecordService {
    public *;
    native <methods>;
}

# 3. Modelos de datos serializados y Server-Driven UI
-keep class com.velorec.recorder.data.model.** { *; }

# 4. Reglas de Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    void <init>(androidx.compose.runtime.Composer, int);
}

# 5. Atributos esenciales para reflexión segura y stacktraces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 6. Desactivar advertencias de paquetes opcionales
-dontwarn java.lang.invoke.**
