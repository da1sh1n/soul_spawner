# Soul Spawner — for mod developers

Soul Spawner has no public API package; nothing else depends on it. This page lists what it
registers and what it uses from [Soul Crystals](../../soul_crystals/docs/api.md). Package
`dev.daishin.soulspawner`; rules and math with no Minecraft classes are in `logic`.

## Uses from Soul Crystals

Only through `CrystalAccess`: `tier`, `mob`, `isFull`, `isBoss`, `hasNoSouls`, `markNoSouls` and
`config().spawner()`. It never touches crystal data directly.

- Every spawned mob gets `markNoSouls`, so spawner mobs never fill crystals. The mob cap counts
  mobs in range by type and that marker.
- A boss crystal of `minecraft:creaking` spawns no mob; each boss round it pops 4
  `minecraft:resin_clump` from the top of the block instead.
- The menu open data carries the allowed payout multipliers and upgrade toggles, so no separate
  config sync is needed.

## Registry ids

| Kind | Ids |
|---|---|
| Block, block entity, item | `soul_spawner:soul_spawner` |
| Block | `soul_spawner:spawn_silencer` |
| Items | `upgrade_base`, `range_upgrade`, `time_upgrade`, `radius_upgrade`, `speed_upgrade_1`, `speed_upgrade_2` |
| Menu | `soul_spawner:soul_spawner` |
| Component | `soul_spawner:spawner_contents` (crystal, upgrades, radius, payout multiplier) |
| C2S packets | `soul_spawner:set_radius`, `soul_spawner:set_payout` |
| Player attachment | `soul_spawner:online_ticks` (persistent long, drives catch-up) |
| Saved data | `soul_spawner_counts` (owner UUID to spawner count) |
| Recipe serializer | `soul_spawner:toggleable_shaped` |
| Advancements | `first_spawner`, `first_upgrade`, in the Soul Crystals tab |

## Disabled upgrades

`soul_spawner:toggleable_shaped` is a shaped recipe with an extra `"upgrade"` field (`range`,
`time`, `radius`, `speed`, `silencer`). Its `matches` returns false while that upgrade is switched
off in the config. A resource condition cannot do this because the world config is not readable
yet when the world's datapacks first load. Cost: the recipe still shows in the recipe book, greyed
out. An installed upgrade whose toggle is off does nothing.
