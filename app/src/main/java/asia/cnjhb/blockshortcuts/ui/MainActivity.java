package asia.cnjhb.blockshortcuts.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import asia.cnjhb.blockshortcuts.R;
import asia.cnjhb.blockshortcuts.common.Config;
import asia.cnjhb.blockshortcuts.common.KeyGroups;
import asia.cnjhb.blockshortcuts.hook.ConfigBridge;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String REPO_URL = "https://github.com/cnjhb/BlockShortcuts";

    private SwitchCompat swEnabled;
    private SwitchCompat swHardwareOnly;
    private SwitchCompat swNav;
    private SwitchCompat swMeta;
    private SwitchCompat swVolume;
    private SwitchCompat swPower;
    private SwitchCompat swMedia;
    private SwitchCompat swFunction;
    private SwitchCompat swCombo;
    private SwitchCompat swQueue;
    private SwitchCompat swDebug;
    private EditText etCustom;
    private EditText etAllow;
    private TextView tvStatus;
    private MaterialCardView cardStatus;
    private View statusDot;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean suppressCallback;
    private final Runnable statusTask = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            handler.postDelayed(this, 3000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swEnabled = findViewById(R.id.sw_enabled);
        swHardwareOnly = findViewById(R.id.sw_hardware_only);
        swNav = findViewById(R.id.sw_nav);
        swMeta = findViewById(R.id.sw_meta);
        swVolume = findViewById(R.id.sw_volume);
        swPower = findViewById(R.id.sw_power);
        swMedia = findViewById(R.id.sw_media);
        swFunction = findViewById(R.id.sw_function);
        swCombo = findViewById(R.id.sw_combo);
        swQueue = findViewById(R.id.sw_queue);
        swDebug = findViewById(R.id.sw_debug);
        etCustom = findViewById(R.id.et_custom_keys);
        etAllow = findViewById(R.id.et_allow_keys);
        tvStatus = findViewById(R.id.tv_status);
        cardStatus = findViewById(R.id.card_status);
        statusDot = findViewById(R.id.v_status_dot);
        MaterialButton btnApply = findViewById(R.id.btn_apply);
        MaterialButton btnAbout = findViewById(R.id.btn_about);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        render(Config.load(this));

        View.OnClickListener listener = v -> {
            if (suppressCallback) {
                return;
            }
            apply(true);
        };
        swEnabled.setOnClickListener(listener);
        swHardwareOnly.setOnClickListener(listener);
        swNav.setOnClickListener(listener);
        swMeta.setOnClickListener(listener);
        swVolume.setOnClickListener(listener);
        swPower.setOnClickListener(listener);
        swMedia.setOnClickListener(listener);
        swFunction.setOnClickListener(listener);
        swCombo.setOnClickListener(listener);
        swQueue.setOnClickListener(listener);
        swDebug.setOnClickListener(listener);
        btnApply.setOnClickListener(v -> apply(true));
        btnAbout.setOnClickListener(v -> showAbout());

        // 打开界面即把已保存配置推给 system_server，避免用户以为要点按钮才生效
        ConfigSender.send(this, Config.load(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(statusTask);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(statusTask);
        super.onPause();
    }

    private void render(Config config) {
        suppressCallback = true;
        swEnabled.setChecked(config.enabled);
        swHardwareOnly.setChecked(config.hardwareOnly);
        swNav.setChecked(config.groups.contains(KeyGroups.NAV));
        swMeta.setChecked(config.groups.contains(KeyGroups.META));
        swVolume.setChecked(config.groups.contains(KeyGroups.VOLUME));
        swPower.setChecked(config.groups.contains(KeyGroups.POWER));
        swMedia.setChecked(config.groups.contains(KeyGroups.MEDIA));
        swFunction.setChecked(config.groups.contains(KeyGroups.FUNCTION));
        swCombo.setChecked(config.groups.contains(KeyGroups.COMBO));
        swQueue.setChecked(config.blockQueueing);
        swDebug.setChecked(config.debug);
        etCustom.setText(joinKeys(config.customKeys));
        etAllow.setText(joinKeys(config.allowKeys));
        suppressCallback = false;
    }

    private Config collect() {
        Config config = new Config();
        config.enabled = swEnabled.isChecked();
        config.hardwareOnly = swHardwareOnly.isChecked();
        config.blockQueueing = swQueue.isChecked();
        config.debug = swDebug.isChecked();
        config.groups.clear();
        addGroup(config, swNav, KeyGroups.NAV);
        addGroup(config, swMeta, KeyGroups.META);
        addGroup(config, swVolume, KeyGroups.VOLUME);
        addGroup(config, swPower, KeyGroups.POWER);
        addGroup(config, swMedia, KeyGroups.MEDIA);
        addGroup(config, swFunction, KeyGroups.FUNCTION);
        addGroup(config, swCombo, KeyGroups.COMBO);
        config.customKeys.addAll(Config.parseKeyList(String.valueOf(etCustom.getText())));
        config.allowKeys.addAll(Config.parseKeyList(String.valueOf(etAllow.getText())));
        return config;
    }

    private static void addGroup(Config config, SwitchCompat view, String group) {
        if (view.isChecked()) {
            config.groups.add(group);
        }
    }

    private void apply(boolean notify) {
        Config config = collect();
        config.save(this);
        ConfigSender.send(this, config);
        if (notify) {
            Toast.makeText(this, R.string.applied, Toast.LENGTH_SHORT).show();
        }
        handler.postDelayed(this::updateStatus, 500L);
    }

    private void updateStatus() {
        String heartbeat = readHeartbeat();
        if (TextUtils.isEmpty(heartbeat)) {
            paintStatus(R.color.status_bad);
            tvStatus.setText(R.string.status_offline);
            return;
        }
        int sep = heartbeat.indexOf('|');
        String ts = sep > 0 ? heartbeat.substring(0, sep) : heartbeat;
        String summary = sep > 0 ? heartbeat.substring(sep + 1) : "";
        long time = 0;
        try {
            time = Long.parseLong(ts);
        } catch (NumberFormatException ignored) {
        }
        long age = System.currentTimeMillis() - time;
        if (age > 10 * 60_000L) {
            paintStatus(R.color.status_warn);
            tvStatus.setText(getString(R.string.status_stale, age / 60000L));
            return;
        }
        paintStatus(R.color.status_ok);
        tvStatus.setText(getString(R.string.status_online, age / 1000L, summary));
    }

    private void paintStatus(int colorRes) {
        int color = ContextCompat.getColor(this, colorRes);
        cardStatus.setStrokeColor(color);
        ViewCompat.setBackgroundTintList(statusDot, ColorStateList.valueOf(color));
    }

    private void showAbout() {
        View content = getLayoutInflater().inflate(R.layout.dialog_about, null);

        String version = getString(R.string.about_version_unknown);
        String build = "";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = info.versionName + " (" + info.getLongVersionCode() + ")";
            boolean debuggable = (info.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            build = getString(debuggable ? R.string.about_build_debug : R.string.about_build_release);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        ((TextView) content.findViewById(R.id.tv_about_version))
                .setText(getString(R.string.about_version_line, version, build));
        ((TextView) content.findViewById(R.id.tv_about_system))
                .setText(getString(R.string.about_system_line,
                        Build.VERSION.RELEASE, Build.VERSION.SDK_INT));

        TextView aboutStatus = content.findViewById(R.id.tv_about_status);
        if (TextUtils.isEmpty(readHeartbeat())) {
            aboutStatus.setText(R.string.about_status_offline);
            aboutStatus.setTextColor(ContextCompat.getColor(this, R.color.status_bad));
        } else {
            aboutStatus.setText(R.string.about_status_online);
            aboutStatus.setTextColor(ContextCompat.getColor(this, R.color.status_ok));
        }

        content.findViewById(R.id.btn_repo).setOnClickListener(v -> openRepo());

        new MaterialAlertDialogBuilder(this)
                .setView(content, 24, 12, 24, 0)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void openRepo() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, R.string.about_no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    private String readHeartbeat() {
        try {
            return Settings.Global.getString(getContentResolver(), ConfigBridge.SETTINGS_KEY);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String joinKeys(List<Integer> keys) {
        if (keys == null || keys.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Integer key : keys) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(key);
        }
        return sb.toString();
    }
}