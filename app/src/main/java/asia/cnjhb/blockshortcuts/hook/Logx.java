package asia.cnjhb.blockshortcuts.hook;

import android.util.Log;

import de.robv.android.xposed.XposedBridge;

/** 同时写 logcat 和 Vector/LSPosed 模块日志，方便排查。 */
final class Logx {

    static final String TAG = PolicyEngine.TAG;

    private Logx() {
    }

    static void i(String message) {
        Log.i(TAG, message);
        XposedBridge.log("BlockShortcuts: " + message);
    }

    static void w(String message) {
        Log.w(TAG, message);
        XposedBridge.log("BlockShortcuts: " + message);
    }
}