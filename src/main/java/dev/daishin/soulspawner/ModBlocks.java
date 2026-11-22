package dev.daishin.soulspawner;

import java.util.function.Function;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks
{
	public static final SoulSpawnerBlock SOUL_SPAWNER = register(
		"soul_spawner",
		SoulSpawnerBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)
	);

	public static final Block SPAWN_SILENCER = register(
		"spawn_silencer",
		Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8F).sound(SoundType.WOOL)
	);

	public static final BlockEntityType<SoulSpawnerBlockEntity> SOUL_SPAWNER_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		SoulSpawnerMod.id("soul_spawner"),
		FabricBlockEntityTypeBuilder.create(SoulSpawnerBlockEntity::new, SOUL_SPAWNER).build()
	);

	private ModBlocks()
	{
	}

	// calling this loads the class, which runs the static registrations
	static void register()
	{
	}

	// 26.1 blocks need their registry key in the properties
	private static <T extends Block> T register(String path, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties)
	{
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SoulSpawnerMod.id(path));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}
}
