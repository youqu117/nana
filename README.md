# 像素桌面桌宠（v0.2 骨架）

本仓库提供 Android 桌宠的最小可运行骨架，目标是先完成悬浮、拖拽、点击反馈与简单走动，再逐步扩展状态机、成长与资产系统。

## 目录结构

```
.
├─ app/                  # Kotlin 源码（悬浮层/输入/宠物渲染）
├─ res/                  # 资源（当前为矢量占位图）
└─ docs/
   ├─ guides/
   │  └─ USAGE.md         # 使用说明（v0.2）
   └─ plan/
      └─ PRODUCT_PLAN.md  # 产品/技术规格文档（含 anim.json 规范）
```

## 快速开始

1. 用 Android Studio 打开工程（或通过命令行构建 APK）。
2. 安装到 Android 设备并运行应用。
3. 授予悬浮窗权限。
4. 在首页点击“启动桌宠”。

详细使用步骤请见：`docs/guides/USAGE.md`。

## 安装与打包说明（手机软件）

本项目是 Android 应用源码，**不能直接把仓库解压到手机使用**。需要先打包成 APK：

### 方法一：Android Studio（推荐）

1. 用 Android Studio 打开项目。
2. 等待 Gradle 同步完成。
3. 选择 `Build > Build Bundle(s) / APK(s) > Build APK(s)`。
4. 生成的 APK 在 `app/build/outputs/apk/`。
5. 把 APK 安装到手机（或用 Android Studio 直接 Run）。

### 方法二：命令行（CI/自动化）

```bash
# Debug 版
./gradlew assembleDebug

# Release 版（需签名配置）
./gradlew assembleRelease
```

> Release 包需要签名，通常在 `gradle.properties` 或 `keystore` 配置中设置。

### 是否还有别的方法？

可以通过 CI/CD（如 GitHub Actions）自动构建 APK，再下载安装到手机。

## 功能简介（v0.2 骨架）

- 悬浮窗显示：桌面与任意 App 上层显示桌宠。
- 交互：支持拖拽移动、边缘吸附、点击反馈。
- 简单自动走动：在屏幕边缘区域内轻量移动。
- 基础内容边界：素材与核心逻辑拆分，可后续热更新内容包。
- 轻量状态模型：预留能量/情绪/亲密度等基础指标。

## 资源说明

当前占位图为 `res/drawable/normal.xml` 与 `res/drawable/tongue.xml`。
如需替换为自定义素材，请保持同名文件并更新为你的资源。

素材包以 base64 文本形式存放在 `assets/`，打包时会自动完成 PNG 生成与校验，
无需手动执行命令（已由 `tools/prepare_assets.py` 统一处理）。

## 文档入口

- 使用说明：`docs/guides/USAGE.md`
- 产品规格与落地方案：`docs/plan/PRODUCT_PLAN.md`
- 预设素材包说明：`assets/README.md`
