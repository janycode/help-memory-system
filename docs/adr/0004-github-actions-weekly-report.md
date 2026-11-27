# 开源周报推送采用 GitHub Actions 定时任务，应用内零改动

## 背景与决定

需要一个每周五 18:00 推送到企业微信的开源项目周报，数据来自 GitHub Trending(weekly) 与 HelloGitHub。决定用 GitHub Actions 的 schedule 工作流加独立 Python 脚本（`.github/scripts/build_weekly_report.py`）实现，后端与前端不做任何改动，不在助记单系统内留档。

## 考虑过的方案

**应用内定时任务**（Spring Scheduler 轮询 + 复用现有通知链路）。能顺带把周报落库存档，但要引入后端实体、控制器、配置与密钥管理；且本机 H2 部署的实例并非 7×24 在线，错过触发时间就断更。

**第三方 RSS/聚合服务**。省去维护，但数据格式与板块结构不可控，条目数量与链接展示受对方模板限制。

## 后果

- 功能完全活在 `.github/` 下，与应用代码解耦；抓取逻辑变更不影响 JAR 构建交付流程。
- 推送依赖仓库 secret `QYWX_WEBHOOK_URL`，未配置时 workflow 跳过通知但不判失败，沿用 auto-build.yml 既有约定。
- GitHub Actions 的 cron 以 UTC 为准且高峰期可能延迟，"18:00" 是计划值而非精确值；这是选择零运维定时任务的固有代价。
- 换取的是零部署成本与零应用改动；若未来需要周报留档或按用户维度订阅，再评估移入应用内，脚本与 payload 格式可直接复用。
