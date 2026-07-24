# Changelog

All notable changes to this project will be documented in this file.

## [0.0.2] - 2026-07-24

### Added

- 声音模组：实现 SoundManager 基于 SoundPool 管理音效，支持交互事件触发播放
- 宠物音效配置：通过 manifest.json 的 sounds 字段定义各交互事件对应的音效路径
- 版本管理系统：创建 version.properties 配置文件，支持动态版本管理
- 版本工具类：VersionUtils 提供版本比较、更新检查等功能
- GitHub Actions CI/CD：配置自动构建 APK 并创建 Release

### Changed

- 代码结构优化：重构文件夹结构，提升代码组织性
- 依赖注入：创建 AppContainer 单例统一管理 PetRepository
- 数据库迁移：从破坏性迁移改为显式迁移，启用 exportSchema
- 线程安全：修复 FocusManager 线程安全问题，采用白名单机制
- 协程管理：为 PetView 创建独立 viewScope，防止内存泄漏
- 状态保存：修复 OverlayService 销毁时状态丢失问题
- 资源配置化：宠物特殊行为通过 manifest.json 配置，替代硬编码

### Fixed

- FocusManager 线程安全问题
- 数据库破坏性迁移导致数据丢失
- OverlayService 状态保存失败
- PetView 协程泄漏
- GitHub Actions 构建失败（Gradle 下载超时）

## [0.0.1] - 2026-07-XX

### Added

- 初始版本发布
- 基础悬浮窗宠物功能
- 宠物选择与交互
- 宠物状态管理
- 基础 UI 界面
