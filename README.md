# 留痕 Vestige

把现实世界的数据桥接成 Markdown 日记。V1 只做一件事：读取 Android 系统日历，
按所选日期生成一份 Obsidian 友好的 Markdown 日记文件。

## 设计要点

- **本地优先 / Markdown 优先 / 不依赖云与后端。**
- **两个对称的扩展点：**
  - `DataPlugin`（`data/plugin/DataPlugin.kt`）抽象「数据从哪来」。日历只是第一个插件，
    未来加天气、步数、睡眠只需新建一个实现并在 `di/AppContainer.kt` 注册。
  - `ExportTarget`（`export/ExportTarget.kt`）抽象「数据写到哪去」。V1 只实现
    `SafExportTarget`（本地目录）；未来 OneDrive / 百度网盘实现同一接口即可，
    主流程与 UI 不变。
- 数据流：`DataPlugin.fetch` → `DaySection` → `DayEntry` → `MarkdownRenderer` → `ExportTarget`。

## 输出格式

文件名固定 `YYYY-MM-DD.md`：

```markdown
---
date: 2026-05-31
generated_by: Vestige/1.0
---

# 2026-05-31

## Calendar

- 09:00-11:00 毕设答辩
- 14:00-15:00 面试

## Notes

```

`generated_by` 是预留的格式版本标记，便于将来迁移；Obsidian 会忽略它。
`## Notes` 是留给你在 Obsidian 里自由编辑的区域。

## 使用流程

1. 选择导出目录（建议直接选 Obsidian Vault 里存放日记的子文件夹）。
2. 选择日期。
3. 点「导出 Markdown」。首次会申请日历读取权限。
4. 若当天文件已存在，默认不覆盖，会弹窗询问是否「覆盖重新生成」——
   **创建一次** 的语义：之后这个文件归你，程序不再主动改动。

## 关键技术决策

- 读日历查 `CalendarContract.Instances`（自动展开重复事件），全天事件单独渲染为 `- 全天 ...`。
- 文件写入走 **SAF**（`OpenDocumentTree` + 持久化 URI 授权），无需任何存储权限，
  天然兼容 Obsidian Vault 与 Android 10+ 分区存储。
- 唯一持久化的状态是导出目录 URI，存于 DataStore。

## 构建

用 Android Studio 打开本目录即可（首次同步会自动生成 Gradle Wrapper 与
`local.properties`）。或命令行：

```
gradle wrapper      # 首次生成 wrapper（若无 gradlew）
./gradlew assembleDebug
```

- minSdk 26 / targetSdk 35 / JDK 17。
- 需要联网下载依赖。

## 兼容性

- 运行时权限 `READ_CALENDAR`（API 23+ 必须运行时申请）。
- minSdk 26 可直接使用 `java.time`，无需 desugaring。
- Google / 本地 / Samsung 等只要通过系统 Calendar Provider 暴露即可统一读取。
- 无标题事件做了 `(无标题)` 兜底。
