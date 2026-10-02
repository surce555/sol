/**
 * Cloudflare Worker - 币安 API 代理
 * 参考金价监控项目的简洁方案：直接替换 hostname，保留所有原始请求头
 */
export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    // CORS 预检
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

    // WebSocket → stream.binance.com:9443
    if (request.headers.get('Upgrade')?.toLowerCase() === 'websocket') {
      url.hostname = 'stream.binance.com';
      url.port = '9443';
    } else {
      // REST API → data-api.binance.vision（国内可直连，此路径为备用）
      url.hostname = 'data-api.binance.vision';
      url.port = '443';
    }

    // 关键：直接传递原始请求头，不做任何修改（保留 WebSocket 握手所需的所有字段）
    const newRequest = new Request(url.toString(), {
      headers: request.headers,
      method: request.method,
      body: request.body,
      redirect: 'follow',
    });

    let response = await fetch(newRequest);
    response = new Response(response.body, response);
    response.headers.set('Access-Control-Allow-Origin', '*');
    return response;
  },
};
