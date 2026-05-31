# 留痕 Vestige

把现实世界的数据桥接成 Markdown 日记。按所选日期，读取 Android 系统日历事件
（并可选附上当天天气），生成一份 Obsidian 友好的 Markdown 日记文件。

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

文件名固定 `YYYY-MM-DD.md`。段落标题与文字**跟随系统语言**（中文系统如下，
其它语言对应 Weather / Events / Notes）：

```markdown
---
date: 2026-05-31
generated_by: Vestige/1.0
---

# 2026-05-31

## 天气

- 晴 18°C ~ 26°C

## 事件

- 我的生日
- 09:00-11:00 毕设答辩
- 14:00-15:00 面试

## 笔记

```

- `generated_by` 是预留的格式版本标记，便于将来迁移；Obsidian 会忽略它。
- 「事件」段落刻意保持简洁：全天事件只写标题、不加前缀；无标题事件留空，
  方便你在 Obsidian 里补写。重心是日记本身，附属数据不喧宾夺主。
- 「天气」依赖定位，取不到（无权限/无网/日期超出 API 范围）时该段落直接省略。
- 「笔记」是留给你在 Obsidian 里自由编辑的区域。

## 使用流程

1. 选择导出目录（建议直接选 Obsidian Vault 里存放日记的子文件夹）。
2. 选择日期。
3. 点「导出 Markdown」。首次会申请日历读取权限。
4. 若当天文件已存在，默认不覆盖，会弹窗询问是否「覆盖重新生成」——
   **创建一次** 的语义：之后这个文件归你，程序不再主动改动。

## 关键技术决策

- 读日历查 `CalendarContract.Instances`（自动展开重复事件）。
- 天气用 **Open-Meteo**（免费、无需 API Key），定位用 `LocationManager` 最后已知位置
  （不依赖 Google Play 服务）。**注意：导出含天气时，经纬度会发送到 api.open-meteo.com，
  这是 App 唯一的对外网络请求。**
- 文件写入走 **SAF**（`OpenDocumentTree` + 持久化 URI 授权），无需任何存储权限，
  天然兼容 Obsidian Vault 与 Android 10+ 分区存储。
- 唯一持久化的状态是导出目录 URI，存于 DataStore。
- 应用名与 Markdown 文字跟随系统语言：中文「留痕」、其它「Vestige」。

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

- 运行时权限：`READ_CALENDAR`（必需）、`ACCESS_COARSE_LOCATION`（可选，仅用于天气）、
  `INTERNET`（天气请求）。三者均 API 23+ 运行时申请；定位被拒只是没有天气段落，不影响导出。
- minSdk 26 可直接使用 `java.time`，无需 desugaring。
- Google / 本地 / Samsung 等只要通过系统 Calendar Provider 暴露即可统一读取。
- 天气仅在 Open-Meteo 支持的日期范围内（约过去 3 个月 ~ 未来 16 天）可用，超出则省略。
