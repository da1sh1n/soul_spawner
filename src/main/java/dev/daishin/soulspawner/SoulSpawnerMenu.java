package dev.daishin.soulspawner;

import dev.daishin.soulspawner.logic.SlotRules;
import dev.daishin.soulspawner.logic.SpawnTimer;
import dev.daishin.soulspawner.logic.TimerState;
import dev.daishin.soulspawner.logic.Upgrade;
import dev.daishin.soulcrystals.api.UpgradeToggles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class SoulSpawnerMenu extends AbstractContainerMenu
{
	public static final int CRYSTAL_X = 26;
	public static final int UPGRADE_X = 80;
	public static final int UPGRADE_SPACING = 24;
	public static final int SLOT_Y = 20;
	public static final int PLAYER_INVENTORY_Y = 84;

	private final Player _player;
	private final SpawnerContainer _container;
	private final ContainerData _data;
	private final ContainerLevelAccess _access;
	private final @Nullable SoulSpawnerBlockEntity _spawner;
	private final List<Integer> _payouts;
	private final UpgradeToggles _toggles;

	// client side: the data arrives with the menu and the slots are filled by the server
	public SoulSpawnerMenu(int id, Inventory inventory, SpawnerOpenData open_data)
	{
		this(id, inventory, new SpawnerContainer(), new SimpleContainerData(SoulSpawnerBlockEntity.DATA_COUNT), null, open_data);
	}

	// server side
	public SoulSpawnerMenu(int id, Inventory inventory, SoulSpawnerBlockEntity spawner)
	{
		this(id, inventory, spawner.container(), spawner.menuData(), spawner, SpawnerOpenData.current(spawner.getBlockPos()));
	}

	private SoulSpawnerMenu(int id, Inventory inventory, SpawnerContainer container, ContainerData data, @Nullable SoulSpawnerBlockEntity spawner, SpawnerOpenData open_data)
	{
		super(ModMenus.SOUL_SPAWNER, id);
		_player = inventory.player;
		_container = container;
		_data = data;
		_spawner = spawner;
		_payouts = open_data.payouts();
		_toggles = open_data.toggles();
		_access = ContainerLevelAccess.create(inventory.player.level(), open_data.pos());

		addSlot(new CrystalSlot(container, CRYSTAL_X, SLOT_Y));
		for (int i = 0; i < SpawnerContainer.UPGRADE_SLOTS; i++)
			addSlot(new UpgradeSlot(container, SpawnerContainer.CRYSTAL_SLOT + 1 + i, UPGRADE_X + i * UPGRADE_SPACING, SLOT_Y));
		addStandardInventorySlots(inventory, 8, PLAYER_INVENTORY_Y);
		addDataSlots(data);
	}

	// ========== State Shared By Both Sides ==========

	public int tier() { return _data.get(SoulSpawnerBlockEntity.DATA_TIER); }

	public int radius() { return _data.get(SoulSpawnerBlockEntity.DATA_RADIUS); }

	public int payout() { return _data.get(SoulSpawnerBlockEntity.DATA_PAYOUT); }

	public boolean isBoss() { return _data.get(SoulSpawnerBlockEntity.DATA_BOSS) == 1; }

	// synced about once a second; the screen counts down in between
	public SpawnTimer timer()
	{
		TimerState state = TimerState.values()[_data.get(SoulSpawnerBlockEntity.DATA_TIMER_STATE)];
		return new SpawnTimer(state, _data.get(SoulSpawnerBlockEntity.DATA_TIMER_TICKS), _data.get(SoulSpawnerBlockEntity.DATA_TIMER_MULTIPLIER));
	}

	public List<Integer> payouts() { return _payouts; }

	public UpgradeToggles toggles() { return _toggles; }

	public boolean hasUpgrade(Upgrade upgrade)
	{
		for (Upgrade installed : _container.installedUpgrades(tier()))
			if (installed == upgrade && _toggles.isEnabled(installed.toggleKey())) return true;
		return false;
	}

	public boolean isUpgradeSlotOpen(int slot) { return SlotRules.isUpgradeSlotOpen(slot - SpawnerContainer.CRYSTAL_SLOT - 1, tier()); }

	// ========== Server Side Actions ==========

	public void setRadius(int radius)
	{
		if (_spawner != null) _spawner.setRadius(radius);
	}

	public void setPayout(int payout)
	{
		if (_spawner != null) _spawner.setPayout(payout);
	}

	private void crystalChanged()
	{
		if (_spawner != null && _player instanceof ServerPlayer player) _spawner.returnClosedUpgrades(player);
	}

	@Override
	public boolean stillValid(Player player) { return stillValid(_access, player, ModBlocks.SOUL_SPAWNER); }

	@Override
	public ItemStack quickMoveStack(Player player, int slot_index)
	{
		Slot slot = slots.get(slot_index);
		if (!slot.hasItem()) return ItemStack.EMPTY;

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		int spawner_slots = _container.getContainerSize();
		boolean moved = slot_index < spawner_slots ? moveItemStackTo(stack, spawner_slots, slots.size(), true) : moveItemStackTo(stack, 0, spawner_slots, false);
		if (!moved) return ItemStack.EMPTY;

		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return original;
	}

	// ========== Slots ==========

	private class CrystalSlot extends Slot
	{
		CrystalSlot(SpawnerContainer container, int x, int y)
		{
			super(container, SpawnerContainer.CRYSTAL_SLOT, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) { return CrystalAccess.tier(stack) > 0 && CrystalAccess.isFull(stack); }

		@Override
		public void setChanged()
		{
			super.setChanged();
			crystalChanged();
		}
	}

	private class UpgradeSlot extends Slot
	{
		UpgradeSlot(SpawnerContainer container, int slot, int x, int y)
		{
			super(container, slot, x, y);
		}

		@Override
		public boolean isActive() { return isUpgradeSlotOpen(getContainerSlot()); }

		@Override
		public boolean mayPlace(ItemStack stack)
		{
			if (!(stack.getItem() instanceof UpgradeItem upgrade_item) || !isActive()) return false;
			return SlotRules.canAddUpgrade(upgradesInOtherSlots(), upgrade_item.upgrade());
		}

		private List<Upgrade> upgradesInOtherSlots()
		{
			List<Upgrade> others = new ArrayList<>();
			for (int i = SpawnerContainer.CRYSTAL_SLOT + 1; i <= SpawnerContainer.UPGRADE_SLOTS; i++)
				if (i != getContainerSlot() && _container.getItem(i).getItem() instanceof UpgradeItem other) others.add(other.upgrade());
			return others;
		}
	}
}
