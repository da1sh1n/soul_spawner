package dev.daishin.soulspawner;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

public final class OnlineTicks
{
	// copyOnDeath: the count must not reset when the player respawns
	public static final AttachmentType<Long> ONLINE_TICKS = AttachmentRegistry.<Long>builder()
		.persistent(Codec.LONG)
		.copyOnDeath()
		.buildAndRegister(SoulSpawnerMod.id("online_ticks"));

	private OnlineTicks()
	{
	}

	public static long of(ServerPlayer player) { return player.getAttachedOrElse(ONLINE_TICKS, 0L); }

	static void register()
	{
		ServerTickEvents.END_SERVER_TICK.register(server ->
		{
			for (ServerPlayer player : server.getPlayerList().getPlayers()) player.setAttached(ONLINE_TICKS, of(player) + 1);
		});
	}
}
