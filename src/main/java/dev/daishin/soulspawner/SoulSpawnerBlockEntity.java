package dev.daishin.soulspawner;

import dev.daishin.lib.DirtyPockets;
import dev.daishin.soulspawner.logic.CatchUp;
import dev.daishin.soulspawner.logic.SpawnRates;
import dev.daishin.soulspawner.logic.SpawnTimer;
import dev.daishin.soulcrystals.api.SpawnerSettings;
import dev.daishin.soulspawner.logic.TimerState;
import dev.daishin.soulspawner.logic.Upgrade;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SoulSpawnerBlockEntity extends BlockEntity implements ExtendedMenuProvider<SpawnerOpenData>
{
	public static final int DATA_RADIUS = 0;
	public static final int DATA_PAYOUT = 1;
	public static final int DATA_TIER = 2;
	public static final int DATA_BOSS = 3;
	public static final int DATA_TIMER_STATE = 4;
	public static final int DATA_TIMER_TICKS = 5;
	public static final int DATA_TIMER_MULTIPLIER = 6;
	public static final int DATA_COUNT = 7;

	private static final int TIMER_SYNC_TICKS = 20; // a fresh countdown value about once per second
	private static final String TIMER_STATE_KEY = "timer_state";
	private static final String TIMER_TICKS_KEY = "timer_ticks";
	private static final String TIMER_MULTIPLIER_KEY = "timer_multiplier";

	private final SpawnerContainer _container = new SpawnerContainer();
	private @Nullable UUID _owner = null;
	private int _radius = SpawnRates.DEFAULT_SPAWN_RADIUS;
	private int _payout = 0; // 0 = never chosen, resolves to the lowest allowed value
	private double _progress = 0;
	private double _owed = 0;
	private long _last_online_ticks = -1;
	private ItemStack _crystal_seen = ItemStack.EMPTY;
	private SpawnTimer _timer = SpawnTimer.IDLE; // last synced value, ticks counted from _timer_at
	private long _timer_at = 0;

	private final ContainerData _menu_data = new ContainerData()
	{
		@Override
		public int get(int id)
		{
			return switch (id)
			{
				case DATA_RADIUS -> _radius;
				case DATA_PAYOUT -> CatchUp.resolvePayout(_payout, CrystalAccess.spawnerSettings().payoutMultipliers());
				case DATA_TIER -> _container.tier();
				case DATA_BOSS -> isBossCrystal() ? 1 : 0;
				case DATA_TIMER_STATE -> _timer.state().ordinal();
				case DATA_TIMER_TICKS -> Math.min(Short.MAX_VALUE, _timer.ticks());
				case DATA_TIMER_MULTIPLIER -> _timer.multiplier();
				default -> 0;
			};
		}

		@Override
		public void set(int id, int value)
		{
		}

		@Override
		public int getCount() { return DATA_COUNT; }
	};

	public SoulSpawnerBlockEntity(BlockPos pos, BlockState state)
	{
		super(ModBlocks.SOUL_SPAWNER_ENTITY, pos, state);
		_container.setOnChanged(this::onContainerChanged);
	}

	public SpawnerContainer container() { return _container; }

	public ContainerData menuData() { return _menu_data; }

	public @Nullable UUID owner() { return _owner; }

	public void setOwner(UUID owner) { _owner = owner; setChanged(); }

	// ========== Settings From The Menu ==========

	public void setRadius(int radius)
	{
		if (!_container.activeUpgrades(CrystalAccess.spawnerSettings().upgradesEnabled()).contains(Upgrade.RADIUS)) return;
		_radius = SpawnRates.clampRadius(radius);
		setChanged();
	}

	public void setPayout(int payout)
	{
		SpawnerSettings settings = CrystalAccess.spawnerSettings();
		boolean has_time = _container.activeUpgrades(settings.upgradesEnabled()).contains(Upgrade.TIME);
		if (!has_time || isBossCrystal() || !settings.payoutMultipliers().contains(payout)) return;
		_payout = payout;
		setChanged();
	}

	// upgrades in slots a lower crystal closed go back to the player, last slot first
	public void returnClosedUpgrades(ServerPlayer player)
	{
		int tier = _container.tier();
		if (tier == 0) return;
		boolean returned = false;
		for (int slot = SpawnerContainer.UPGRADE_SLOTS; slot > tier; slot--)
		{
			ItemStack stack = _container.removeItemNoUpdate(SpawnerContainer.CRYSTAL_SLOT + slot);
			if (stack.isEmpty()) continue;
			DirtyPockets.give(player, stack);
			returned = true;
		}
		if (returned) _container.setChanged();
	}

	// ========== Spawn Loop ==========

	public static void serverTick(Level level, BlockPos pos, BlockState state, SoulSpawnerBlockEntity spawner)
	{
		if (level instanceof ServerLevel server_level) spawner.tick(server_level, pos);
	}

	private void tick(ServerLevel level, BlockPos pos)
	{
		SpawnerSettings settings = CrystalAccess.spawnerSettings();
		ItemStack crystal = _container.crystal();
		int tier = CrystalAccess.tier(crystal);
		Optional<EntityType<?>> mob = CrystalAccess.mob(crystal);
		if (_owner == null || tier == 0 || !CrystalAccess.isFull(crystal) || mob.isEmpty())
		{
			resetRunState();
			syncTimer(level, pos, SpawnTimer.IDLE);
			return;
		}

		List<Upgrade> upgrades = _container.activeUpgrades(settings.upgradesEnabled());
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(_owner);
		if (owner == null || !isInRange(owner, level, pos, upgrades.contains(Upgrade.RANGE)))
		{
			syncTimer(level, pos, new SpawnTimer(TimerState.WAITING_OWNER, 0, CatchUp.NO_PAYOUT));
			return;
		}

		boolean boss = CrystalAccess.isBoss(mob.get());
		int speed_level = 0;
		for (Upgrade upgrade : upgrades) speed_level = Math.max(speed_level, upgrade.speedLevel());

		boolean catch_up = !boss && settings.catchUpEnabled() && upgrades.contains(Upgrade.TIME);
		long online_ticks = OnlineTicks.of(owner);
		if (catch_up) addMissedRounds(online_ticks, SpawnRates.progressPerTick(speed_level, false, CatchUp.NO_PAYOUT) / SpawnRates.BASE_INTERVAL_TICKS);
		else _owed = 0;
		_last_online_ticks = online_ticks;

		boolean paying_out = catch_up && CatchUp.isPayingOut(_owed);
		int payout = paying_out ? CatchUp.resolvePayout(_payout, settings.payoutMultipliers()) : CatchUp.NO_PAYOUT;
		double progress_per_tick = SpawnRates.progressPerTick(speed_level, boss, payout);
		_progress += progress_per_tick;
		if (_progress >= SpawnRates.BASE_INTERVAL_TICKS)
		{
			_progress -= SpawnRates.BASE_INTERVAL_TICKS;
			runRound(level, pos, mob.get(), tier, boss, upgrades, settings, payout, paying_out);
		}

		// after a round the countdown shows the next one, at the speed and payout then in force
		boolean still_paying = catch_up && CatchUp.isPayingOut(_owed);
		int shown_payout = still_paying ? payout : CatchUp.NO_PAYOUT;
		double shown_rate = SpawnRates.progressPerTick(speed_level, boss, shown_payout);
		TimerState state = boss && mob.get() == EntityType.CREAKING ? TimerState.RESIN : TimerState.SPAWNING;
		syncTimer(level, pos, new SpawnTimer(state, SpawnRates.ticksUntilRound(_progress, shown_rate), shown_payout));
	}

	private void runRound(ServerLevel level, BlockPos pos, EntityType<?> mob, int tier, boolean boss, List<Upgrade> upgrades, SpawnerSettings settings, int payout, boolean paying_out)
	{
		boolean silent = isSilenced(level, pos) && settings.upgradesEnabled().silencer();
		int radius = SpawnRates.spawnRadius(upgrades.contains(Upgrade.RADIUS), _radius);
		SpawnRound round = new SpawnRound(level, pos, mob, SpawnRates.mobsPerRound(tier, boss), boss, radius, SpawnRates.mobCap(settings, tier, boss, payout), silent);
		if (round.run() && paying_out) _owed -= 1;
		setChanged();
	}

	// ========== Timer Sync ==========

	// the value as it stands now: the synced ticks minus the game ticks since they were synced
	public SpawnTimer timer()
	{
		if (!_timer.isCounting() || level == null) return _timer;
		int ticks = (int) Math.max(0, _timer.ticks() - (level.getGameTime() - _timer_at));
		return new SpawnTimer(_timer.state(), ticks, _timer.multiplier());
	}

	// sends a fresh value to the clients when the state changed or the old one drifted from the live countdown
	void syncTimer(ServerLevel level, BlockPos pos, SpawnTimer live)
	{
		long now = level.getGameTime();
		boolean same_kind = live.state() == _timer.state() && live.multiplier() == _timer.multiplier();
		boolean stale = live.isCounting() && (now - _timer_at >= TIMER_SYNC_TICKS || Math.abs(live.ticks() - timer().ticks()) > 1);
		if (same_kind && !stale) return;
		_timer = live;
		_timer_at = now;
		level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

	// only the timer: the client keeps no other spawner data
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		CompoundTag tag = new CompoundTag();
		tag.putInt(TIMER_STATE_KEY, _timer.state().ordinal());
		tag.putInt(TIMER_TICKS_KEY, _timer.ticks());
		tag.putInt(TIMER_MULTIPLIER_KEY, _timer.multiplier());
		return tag;
	}

	private void addMissedRounds(long online_ticks, double rounds_per_tick)
	{
		if (_last_online_ticks < 0) return;
		double missed = CatchUp.owedRounds(online_ticks - _last_online_ticks, rounds_per_tick);
		if (missed == 0) return;
		_owed += missed;
		setChanged();
	}

	private void resetRunState()
	{
		_progress = 0;
		_owed = 0;
		_last_online_ticks = -1;
	}

	private static boolean isInRange(ServerPlayer owner, ServerLevel level, BlockPos pos, boolean has_range)
	{
		if (!owner.isAlive() || owner.level() != level) return false;
		double range = SpawnRates.ownerRange(has_range);
		return owner.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= range * range;
	}

	private static boolean isSilenced(ServerLevel level, BlockPos pos)
	{
		for (Direction direction : Direction.values())
			if (level.getBlockState(pos.relative(direction)).is(ModBlocks.SPAWN_SILENCER)) return true;
		return false;
	}

	private boolean isBossCrystal()
	{
		Optional<EntityType<?>> mob = CrystalAccess.mob(_container.crystal());
		return mob.isPresent() && CrystalAccess.isBoss(mob.get());
	}

	// a different crystal in the slot clears the backlog
	private void onContainerChanged()
	{
		if (!ItemStack.matches(_container.crystal(), _crystal_seen))
		{
			_crystal_seen = _container.crystal().copy();
			resetRunState();
		}
		setChanged();
	}

	// ========== Saving ==========

	@Override
	protected void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		ContainerHelper.loadAllItems(input, _container.getItems());
		_crystal_seen = _container.crystal().copy();
		_owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		_radius = input.getIntOr("radius", SpawnRates.DEFAULT_SPAWN_RADIUS);
		_payout = input.getIntOr("payout", 0);
		_progress = input.getDoubleOr("progress", 0);
		_owed = input.getDoubleOr("owed", 0);
		_last_online_ticks = input.getLongOr("last_online_ticks", -1);
		// only present in the update tag; a disk load leaves the idle default until the first tick
		TimerState[] states = TimerState.values();
		_timer = new SpawnTimer(states[input.getIntOr(TIMER_STATE_KEY, 0)], input.getIntOr(TIMER_TICKS_KEY, 0), input.getIntOr(TIMER_MULTIPLIER_KEY, CatchUp.NO_PAYOUT));
		_timer_at = level != null ? level.getGameTime() : 0;
	}

	@Override
	protected void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, _container.getItems());
		output.storeNullable("owner", UUIDUtil.CODEC, _owner);
		output.putInt("radius", _radius);
		output.putInt("payout", _payout);
		output.putDouble("progress", _progress);
		output.putDouble("owed", _owed);
		output.putLong("last_online_ticks", _last_online_ticks);
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components)
	{
		super.applyImplicitComponents(components);
		SpawnerContents contents = components.get(ModComponents.SPAWNER_CONTENTS);
		if (contents == null) return;
		_container.setItem(SpawnerContainer.CRYSTAL_SLOT, contents.crystal().copy());
		// old data or /give can carry more upgrades than there are slots
		for (int i = 0; i < Math.min(contents.upgrades().size(), SpawnerContainer.UPGRADE_SLOTS); i++) _container.setItem(SpawnerContainer.CRYSTAL_SLOT + 1 + i, contents.upgrades().get(i).copy());
		_radius = contents.radius();
		_payout = contents.payout();
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components)
	{
		super.collectImplicitComponents(components);
		List<ItemStack> upgrades = new ArrayList<>();
		for (int i = 1; i <= SpawnerContainer.UPGRADE_SLOTS; i++) upgrades.add(_container.getItem(i).copy());
		if (!_container.isEmpty()) components.set(ModComponents.SPAWNER_CONTENTS, new SpawnerContents(_container.crystal().copy(), upgrades, _radius, _payout));
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state)
	{
		super.preRemoveSideEffects(pos, state);
		if (_owner != null && level != null && level.getServer() != null) SpawnerCounts.get(level.getServer()).remove(_owner);
	}

	// ========== Menu ==========

	@Override
	public Component getDisplayName() { return Component.translatable("container.soul_spawner.soul_spawner"); }

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new SoulSpawnerMenu(id, inventory, this); }

	@Override
	public SpawnerOpenData getScreenOpeningData(ServerPlayer player) { return SpawnerOpenData.current(worldPosition); }
}
