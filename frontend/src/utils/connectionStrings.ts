export interface ConnectionString {
  label: string
  command: string
  language?: string
}

export function parseHostPort(url: string): { host: string; port: string } {
  let host = url
  let port = ''

  // Remove protocol prefix
  host = host.replace(/^https?:\/\//, '').replace(/^tcp:\/\//, '')

  // Extract port
  const colonIdx = host.lastIndexOf(':')
  if (colonIdx > 0) {
    port = host.substring(colonIdx + 1)
    host = host.substring(0, colonIdx)
  }

  // Remove path
  const slashIdx = host.indexOf('/')
  if (slashIdx > 0) {
    host = host.substring(0, slashIdx)
  }

  return { host, port }
}

function sshCommand(host: string, username?: string): string {
  if (username && host) return `ssh ${username}@${host}`
  if (host) return `ssh ${host}`
  return ''
}

export function generateConnectionStrings(
  category: string,
  url: string,
  username?: string,
  password?: string
): ConnectionString[] {
  const result: ConnectionString[] = []
  const { host, port } = parseHostPort(url)

  if (!url) return result

  switch (category) {
    case 'Database': {
      // JDBC URL
      const dbName = ''
      const jdbcUrl = `jdbc:mysql://${host}${port ? ':' + port : ':3306'}${dbName ? '/' + dbName : ''}?useSSL=false&serverTimezone=Asia/Shanghai`
      result.push({ label: 'JDBC 连接串', command: jdbcUrl, language: 'sql' })

      // MySQL CLI
      let mysqlCli = `mysql -h ${host}${port ? ' -P ' + port : ''}`
      if (username) mysqlCli += ` -u ${username}`
      if (password) mysqlCli += ` -p'${password}'`
      result.push({ label: 'MySQL CLI', command: mysqlCli, language: 'bash' })

      // PostgreSQL
      const pgUrl = `jdbc:postgresql://${host}${port ? ':' + port : ':5432'}/`
      result.push({ label: 'PostgreSQL JDBC', command: pgUrl, language: 'sql' })

      const pgCli = `psql -h ${host}${port ? ' -p ' + port : ''} -U ${username || 'user'} -d postgres`
      result.push({ label: 'PostgreSQL CLI', command: pgCli, language: 'bash' })
      break
    }

    case 'Cache': {
      // Redis CLI
      let redisCli = `redis-cli -h ${host}`
      if (port) redisCli += ` -p ${port}`
      if (password) redisCli += ` -a '${password}'`
      result.push({ label: 'Redis CLI', command: redisCli, language: 'bash' })

      // Redis 字符串连接
      const redisUrl = `redis://${password ? ':' + password + '@' : ''}${host}:${port || '6379'}`
      result.push({ label: 'Redis URL', command: redisUrl, language: 'bash' })

      // SSH tunnel
      if (username) {
        result.push({ label: 'SSH 登录', command: sshCommand(host, username), language: 'bash' })
      }
      break
    }

    case 'MessageQueue': {
      // AMQP URL
      const amqpUrl = `amqp://${username || 'guest'}:${password || 'guest'}@${host}:${port || '5672'}`
      result.push({ label: 'AMQP URL', command: amqpUrl, language: 'bash' })

      // RabbitMQ Management
      if (port) {
        const mgmtPort = port === '5672' ? '15672' : port
        result.push({ label: 'RabbitMQ 管理界面', command: `http://${host}:${mgmtPort}`, language: 'bash' })
      }
      break
    }

    case 'API': {
      // curl command
      let curlCmd = `curl -X GET '${url}'`
      if (username) {
        const auth = password ? `${username}:${password}` : username
        curlCmd += ` -u '${auth}'`
      }
      curlCmd += ' -H "Content-Type: application/json"'
      result.push({ label: 'curl 请求', command: curlCmd, language: 'bash' })

      // HTTPie
      let httpie = `http GET '${url}'`
      if (username) httpie += ` --auth ${username}${password ? ':' + password : ''}`
      result.push({ label: 'HTTPie', command: httpie, language: 'bash' })
      break
    }

    case 'Storage': {
      if (port === '22' || port === '') {
        const scp = `scp -P ${port || '22'} ${username ? username + '@' : ''}${host}:/path/to/file ./`
        result.push({ label: 'SCP 下载', command: scp, language: 'bash' })
        result.push({ label: 'SSH 登录', command: sshCommand(host, username), language: 'bash' })
      } else {
        result.push({ label: '服务地址', command: url, language: 'bash' })
      }
      break
    }

    case 'Monitoring': {
      result.push({ label: '访问地址', command: url, language: 'bash' })
      if (username && password) {
        result.push({ label: '登录信息', command: `用户名: ${username}  密码: ${password}`, language: 'bash' })
      }
      break
    }

    default: {
      result.push({ label: '服务地址', command: url, language: 'bash' })
      if (username) {
        result.push({ label: 'SSH 登录', command: sshCommand(host, username), language: 'bash' })
      }
    }
  }

  return result
}
