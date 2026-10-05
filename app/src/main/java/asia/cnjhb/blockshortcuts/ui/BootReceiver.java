package asia.cnjhb.blockshortcuts.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import asia.cnjhb.blockshortcuts.common.Config;
import asia.cnjhb.blockshortcuts.hook.ConfigBridge;

/** 开机后把配置重新推给 system_server（开机时 App 不会被启动）。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        Config config = Config.load(context);
        ConfigSender.send(context, config);
    }
}