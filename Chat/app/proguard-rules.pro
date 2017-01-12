# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/ankurtibrewal/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes Signature
-keepclassmembers class com.nuggetchat.lib.model.** {
    public *;
}

-keepclassmembers class com.nuggetchat.messenger.datamodel.** {
    public *;
}

-keep class com.cloudinary.** { *; }
-keepnames class com.fasterxml.jackson.annotation.** { *; }

-dontwarn org.w3c.dom.**
-dontwarn java.awt.Desktop
-dontwarn java.awt.Desktop$Action
-dontwarn sun.misc.Unsafe
-dontwarn org.slf4j.LoggerFactory
-dontwarn org.slf4j.Logger
