package asia.cnjhb.blockshortcuts.hook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;

import asia.cnjhb.blockshortcuts.common.Config;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 运行在 system_server 里的配置通道：
 * 1. 接收 App 广播（App 启动/点击“应用”时推送，立即生效）；
 * 2. 启动后的 10 分钟内轮询 App 的 ContentProvider（兜底，例如 App 从未打开过）；
 * 3. 收到按键事件时按需重新拉取（最多每 10 秒一次，且放到后台线程，不会阻塞按键分发）。
 * 4. 把生效状态写回 Settings.Global，供 App 显示。
 */
public final class ConfigBridge {

    public static final String ACTION_APPLY = "asia.cnjhb.blockshortcuts.action.APPLY_CONFIG";
    public static final String EXTRA_JSON = "json";
    public static final String SETTINGS_KEY = "blockshortcuts_heartbeat";
    public static final String PROVIDER_AUTHORITY = "asia.cnjhb.blockshortcuts.config";
    private static final String COLUMN_JSON = "json";

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean REFRESHING = new AtomicBoolean(false);
    private static final long STARTUP_POLL_INTERVAL = 15_000L;
    private static final long STARTUP_POLL_WINDOW = 10 * 60_000L;
    private static final long EVENT_REFRESH_INTERVAL = 10_000L;

    private static Context appContext;
    private static Handler worker;
    private static volatile long lastRefresh;

    private ConfigBridge() {
    }

    public static void start(Context context) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        appContext = context.getApplicationContext();
        HandlerThread thread = new HandlerThread("BlockShortcuts-config");
        thread.start();
        worker = new Handler(thread.getLooper());

        try {
            appContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    applyJson(intent.getStringExtra(EXTRA_JSON), "broadcast");
                }
            }, new IntentFilter(ACTION_APPLY), Context.RECEIVER_EXPORTED);
            Logx.i("config receiver registered");
        } catch (Throwable t) {
            Logx.w("registerReceiver failed: " + t);
        }

        worker.post(new Runnable() {
            @Override
            public void run() {
                long deadline = System.currentTimeMillis() + STARTUP_POLL_WINDOW;
                boolean done = false;
                while (!done && System.currentTimeMillis() < deadline) {
                    done = refresh();
                    if (done) {
                        break;
                    }
                    try {
                        Thread.sleep(STARTUP_POLL_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                Logx.i("startup config poll finished, applied=" + done);
            }
        });
        writeHeartbeat();
    }

    /** 按键事件里调用：只在后台线程做限频拉取，不阻塞按键处理。 */
    public static void maybeRefresh() {
        Handler handler = worker;
        Context context = appContext;
        if (handler == null || context == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastRefresh < EVENT_REFRESH_INTERVAL || REFRESHING.get()) {
            return;
        }
        if (!REFRESHING.compareAndSet(false, true)) {
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    refresh();
                } finally {
                    REFRESHING.set(false);
                }
            }
        });
    }

    private static void applyJson(String json, String source) {
        Config parsed = Config.parse(json);
        if (parsed == null) {
            Logx.w("ignore invalid config from " + source + ": " + json);
            return;
        }
        PolicyEngine.setConfig(parsed);
        Logx.i("config applied from " + source + ": " + parsed.summary());
        writeHeartbeat();
    }

    private static boolean refresh() {
        Context context = appContext;
        if (context == null) {
            return false;
        }
        Cursor cursor = null;
        try {
            lastRefresh = System.currentTimeMillis();
            cursor = context.getContentResolver()
                    .query(Uri.parse("content://" + PROVIDER_AUTHORITY), null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return false;
            }
            int index = cursor.getColumnIndex(COLUMN_JSON);
            if (index < 0) {
                return false;
            }
            applyJson(cursor.getString(index), "provider");
            return true;
        } catch (Throwable t) {
            Logx.w("provider query failed: " + t);
            return false;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void writeHeartbeat() {
        Context context = appContext;
        if (context == null) {
            return;
        }
        try {
            String value = System.currentTimeMillis() + "|" + PolicyEngine.config().summary();
            Settings.Global.putString(context.getContentResolver(), SETTINGS_KEY, value);
        } catch (Throwable t) {
            Logx.w("write heartbeat failed: " + t);
        }
    }
}