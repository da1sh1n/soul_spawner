package dev.daishin.soulspawner;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class SoulSpawnerBlock extends BaseEntityBlock
{
	public static final MapCodec<SoulSpawnerBlock> CODEC = simpleCodec(SoulSpawnerBlock::new);
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	private static final int PULSE_STRENGTH = 15;
	private static final int PULSE_TICKS = 2;

	public SoulSpawnerBlock(BlockBehaviour.Properties properties)
	{
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected MapCodec<SoulSpawnerBlock> codec() { return CODEC; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(POWERED); }

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SoulSpawnerBlockEntity(pos, state); }

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.SOUL_SPAWNER_ENTITY, SoulSpawnerBlockEntity::serverTick);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (level.getBlockEntity(pos) instanceof SoulSpawnerBlockEntity spawner) player.openMenu(spawner);
		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
	{
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!(placer instanceof ServerPlayer player) || !(level.getBlockEntity(pos) instanceof SoulSpawnerBlockEntity spawner)) return;
		spawner.setOwner(player.getUUID());
		SpawnerCounts.get(player.level().getServer()).add(player.getUUID());
	}

	// ========== Redstone Pulse ==========

	static void pulse(ServerLevel level, BlockPos pos)
	{
		BlockState state = level.getBlockState(pos);
		if (!state.is(ModBlocks.SOUL_SPAWNER) || state.getValue(POWERED)) return;
		level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
		level.scheduleTick(pos, ModBlocks.SOUL_SPAWNER, PULSE_TICKS);
		updateNeighborsOfNeighbors(level, pos);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		if (!state.getValue(POWERED)) return;
		level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
		updateNeighborsOfNeighbors(level, pos);
	}

	// strong power reaches blocks next to our neighbours, so those get an update too
	private static void updateNeighborsOfNeighbors(ServerLevel level, BlockPos pos)
	{
		for (Direction direction : Direction.values()) level.updateNeighborsAt(pos.relative(direction), ModBlocks.SOUL_SPAWNER, null);
	}

	@Override
	protected boolean isSignalSource(BlockState state) { return true; }

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return state.getValue(POWERED) ? PULSE_STRENGTH : 0; }

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return getSignal(state, level, pos, direction); }
}
