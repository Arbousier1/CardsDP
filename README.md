# CardsDP

将原数据包逻辑重构为 Paper 插件实现，玩法逻辑保持与数据包一致。

## 特性

- 数据包行为映射到 Java 运行时（桌子、牌堆、抽取、翻转、洗牌、Joker）
- 兼容新组件 API（1.21.x）
- MiniMessage + i18n（默认中文，含英文回退）
- 保留原数据包资源与配方定义

## 环境要求

- Java 21+
- Paper `1.21.11-R0.1-SNAPSHOT`（或兼容版本）

## 构建

```bash
gradle build
```

产物位于 `build/libs/`。

## 安装

1. 将插件 jar 放入服务器 `plugins/`
2. 启动服务器生成配置
3. 按需修改 `plugins/CardsDP/config.yml` 的语言配置

```yaml
i18n:
  locale: zh_cn
  fallback: en_us
```

## 资源包说明

插件逻辑可独立运行，但**完整视觉效果需要资源包**（`dqc.cards:*` 模型/材质）。
未安装资源包时，功能可用但显示会退化为原版物品外观。

## 数据包映射

详见 `MIGRATION.md`。
