package dev.daishin.soulspawner;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// spawners placed per owner, for max_spawners_per_player
public class SpawnerCounts extends SavedData
{
	public static final Codec<SpawnerCounts> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).xmap(SpawnerCounts::new, counts -> counts._counts);
	public static final SavedDataType<SpawnerCounts> TYPE = new SavedDataType<>(SoulSpawnerMod.id("soul_spawner_counts"), SpawnerCounts::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<UUID, Integer> _counts;

	public SpawnerCounts()
	{
		_counts = new HashMap<>();
	}

	private SpawnerCounts(Map<UUID, Integer> counts)
	{
		_counts = new HashMap<>(counts);
	}

	public static SpawnerCounts get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }

	public int countOf(UUID owner) { return _counts.getOrDefault(owner, 0); }

	public void add(UUID owner)
	{
		_counts.merge(owner, 1, Integer::sum);
		setDirty();
	}

	public void remove(UUID owner)
	{
		_counts.computeIfPresent(owner, (key, count) -> count > 1 ? count - 1 : null);
		setDirty();
	}
}
