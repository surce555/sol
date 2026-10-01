/**
 * Cloudflare Worker - 稳定版
 * WebSocket → 直连 stream.binance.com
 * REST API → 由 App 直接访问 data-api.binance.vision，无需经过此 Worker
 * （此 Worker 仅作 WebSocket 代理使用）
 */
export default {
  async fetch(request) {
    const url = new URL(request.url);

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
        'Access-Control-Allow-Headers': '*',
      }});
    }

    // 健康检查
    if (url.pathname === '/' || url.pathname === '') {
      return new Response(JSON.stringify({ status: 'online', time: new Date().toISOString() }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
      });
    }

    // WebSocket → 直连币安流数据（此路径正常工作）
    if (request.headers.get('Upgrade')?.toLowerCase() === 'websocket') {
      return fetch(`https://stream.binance.com:9443${url.pathname}${url.search}`, request);
    }

    // REST API → 转发到 data-api.binance.vision（干净请求头）
    try {
      const response = await fetch(
        `https://data-api.binance.vision${url.pathname}${url.search}`,
        {
          method: 'GET',
          headers: {
            'Accept': 'application/json',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
          },
        }
      );
      const newResponse = new Response(response.body, response);
      newResponse.headers.set('Access-Control-Allow-Origin', '*');
      return newResponse;
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), {
        status: 502,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
      });
    }
  },
};
