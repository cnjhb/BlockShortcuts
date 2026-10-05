package asia.cnjhb.blockshortcuts.ui;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import asia.cnjhb.blockshortcuts.common.Config;

/**
 * 让 system_server 里的模块能读到 App 的配置（App 未运行时也能恢复）。
 * 只读，无任何危险操作。
 *
 * <p>Provider 被 system_server 查询时，App 进程会被拉起；此时顺便把配置广播出去，
 * 这样开机后即使没有打开过设置界面，模块也能立刻拿到用户配置。
 */
public class ConfigProvider extends ContentProvider {

    private static final String COLUMN_JSON = "json";

    @Override
    public boolean onCreate() {
        push();
        return true;
    }

    private void push() {
        try {
            android.content.Context context = getContext();
            if (context != null) {
                ConfigSender.send(context, Config.load(context));
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{COLUMN_JSON});
        try {
            cursor.addRow(new Object[]{Config.load(getContext()).toJson()});
        } catch (Throwable t) {
            cursor.addRow(new Object[]{"{}"});
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.asia.cnjhb.blockshortcuts.config";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}