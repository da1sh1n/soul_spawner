# Soul Spawner — how to play

## Getting started

1. Craft an empty Soul Spawner (or find one) and place it. You are now its owner.
2. Fill a soul crystal ([Soul Crystals guide](../../soul_crystals/docs/guide.md)).
3. Right-click the spawner and put the full crystal in the crystal slot.
4. Stay within 16 blocks. Every 30 seconds it spawns a round of mobs.

## How it spawns

| Crystal | Mobs per round | Mob cap nearby | Upgrade slots |
|---|---|---|---|
| Small | 4 | 8 | 1 |
| Medium | 6 | 16 | 2 |
| Large | 9 | 32 | 3 |
| Boss crystal | 1 | 2 | by crystal tier |

- One round every **30 seconds**. Speed upgrades make it faster.
- **Only the owner** makes it spawn: the owner must be within 16 blocks (32 with the Range
  upgrade). Other players don't count.
- It ignores light. It only needs free space for the mob.
- Each round it picks different free spots in the radius where the mob fits and has a floor, never above
  the spawner's own height, and spawns the full count (fewer only if the cap or the free spots run out).
  A round where nothing fits sends no redstone pulse and keeps any owed catch-up rounds.
- Look at a spawner (or open it) to see the time until the next round, "No crystal" or "Waiting for owner".
- It stops spawning when the mob cap is reached, and continues once mobs are killed.
- Spawned mobs drop normal loot and XP but give **no souls**. Buffed mobs (e.g. ominous ones) are
  spawned as the normal mob.
- Each round sends a redstone pulse (strength 15, 2 ticks) out of every side.
- The crystal is not used up. Take it out and the spawner stops.

### Bosses

- Boss crystals (Warden, Wither, Elder Guardian, Creaking) spawn 1 mob per round at ¼ the speed:
  one every 120 s, 80 s with Speed I, 53 s with Speed II. Cap 2.
- The Range upgrade works. The Time upgrade does nothing (it shows a warning triangle).
- A spawned Wither still explodes and breaks blocks when it spawns.
- **Creaking:** a Creaking crystal spawns no Creaking. Instead, every boss round 4 resin clumps pop
  out of the top of the spawner.

## Upgrades

Upgrades sit in the spawner's upgrade slots. The crystal's tier decides how many slots are open
(1 / 2 / 3). Only one of each type per spawner.

| Upgrade | Effect |
|---|---|
| Range | Owner range 16 → 32 blocks. |
| Time | Catch-up: spawns you missed while away are paid out when you return (see below). Not for bosses. |
| Radius | Adds a slider: spawn radius 1–8 blocks. Without it the radius is 4. |
| Speed I | 1.5× speed: a round every 20 s. |
| Speed II | 2.25× speed: a round every 13.3 s. Crafted from Speed I. Only one Speed upgrade per spawner. |

Max output with Speed II: small 18, medium 27, large 40.5 mobs per minute.

Swapping in a smaller crystal closes slots. The upgrades in the closed slots come back to you (last
slot first). If your inventory is full you get **Dirty Pockets** and they arrive when a slot frees.

## Catch-up (Time upgrade)

- While you are online but away from the spawner (more than 16/32 blocks away, or its chunk isn't
  loaded), it keeps count of missed spawns at **half** its normal speed. There is no limit.
- Time offline doesn't count.
- When you come back in range it pays out the missed spawns faster: the **payout slider** sets
  3×, 4× or 5×, applied to both speed and mob cap (a large crystal at 5× caps at 160). Once the
  backlog is empty it goes back to normal.
- The payout pauses at the raised cap if mobs aren't being killed.
- Taking the crystal out, swapping it, or breaking the spawner clears the backlog.
- The server may remove some payout values (see [config](../../soul_crystals/docs/config.md)).

## Spawn Silencer

A block, not an upgrade (it uses no slot). Place it touching a spawner on any side and that
spawner's mobs are silent. One silencer can touch several spawners. It stays when a spawner is
broken.

## Breaking and moving

- Any pickaxe mines it. No Silk Touch needed.
- It drops as one item holding the crystal, upgrades and settings (radius, payout). The tooltip
  lists what's inside. Place it to restore everything.
- Whoever places it becomes the new owner.
- The server can limit how many spawners each player owns.

## Recipes

Key: `ib` iron bars, `ns` netherite scrap, `ss` soul sand, `ob` obsidian, `gi` gold ingot,
`ub` upgrade base, `ee` eye of ender, `gob` gold block, `ck` clock, `rc` recovery compass,
`rb` redstone block, `ep` ender pearl, `ab` amethyst block, `br` blaze rod, `bz` breeze rod,
`qb` quartz block, `s1` Speed I upgrade, `w` any wool, `as` amethyst shard, `nb` note block,
`_` empty.

| Item | Row 1 | Row 2 | Row 3 |
|---|---|---|---|
| Empty spawner | ib ns ib | ib ss ib | ib ns ib |
| Upgrade base | ob gi ob | gi ns gi | ob gi ob |
| Range upgrade | _ ee _ | ee ub ee | gob gob gob |
| Time upgrade | _ ck _ | rc ub rc | rb rb rb |
| Radius upgrade | _ ep _ | ep ub ep | ab ab ab |
| Speed I | _ br _ | br ub br | qb qb qb |
| Speed II | _ bz _ | bz s1 bz | qb qb qb |
| Spawn Silencer | w as w | as nb as | w as w |

## Loot

| Where | Chance | What |
|---|---|---|
| Ominous trial vault | 20% | empty Soul Spawner |
| Ancient City chest | 10% | empty Soul Spawner |
| Mineshaft chest | 1% | empty Soul Spawner |

These rolls are separate from the crystal loot, so a chest can give both.

## Advancements

In the Soul Crystals tab:
- Craft your first spawner.
- Craft your first upgrade.
