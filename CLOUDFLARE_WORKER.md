# ⚡ Cloudflare Worker 币安 API 免费加速代理部署教程

当国内或部分网络环境下无法直接连接 Binance 时，可以使用免费的 Cloudflare Workers 搭建专属反向代理节点。

---

## 🛠️ 三步快速部署

### 第一步：注册/登录 Cloudflare
1. 打开 [https://dash.cloudflare.com/](https://dash.cloudflare.com/) 并登录。
2. 左侧菜单栏点击 **Workers 和 Pages (Workers & Pages)**。
3. 点击 **创建 (Create application)** -> 选择 **创建 Worker (Create Worker)**。
4. 给 Worker 取个名字（例如 `sol-binance-proxy`），点击 **部署 (Deploy)**。

### 第二步：粘贴代理代码
1. 部署完成后，点击 **编辑代码 (Edit code)**。
2. 删除编辑器里现有的所有默认代码，复制本项目中的 [cloudflare-worker/worker.js](cloudflare-worker/worker.js) 内容并全部粘贴进去。
3. 点击右上角 **保存并部署 (Save and deploy)**。

### 第三步：复制地址填入 App
1. 在 Worker 概览页面复制分配的域名，例如：
   ```
   https://sol-binance-proxy.yourname.workers.dev
   ```
2. 打开手机上的 **SOL Tracker** 应用。
3. 点击首页右上角 ⚙️ **设置** 图标。
4. 开启 **启用代理加速**，并将刚刚复制的 Worker 地址粘贴进去。
5. 点击 **保存并测试**，应用将即刻通过 Cloudflare 专线拉取行情与 WebSocket 实时推送！

---

## 💡 特性支持
- ✅ 支持 `api.binance.com` 所有 REST 请求（实时价格、K线历史、盘口数据）
- ✅ 支持 `stream.binance.com:9443` WebSocket 实时流升级，毫秒级推送
- ✅ 免费额度每日 100,000 次请求，个人盯盘完全免费且充足
