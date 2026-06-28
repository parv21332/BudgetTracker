# Add project specific ProGuard rules here.

# Keep Room entities
-keep class com.budgettracker.app.data.model.** { *; }

# Keep MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Keep iText PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Keep Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# Keep ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Repository classes
-keep class com.budgettracker.app.data.repository.** { *; }

# General rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
