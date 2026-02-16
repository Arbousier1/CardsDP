# 多语言与提示

## 1. 配置项

文件：`plugins/CardsDP/config.yml`

```yaml
i18n:
  locale: zh_cn
  fallback: en_us
```

- `locale`：优先语言
- `fallback`：缺失词条时回退语言

## 2. 语言文件位置

- `src/main/resources/messages/zh_cn.yml`
- `src/main/resources/messages/en_us.yml`

## 3. 消息格式

- 消息文本使用 MiniMessage 格式。
- 主要用于：
- 插件启停日志
- 动作栏错误提示（例如槽位锁定、副手不兼容）

## 4. 中文乱码排查

- 确认语言文件为 UTF-8 编码。
- 确认服务器运行环境和控制台支持 UTF-8。
- 若你在 Windows 终端查看源码出现乱码，优先在 IDE 内用 UTF-8 打开。
