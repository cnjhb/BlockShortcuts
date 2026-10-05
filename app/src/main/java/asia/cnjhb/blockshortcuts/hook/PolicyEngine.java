package asia.cnjhb.blockshortcuts.hook;

import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import asia.cnjhb.blockshortcuts.common.Config;
import asia.cnjhb.blockshortcuts.common.KeyGroups;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 拦截判定核心：判断某个 KeyEvent 是否应该被 BlockShortcuts 吃掉。
 */
public final class PolicyEngine {

    static final String TAG = "BlockShortcuts";

    public static final int PASS = 0;
    public static final int BLOCK = 1;

    private static volatile Set<Integer> blockedKeys = Collections.emptySet();
    private static volatile Config config = Config.createDefault();

    private PolicyEngine() {
    }

    static void setConfig(Config value) {
        config = value;
        Set<Integer> keys = KeyGroups.expandKeys(value.groupSet());
        keys.addAll(value.customKeys);
        blockedKeys = Collections.unmodifiableSet(new HashSet<>(keys));
    }

    public static Config config() {
        return config;
    }

    /**
     * @param event       按键事件
     * @param queueStage  true = interceptKeyBeforeQueueing 阶段；false = interceptKeyBeforeDispatching 阶段
     */
    public static int decide(KeyEvent event, boolean queueStage) {
        Config cfg = config;
        if (cfg == null || !cfg.enabled) {
            return PASS;
        }
        if (queueStage && !cfg.blockQueueing) {
            return PASS;
        }
        if (event == null) {
            return PASS;
        }
        if (cfg.hardwareOnly && isVirtualKeyboard(event)) {
            return PASS;
        }

        int keyCode = event.getKeyCode();
        if (cfg.allowKeys.contains(keyCode)) {
            return PASS;
        }

        int metaState = event.getMetaState();
        boolean hit = blockedKeys.contains(keyCode);
        if (!hit && cfg.groups.contains(KeyGroups.COMBO)) {
            hit = KeyGroups.matchesCombo(keyCode, metaState);
        }
        if (!hit) {
            return PASS;
        }
        if (cfg.debug) {
            Logx.i( "block " + (queueStage ? "queue " : "dispatch ")
                    + " key=" + keyCode + " name=" + KeyEvent.keyCodeToString(keyCode)
                    + " meta=0x" + Integer.toHexString(metaState)
                    + " action=" + event.getAction()
                    + " device=" + event.getDeviceId());
        }
        return BLOCK;
    }

    /**
     * 软键盘注入的事件 deviceId 为 KeyCharacterMap.VIRTUAL_KEYBOARD(-1)，
     * InputDevice 也被标记为虚拟设备。实体/外接键盘两者都不是。
     */
    static boolean isVirtualKeyboard(KeyEvent event) {
        if (event.getDeviceId() == KeyCharacterMap.VIRTUAL_KEYBOARD) {
            return true;
        }
        InputDevice device = event.getDevice();
        return device != null && device.isVirtual();
    }
}