# Xposed 入口类由 assets/xposed_init 以字符串形式加载，必须保留类名与成员
-keep class asia.cnjhb.blockshortcuts.hook.HookEntry { *; }
-keep class asia.cnjhb.blockshortcuts.hook.** { *; }

# 传统 Xposed API 由框架提供，运行期反射调用，保留签名
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**
-keep class android.app.AndroidAppHelper { *; }
