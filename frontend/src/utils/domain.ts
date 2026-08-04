/**
 * 将 URL 中的子域名替换为 leaderrun
 * 例如: https://devops.myaccount.com/rocketmq/... → https://devops.leaderrun.com/rocketmq/...
 * IP 地址和 localhost 不做替换
 */
export function normalizeDomain(url: string): string {
  try {
    const parsed = new URL(url)
    const parts = parsed.hostname.split('.')
    if (parts.length >= 3 && !/^\d+$/.test(parts[0])) {
      parts[0] = 'leaderrun'
      parsed.hostname = parts.join('.')
    }
    return parsed.toString()
  } catch {
    return url
  }
}
