package dev.daishin.soulspawner.client;

import dev.daishin.soulspawner.ModBlocks;
import dev.daishin.soulspawner.SoulSpawnerBlockEntity;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// small text under the crosshair while it points at a Soul Spawner within normal reach
public class SpawnTimerHud implements HudElement
{
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int OFFSET_BELOW_CROSSHAIR = 12;
	private static final float TEXT_SCALE = 0.75F;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta)
	{
		Minecraft client = Minecraft.getInstance();
		if (client.screen != null || client.level == null || client.player == null) return;
		if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
		if (!client.player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)) return;
		if (!client.level.getBlockState(hit.getBlockPos()).is(ModBlocks.SOUL_SPAWNER)) return;
		if (!(client.level.getBlockEntity(hit.getBlockPos()) instanceof SoulSpawnerBlockEntity spawner)) return;

		Component text = SpawnTimerText.of(spawner.timer());
		graphics.pose().pushMatrix();
		graphics.pose().translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F + OFFSET_BELOW_CROSSHAIR);
		graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
		graphics.centeredText(client.font, text, 0, 0, TEXT_COLOR);
		graphics.pose().popMatrix();
	}
}
