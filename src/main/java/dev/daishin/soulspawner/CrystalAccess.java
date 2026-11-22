package dev.daishin.soulspawner;

import dev.daishin.soulcrystals.api.CrystalTier;
import dev.daishin.soulcrystals.api.SoulCrystalsApi;
import dev.daishin.soulspawner.logic.SpawnerSettings;
import dev.daishin.soulspawner.logic.UpgradeToggles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

// the only place that talks to Soul Crystals
final class CrystalAccess
{
	private CrystalAccess()
	{
	}

	// crystal tier 1..3, or 0 when the stack is not a crystal
	static int tier(ItemStack stack) { return SoulCrystalsApi.tier(stack).map(CrystalTier::level).orElse(0); }

	static boolean isFull(ItemStack stack) { return SoulCrystalsApi.isFull(stack); }

	static Optional<EntityType<?>> mob(ItemStack stack) { return SoulCrystalsApi.mob(stack); }

	static boolean isBoss(EntityType<?> mob) { return SoulCrystalsApi.isBoss(mob); }

	static boolean hasNoSouls(Entity entity) { return SoulCrystalsApi.hasNoSouls(entity); }

	static void markNoSouls(Entity entity) { SoulCrystalsApi.markNoSouls(entity); }

	// server side only: the crystals config does not exist without a running server
	static SpawnerSettings spawnerSettings()
	{
		dev.daishin.soulcrystals.api.SpawnerSettings config = SoulCrystalsApi.config().spawner();
		List<Integer> mob_caps = new ArrayList<>();
		for (CrystalTier tier : CrystalTier.values()) mob_caps.add(config.mobCaps().get(tier));
		dev.daishin.soulcrystals.api.UpgradeToggles toggles = config.upgradesEnabled();
		return new SpawnerSettings(
			config.catchUpEnabled(),
			config.payoutMultipliers(),
			mob_caps,
			config.mobCapScale(),
			config.maxSpawnersPerPlayer(),
			new UpgradeToggles(toggles.range(), toggles.time(), toggles.radius(), toggles.speed(), toggles.silencer())
		);
	}
}
