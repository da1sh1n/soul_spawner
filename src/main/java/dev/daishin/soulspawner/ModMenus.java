package dev.daishin.soulspawner;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModMenus
{
	public static final ExtendedMenuType<SoulSpawnerMenu, SpawnerOpenData> SOUL_SPAWNER = Registry.register(
		BuiltInRegistries.MENU,
		SoulSpawnerMod.id("soul_spawner"),
		new ExtendedMenuType<>(SoulSpawnerMenu::new, SpawnerOpenData.STREAM_CODEC)
	);

	private ModMenus()
	{
	}

	// calling this loads the class, which runs the static registrations
	static void register()
	{
	}
}
