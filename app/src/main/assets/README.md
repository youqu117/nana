# 预设素材包（base64 占位）

由于 PR 系统不支持二进制文件，本仓库将 PNG 以 base64 文本形式存放。
打包时会自动生成真实 PNG（统一入口：`tools/prepare_assets.py`）。

生成后会得到与规范一致的目录结构：

```
app/src/main/assets/
  pets/
    dog_shiba/
      preview.png
      static/
        pet_normal.png
        pet_tongue.png
      anim/
        idle/
          sheet.png
          anim.json
      meta.json
    cat_orange/
      ...
  settings_default.json
```

说明：`*.png.base64` 是 PNG 的文本占位。生成脚本会写出同名 `.png` 文件。

## 扩展素材清单（annotations.json）

新增素材以扁平目录形式放在 `app/src/main/assets/` 根目录，
文件名与 `app/src/main/assets/annotations.json` 对应。

### 命名与备注规则

- 命名格式：
  - 有颜色：`{animal}_{color}_{type}.png`
  - 默认色：`{animal}_{type}.png`
- `type` 仅允许 `static` 或 `idle_sheet`。
- `description` 用一句话描述内容（动物 + 颜色 + 类型），便于后续检索与批次核对。

## 批次记录

批次清单用于记录已接收的素材批次与对应文件名，便于后续补齐 base64 文件。

- `app/src/main/assets/batches/batch1/manifest.json`
- `app/src/main/assets/batches/batch2/manifest.json`
- `app/src/main/assets/batches/batch3/manifest.json`
