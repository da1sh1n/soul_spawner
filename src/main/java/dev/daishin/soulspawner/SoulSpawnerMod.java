package dev.daishin.soulspawner;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoulSpawnerMod implements ModInitializer
{
	public static final String MOD_ID = "soul_spawner";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize()
	{
		ModComponents.register();
		ModBlocks.register();
		ModItems.register();
		ModMenus.register();
		ModRecipes.register();
		OnlineTicks.register();
		SpawnerNetworking.register();
		SpawnerLoot.register();
	}
}
