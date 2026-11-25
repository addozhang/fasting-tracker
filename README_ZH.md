<div align="center">

# Fasting Tracker（轻断食追踪）

**一款静谧、零权限、原生支持 Android 16 实时活动与澎湃 OS 超级岛的开源轻断食计时器。**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026--36)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![SoloCraft](https://img.shields.io/badge/SoloCraft-独立软件工坊-D96B27)](https://solocraft.app)

[English](README.md) · [简体中文](README_ZH.md) · [产品主页 (solocraft.app)](https://solocraft.app/fasting-tracker) · [隐私政策](PRIVACY.md)

</div>

---

## 为什么做这款应用？

如今绝大多数轻断食 App 都越来越臃肿：动辄每年几百元的订阅费、开屏与穿插广告、强制注册登录、甚至卖代餐带货。

**Fasting Tracker** 只想回归工具的本质：**安静、准确、绝对隐私，完全属于你自己**。

- **100% 开源免费（GPL-3.0）** — 无广告、无订阅、无内购付费墙。
- **极致本地隐私** — 无需账号、无埋点统计、无云端同步、无 `INTERNET` 网络权限，所有数据仅保存在本机 Room 数据库，支持手动 JSON 导出与导入。
- **有机极简设计（Organic Minimal）** — 温润克制的 Material 3 视觉语言、等宽数字计时、可点击代谢阶段圆环、周柱状图、月度热力图，以及 2×2 / 4×2 桌面小组件。
- **Android 16+ 实时活动 & 澎湃 OS 4 超级岛原生适配** — 零前台服务省电架构，同时支持**断食中**与**进食窗口**双状态上岛、6 阶段专属动态矢量图标，并提供**超级岛、锁屏显示、精简通知模式**三个独立开关。
- **克制顺手的断食逻辑** — 内置 `14:10`、`16:8`、`18:6`、`20:4`、`24h`、`48h`、`72h` 及自定义计划；不足 1 小时误触自动放弃不污染统计；达标后自然延续记录生酮/自噬阶段；支持补录与编辑历史记录且自动防时间重叠。
- **原生中英双语** — 支持应用内独立语言切换。

---

## 双渠道构建（FOSS / Play）

同一份开源代码提供两个纯净构建变体：

| 构建变体 | 分发渠道 | 闭源依赖 | 自愿赞赏方式 |
| :--- | :--- | :--- | :--- |
| **`foss`** | GitHub Releases · F-Droid · `solocraft.app` | **无**（100% 纯开源） | 跳转浏览器打开 `solocraft.app/fasting-tracker#support` |
| **`play`** | Google Play 商店 | 仅含 Google Play Billing | 通过 Google Play 应用内自愿赞赏 |

---

## 本地编译

环境要求：JDK 17、Android SDK（API 36）。

```bash
# 运行双 Flavor 单元测试
./gradlew :app:testPlayDebugUnitTest :app:testFossDebugUnitTest

# 编译 FOSS 版 APK
./gradlew :app:assembleFossDebug
./gradlew :app:assembleFossRelease

# 编译 Google Play 版 AAB
./gradlew :app:bundlePlayRelease
```

---

## 支持与致谢

本项目由 [**SoloCraft 独立软件工坊**](https://solocraft.app) 出品。  
如果 Fasting Tracker 对你的轻断食习惯有所帮助，欢迎点个 Star，或 [请作者喝杯咖啡](https://solocraft.app/fasting-tracker#support)。

采用 [GNU General Public License v3.0](LICENSE) 开源协议。
