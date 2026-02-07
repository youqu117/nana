# 资产目录说明（中文版）

适用版本：`1.1.0`


本目录是桌宠运行时资产根目录：`app/src/main/assets`。

## 1. 目录作用

- 存放可被 App 直接读取的宠物资源包。
- 资源扫描后会出现在“商店/领养”列表中。

## 2. 推荐目录结构

```text
app/src/main/assets/
  pets/
    <pet_id>/
      manifest.json
      preview.png
      static/
        pet_normal.png
        pet_tongue.png
      anim/
        idle/
          sheet.png
          anim.json
```

说明：

- `manifest.json`：宠物元数据与资源路径。
- `preview.png`：商店/列表预览图。
- `static/*`：静态展示图（兜底图）。
- `anim/*`：动画帧与配置。

## 3. 扩展素材记录（已收口）

- `annotations.json`：额外素材注释与来源记录。
- `batches/*`：仅用于历史批次记录，不参与运行时扫描。
- 运行时有效目录已统一为：`pets/*`。

## 4. 第三方素材

- 第三方原始素材仓库建议使用：`resources/third_party/`。
- `assets/` 仅保留运行时直接读取的资源，避免 APK 冗余膨胀。
- 引入第三方素材时，请保留原授权文本并按需拷贝到运行目录。

## 5. 注意事项

1. 不要随意删除 `pets/` 下正在使用的资源。
2. 资源命名尽量稳定，避免频繁改路径导致 manifest 失效。
3. 备用素材请放到 `tools/unused_assets`，不要放在本目录污染运行时扫描。
4. 自定义导入失败时，优先检查 `manifest.json` 字段与路径是否匹配。



