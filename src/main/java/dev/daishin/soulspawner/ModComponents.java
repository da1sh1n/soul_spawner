package dev.daishin.soulspawner;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModComponents
{
	public static final DataComponentType<SpawnerContents> SPAWNER_CONTENTS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		SoulSpawnerMod.id("spawner_contents"),
		DataComponentType.<SpawnerContents>builder().persistent(SpawnerContents.CODEC).networkSynchronized(SpawnerContents.STREAM_CODEC).build()
	);

	private ModComponents()
	{
	}

	// calling this loads the class, which runs the static registrations
	static void register()
	{
	}
}
