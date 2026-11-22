package dev.daishin.soulspawner;

import dev.daishin.soulspawner.logic.Upgrade;
import net.minecraft.world.item.Item;

public class UpgradeItem extends Item
{
	private final Upgrade _upgrade;

	public UpgradeItem(Item.Properties properties, Upgrade upgrade)
	{
		super(properties);
		_upgrade = upgrade;
	}

	public Upgrade upgrade() { return _upgrade; }
}
