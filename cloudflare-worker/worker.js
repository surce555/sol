/**
 * Cloudflare Worker 币安 API & WebSocket 代理加速脚本 (优化增强版)
 */

const BINANCE_REST_HOST = 'data-api.binance.vision';
const BINANCE_WS_HOST = 'stream.binance.com:9443';

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 1. 处理跨域预检请求
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
          'Access-Control-Allow-Headers': '*',
          'Access-Control-Max-Age': '86400',
        },
      });
    }

    // 2. 根路径健康检查 (避免浏览器直接访问报币安官方的 403 错误)
    if (url.pathname === '/' || url.pathname === '') {
      const healthData = {
        status: 'online',
        service: 'SOL Tracker Binance Proxy',
        message: '代理服务运行正常！请将此地址填入 App 中使用。',
        time: new Date().toISOString(),
        test_api: `${url.origin}/api/v3/ticker/price?symbol=SOLUSDT`,
      };
      return new Response(JSON.stringify(healthData, null, 2), {
        status: 200,
        headers: {
          'Content-Type': 'application/json; charset=utf-8',
          'Access-Control-Allow-Origin': '*',
        },
      });
    }

    // 3. WebSocket 协议代理 (支持 stream.binance.com:9443)
    const upgradeHeader = request.headers.get('Upgrade');
    if (upgradeHeader && upgradeHeader.toLowerCase() === 'websocket') {
      const targetWsUrl = `https://${BINANCE_WS_HOST}${url.pathname}${url.search}`;
      const newHeaders = new Headers(request.headers);
      newHeaders.set('Host', 'stream.binance.com');
      return fetch(targetWsUrl, { headers: newHeaders });
    }

    // 4. HTTP REST API 代理 (支持 api.binance.com)
    const targetApiUrl = `https://${BINANCE_REST_HOST}${url.pathname}${url.search}`;
    const newHeaders = new Headers();
    
    // 过滤掉可能引起币安 WAF 拦截的 Cloudflare 特殊内部头
    for (const [key, value] of request.headers.entries()) {
      const lower = key.toLowerCase();
      if (!lower.startsWith('cf-') && lower !== 'x-forwarded-for' && lower !== 'x-real-ip') {
        newHeaders.set(key, value);
      }
    }
    
    newHeaders.set('Host', BINANCE_REST_HOST);
    newHeaders.set('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36');

    const newRequest = new Request(targetApiUrl, {
      method: request.method,
      headers: newHeaders,
      body: request.method !== 'GET' && request.method !== 'HEAD' ? request.body : null,
      redirect: 'follow',
    });

    try {
      const response = await fetch(newRequest);
      const newResponse = new Response(response.body, response);
      newResponse.headers.set('Access-Control-Allow-Origin', '*');
      newResponse.headers.set('Access-Control-Allow-Headers', '*');
      return newResponse;
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), {
        status: 502,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*',
        },
      });
    }
  },
};
