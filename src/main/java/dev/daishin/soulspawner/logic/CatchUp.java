package dev.daishin.soulspawner.logic;

import java.util.List;

public final class CatchUp
{
	public static final int MIN_GAP_TICKS = 20;
	public static final int NO_PAYOUT = 1;

	private CatchUp()
	{
	}

	// the backlog builds at half the normal speed while the owner is online but away
	public static double owedRounds(long gap_ticks, double rounds_per_tick)
	{
		if (gap_ticks <= MIN_GAP_TICKS) return 0;
		return gap_ticks * rounds_per_tick / 2;
	}

	public static boolean isPayingOut(double owed) { return owed >= 1; }

	// a removed value falls back to the highest allowed one below it, else the lowest; 0 = never chosen
	public static int resolvePayout(int chosen, List<Integer> allowed)
	{
		if (allowed.isEmpty()) return NO_PAYOUT;
		int lowest = allowed.getFirst();
		int best_below = 0;
		for (int value : allowed)
		{
			if (value < lowest) lowest = value;
			if (value <= chosen && value > best_below) best_below = value;
		}
		return best_below > 0 ? best_below : lowest;
	}
}
