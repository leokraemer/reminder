# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Yunlong\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
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

# view AndroidManifest.xml #generated:32
-keep class com.samsung.android.sdk.accessory.RegisterUponInstallReceiver { <init>(...); }

# view AndroidManifest.xml #generated:27
-keep class com.samsung.android.sdk.accessory.ServiceConnectionIndicationBroadcastReceiver { <init>(...); }

# view AndroidManifest.xml #generated:24
-keep class com.samsung.android.sdk.accessory.example.helloaccessoryprovider.service.HelloAccessoryProviderService { <init>(...); }

-keepclassmembers class com.samsung.** { *; }
-keep class com.samsung.** { *; }
-dontwarn com.samsung.**
-keepattributes InnerClasses
