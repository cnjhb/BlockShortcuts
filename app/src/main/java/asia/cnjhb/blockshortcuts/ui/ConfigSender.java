package asia.cnjhb.blockshortcuts.ui;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import asia.cnjhb.blockshortcuts.common.Config;
import asia.cnjhb.blockshortcuts.hook.ConfigBridge;

/** 把配置广播给 system_server 中的模块。 */
final class ConfigSender {

    private ConfigSender() {
    }

    static void send(Context context, Config config) {
        try {
            // 不能设置 setPackage：system_server 里注册的接收者归属包名是 "system"
            // 而不是 "android"，定向发送会导致匹配不到。使用隐式广播按 action 匹配。
            Intent intent = new Intent(ConfigBridge.ACTION_APPLY);
            intent.putExtra(ConfigBridge.EXTRA_JSON, config.toJson());
            context.sendBroadcast(intent);
            Log.i("BlockShortcuts", "config broadcast sent: " + config.summary());
        } catch (Throwable t) {
            Log.w("BlockShortcuts", "send config failed: " + t);
        }
    }
}