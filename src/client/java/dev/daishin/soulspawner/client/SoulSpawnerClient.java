package dev.daishin.soulspawner.client;

import dev.daishin.soulspawner.ModMenus;
import dev.daishin.soulspawner.SoulSpawnerMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;

public class SoulSpawnerClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		MenuScreens.register(ModMenus.SOUL_SPAWNER, SoulSpawnerScreen::new);
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Identifier.fromNamespaceAndPath(SoulSpawnerMod.MOD_ID, "spawn_timer"), new SpawnTimerHud());
	}
}
