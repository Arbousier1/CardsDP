# 安装与配置

## 环境要求

- Java 21+
- Paper 1.21.x（当前构建目标：`1.21.11-R0.1-SNAPSHOT`）

## 安装步骤

1. 将插件 Jar 放入服务器 `plugins/` 目录。
2. 启动服务器，自动生成配置文件。
3. 按需修改 `plugins/CardsDP/config.yml`。
4. 重启服务器（或按你的运维方式重载插件）。

## 基础配置

文件：`config.yml`

```yaml
i18n:
  locale: zh_cn
  fallback: en_us
```

- `locale`：主语言
- `fallback`：缺失文本时回退语言

## 资源包说明

- 不装资源包：玩法正常，可交互、可合成、可对战。
- 安装资源包：可显示完整自定义模型与外观。
