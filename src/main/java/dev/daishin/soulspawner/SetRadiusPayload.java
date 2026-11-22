package dev.daishin.soulspawner;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetRadiusPayload(int radius) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<SetRadiusPayload> TYPE = new CustomPacketPayload.Type<>(SoulSpawnerMod.id("set_radius"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetRadiusPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SetRadiusPayload::radius,
		SetRadiusPayload::new
	);

	@Override
	public Type<SetRadiusPayload> type() { return TYPE; }
}
