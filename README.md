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

1. 在 Android 设备上安装并运行应用。
2. 授予悬浮窗权限。
3. 在首页点击“启动桌宠”。

详细使用步骤请见：`docs/guides/USAGE.md`。

## 资源说明

当前占位图为 `res/drawable/normal.xml` 与 `res/drawable/tongue.xml`。
如需替换为自定义素材，请保持同名文件并更新为你的资源。

素材包以 base64 文本形式存放在 `assets/`，生成 PNG 请执行：

```
python tools/materialize_assets.py
```

扩展素材清单校验：

```
python tools/validate_assets.py
```

## 文档入口

- 使用说明：`docs/guides/USAGE.md`
- 产品规格与落地方案：`docs/plan/PRODUCT_PLAN.md`
- 预设素材包说明：`assets/README.md`
