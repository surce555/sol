/**
 * Cloudflare Worker - 最终版
 * REST API → 转发给 Vercel 中继（AWS IP，不被币安封）
 * WebSocket → 直连 stream.binance.com
 */

// ⬇️ 部署 Vercel 项目后，把 Vercel 的域名填在这里
const VERCEL_REST_PROXY = 'https://YOUR_VERCEL_APP.vercel.app';

const BINANCE_WS_HOST = 'stream.binance.com:9443';

export default {
  async fetch(request) {
    const url = new URL(request.url);

    // CORS 预检
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
          'Access-Control-Allow-Headers': '*',
        },
      });
    }

    // 健康检查
    if (url.pathname === '/' || url.pathname === '') {
      return new Response(JSON.stringify({ status: 'online', vercel: VERCEL_REST_PROXY }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
      });
    }

    // WebSocket → 直连币安（正常工作，保持不变）
    if (request.headers.get('Upgrade')?.toLowerCase() === 'websocket') {
      return fetch(`https://${BINANCE_WS_HOST}${url.pathname}${url.search}`, request);
    }

    // REST API → 转发到 Vercel 中继（绕过币安对 Cloudflare IP 的封锁）
    const targetUrl = `${VERCEL_REST_PROXY}${url.pathname}${url.search}`;

    try {
      const response = await fetch(targetUrl, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
        },
      });

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
