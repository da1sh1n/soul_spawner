package dev.daishin.soulspawner.logic;

public enum Upgrade
{
	RANGE("range", 0),
	TIME("time", 0),
	RADIUS("radius", 0),
	SPEED_1("speed", 1),
	SPEED_2("speed", 2);

	private final String _toggle_key;
	private final int _speed_level;

	Upgrade(String toggle_key, int speed_level)
	{
		_toggle_key = toggle_key;
		_speed_level = speed_level;
	}

	// the key used in the config toggles and in toggleable_shaped recipes
	public String toggleKey() { return _toggle_key; }

	// 0 for upgrades that are not a speed upgrade
	public int speedLevel() { return _speed_level; }

	// one of each upgrade type per spawner; both speed tiers count as one type
	public boolean sameTypeAs(Upgrade other) { return _toggle_key.equals(other._toggle_key); }
}
