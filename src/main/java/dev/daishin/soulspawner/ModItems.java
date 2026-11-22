package dev.daishin.soulspawner;

import dev.daishin.lib.Registration;
import dev.daishin.soulspawner.logic.Upgrade;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems
{
	public static final Item SOUL_SPAWNER = Registration.item(
		SoulSpawnerMod.id("soul_spawner"),
		properties -> new SoulSpawnerItem(ModBlocks.SOUL_SPAWNER, properties),
		new Item.Properties().useBlockDescriptionPrefix()
	);
	public static final Item SPAWN_SILENCER = Registration.item(
		SoulSpawnerMod.id("spawn_silencer"),
		properties -> new BlockItem(ModBlocks.SPAWN_SILENCER, properties),
		new Item.Properties().useBlockDescriptionPrefix()
	);
	public static final Item UPGRADE_BASE = plain("upgrade_base");
	public static final Item RANGE_UPGRADE = upgrade("range_upgrade", Upgrade.RANGE);
	public static final Item TIME_UPGRADE = upgrade("time_upgrade", Upgrade.TIME);
	public static final Item RADIUS_UPGRADE = upgrade("radius_upgrade", Upgrade.RADIUS);
	public static final Item SPEED_UPGRADE_1 = upgrade("speed_upgrade_1", Upgrade.SPEED_1);
	public static final Item SPEED_UPGRADE_2 = upgrade("speed_upgrade_2", Upgrade.SPEED_2);

	private ModItems()
	{
	}

	// calling this loads the class, which runs the static registrations
	static void register()
	{
		Registration.addToTab(SOUL_SPAWNER, SPAWN_SILENCER, UPGRADE_BASE, RANGE_UPGRADE, TIME_UPGRADE, RADIUS_UPGRADE, SPEED_UPGRADE_1, SPEED_UPGRADE_2);
	}

	private static Item plain(String path)
	{
		return Registration.item(SoulSpawnerMod.id(path), Item::new, new Item.Properties());
	}

	private static Item upgrade(String path, Upgrade upgrade)
	{
		return Registration.item(SoulSpawnerMod.id(path), properties -> new UpgradeItem(properties, upgrade), new Item.Properties());
	}
}
