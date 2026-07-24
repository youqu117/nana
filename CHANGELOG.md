# Changelog

All notable changes to this project will be documented in this file.

## [0.0.3] - 2026-07-24

### Added

- 像素风宠物生成器：PixelPetGenerator 动态绘制11种宠物的像素风图片
- 内置素材支持：无需外部图片资源，首次安装即可看到完整宠物列表
- 动画帧生成：每种宠物支持4帧动态动画效果
- 宠物名称本地化：所有宠物名称改为中文（橘猫、柴犬、像素龙、熊猫、企鹅宝宝、乌龟、蜘蛛、欢欢、拉拉、琪琪、悠哈）

### Changed

- BitmapUtils：图片加载失败时自动使用生成器创建后备图片
- AssetScanner：不再因缺少图片文件而跳过宠物，确保所有宠物都能显示
- 所有宠物manifest.json统一配置，添加完整的hitbox和anchors参数

### Fixed

- 首次安装后宠物列表为空的问题

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
