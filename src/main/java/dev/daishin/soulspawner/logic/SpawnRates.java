package dev.daishin.soulspawner.logic;

import dev.daishin.soulcrystals.api.CrystalTier;
import dev.daishin.soulcrystals.api.SpawnerSettings;
public final class SpawnRates
{
	public static final int BASE_INTERVAL_TICKS = 600; // one round every 30 s
	public static final int BOSS_SLOWDOWN = 4;
	public static final int BOSS_MOB_CAP = 2;
	public static final int BOSS_MOBS_PER_ROUND = 1;
	public static final int OWNER_RANGE = 16;
	public static final int OWNER_RANGE_UPGRADED = 32;
	public static final int DEFAULT_SPAWN_RADIUS = 4;
	public static final int MIN_SPAWN_RADIUS = 1;
	public static final int MAX_SPAWN_RADIUS = 8;

	private static final int[] MOBS_PER_ROUND = {4, 6, 9};
	private static final double[] SPEED_BY_LEVEL = {1.0, 1.5, 2.25};

	private SpawnRates()
	{
	}

	public static int mobsPerRound(int tier, boolean boss) { return boss ? BOSS_MOBS_PER_ROUND : MOBS_PER_ROUND[tier - 1]; }

	// payout is 1 outside a catch-up payout; the cap grows with it, bosses included
	public static int mobCap(SpawnerSettings settings, int tier, boolean boss, int payout)
	{
		int base_cap = boss ? BOSS_MOB_CAP : settings.mobCaps().get(CrystalTier.values()[tier - 1]);
		return (int) Math.round(base_cap * settings.mobCapScale() * payout);
	}

	// progress added per tick towards BASE_INTERVAL_TICKS
	public static double progressPerTick(int speed_level, boolean boss, int payout)
	{
		return SPEED_BY_LEVEL[speed_level] * payout / (boss ? BOSS_SLOWDOWN : 1);
	}

	public static int ticksUntilRound(double progress, double progress_per_tick) { return (int) Math.ceil((BASE_INTERVAL_TICKS - progress) / progress_per_tick); }

	public static int ownerRange(boolean has_range) { return has_range ? OWNER_RANGE_UPGRADED : OWNER_RANGE; }

	public static int spawnRadius(boolean has_radius, int chosen) { return has_radius ? clampRadius(chosen) : DEFAULT_SPAWN_RADIUS; }

	public static int clampRadius(int radius) { return Math.max(MIN_SPAWN_RADIUS, Math.min(MAX_SPAWN_RADIUS, radius)); }
}
