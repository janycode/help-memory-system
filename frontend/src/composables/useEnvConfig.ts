const STORAGE_KEY = 'rocketmq_env_config'

interface EnvConfig {
  dev: { url: string; topicsUrl: string }
  test: { url: string; topicsUrl: string }
}

const defaultConfig: EnvConfig = {
  dev: {
    url: 'http://192.168.33.10:9880/topic/sendTopicMessage.do',
    topicsUrl: 'http://192.168.33.10:9880/topic/list.queryTopicType'
  },
  test: {
    url: 'https://devops.leaderrun.com/rocketmq/topic/sendTopicMessage.do',
    topicsUrl: 'https://devops.leaderrun.com/rocketmq/topic/list.query'
  }
}

export function useEnvConfig() {
  const load = (): EnvConfig => {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : defaultConfig
  }

  const save = (config: EnvConfig) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(config))
  }

  const reset = () => {
    localStorage.removeItem(STORAGE_KEY)
    return { ...defaultConfig }
  }

  return { load, save, reset, defaultConfig }
}
