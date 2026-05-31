# 留痕 Vestige

一个能独立使用的日记 App：按日期读取 Android 系统日历事件（并可选附上当天天气），
生成 Markdown 草稿，**直接在 App 内编辑成稿**。文件以普通 Markdown 落盘，因此也能
（可选地）放进 Obsidian Vault 获得更好的阅读体验——但 Obsidian 不是必需的。

## 设计要点

- **本地优先 / Markdown 优先 / 不依赖云与后端 / 不依赖 Obsidian。**
- **两个对称的扩展点：**
  - `DataPlugin`（`data/plugin/DataPlugin.kt`）抽象「数据从哪来」。日历只是第一个插件，
    未来加天气、步数、睡眠只需新建一个实现并在 `di/AppContainer.kt` 注册。
  - `NoteStore`（`export/NoteStore.kt`）抽象「日记读写到哪」。V1 只实现
    `SafNoteStore`（本地目录，按年/月分文件夹）；未来 OneDrive / 百度网盘实现同一接口即可，
    主流程与 UI 不变。
- 数据流：`DataPlugin.fetch` → `DaySection` → `DayEntry` → `MarkdownRenderer` →（编辑）→ `NoteStore`。

## 存储结构

首次会让你选一个目录（选择器默认定位到 **Documents**，点确认即授权并永久记住）。
日记统一收纳在该目录下的 `Vestige/` 子文件夹，再按 `年/月` 分，避免污染所选目录、
也避免堆在一个巨大目录里：

```
<你授权的目录>/        # 默认 Documents
  Vestige/
    2026/
      05/
        2026-05-31.md
        2026-05-30.md
      06/
        2026-06-01.md
```

## 文件格式

段落标题与文字**跟随系统语言**（中文系统如下，其它语言对应 Weather / Events / Notes）：

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
- 「事件」段落刻意保持简洁：全天事件只写标题、不加前缀；无标题事件留空待补。
  重心是日记本身，附属数据不喧宾夺主。
- 「天气」依赖定位，取不到（无权限/无网/日期超出 API 范围）时该段落直接省略。
- 「笔记」是留给你写日记正文的区域。

## 使用流程

1. 首次在右上角选择保存目录（默认定位 Documents，很少改，之后收在角落）。
2. 首页点「**写今天的日记**」：当天文件不存在则自动生成草稿（天气+事件）并进入编辑器；
   已存在则**载入续写**。其它日期走「其它日期…」。
3. 在编辑器里直接写。**自动保存**：每 1 分钟、退出编辑页、App 切后台 各存一次，
   无需手动点保存。
4. 点左上角标题「**留痕**」→ 从左侧滑出目录树抽屉（背景变暗）。顶部搜索栏为空时显示
   `年 ▸ 月 ▸ 日` 可展开目录树；输入关键词回车/点搜索则切换为全文检索结果（日期+摘要）。
   点任一天进编辑器查看或续写。
5. 文件按 `Vestige/年/月` 落盘，可直接用 Obsidian 或任意文本编辑器打开。

## 关键技术决策

- 读日历查 `CalendarContract.Instances`（自动展开重复事件）。
- 天气用 **Open-Meteo**（免费、无需 API Key），定位用 `LocationManager` 最后已知位置
  （不依赖 Google Play 服务）。**注意：导出含天气时，经纬度会发送到 api.open-meteo.com，
  这是 App 唯一的对外网络请求。**
- 文件读写走 **SAF**（`OpenDocumentTree` + 持久化 URI 授权），无需任何存储权限，
  按 `Vestige/年/月` 子目录 `findFile`/`createDirectory`，天然兼容 Obsidian Vault 与 Android 10+ 分区存储。
  目录选择器通过 `DocumentsContract.EXTRA_INITIAL_URI` 默认定位到 Documents（尽力而为）。
- 浏览/搜索通过 `DocumentFile` 枚举 `Vestige/年/月` 下的 `*.md`、按文件名解析日期；
  搜索读取各文件内容做大小写无关匹配并截取摘要。`DocumentFile` 较慢，MVP 不做索引缓存。
- 单 Activity，用 `screen` 状态在「首页/编辑页」间切换，不引入导航库；浏览/搜索做成首页的
  `ModalNavigationDrawer`（自带遮罩变暗），点标题打开。
- 唯一持久化的状态是保存目录 URI，存于 DataStore。
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
