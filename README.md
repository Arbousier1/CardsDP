# CardsDP

CardsDP is now a pure Paper plugin implementation.
It no longer loads gameplay data from Minecraft datapack files at runtime.

## What changed

- Removed runtime dependency on `data/` datapack JSON/mcfunction files
- Built-in card/table/joker definitions are now provided directly in Java
- All game logic runs in plugin services/listeners on Paper API
- i18n and MiniMessage messaging remain supported (`zh_cn` default, `en_us` fallback)

## Requirements

- Java 21+
- Paper `1.21.11-R0.1-SNAPSHOT` (or compatible 1.21.x API)

## Build

```bash
gradle build
```

Jar output:

`build/libs/`

## Configure locale

`src/main/resources/config.yml`

```yaml
i18n:
  locale: zh_cn
  fallback: en_us
```

## Resource pack note

Core gameplay works without a resource pack.
For full visuals (custom card/table models), you still need the matching resource pack.

## Wiki

- Chinese wiki home: `docs/wiki/Home.md`
- Legacy entry page: `docs/WIKI.zh-CN.md`

## One-Click Wiki Publish

- GitHub Actions:
- Open `Actions` -> `Publish Wiki` -> `Run workflow`

- Local PowerShell:
```powershell
$env:GITHUB_TOKEN = "ghp_xxx"
.\scripts\publish-wiki.ps1 -Repo "owner/repo"
```
