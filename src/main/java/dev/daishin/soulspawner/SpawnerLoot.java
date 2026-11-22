package dev.daishin.soulspawner;

import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

// an extra pool per table, so the rolls stay separate from the crystal loot
public final class SpawnerLoot
{
	private static final Map<ResourceKey<LootTable>, Float> CHANCES = Map.of(
		BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, 0.20F,
		BuiltInLootTables.ANCIENT_CITY, 0.10F,
		BuiltInLootTables.ABANDONED_MINESHAFT, 0.01F
	);

	private SpawnerLoot()
	{
	}

	static void register()
	{
		LootTableEvents.MODIFY.register((key, table, source, registries) ->
		{
			Float chance = CHANCES.get(key);
			if (chance == null || !source.isBuiltin()) return;
			table.withPool(LootPool.lootPool().add(LootItem.lootTableItem(ModItems.SOUL_SPAWNER)).when(LootItemRandomChanceCondition.randomChance(chance)));
		});
	}
}
