# Soul Spawner — in-game test checklist

What has been checked by hand in the Prism instance "Soul Mods Test". Automated GameTests don't
count here: this list is only for things seen in a real game. Tick an item when it worked; write a
note next to it when it didn't.

Started over on 2026-11-22: everything unchecked except the items marked below.

## Soul Spawner
- [ ] New spawner, silencer and upgrade textures; spawner cage is see-through like vanilla
- [ ] Craft spawner, upgrade base and each upgrade (time: clock + recovery compass)
- [ ] Place spawner; only full crystals go in; slots open by crystal tier
- [ ] Spawns exactly 4 / 6 / 9 per round, only where mobs fit, never above the spawner, one per block
- [ ] Radius upgrade + slider (1–8); small radius still spawns the full count when there is room
- [ ] Speed I / II shorten the round
- [ ] Mob cap stops spawning; resumes after kills
- [ ] Range upgrade: owner at 32 blocks keeps it running
- [ ] Time upgrade: catch-up after being away; payout slider 3× / 4× / 5×
- [ ] Boss crystal: 1 per round, warning on time upgrade; Creaking crystal drops resin instead
- [x] Spawn Silencer touching the spawner makes mobs silent (tested 2026-11-22)
- [ ] Redstone pulse on each round
- [ ] Timer text under the crosshair and in the menu ("Next spawn", "No crystal", "Waiting for owner")
- [ ] Break spawner → item keeps crystal, upgrades and settings; placing restores them
- [ ] Per-player spawner limit from config
