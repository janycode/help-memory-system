const STORAGE_KEY = 'rocketmq_env_config'

interface EnvConfigItem {
  url: string
  topicsUrl: string
  tokenType: string
  version: string
}

interface EnvConfig {
  dev: EnvConfigItem
  test: EnvConfigItem
}

const defaultConfig: EnvConfig = {
  dev: {
    url: 'http://192.168.33.10:9880/topic/sendTopicMessage.do',
    topicsUrl: 'http://192.168.33.10:9880/topic/list.queryTopicType',
    tokenType: 'XSRF-TOKEN',
    version: 'v1.0'
  },
  test: {
    url: 'https://devops.leaderrun.com/rocketmq/topic/sendTopicMessage.do',
    topicsUrl: 'https://devops.leaderrun.com/rocketmq/topic/list.query',
    tokenType: 'JSESSIONID',
    version: 'v1.0'
  }
}

const cloneItem = (item: EnvConfigItem): EnvConfigItem => ({ ...item })

/**
 * 合并已保存配置与默认值：保证新增字段（如 tokenType）在旧 localStorage 数据上也有默认值
 */
const mergeItem = (defaultItem: EnvConfigItem, savedItem: Partial<EnvConfigItem> | undefined): EnvConfigItem => ({
  url: savedItem?.url ?? defaultItem.url,
  topicsUrl: savedItem?.topicsUrl ?? defaultItem.topicsUrl,
  tokenType: savedItem?.tokenType ?? defaultItem.tokenType,
  version: savedItem?.version ?? defaultItem.version
})

export function useEnvConfig() {
  const load = (): EnvConfig => {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (!saved) {
      return { dev: cloneItem(defaultConfig.dev), test: cloneItem(defaultConfig.test) }
    }
    const parsed = JSON.parse(saved) as Partial<EnvConfig> | null
    return {
      dev: mergeItem(defaultConfig.dev, parsed?.dev),
      test: mergeItem(defaultConfig.test, parsed?.test)
    }
  }

  const save = (config: EnvConfig) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(config))
  }

  const reset = () => {
    localStorage.removeItem(STORAGE_KEY)
    return { dev: cloneItem(defaultConfig.dev), test: cloneItem(defaultConfig.test) }
  }

  return { load, save, reset, defaultConfig }
}
