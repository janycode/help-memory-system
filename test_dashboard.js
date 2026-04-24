// 简单的仪表盘测试脚本
const axios = require('axios');

async function testDashboard() {
  try {
    console.log('测试仪表盘接口...');

    // 测试后端接口
    const response = await axios.get('http://localhost:8080/api/dashboard/stats');
    console.log('后端接口响应:', response.data);

    // 测试前端代理接口
    const frontendResponse = await axios.get('http://localhost:5174/api/dashboard/stats', {
      headers: {
        'Host': 'localhost:5174'
      }
    });
    console.log('前端代理接口响应:', frontendResponse.data);

  } catch (error) {
    console.error('测试失败:', error.message);
    if (error.response) {
      console.error('响应状态:', error.response.status);
      console.error('响应数据:', error.response.data);
    }
  }
}

testDashboard();