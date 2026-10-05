package asia.cnjhb.blockshortcuts.common;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 模块配置。以 JSON 字符串形式在 App 与 system_server 之间传递。
 */
public final class Config {

    public static final String PREFS = "blockshortcuts_config";
    public static final String KEY_JSON = "json";
    public static final int VERSION = 1;

    public boolean enabled = true;
    /** 仅拦截实体/外接键盘事件，软键盘（deviceId = -1）不受影响。 */
    public boolean hardwareOnly = true;
    /** 在入队阶段就拦截：应用层也收不到按键，副作用是屏幕关闭时无法用键盘唤醒。 */
    public boolean blockQueueing = false;
    public boolean debug = false;
    public final Set<String> groups = new LinkedHashSet<>(Arrays.asList(KeyGroups.ALL));
    public final List<Integer> customKeys = new ArrayList<>();
    public final List<Integer> allowKeys = new ArrayList<>();

    public static Config createDefault() {
        return new Config();
    }

    public static Config load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JSON, null);
        Config config = json == null ? null : parse(json);
        return config == null ? createDefault() : config;
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_JSON, toJson())
                .apply();
    }

    public String toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("enabled", enabled);
            obj.put("hardwareOnly", hardwareOnly);
            obj.put("blockQueueing", blockQueueing);
            obj.put("debug", debug);
            JSONArray g = new JSONArray();
            for (String s : groups) {
                g.put(s);
            }
            obj.put("groups", g);
            obj.put("customKeys", toArray(customKeys));
            obj.put("allowKeys", toArray(allowKeys));
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static Config parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject obj = new JSONObject(json);
            Config config = new Config();
            config.enabled = obj.optBoolean("enabled", true);
            config.hardwareOnly = obj.optBoolean("hardwareOnly", true);
            config.blockQueueing = obj.optBoolean("blockQueueing", false);
            config.debug = obj.optBoolean("debug", false);
            JSONArray g = obj.optJSONArray("groups");
            if (g != null) {
                config.groups.clear();
                for (int i = 0; i < g.length(); i++) {
                    String name = g.optString(i, null);
                    if (name != null && KeyGroups.isGroup(name)) {
                        config.groups.add(name);
                    }
                }
            }
            config.customKeys.clear();
            config.customKeys.addAll(parseKeys(obj.optJSONArray("customKeys")));
            config.allowKeys.clear();
            config.allowKeys.addAll(parseKeys(obj.optJSONArray("allowKeys")));
            return config;
        } catch (Exception e) {
            return null;
        }
    }

    private static JSONArray toArray(List<Integer> values) {
        JSONArray array = new JSONArray();
        for (Integer v : values) {
            array.put(v);
        }
        return array;
    }

    private static List<Integer> parseKeys(JSONArray array) {
        List<Integer> out = new ArrayList<>();
        if (array == null) {
            return out;
        }
        for (int i = 0; i < array.length(); i++) {
            int value = array.optInt(i, Integer.MIN_VALUE);
            if (value != Integer.MIN_VALUE && !out.contains(value)) {
                out.add(value);
            }
        }
        return out;
    }

    /** 解析用户输入的键码列表，例如 "24, 82, 999" */
    public static List<Integer> parseKeyList(String text) {
        List<Integer> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String part : text.split("[,，\\s]+")) {
            if (part.isEmpty()) {
                continue;
            }
            try {
                int value = Integer.parseInt(part.trim());
                if (!out.contains(value)) {
                    out.add(value);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public String summary() {
        return "enabled=" + enabled
                + " hardwareOnly=" + hardwareOnly
                + " blockQueueing=" + blockQueueing
                + " groups=" + groups
                + " customKeys=" + customKeys
                + " allowKeys=" + allowKeys
;
    }

    public Collection<String> groupSet() {
        return groups;
    }
}