package dev.daishin.soulspawner.logic;

public record UpgradeToggles(boolean range, boolean time, boolean radius, boolean speed, boolean silencer)
{
	public static final UpgradeToggles ALL_ENABLED = new UpgradeToggles(true, true, true, true, true);

	// key is an Upgrade toggle key or "silencer"
	public boolean isEnabled(String key)
	{
		return switch (key)
		{
			case "range" -> range;
			case "time" -> time;
			case "radius" -> radius;
			case "speed" -> speed;
			case "silencer" -> silencer;
			default -> throw new IllegalArgumentException("Unknown upgrade key: " + key);
		};
	}

	public boolean allows(Upgrade upgrade) { return isEnabled(upgrade.toggleKey()); }
}
