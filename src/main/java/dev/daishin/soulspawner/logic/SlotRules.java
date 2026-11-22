package dev.daishin.soulspawner.logic;

import java.util.List;

public final class SlotRules
{
	private SlotRules()
	{
	}

	// upgrade_slot counts from 0; a crystal of tier N opens the first N upgrade slots
	public static boolean isUpgradeSlotOpen(int upgrade_slot, int tier) { return upgrade_slot < tier; }

	public static boolean canAddUpgrade(List<Upgrade> present, Upgrade added)
	{
		for (Upgrade upgrade : present)
			if (upgrade.sameTypeAs(added)) return false;
		return true;
	}
}
