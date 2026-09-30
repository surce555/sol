/**
 * Cloudflare Worker 币安 API & WebSocket 代理加速脚本
 * 
 * 功能：
 * 1. 代理币安 REST API (api.binance.com)
 * 2. 代理币安 WebSocket 实时流 (stream.binance.com:9443)
 * 3. 自动处理跨域 CORS 与 WebSocket 握手
 */

const BINANCE_REST_HOST = 'api.binance.com';
const BINANCE_WS_HOST = 'stream.binance.com:9443';

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 处理 CORS 预检请求
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

    // 1. WebSocket 协议代理 (支持 stream.binance.com)
    const upgradeHeader = request.headers.get('Upgrade');
    if (upgradeHeader && upgradeHeader.toLowerCase() === 'websocket') {
      const targetWsUrl = `https://${BINANCE_WS_HOST}${url.pathname}${url.search}`;
      
      const newHeaders = new Headers(request.headers);
      newHeaders.set('Host', 'stream.binance.com');
      
      return fetch(targetWsUrl, {
        headers: newHeaders,
      });
    }

    // 2. HTTP REST API 代理 (支持 api.binance.com)
    const targetApiUrl = `https://${BINANCE_REST_HOST}${url.pathname}${url.search}`;
    const newHeaders = new Headers(request.headers);
    newHeaders.set('Host', BINANCE_REST_HOST);
    newHeaders.set('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)');

    const newRequest = new Request(targetApiUrl, {
      method: request.method,
      headers: newHeaders,
      body: request.body,
      redirect: 'follow',
    });

    try {
      const response = await fetch(newRequest);
      const newResponse = new Response(response.body, response);
      
      // 附加跨域响应头
      newResponse.headers.set('Access-Control-Allow-Origin', '*');
      newResponse.headers.set('Access-Control-Allow-Headers', '*');
      return newResponse;
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), {
        status: 502,
        headers: { 'Content-Type': 'application/json' },
      });
    }
  },
};
