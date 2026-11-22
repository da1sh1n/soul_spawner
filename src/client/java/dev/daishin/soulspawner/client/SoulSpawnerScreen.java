package dev.daishin.soulspawner.client;

import dev.daishin.soulspawner.SetPayoutPayload;
import dev.daishin.soulspawner.SetRadiusPayload;
import dev.daishin.soulspawner.SoulSpawnerMenu;
import dev.daishin.soulspawner.SpawnerContainer;
import dev.daishin.soulspawner.UpgradeItem;
import dev.daishin.soulspawner.logic.SpawnRates;
import dev.daishin.soulspawner.logic.SpawnTimer;
import dev.daishin.soulspawner.logic.Upgrade;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SoulSpawnerScreen extends AbstractContainerScreen<SoulSpawnerMenu>
{
	private static final int SLIDER_X = 8;
	private static final int SLIDER_WIDTH = 160;
	private static final int SLIDER_HEIGHT = 18;
	private static final int RADIUS_SLIDER_Y = 44;
	private static final int PAYOUT_SLIDER_Y = 64;
	private static final int PANEL_COLOR = 0xFFC6C6C6;
	private static final int PANEL_LIGHT = 0xFFFFFFFF;
	private static final int PANEL_DARK = 0xFF555555;
	private static final int SLOT_COLOR = 0xFF8B8B8B;
	private static final int CLOSED_SLOT_COLOR = 0xFF3A3A3A;
	private static final int WARNING_COLOR = 0xFFFFCC00;
	private static final int WARNING_SIZE = 9;

	private static final int TIMER_COLOR = 0xFF404040;
	private static final int TIMER_RIGHT_EDGE = 168;
	private static final float TIMER_SCALE = 0.75F;

	private int _timer_seen = -1; // the synced ticks last seen; a change restarts the live countdown
	private int _timer_elapsed = 0;
	private StepSlider _radius_slider;
	private StepSlider _payout_slider;

	public SoulSpawnerScreen(SoulSpawnerMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title, 176, 166);
		inventoryLabelY = SoulSpawnerMenu.PLAYER_INVENTORY_Y - 11;
	}

	@Override
	protected void init()
	{
		super.init();
		List<Integer> radii = new ArrayList<>();
		for (int radius = SpawnRates.MIN_SPAWN_RADIUS; radius <= SpawnRates.MAX_SPAWN_RADIUS; radius++) radii.add(radius);
		_radius_slider = addRenderableWidget(new StepSlider(
			leftPos + SLIDER_X, topPos + RADIUS_SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT, "gui.soul_spawner.radius", radii,
			value -> ClientPlayNetworking.send(new SetRadiusPayload(value))
		));
		_payout_slider = addRenderableWidget(new StepSlider(
			leftPos + SLIDER_X, topPos + PAYOUT_SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT, "gui.soul_spawner.payout", menu.payouts(),
			value -> ClientPlayNetworking.send(new SetPayoutPayload(value))
		));
	}

	@Override
	protected void containerTick()
	{
		_radius_slider.visible = menu.hasUpgrade(Upgrade.RADIUS);
		_payout_slider.visible = menu.hasUpgrade(Upgrade.TIME) && !menu.isBoss() && !menu.payouts().isEmpty();
		if (!_radius_slider.isDragging()) _radius_slider.showValue(menu.radius());
		if (!_payout_slider.isDragging()) _payout_slider.showValue(menu.payout());

		int synced = menu.timer().ticks();
		_timer_elapsed = synced == _timer_seen ? _timer_elapsed + 1 : 0;
		_timer_seen = synced;
	}

	// the countdown in the title row, right aligned, clear of the slots and sliders
	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouse_x, int mouse_y)
	{
		super.extractLabels(graphics, mouse_x, mouse_y);
		SpawnTimer synced = menu.timer();
		Component text = SpawnTimerText.of(new SpawnTimer(synced.state(), Math.max(0, synced.ticks() - _timer_elapsed), synced.multiplier()));
		graphics.pose().pushMatrix();
		graphics.pose().translate(TIMER_RIGHT_EDGE - font.width(text) * TIMER_SCALE, titleLabelY + 1);
		graphics.pose().scale(TIMER_SCALE, TIMER_SCALE);
		graphics.text(font, text, 0, 0, TIMER_COLOR, false);
		graphics.pose().popMatrix();
	}

	// ========== Drawing ==========

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouse_x, int mouse_y, float a)
	{
		super.extractBackground(graphics, mouse_x, mouse_y, a);
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_DARK);
		graphics.fill(leftPos, topPos, leftPos + imageWidth - 1, topPos + imageHeight - 1, PANEL_LIGHT);
		graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, PANEL_COLOR);
		for (Slot slot : menu.slots)
		{
			int color = slot.isActive() ? SLOT_COLOR : CLOSED_SLOT_COLOR;
			graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, color);
		}
	}

	@Override
	protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouse_x, int mouse_y)
	{
		super.extractSlot(graphics, slot, mouse_x, mouse_y);
		if (isBossTimeUpgrade(slot.getItem())) drawWarningTriangle(graphics, slot.x + 11, slot.y + 8);
	}

	// a yellow triangle with a dark exclamation mark
	private void drawWarningTriangle(GuiGraphicsExtractor graphics, int x, int y)
	{
		for (int row = 0; row < WARNING_SIZE; row++)
		{
			int half_width = row / 2;
			graphics.fill(x - half_width, y + row, x + half_width + 1, y + row + 1, WARNING_COLOR);
		}
		graphics.text(font, "!", x - 2, y + 1, 0xFF000000, false);
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack)
	{
		List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
		if (isBossTimeUpgrade(stack)) lines.add(Component.translatable("gui.soul_spawner.catch_up_disabled_for_bosses").withStyle(ChatFormatting.RED));
		return lines;
	}

	private boolean isBossTimeUpgrade(ItemStack stack) { return menu.isBoss() && stack.getItem() instanceof UpgradeItem upgrade_item && upgrade_item.upgrade() == Upgrade.TIME; }
}
