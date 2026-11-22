package dev.daishin.soulspawner.client;

import dev.daishin.soulspawner.logic.CatchUp;
import dev.daishin.soulspawner.logic.SpawnTimer;
import net.minecraft.network.chat.Component;

final class SpawnTimerText
{
	private static final int TICKS_PER_SECOND = 20;

	private SpawnTimerText()
	{
	}

	// the line shown on the HUD and in the menu for one timer value
	static Component of(SpawnTimer timer)
	{
		int seconds = (timer.ticks() + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
		return switch (timer.state())
		{
			case NO_CRYSTAL -> Component.translatable("gui.soul_spawner.timer.no_crystal");
			case WAITING_OWNER -> Component.translatable("gui.soul_spawner.timer.waiting_owner");
			case RESIN -> Component.translatable("gui.soul_spawner.timer.next_resin", seconds);
			case SPAWNING -> timer.multiplier() == CatchUp.NO_PAYOUT ? Component.translatable("gui.soul_spawner.timer.next_spawn", seconds)
				: Component.translatable("gui.soul_spawner.timer.next_spawn_payout", seconds, timer.multiplier());
		};
	}
}
