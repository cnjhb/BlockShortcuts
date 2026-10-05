package asia.cnjhb.blockshortcuts.common;

import android.view.KeyEvent;

/**
 * 快捷键分组定义。模块在 system_server 中使用，App UI 也用于展示分组名称。
 */
public final class KeyGroups {

    public static final String NAV = "nav";
    public static final String META = "meta";
    public static final String VOLUME = "volume";
    public static final String POWER = "power";
    public static final String MEDIA = "media";
    public static final String FUNCTION = "function";
    public static final String COMBO = "combo";

    public static final String[] ALL = {NAV, META, VOLUME, POWER, MEDIA, FUNCTION, COMBO};

    private static final int[] NAV_KEYS = {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_SEARCH,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_VOICE_ASSIST,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_NOTIFICATION,
            KeyEvent.KEYCODE_STEM_PRIMARY,
    };

    private static final int[] META_KEYS = {
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT,
    };

    private static final int[] VOLUME_KEYS = {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
    };

    private static final int[] POWER_KEYS = {
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_SLEEP,
            KeyEvent.KEYCODE_WAKEUP,
    };

    private static final int[] MEDIA_KEYS = {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_EJECT,
    };

    private static final int[] FUNCTION_KEYS = {
            KeyEvent.KEYCODE_F1,
            KeyEvent.KEYCODE_F2,
            KeyEvent.KEYCODE_F3,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_F5,
            KeyEvent.KEYCODE_F6,
            KeyEvent.KEYCODE_F7,
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F9,
            KeyEvent.KEYCODE_F10,
            KeyEvent.KEYCODE_F11,
            KeyEvent.KEYCODE_F12,
            KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_SYSRQ,
            KeyEvent.KEYCODE_BREAK,
            KeyEvent.KEYCODE_SCROLL_LOCK,
            KeyEvent.KEYCODE_NUM_LOCK,
            KeyEvent.KEYCODE_INSERT,
    };

    /** 需要"必须按下"的修饰键组合（修饰键本身不屏蔽，只屏蔽组合）。 */
    private static final int[][] COMBOS = {
            // keyCode, 必须按下的 meta 位, 必须未按下的 meta 位
            {KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON, 0},
            {KeyEvent.KEYCODE_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON, 0},
            {KeyEvent.KEYCODE_TAB, KeyEvent.META_ALT_ON, 0},
            {KeyEvent.KEYCODE_TAB, KeyEvent.META_CTRL_ON, 0},
            {KeyEvent.KEYCODE_SPACE, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_0, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_1, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_2, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_3, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_4, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_5, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_6, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_7, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_8, KeyEvent.META_META_ON, 0},
            {KeyEvent.KEYCODE_9, KeyEvent.META_META_ON, 0},
    };

    private KeyGroups() {
    }

    public static boolean isGroup(String group) {
        for (String g : ALL) {
            if (g.equals(group)) {
                return true;
            }
        }
        return false;
    }

    public static boolean inGroup(String group, int keyCode) {
        int[] keys;
        if (NAV.equals(group)) {
            keys = NAV_KEYS;
        } else if (META.equals(group)) {
            keys = META_KEYS;
        } else if (VOLUME.equals(group)) {
            keys = VOLUME_KEYS;
        } else if (POWER.equals(group)) {
            keys = POWER_KEYS;
        } else if (MEDIA.equals(group)) {
            keys = MEDIA_KEYS;
        } else if (FUNCTION.equals(group)) {
            keys = FUNCTION_KEYS;
        } else {
            return false;
        }
        for (int k : keys) {
            if (k == keyCode) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesCombo(int keyCode, int metaState) {
        for (int[] combo : COMBOS) {
            if (combo[0] != keyCode) {
                continue;
            }
            int required = combo[1];
            int forbidden = combo[2];
            if ((metaState & required) == required && (metaState & forbidden) == 0) {
                return true;
            }
        }
        return false;
    }

    /** 把当前启用的分组展开成会被拦截的键码集合（不含组合键）。 */
    public static java.util.Set<Integer> expandKeys(java.util.Collection<String> groups) {
        java.util.Set<Integer> out = new java.util.HashSet<>();
        for (String g : groups) {
            if (NAV.equals(g)) {
                addAll(out, NAV_KEYS);
            } else if (META.equals(g)) {
                addAll(out, META_KEYS);
            } else if (VOLUME.equals(g)) {
                addAll(out, VOLUME_KEYS);
            } else if (POWER.equals(g)) {
                addAll(out, POWER_KEYS);
            } else if (MEDIA.equals(g)) {
                addAll(out, MEDIA_KEYS);
            } else if (FUNCTION.equals(g)) {
                addAll(out, FUNCTION_KEYS);
            }
        }
        return out;
    }

    private static void addAll(java.util.Set<Integer> out, int[] keys) {
        for (int k : keys) {
            out.add(k);
        }
    }
}