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
- Recommended: add repository secret `WIKI` (PAT)
- Compatible secret name: `WIKI_TOKEN`
- PAT scope:
- Private repository: `repo`
- Public repository: `public_repo`

- Local PowerShell:
```powershell
$env:GITHUB_TOKEN = "ghp_xxx"
.\scripts\publish-wiki.ps1 -Repo "owner/repo"
```

If workflow logs show `repository ... .wiki.git not found`:

1. Enable Wiki in GitHub repository settings.
2. Create or verify `WIKI` secret (or `WIKI_TOKEN`).
3. Re-run `Publish Wiki`.
