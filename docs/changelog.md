# Changelog — Soul Spawner

Versions are major.minor.patch: **major** = a change in game mechanics, **minor** = a feature added or removed,
**patch** = fixes, refactors, tests and docs. Newest first.

## 2.0.3 (patch)

- README: how this project was made

## 2.0.2 (patch)

- log the mod version at startup

## 2.0.1 (patch)

- use the Soul Crystals config records directly instead of copies
- no per-tick rebuild of the settings
- unused SpawnRates.CRYSTAL_TIERS removed
- tests: spawner save/load round trip, spawner counts survive a save

## 2.0.0 (major)

- the mob cap counts mobs within max(4, spawn radius) blocks, like a vanilla spawner: mobs that wander off make room

## 1.0.1 (patch)

- riders of spawned mobs are soul-free too, like a spider jockey's skeleton

## 1.0.0

- First published version.
