# Nana 像素桌宠（Android）

> 当前版本：`1.1.0`
> 代码主包：`com.pixelpet`

## 项目简介

Nana 是一款手机像素桌宠应用，支持：

- 桌面/应用上层悬浮显示（Overlay）
- 宠物领养、命名、喂食、状态成长
- 小屋互动（音乐、睡觉、问候、唱歌等）
- 等级成长与情绪反馈
- FocusLock（专注防沉迷）与 Todo 联动

## 当前核心能力

1. 多宠物实例管理（商店领养 + 我的宠物管理）
2. 悬浮桌宠服务（启动/停止、位置与状态保存）
3. 宠物行为状态机（Idle/Walk/Run/Sleep/Cute 等）
4. 宠物小屋互动特效（音符、喷火、表情符号）
5. 自定义资源导入（ZIP / 图片）
6. FocusLock 专注流程（无障碍拦截、锁屏、Todo）

## 代码结构（简版）

- `app/src/main/java/com/pixelpet/ui/main`：主入口与导航
- `app/src/main/java/com/pixelpet/ui/home`：首页与玩家指引
- `app/src/main/java/com/pixelpet/ui/pets`：宠物列表/管理
- `app/src/main/java/com/pixelpet/ui/petroom`：小屋互动
- `app/src/main/java/com/pixelpet/pet/model`：宠物状态模型
- `app/src/main/java/com/pixelpet/pet/runtime`：行为与状态机
- `app/src/main/java/com/pixelpet/pet/interaction`：互动事件（Emote）
- `app/src/main/java/com/pixelpet/pet/view`：宠物渲染控件
- `app/src/main/java/com/pixelpet/focuslock`：专注与 Todo 模块

详细结构请看：`docs/STRUCTURE.md`

## 资源目录说明

- `app/src/main/assets`：运行时有效宠物资源（请勿随意删除）
- `app/src/main/res/drawable`：UI 与互动特效图标
- `tools/unused_assets`：备用/下载源素材，不参与运行

## 开发与运行

### 环境

- Android Studio（建议最新稳定版）
- JDK 17+
- Gradle Wrapper（仓库已内置）

### 常用命令

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

Windows:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
```

### 运行前权限

- 悬浮窗权限（桌宠必需）
- 无障碍权限（FocusLock 必需）
- 通知权限（前台服务建议开启）

## 文档索引

- 使用说明：`docs/guides/USAGE.md`
- 架构说明：`docs/STRUCTURE.md`
- 产品规划：`docs/plan/PRODUCT_PLAN.md`
- 资源说明：`app/src/main/assets/README.md`

## 说明

当前仓库历史变更较多，部分旧提交曾出现命名/编码混杂问题。现行文档已以 `com.pixelpet` 与 `1.1.0` 实现为准。
