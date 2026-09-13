# Proguard rules for SAVESX2
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep core and UI model classes
-keep class xyz.mininxd.savesx2.core.** { *; }
-keep class xyz.mininxd.savesx2.model.** { *; }
-keep class xyz.mininxd.savesx2.ui.components.** { *; }

# Kotlin Coroutines internal factories
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Line numbers for clean stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
