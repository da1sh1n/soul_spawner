package dev.daishin.soulspawner;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class SpawnerNetworking
{
	private SpawnerNetworking()
	{
	}

	static void register()
	{
		PayloadTypeRegistry.serverboundPlay().register(SetRadiusPayload.TYPE, SetRadiusPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetPayoutPayload.TYPE, SetPayoutPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SetRadiusPayload.TYPE, (payload, context) ->
		{
			if (context.player().containerMenu instanceof SoulSpawnerMenu menu) menu.setRadius(payload.radius());
		});
		ServerPlayNetworking.registerGlobalReceiver(SetPayoutPayload.TYPE, (payload, context) ->
		{
			if (context.player().containerMenu instanceof SoulSpawnerMenu menu) menu.setPayout(payload.payout());
		});
	}
}
