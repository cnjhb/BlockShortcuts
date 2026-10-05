# 屏蔽键盘快捷键 · Block Keyboard Shortcuts

一个 **LSPosed / JingMatrix Vector 模块**：在 system_server 中拦截外接实体键盘触发的系统级快捷键（音量、电源、导航、媒体、功能键、Win 键、系统组合键等）。

- 应用包名：`asia.cnjhb.blockshortcuts`
- 最低系统：Android 13（API 33）
- 已在 OnePlus PJD110 / LineageOS 23.2（Android 16, SDK 36）+ Vector v2.2 上验证

---

## 功能特性

| 能力 | 说明 |
| --- | --- |
| 导航键 | 主页 / 返回 / 菜单 / 最近任务 / 搜索 / 助手 / 相机 / 通知 |
| Win（Meta）键 | 单独 Win 键，以及 Win+数字、Win+空格等组合 |
| 音量键 | 加 / 减 / 静音 |
| 电源键 | 电源 / 休眠 / 唤醒（不影响息屏状态下的唤醒） |
| 媒体键 | 播放暂停 / 停止 / 弹出 / 上下曲 / 快进快退 |
| 功能键 | F1–F12、Esc、PrtSc、ScrollLock、NumLock、Pause、Insert |
| 系统组合键 | Ctrl+Alt+Del、Ctrl+Alt+Backspace、Alt+Tab、Ctrl+Tab、Win+数字、Win+空格 |
| 自定义键码 | 用 Android `KeyEvent` 键码数字补充屏蔽列表 |
| 白名单 | 列出的键码永远不被屏蔽，优先级最高 |
| 仅实体键盘 | 默认开启；软键盘输入与 `adb shell input keyevent` 等注入事件不受影响 |
| 入队阶段拦截 | 可选。进一步在按键入队前拦截，应用层也收不到；可禁用应用自实现的组合键（如浏览器 Ctrl+W） |
| 中英双语 | 界面跟随系统语言，可在系统「应用语言」中单独切换 |

界面为 Material 3 卡片式布局，自带深浅色配色与状态指示（绿=已注入、黄=心跳过期、红=离线）。

---

## 环境要求

- Android 13 及以上（`minSdk 33`）
- LSPosed 或 **JingMatrix Vector**（本项目在 Vector v2.2 上实测；使用传统 Xposed API `de.robv.android.xposed:api:82`）
- 设备已 root（仅安装/配置阶段需要，用于授权作用域）

---

## 安装

### 1. 编译

```bash
git clone <this-repo>
cd BlockShortcuts

# 调试包：11 MB 左右，未混淆，便于断点与查看日志
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# 发布包：R8 混淆 + 资源压缩，约 1.5 MB，建议日常使用这个
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

构建环境：JDK 17、Gradle 9.3.1（wrapper）、AGP 9.1.0、compileSdk 36.1、Material 1.10.0。
依赖只有 `androidx.core:core`、`androidx.appcompat`、`com.google.android.material`（代码全为 Java，不引入 Kotlin 运行时）。

### 2. 安装 APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. 在 Vector / LSPosed 中启用并勾选作用域

1. 打开管理器 → 模块 → 启用「屏蔽键盘快捷键」。
2. **作用域必须勾选「系统框架 / System Framework」**，否则模块不会注入 `system_server`，界面状态卡会显示红色「未检测到模块」。
3. 重启 `system_server` 或整机使模块加载。

### 4. 没有管理器时（纯 CLI，Vector）

```bash
adb shell
su
/data/adb/lspd/cli modules enable asia.cnjhb.blockshortcuts
/data/adb/lspd/cli scope add asia.cnjhb.blockshortcuts system/0
killall system_server
```

> ⚠️ **最大的坑**：Vector 数据库里 system_server 的作用域包名是 `system`，**不是 `android`**。
> `scope add asia.cnjhb.blockshortcuts android/0` 只会让模块注入自己的 App 进程，看起来"启用成功"但完全无效。

### 5. 覆盖安装后的注意事项 ⚠️

每次安装 APK，`base.apk` 路径都会变化，Vector 数据库中的记录会失效，表现为模块突然不生效。覆盖安装后需要同步路径并重启：

```bash
NEW=$(adb shell pm path asia.cnjhb.blockshortcuts | sed 's/package://' | tr -d '\r')
adb shell "sqlite3 /data/adb/lspd/config/modules_config.db \
  \"update modules set apk_path='$NEW' where module_pkg_name='asia.cnjhb.blockshortcuts';\""
adb shell killall system_server
```

---

## 配置项

界面中的开关即为配置项，**改动后自动同步**（广播 + ContentProvider 双通道），无需手动点按钮。配置保存在应用私有 `SharedPreferences`（`blockshortcuts_config`）中。

| 配置 | 默认 | 说明 |
| --- | --- | --- |
| 启用屏蔽 | 开 | 总开关 |
| 仅拦截实体键盘 | 开 | 只拦截 `deviceId != VIRTUAL_KEYBOARD(-1)` 的真实硬件事件 |
| 快捷键分组 | 全开 | 导航 / Win / 音量 / 电源 / 媒体 / 功能 / 系统组合 |
| 补充屏蔽键码 | 空 | 逗号分隔的键码数字，如 `24, 82` |
| 白名单键码 | 空 | 逗号分隔的键码数字，如 `111` |
| 入队阶段拦截 | 关 | 额外 hook `interceptKeyBeforeQueueing`，返回 `0` 消费按键 |
| 记录拦截日志 | 关 | 输出到 logcat 与 Xposed 日志 |

> 注意：`adb shell input keyevent` 注入的事件属于虚拟键盘（`deviceId = -1`），在默认「仅拦截实体键盘」下**不会**被拦截。做自动化验证时需要先在界面上关掉该选项。

---

## 工作原理

### Hook 点（均在 `com.android.server.policy.PhoneWindowManager`）

| 方法 | 返回值语义 | 处理 |
| --- | --- | --- |
| `interceptKeyBeforeDispatching(IBinder, KeyEvent)` | `true` = 消费 | 设置结果为 `true`（不同 ROM 为 `int` 时设 `1`），按键不再派发给应用 |
| `interceptKeyBeforeQueueing(KeyEvent, int)` | `0` = 消费 | 仅在「入队阶段拦截」开启时 hook，返回 `0` |

两个方法通过反射 + `XposedBridge.hookAllMethods` 挂载，兼容返回 `boolean` 或 `int` 两种签名。

### 实体键盘判定

```java
event.getDeviceId() == KeyCharacterMap.VIRTUAL_KEYBOARD  // -1
InputDevice.isVirtual(deviceId)                          // 兜底
```

### 配置下发与心跳

`system_server` 无法直接读取其他应用的 `SharedPreferences`，因此使用三条通道互为备份：

1. **广播**：`asia.cnjhb.blockshortcuts.action.APPLY_CONFIG`（extra `json`），App 每次改动开关、开机（`BOOT_COMPLETED`）时发送。
   > 广播**不能**调用 `setPackage("android")`：`system_server` 中运行时注册的接收者归属包名是 `system`，定向到 `android` 会匹配不到。
2. **ContentProvider**：`content://asia.cnjhb.blockshortcuts.config`，模块在启动后每 15s 轮询（最长 10 分钟），并在每次按键事件时按 10s 节流刷新。
3. **心跳**：模块把 `时间戳|enabled=… groups=[…]` 写入 `Settings.Global` 的 `blockshortcuts_heartbeat`，界面据此显示运行状态。

---

## 验证与排错

```bash
# 1. 心跳：能读到且时间戳新鲜 = 模块正在 system_server 中运行
adb shell settings get global blockshortcuts_heartbeat

# 2. Xposed / Vector 日志
adb shell "cat /data/adb/lspd/log/modules_*.log" | grep BlockShortcuts

# 3. logcat（需在界面开启「记录拦截日志」）
adb logcat -s BlockShortcuts

# 4. 确认作用域
adb shell "/data/adb/lspd/cli scope ls asia.cnjhb.blockshortcuts"
```

正常日志：

```
initZygote: startsSystemServer=true
handleLoadPackage: android
hooks installed: interceptKeyBeforeDispatching=…, interceptKeyBeforeQueueing=…
config applied from broadcast: enabled=true hardwareOnly=true …
```

| 现象 | 排查方向 |
| --- | --- |
| 界面红色「未检测到模块」 | 作用域未勾选系统框架，或数据库里是 `android/0` 而非 `system/0` |
| 覆盖安装后失效 | `modules.apk_path` 未同步，见上文第 5 步 |
| 界面状态正常但按键没被拦 | 仍开着「仅拦截实体键盘」，而事件来自软键盘 / `input keyevent` |
| 息屏后键盘无法唤醒 | 「入队阶段拦截」的预期副作用，关掉即可 |
| `Failed to find provider info … (user not unlocked)` | 开机未解锁导致 Provider 不可用，属正常现象，解锁后会自动恢复 |

---

## 开发

### 工程结构

```
app/src/main/
├── AndroidManifest.xml                  # 模块元数据、ConfigProvider、BootReceiver
├── assets/xposed_init                   # 入口类：asia.cnjhb.blockshortcuts.hook.HookEntry
├── java/asia/cnjhb/blockshortcuts/
│   ├── hook/
│   │   ├── HookEntry.java               # Xposed 入口，挂载 PhoneWindowManager
│   │   ├── PolicyEngine.java            # 拦截判定、白名单、实体键盘识别
│   │   ├── ConfigBridge.java            # 广播接收 / Provider 轮询 / 心跳写入
│   │   └── Logx.java                    # 同时写 logcat 与 Xposed 日志
│   ├── common/
│   │   ├── Config.java                  # 配置模型、JSON、SharedPreferences 持久化
│   │   └── KeyGroups.java               # 分组 → 键码集合、修饰键组合表
│   └── ui/
│       ├── MainActivity.java            # Material 3 配置界面
│       ├── ConfigProvider.java          # 供 system_server 查询配置
│       ├── ConfigSender.java            # 广播下发配置
│       └── BootReceiver.java            # 开机重新下发配置
└── res/
    ├── layout/activity_main.xml
    ├── values/strings.xml               # 英文（默认）
    ├── values-zh-rCN/strings.xml        # 简体中文
    ├── values/values-night/             # 深色配色与主题
    └── drawable/ic_*.xml                # 12 个矢量图标
```

### 多语言

- `res/values/strings.xml` 为**默认英文**，`res/values-zh-rCN/strings.xml` 为简体中文。
- `res/xml/locales_config.xml` 声明支持的语言（`en`、`zh-CN`），因此系统「应用语言」里可以单独切换本模块语言。
- 新增语言：复制 `values/strings.xml` 为 `values-xx/strings.xml` 并翻译，同时在 `locales_config.xml` 中登记。

### APK 体积

| 构建 | 体积 | 说明 |
| --- | --- | --- |
| `assembleDebug` | ≈ 11 MB | 未混淆，`classes.dex` 约 9.5 MB |
| `assembleRelease` | ≈ 1.5 MB | R8 混淆 + `isShrinkResources`，dex 约 740 KB |

体积主要来自 `com.google.android.material:material`：它带有 Kotlin 写的实现，debug 包会把这些代码原样打进 dex。想更小可以裁掉用不到的 Material 组件，或改用 `androidx.appcompat` + 手写样式。

### 发布签名

`release` 构建默认回退到 `~/.android/debug.keystore`，便于本地直接出包；要正式签名请在 `local.properties`（已被 gitignore）中配置：

```properties
RELEASE_STORE_FILE=/path/to/your.jks
RELEASE_KEY_ALIAS=your-alias
RELEASE_STORE_PASSWORD=***
RELEASE_KEY_PASSWORD=***
```

也支持环境变量 `BS_STORE_FILE` / `BS_KEY_ALIAS` / `BS_STORE_PASSWORD` / `BS_KEY_PASSWORD`，以及 AGP 社区惯例写法 `storeFile` / `keyAlias` / `storePassword` / `keyPassword`（CI 里常用）。

首次发布前先生成一套正式密钥并永久保管——之后所有版本必须用同一把签名，否则用户无法覆盖升级：

```bash
keytool -genkeypair -v -keystore blockshortcuts.jks -alias blockshortcuts \
        -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 blockshortcuts.jks > blockshortcuts.jks.b64   # 贴进 CI 密钥
```

### 提交到 LSPosed 模块仓库

模块列表（<https://modules.lsposed.org>）的数据来自 `Xposed-Modules-Repo` 组织下的仓库，更新包则从**仓库的 GitHub Release 资产**下载。本仓库已按该格式准备：

| 文件 / 机制 | 作用 |
| --- | --- |
| `SUMMARY` | 根目录单行描述，模块列表展示用 |
| `SOURCE_URL` | 指向本源码仓库 |
| `README.md` / `LICENSE` | 说明与许可证（GPL-3.0） |
| `.github/workflows/android.yml` | 构建签名 release APK 并上传为 Release 资产 |
| Release tag 命名 | `<提交 issue 号>-<版本号>`，如 `1289-1.7.0`，LSPosed 依赖这个格式识别版本 |

一次完整发布的流程：

1. 在 <https://github.com/Xposed-Modules-Repo/submission> 新建 issue，填写包名 `asia.cnjhb.blockshortcuts` 与说明。
2. 在本仓库添加仓库 Secrets：
   - `SIGNING_KEY`：`blockshortcuts.jks.b64` 的内容
   - `KEY_STORE_PASSWORD` / `ALIAS` / `KEY_PASSWORD`
3. 添加仓库变量 `LSPOSED_ISSUE_NUMBER`（上一步拿到的 issue 号），或手动触发 workflow 时在 `issue_number` 输入框填写。
4. 改 `versionName` → 推送，CI 自动构建并发布 `BlockShortcuts_<版本>.apk` 到 tag `<issue号>-<版本号>` 的 Release。

包名归属验证：官方快速通道是在 App ID 对应域名的根域添加 TXT 记录 `lsposed-modules-repo-verification=<GitHub 用户名>`。若没有自有域名，只能在 issue 中说明并等待维护者人工审核。

### 依赖来源

```kotlin
// settings.gradle.kts
maven("https://api.xposed.info/") { content { includeGroup("de.robv.android.xposed") } }

// app/build.gradle.kts —— 传统 Xposed API 只在编译期可见
compileOnly(libs.xposed.api)
```

---

## 许可证

[GPL-3.0](LICENSE) — 你可以自由使用、修改与再分发，但衍生作品必须同样以 GPL-3.0 发布并保留版权声明。

## 已知限制

- 只处理**系统级**拦截点。应用自己实现的组合键（浏览器 Ctrl+W、终端 Ctrl+C 等）需要开启「入队阶段拦截」或屏蔽对应修饰键才能禁用。
- ROM 若重命名或移除 `PhoneWindowManager` 的相关方法，模块会记录 `WARNING: 未找到任何按键拦截方法`，此时无法生效。
- 模块需要 `system_server` 权限，重启 `system_server` 前若有未保存状态会丢失（正常现象）。

---

## English

**Block Keyboard Shortcuts** is an LSPosed / JingMatrix Vector module that swallows system-level shortcuts triggered by a physical keyboard — volume, power, navigation, media, function keys, the Win key and system combos.

**Requirements**: Android 13+, a rooted device, and LSPosed or JingMatrix Vector (tested on Vector v2.2).

**Install**

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# then in the manager: enable the module and tick the "System Framework" scope
```

Without a manager app (Vector CLI):

```bash
/data/adb/lspd/cli modules enable asia.cnjhb.blockshortcuts
/data/adb/lspd/cli scope add asia.cnjhb.blockshortcuts system/0   # note: "system", not "android"
killall system_server
```

**How it works**: the module hooks `PhoneWindowManager.interceptKeyBeforeDispatching` (consume the key) and optionally `interceptKeyBeforeQueueing` (returns `0`). Configuration is pushed to `system_server` by broadcast plus a `ContentProvider` fallback, and the module writes a heartbeat to `Settings.Global` (`blockshortcuts_heartbeat`) that the app UI displays.

**Verify**

```bash
adb shell settings get global blockshortcuts_heartbeat
adb shell "cat /data/adb/lspd/log/modules_*.log" | grep BlockShortcuts
adb logcat -s BlockShortcuts          # enable "log blocked keys" in the UI first
```

Note that `adb shell input keyevent` injects virtual-keyboard events (`deviceId = -1`); with the default "physical keyboard only" option they are **not** blocked — turn that option off for automated testing.

**Localization**: English is the default (`res/values/`), Simplified Chinese in `res/values-zh-rCN/`; both are listed in `res/xml/locales_config.xml`.