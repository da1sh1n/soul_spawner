package dev.daishin.soulspawner;

import dev.daishin.soulspawner.logic.Upgrade;
import dev.daishin.soulcrystals.api.UpgradeToggles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// slot 0 is the crystal, slots 1..3 are the upgrades; the same class backs the block and the client menu
public class SpawnerContainer extends SimpleContainer
{
	public static final int CRYSTAL_SLOT = 0;
	public static final int UPGRADE_SLOTS = 3;

	private @Nullable Runnable _on_changed = null;

	public SpawnerContainer()
	{
		super(1 + UPGRADE_SLOTS);
	}

	public void setOnChanged(Runnable on_changed) { _on_changed = on_changed; }

	@Override
	public void setChanged()
	{
		super.setChanged();
		if (_on_changed != null) _on_changed.run();
	}

	@Override
	public int getMaxStackSize() { return 1; }

	public ItemStack crystal() { return getItem(CRYSTAL_SLOT); }

	public int tier() { return CrystalAccess.tier(crystal()); }

	// upgrades sitting in the slots the current crystal tier opens, minus the ones turned off in the config
	public List<Upgrade> activeUpgrades(UpgradeToggles toggles)
	{
		List<Upgrade> active = new ArrayList<>();
		for (Upgrade upgrade : installedUpgrades(tier()))
			if (toggles.isEnabled(upgrade.toggleKey())) active.add(upgrade);
		return active;
	}

	// upgrades in the first open_slots upgrade slots
	public List<Upgrade> installedUpgrades(int open_slots)
	{
		List<Upgrade> installed = new ArrayList<>();
		for (int i = 0; i < Math.min(open_slots, UPGRADE_SLOTS); i++)
			if (getItem(CRYSTAL_SLOT + 1 + i).getItem() instanceof UpgradeItem upgrade_item) installed.add(upgrade_item.upgrade());
		return installed;
	}
}
