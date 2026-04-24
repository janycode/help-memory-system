// 测试URL字段显示的简单脚本
// 这个脚本模拟前端环境数据的处理

const mockEnvironmentData = {
  id: 1,
  name: "测试环境",
  type: "DEV",
  url: "https://test.example.com",
  username: "testuser",
  password: "testpass",
  description: "这是一个测试环境",
  createdAt: "2024-01-01T00:00:00"
}

// 模拟表格行的渲染逻辑
function renderEnvironmentRow(environment) {
  console.log("环境数据:", environment)
  console.log("URL字段值:", environment.url)
  console.log("URL字段类型:", typeof environment.url)
  console.log("URL字段是否存在:", 'url' in environment)
  console.log("URL字段是否为真值:", !!environment.url)

  // 模拟 UrlLink 组件的渲染条件
  const shouldShowUrlLink = environment.url
  console.log("是否应该显示UrlLink:", shouldShowUrlLink)

  if (shouldShowUrlLink) {
    console.log("✅ URL字段应该显示")
  } else {
    console.log("❌ URL字段不应该显示")
  }
}

// 测试空URL的情况
const emptyUrlEnvironment = {
  id: 2,
  name: "空URL环境",
  type: "TEST",
  url: "",
  username: "testuser2",
  password: "testpass2",
  description: "这是一个空URL的测试环境"
}

const nullUrlEnvironment = {
  id: 3,
  name: "null URL环境",
  type: "PROD",
  url: null,
  username: "testuser3",
  password: "testpass3",
  description: "这是一个null URL的测试环境"
}

const undefinedUrlEnvironment = {
  id: 4,
  name: "undefined URL环境",
  type: "STAGING",
  username: "testuser4",
  password: "testpass4",
  description: "这是一个undefined URL的测试环境"
}

console.log("=== 测试正常URL ===")
renderEnvironmentRow(mockEnvironmentData)

console.log("\n=== 测试空字符串URL ===")
renderEnvironmentRow(emptyUrlEnvironment)

console.log("\n=== 测试null URL ===")
renderEnvironmentRow(nullUrlEnvironment)

console.log("\n=== 测试undefined URL ===")
renderEnvironmentRow(undefinedUrlEnvironment)