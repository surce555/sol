# 🪙 SOL Tracker - Solana 价格盯盘工具

<div align="center">

![Build Status](https://github.com/surce555/sol/actions/workflows/build.yml/badge.svg)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue)
![License](https://img.shields.io/badge/license-MIT-purple)

**专为 Solana (SOL) 设计的安卓价格监控工具，数据来源 Binance**

</div>

---

## ✨ 功能特性

| 功能 | 说明 |
|------|------|
| 📊 **实时价格** | 通过 Binance WebSocket 推送，毫秒级更新 |
| 📈 **K线图** | 支持 1H / 4H / 日K / 周K / 月K 多周期切换 |
| 📉 **成交量图** | 与K线联动的柱状成交量图，牛熊配色 |
| 📐 **均线指标** | MA7 / MA25 / MA99 移动平均线 |
| 📖 **深度盘口** | 实时买卖挂单 Top 5 深度显示 |
| 🔔 **价格提醒** | 自定义突破/跌破目标价推送通知 |
| 🌙 **深色主题** | 以 Solana 品牌色 (#9945FF / #14F195) 为主调 |

---

## 📸 界面预览

| 行情首页 | K线图 | 价格提醒 |
|:---:|:---:|:---:|
| 实时价格 + 统计 + 盘口 | 蜡烛图 + 成交量 + MA | 提醒管理 + 推送通知 |

---

## 🏗️ 技术架构

```
com.soltracker.app/
├── data/
│   ├── model/          # 数据模型 (Kline, Ticker, etc.)
│   ├── network/        # Retrofit API + WebSocket 客户端
│   ├── db/             # Room 数据库 (价格提醒持久化)
│   └── repository/     # 数据仓库层
├── viewmodel/          # MainViewModel (状态管理)
├── ui/
│   ├── screen/         # HomeScreen / ChartScreen / AlertScreen
│   ├── navigation/     # Bottom Navigation
│   └── theme/          # Material3 主题 + Solana 色彩
├── notification/       # 推送通知服务
└── receiver/           # 开机自启接收器
```

### 🛠️ 技术栈

- **UI**: Jetpack Compose + Material Design 3
- **网络**: Retrofit2 + OkHttp3 (REST) + WebSocket (实时流)
- **数据**: Room Database + DataStore
- **架构**: MVVM + Kotlin Coroutines + StateFlow
- **图表**: MPAndroidChart (蜡烛图 + 柱状图)
- **CI/CD**: GitHub Actions (自动构建 APK)

---

## 📦 下载 APK

前往 [Releases](https://github.com/surce555/sol/releases) 页面下载最新 APK。

> **注意**: 安装时需要开启「允许安装未知来源应用」。

---

## 🔧 本地构建

```bash
git clone https://github.com/surce555/sol.git
cd sol
./gradlew assembleDebug
# APK 输出路径: app/build/outputs/apk/debug/
```

---

## 🚀 GitHub Actions 云编译

每次 push 到 `main` 分支会自动触发构建，在 Actions 页面可下载 APK。

### 发布正式版本

```bash
git tag v1.0.0
git push origin v1.0.0
# 自动创建 GitHub Release 并附带签名 APK
```

### 配置签名 (可选)

在 GitHub 仓库 Settings → Secrets 中添加:
- `KEYSTORE_BASE64`: Base64 编码的 keystore 文件
- `KEYSTORE_PASSWORD`: keystore 密码
- `KEY_ALIAS`: key alias
- `KEY_PASSWORD`: key 密码

---

## ⚠️ 免责声明

本工具仅供行情数据参考，**不构成任何投资建议**。加密货币市场风险极高，请自行承担投资风险。

---

## 📄 License

MIT License © 2024 SOL Tracker
