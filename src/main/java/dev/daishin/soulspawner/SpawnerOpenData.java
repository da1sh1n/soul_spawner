package dev.daishin.soulspawner;

import dev.daishin.soulspawner.logic.SpawnerSettings;
import dev.daishin.soulspawner.logic.UpgradeToggles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// sent with the menu so the client needs no separate config sync
public record SpawnerOpenData(BlockPos pos, List<Integer> payouts, UpgradeToggles toggles)
{
	private static final StreamCodec<RegistryFriendlyByteBuf, UpgradeToggles> TOGGLES_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, UpgradeToggles::range,
		ByteBufCodecs.BOOL, UpgradeToggles::time,
		ByteBufCodecs.BOOL, UpgradeToggles::radius,
		ByteBufCodecs.BOOL, UpgradeToggles::speed,
		ByteBufCodecs.BOOL, UpgradeToggles::silencer,
		UpgradeToggles::new
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerOpenData> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, SpawnerOpenData::pos,
		ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SpawnerOpenData::payouts,
		TOGGLES_CODEC, SpawnerOpenData::toggles,
		SpawnerOpenData::new
	);

	static SpawnerOpenData current(BlockPos pos)
	{
		SpawnerSettings settings = CrystalAccess.spawnerSettings();
		return new SpawnerOpenData(pos, settings.payout_multipliers(), settings.upgrades_enabled());
	}
}
