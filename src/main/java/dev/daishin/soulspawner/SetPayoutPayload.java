package dev.daishin.soulspawner;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetPayoutPayload(int payout) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<SetPayoutPayload> TYPE = new CustomPacketPayload.Type<>(SoulSpawnerMod.id("set_payout"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetPayoutPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SetPayoutPayload::payout,
		SetPayoutPayload::new
	);

	@Override
	public Type<SetPayoutPayload> type() { return TYPE; }
}
