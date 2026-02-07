# 项目结构说明（中文）

适用版本：`1.1.0`


本文档描述当前仓库的目录分层、模块职责与资源分类，供后续开发统一参考。

## 一、代码模块

### 1) 业务核心

- `app/src/main/java/com/pixelpet/content`
  - 内容包加载、manifest 解析。

- `app/src/main/java/com/pixelpet/data`
  - Room 数据库、DAO、Repository、资产扫描。

### 2) 宠物系统

- `app/src/main/java/com/pixelpet/pet/model`
  - 状态模型与行为枚举（如 `PetState`、`PetBehavior`）。

- `app/src/main/java/com/pixelpet/pet/runtime`
  - 行为状态机、自动行为、交互入口（如 `PetRuntime`）。

- `app/src/main/java/com/pixelpet/pet/interaction`
  - 互动事件定义（如音乐、睡觉、问候、喷火等 Emote）。

- `app/src/main/java/com/pixelpet/pet/level`
  - 等级与成长计算逻辑。

- `app/src/main/java/com/pixelpet/pet/view`
  - 宠物显示控件（`PetView`）与缩放/渲染处理。

### 3) UI 分层

- `app/src/main/java/com/pixelpet/ui/main`
  - 主 Activity、底部导航与页面装配。

- `app/src/main/java/com/pixelpet/ui/home`
  - 首页、状态展示、玩家指引入口。

- `app/src/main/java/com/pixelpet/ui/pets`
  - 我的宠物列表与管理。

- `app/src/main/java/com/pixelpet/ui/shop`
  - 宠物领养/商店页面。

- `app/src/main/java/com/pixelpet/ui/settings`
  - 设置与资源导入管理。

- `app/src/main/java/com/pixelpet/ui/petroom`
  - 宠物小屋互动页面。
  - 按 `instanceId` 独立保存小屋布置（同款宠物也可分别布置）。
  - 支持两种装饰来源：`上传图片` 与 `内置素材库`（`res://` 引用）。

- `app/src/main/java/com/pixelpet/ui/adapters`
  - RecyclerView 相关适配器。

### 4) 系统能力与扩展模块

- `app/src/main/java/com/pixelpet/overlay`
  - 悬浮窗服务与窗口管理。

- `app/src/main/java/com/pixelpet/input`
  - 手势识别（点击、双击、长按、拖拽）。

- `app/src/main/java/com/pixelpet/focuslock`
  - 专注锁模块（Todo、锁屏、无障碍服务、拦截逻辑）。

## 二、资源分类

### 1) 运行时资源

- `app/src/main/assets`
  - 有效宠物资产与内容包主目录（运行时读取）。
  - 统一入口为 `assets/pets`，`assets/batches` 仅做历史归档。

- `resources/third_party`
  - 第三方原始素材仓库（非运行时，不打包进 APK）。
  - 用于挑选图标/特效/9-slice 素材，再按需导入到 `res/` 或 `assets/pets/`。

- `app/src/main/res`
  - 布局、字符串、颜色、drawable、字体等 Android 资源。

### 2) 互动特效资源（推荐规范）

- 基础特效：`app/src/main/res/drawable`
  - 兼容旧版素材，如 `fx_fire.png`、`fx_note.png`。

- 彩色特效包：`app/src/main/res/drawable-nodpi`
  - 新增统一彩色特效素材，命名统一为 `fx_*_color.png`。
  - 例如：
    - `fx_music_note_color.png`
    - `fx_music_notes_color.png`
    - `fx_violin_color.png`
    - `fx_piano_color.png`
    - `fx_flute_color.png`
    - `fx_sleep_color.png`
    - `fx_fire_color.png`
    - `fx_spark_color.png`
    - `fx_heart_color.png`
    - `fx_wave_color.png`
    - `fx_smile_color.png`
    - `fx_food_color.png`
    - `fx_think_color.png`
    - `fx_sun_color.png`
    - `fx_exclaim_color.png`

### 3) 备用素材目录

- `tools/unused_assets`
  - 备用/源素材，不参与应用运行。
  - 可保留用于后续二次制作，也可按需清理。

## 三、互动逻辑链路（推荐扩展方式）

1. 在 `pet/interaction` 定义新事件类型。
2. 在 `pet/runtime` 中发射事件与绑定状态变化。
3. 在 `ui/petroom` 中渲染对应视觉反馈。

## 四、开发约定

- 新功能优先按现有分层放置，避免跨模块堆叠。
- 资源命名保持统一前缀（如 `fx_`、`bg_`、`ic_`）。
- 变更目录结构后，必须同步：
  - `AndroidManifest.xml`
  - 布局中的自定义类路径
  - 相关文档（本文件 + README）



