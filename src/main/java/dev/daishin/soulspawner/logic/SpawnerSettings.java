package dev.daishin.soulspawner.logic;

import java.util.List;

// mirrors the spawner section of the Soul Crystals config; mob_caps is ordered small, medium, large
public record SpawnerSettings(
	boolean catch_up_enabled,
	List<Integer> payout_multipliers,
	List<Integer> mob_caps,
	double mob_cap_scale,
	int max_spawners_per_player,
	UpgradeToggles upgrades_enabled
)
{
	public static final SpawnerSettings DEFAULTS = new SpawnerSettings(true, List.of(3, 4, 5), List.of(8, 16, 32), 1.0, 0, UpgradeToggles.ALL_ENABLED);

	public int baseMobCap(int tier) { return mob_caps.get(tier - 1); }
}
