/**
 * Vercel Serverless Function - 币安 REST API 代理
 * 运行在 AWS 基础设施上，IP 不被币安封锁
 */
export default async function handler(req, res) {
  // CORS 预检
  if (req.method === 'OPTIONS') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', '*');
    return res.status(200).end();
  }

  const url = new URL(req.url, `https://${req.headers.host}`);
  const path = url.pathname;
  const search = url.search;

  // 健康检查
  if (path === '/' || path === '') {
    return res.status(200).json({
      status: 'online',
      service: 'SOL Tracker Binance REST Proxy (Vercel)',
      time: new Date().toISOString(),
    });
  }

  // 转发到币安（使用干净的请求头）
  const targetUrl = `https://api.binance.com${path}${search}`;

  try {
    const response = await fetch(targetUrl, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      },
    });

    const body = await response.text();
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Content-Type', 'application/json');
    return res.status(response.status).send(body);

  } catch (err) {
    return res.status(502).json({ error: err.message });
  }
}
