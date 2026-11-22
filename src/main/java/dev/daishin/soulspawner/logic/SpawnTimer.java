package dev.daishin.soulspawner.logic;

// what the spawner shows about its next round; ticks only count down in SPAWNING and RESIN
public record SpawnTimer(TimerState state, int ticks, int multiplier)
{
	public static final SpawnTimer IDLE = new SpawnTimer(TimerState.NO_CRYSTAL, 0, CatchUp.NO_PAYOUT);

	public boolean isCounting() { return state == TimerState.SPAWNING || state == TimerState.RESIN; }
}
