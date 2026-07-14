# Nana Pet

> 一只住在手机里的像素桌宠：陪你生活、提醒你休息，也帮你保持专注。

[![Android APK Build](https://github.com/youqu117/nana/actions/workflows/android-build.yml/badge.svg)](https://github.com/youqu117/nana/actions/workflows/android-build.yml)
[![Latest Release](https://img.shields.io/github/v/release/youqu117/nana?display_name=tag)](https://github.com/youqu117/nana/releases)

Nana Pet 是一款基于 Android 的像素桌宠应用。领养宠物后，它可以悬浮在其他应用之上，拥有独立的状态、行为和互动反馈；你也可以在小屋中布置场景、导入自定义素材，并使用 FocusLock 和 Todo 进入一段专注时间。

项目当前版本为 **1.1.0**，应用包名为 `com.pixelpet`，最低支持 Android 7.0（API 24）。

## 目录

- [功能一览](#功能一览)
- [快速开始](#快速开始)
- [权限说明](#权限说明)
- [项目结构](#项目结构)
- [本地开发](#本地开发)
- [GitHub 云端构建](#github-云端构建)
- [自定义宠物素材](#自定义宠物素材)
- [数据与隐私](#数据与隐私)
- [常见问题](#常见问题)
- [开发约定](#开发约定)

## 功能一览

### 像素桌宠

- 在桌面或其他应用上方显示宠物悬浮窗。
- 支持点击、双击、长按和拖拽等手势。
- 宠物拥有 Idle、Walk、Run、Sleep、Cute 等行为状态。
- 可调节缩放、透明度、移动速度、自由漂移和垂直移动。
- 支持启动、暂停、召回和重新召唤桌宠服务。

### 领养与成长

- 在商店中从内置资产领养宠物。
- 支持多只宠物实例，每只宠物可独立命名和管理。
- 记录饱食度、心情、精力、亲密度和等级等状态。
- 支持喂食、重命名、召唤、回家和删除。

### 宠物小屋

- 为每个宠物实例保存独立的小屋布置。
- 支持音乐、睡觉、问候、唱歌、喷火等互动。
- 通过音符、爱心、烟火、表情和其他像素特效反馈互动结果。
- 支持上传图片和使用内置素材作为装饰。

### FocusLock 专注模式

- 创建和管理 Todo 任务。
- 设置专注时长，支持 25、45、50 分钟等快捷时长。
- 通过无障碍服务检测被限制应用的打开行为。
- 专注期间显示锁定页面，帮助减少无意识切换应用。
- 记录当天累计专注时间。

## 快速开始

### 安装 Release

打开 [Releases](https://github.com/youqu117/nana/releases) 页面，下载最新构建中的 APK：

- `app-debug.apk`：用于测试，体积较大，适合开发验证。
- `app-release-unsigned.apk`：未签名 Release 包，适合后续自行签名或作为打包输入。

Android 可能会提示允许安装未知来源应用，请按系统提示操作。正式分发前需要使用自己的签名密钥重新签名 Release APK。

### 第一次使用

1. 安装并打开 Nana Pet。
2. 在系统设置中授予悬浮窗权限。
3. 打开“商店”，领养至少一只宠物。
4. 回到首页，开启“悬浮窗”服务。
5. 返回桌面或打开其他应用，观察宠物是否正常出现。
6. 在“我的”页面喂食、命名和管理宠物，在“小屋”中进行互动。

使用 FocusLock 时，再进入专注页面按提示开启无障碍权限，并配置 Todo 和限制规则。

## 权限说明

Nana Pet 的部分功能依赖 Android 系统权限。应用不会因为缺少权限而影响基础页面浏览，但对应功能无法工作。

| 权限 | 用途 | 影响范围 |
| --- | --- | --- |
| 悬浮窗 | 在其他应用上方显示桌宠 | 桌宠悬浮服务 |
| 前台服务 | 让悬浮桌宠在后台持续运行 | 桌宠悬浮服务 |
| 通知 | 显示前台服务状态和退出入口 | Android 13+ 建议开启 |
| 无障碍服务 | 检测限制应用并触发专注锁定 | FocusLock |
| 开机启动 | 在设备重启后恢复相关能力 | 可选，取决于系统策略 |

无障碍服务仅用于 FocusLock 的应用拦截流程。用户可以随时在系统设置中关闭该权限。

## 项目结构

```text
nana/
├── app/
│   └── src/main/
│       ├── java/com/pixelpet/
│       │   ├── content/       # 内容包与素材加载
│       │   ├── data/          # Room 数据、DAO、Repository
│       │   ├── focuslock/     # Todo、专注、锁定页、无障碍服务
│       │   ├── input/         # 点击、双击、长按、拖拽手势
│       │   ├── overlay/       # 悬浮窗服务和窗口管理
│       │   ├── pet/           # 状态模型、行为运行时、互动和渲染
│       │   └── ui/            # 首页、宠物、商店、设置、小屋
│       ├── assets/
│       │   ├── pets/           # 运行时宠物内容包
│       │   └── settings_default.json
│       └── res/                # Android 布局、字符串、图片和特效
├── docs/
│   ├── guides/USAGE.md         # 更详细的使用说明
│   ├── plan/PRODUCT_PLAN.md    # 产品规划
│   └── STRUCTURE.md            # 模块和资源结构
├── resources/third_party/      # 第三方原始素材，不直接参与运行
├── tools/                      # 素材准备、校验和迁移脚本
├── gradlew / gradlew.bat       # Gradle Wrapper
└── .github/workflows/          # GitHub Actions 云端构建
```

详细模块职责和资源规范请参阅 [`docs/STRUCTURE.md`](docs/STRUCTURE.md)。

## 本地开发

### 环境要求

- Android Studio 最新稳定版
- JDK 17 或更高版本
- Android SDK 34
- 可用的 Android SDK Build Tools
- Git

项目使用 Gradle Wrapper，不需要单独安装 Gradle。Android Studio 打开仓库后，等待 Gradle 同步完成即可。

### 常用命令

Linux/macOS：

```bash
# 编译 Kotlin
./gradlew :app:compileDebugKotlin

# 构建 Debug APK
./gradlew :app:assembleDebug

# 同时构建 Debug 和 Release APK
./gradlew --no-daemon assembleDebug assembleRelease
```

Windows PowerShell：

```powershell
# 编译 Kotlin
.\gradlew.bat :app:compileDebugKotlin

# 构建 Debug APK
.\gradlew.bat :app:assembleDebug

# 同时构建 Debug 和 Release APK
.\gradlew.bat --no-daemon assembleDebug assembleRelease
```

构建产物位于：

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

### 开发建议

1. 修改业务逻辑后先执行 `:app:compileDebugKotlin`，快速发现 Kotlin 编译问题。
2. 修改布局、资源或 Manifest 后执行 `:app:assembleDebug`。
3. 修改宠物内容包后运行仓库中的资源校验脚本。
4. 提交前确认没有把 `build/`、`.gradle/`、`local.properties` 或签名文件加入 Git。

## GitHub 云端构建

仓库内置 [Android APK Build](.github/workflows/android-build.yml) 工作流：

- 推送到 `main` 分支时自动触发。
- 也可以在 GitHub Actions 页面手动运行 `workflow_dispatch`。
- 使用 Java 17 和 Gradle Wrapper 编译 `assembleDebug`、`assembleRelease`。
- 将两个 APK 上传为 Actions artifact。
- 构建成功后自动创建 GitHub Release，并附加 APK 文件。

查看构建状态：[Actions](https://github.com/youqu117/nana/actions)。

当前工作流生成的是未签名 Release 包。若要发布到应用商店，需要在 GitHub Actions Secrets 中配置签名密钥，并在单独的发布工作流中完成签名、校验和上传。

## 自定义宠物素材

运行时宠物内容位于 `app/src/main/assets/pets/<pet_id>/`。每个宠物至少需要一个 `manifest.json`，示例：

```json
{
  "id": "la_la",
  "name": "La La",
  "version": 1,
  "preview": "pets/la_la/preview.png",
  "static_normal": "pets/la_la/preview.png",
  "idle_sheet": "pets/la_la/preview.png",
  "idle_anim": "pets/la_la/anim/idle/anim.json",
  "default_scale": 1,
  "hitbox": { "x": 0, "y": 0, "w": 48, "h": 48 },
  "anchors": {
    "root_x": 24,
    "root_y": 48,
    "head_x": 24,
    "head_y": 10,
    "face_x": 24,
    "face_y": 15
  }
}
```

注意事项：

- `id` 应保持稳定且只使用小写字母、数字和下划线。
- `preview` 等路径必须相对于 `assets/`，并与实际文件大小写完全一致。
- 图片、动画和 manifest 应放在同一个宠物目录中，避免引用 `tools/unused_assets`。
- 通过应用“设置”页导入 ZIP 时，ZIP 根目录应包含可识别的 `manifest.json` 或标准宠物目录结构。
- 导入图片/GIF 时，应用会尝试生成最小内容清单；复杂动画建议手动制作完整内容包。

第三方原始素材存放在 `resources/third_party/`，该目录用于制作和整理，不会直接作为运行时宠物资源使用。使用第三方素材时，请遵守其原始授权条款。

## 数据与隐私

- 宠物状态、Todo 和应用设置默认保存在设备本地。
- 项目没有内置账号系统，也没有要求上传宠物数据的远程服务。
- 悬浮窗和无障碍能力只在用户主动授予权限后启用。
- 删除应用或清除应用数据会删除本地状态，请在测试重要数据前自行备份。

## 常见问题

### 桌宠没有显示

确认已经开启悬浮窗权限、首页服务开关处于运行状态，并且至少领养了一只宠物。部分定制 Android 系统还需要允许应用自启动和后台运行。

### 桌宠运行一段时间后消失

检查系统电池优化、后台限制和自启动策略。建议将 Nana Pet 加入系统的后台运行白名单，并确认前台服务通知没有被关闭。

### FocusLock 没有拦截应用

确认无障碍服务已开启、专注状态正在运行，并检查限制规则或 Todo 目标是否已保存。不同厂商对无障碍服务的后台行为限制不同。

### 自定义素材导入失败

检查 ZIP 内是否存在 manifest、资源路径是否正确、文件名大小写是否匹配，并查看应用提示的导入结果。可以先参考 `app/src/main/assets/pets/` 中的内置内容包。

### Release APK 为什么不能直接发布

工作流生成的 `app-release-unsigned.apk` 没有签名。Android 应用正式安装和商店发布需要使用开发者自己的 keystore 签名，不能把项目测试包当作正式发行包。

## 开发约定

- 新功能按现有模块边界放置，避免把业务逻辑堆积在 Activity 中。
- 宠物交互遵循“`pet/interaction` 定义事件，`pet/runtime` 驱动状态，`ui/petroom` 展示反馈”的链路。
- 资源命名保持统一前缀，例如 `fx_`、`bg_`、`ic_`。
- 修改包名、Activity、Service 或资源路径时，同步检查 `AndroidManifest.xml` 和相关布局引用。
- 变更目录结构时，同步更新 [`docs/STRUCTURE.md`](docs/STRUCTURE.md) 与本 README。
- 提交前使用 `git diff --check` 检查空白字符问题。

## 相关文档

- [详细使用说明](docs/guides/USAGE.md)
- [项目结构说明](docs/STRUCTURE.md)
- [产品规划](docs/plan/PRODUCT_PLAN.md)
- [运行时资产说明](app/src/main/assets/README.md)
- [GitHub Actions](.github/workflows/android-build.yml)

## 项目状态

当前项目处于持续开发阶段。核心的桌宠悬浮、宠物管理、小屋互动、素材导入和 FocusLock 流程已经接入；界面细节、动画资源和跨厂商后台兼容性仍会继续迭代。

欢迎通过 Issue 反馈问题或提出功能建议。提交代码前，请先说明复现步骤、设备 Android 版本和相关日志，便于定位问题。
