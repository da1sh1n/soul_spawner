# Soul Spawner

Version **1.0.1** — what changed: [docs/changelog.md](docs/changelog.md).

Craft your own spawner. Put a full soul crystal in it and it spawns that mob — zombies, blazes,
even a Warden.

## Features

- Craftable spawner block. Vanilla spawners are untouched.
- Runs on a full Soul Crystal: small spawns 4 mobs per round, medium 6, large 9. Bosses spawn 1.
- Spawns only while its owner is nearby. Ignores light level.
- Upgrades: **Range**, **Time** (catch-up for spawns missed while you were away), **Radius**
  (spawn area slider), **Speed I / II**.
- **Spawn Silencer** block: mobs from the spawners it touches make no sound.
- Sends a redstone pulse every spawn round, for farm machinery.
- Breaking the spawner keeps its crystal, upgrades and settings inside the item.
- Spawned mobs drop normal loot and XP, but give no souls.
- Found in mineshafts, Ancient Cities and ominous trial vaults.

## Requirements

- Minecraft 26.1.x, Fabric Loader 0.19.5+, Fabric API
- [Daishin's Lib](../daishins_lib/README.md)
- [Soul Crystals](../soul_crystals/README.md)

## Docs

- [How to play](docs/guide.md) — the spawner, upgrades, catch-up, recipes, loot.
- [Config](../soul_crystals/docs/config.md) — the `spawner` section, plus server performance notes.
- [For mod developers](docs/api.md) — what it registers and what it uses from Soul Crystals.
