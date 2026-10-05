package asia.cnjhb.blockshortcuts.hook;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.view.KeyEvent;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed 模块入口：只在 system_server（包名 "android"）中挂钩按键拦截逻辑。
 */
public final class HookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TARGET_PACKAGE = "android";
    private static final String POLICY_CLASS = "com.android.server.policy.PhoneWindowManager";

    private volatile boolean configStarted;

    @Override
    public void initZygote(StartupParam startupParam) {
        log("initZygote: startsSystemServer=" + startupParam.startsSystemServer
                + " modulePath=" + startupParam.modulePath);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!TARGET_PACKAGE.equals(loadPackageParam.packageName)) {
            return;
        }
        log("handleLoadPackage: " + loadPackageParam.processName + ", classLoader="
                + loadPackageParam.classLoader);
        try {
            installPolicyHooks(loadPackageParam);
        } catch (Throwable t) {
            log("install policy hooks failed: " + t);
            t.printStackTrace();
        }
    }

    private void installPolicyHooks(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        ClassLoader classLoader = lpparam.classLoader;
        Class<?> policy = XposedHelpers.findClass(POLICY_CLASS, classLoader);

        Set<XC_MethodHook.Unhook> dispatchHooks = XposedBridge.hookAllMethods(policy,
                "interceptKeyBeforeDispatching", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        ensureConfigStarted();
                        ConfigBridge.maybeRefresh();
                        Object event = argAt(param, 1);
                        if (event instanceof KeyEvent
                                && PolicyEngine.decide((KeyEvent) event, false) == PolicyEngine.BLOCK) {
                            param.setResult(consumedResult(param.method));
                        }
                    }
                });

        Set<XC_MethodHook.Unhook> queueHooks = XposedBridge.hookAllMethods(policy,
                "interceptKeyBeforeQueueing", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        ensureConfigStarted();
                        ConfigBridge.maybeRefresh();
                        Object event = argAt(param, 0);
                        if (event instanceof KeyEvent
                                && PolicyEngine.decide((KeyEvent) event, true) == PolicyEngine.BLOCK) {
                            // interceptKeyBeforeQueueing 返回 0 表示事件不再传递给前台应用。
                            param.setResult(0);
                        }
                    }
                });

        log("hooks installed: interceptKeyBeforeDispatching=" + describe(dispatchHooks)
                + ", interceptKeyBeforeQueueing=" + describe(queueHooks));

        if (dispatchHooks.isEmpty() && queueHooks.isEmpty()) {
            log("WARNING: 未找到任何按键拦截方法，ROM 版本可能不兼容");
        }

        try {
            XposedBridge.hookAllMethods(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object application = param.thisObject;
                    if (application instanceof Application) {
                        startConfig((Application) application);
                    }
                }
            });
        } catch (Throwable t) {
            log("hook Application.onCreate failed: " + t);
        }
    }

    private void ensureConfigStarted() {
        if (configStarted) {
            return;
        }
        try {
            Application application = AndroidAppHelper.currentApplication();
            if (application != null) {
                startConfig(application);
            }
        } catch (Throwable t) {
            log("AndroidAppHelper failed: " + t);
        }
    }

    private synchronized void startConfig(Application application) {
        if (configStarted) {
            return;
        }
        configStarted = true;
        try {
            ConfigBridge.start(application);
        } catch (Throwable t) {
            configStarted = false;
            log("ConfigBridge.start failed: " + t);
        }
    }

    private static Object argAt(XC_MethodHook.MethodHookParam param, int index) {
        Object[] args = param.args;
        return args != null && index < args.length ? args[index] : null;
    }

    /**
     * Android 16 的 interceptKeyBeforeDispatching 返回 boolean（true = 消费掉，不再派发给应用）；
     * 旧版本返回 int，1 (INTERCEPT_ACTION) 同样是消费。
     */
    private static Object consumedResult(Member member) {
        if (member instanceof Method) {
            Class<?> type = ((Method) member).getReturnType();
            if (type == boolean.class || type == Boolean.class) {
                return Boolean.TRUE;
            }
        }
        return 1;
    }

    private static String describe(Set<XC_MethodHook.Unhook> unhooks) {
        if (unhooks == null || unhooks.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (XC_MethodHook.Unhook unhook : unhooks) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            Member member = unhook.getHookedMethod();
            if (member instanceof Method) {
                Method method = (Method) member;
                sb.append(method.getReturnType().getSimpleName()).append(' ').append(method.getName())
                        .append(java.util.Arrays.toString(method.getParameterTypes()));
            } else {
                sb.append(member);
            }
        }
        return sb.toString();
    }

    private static void log(String message) {
        Logx.i(message);
    }
}