package dev.daishin.soulspawner;

import com.mojang.serialization.DataResult;
import dev.daishin.soulspawner.logic.CatchUp;
import dev.daishin.soulspawner.logic.SlotRules;
import dev.daishin.soulspawner.logic.SpawnRates;
import dev.daishin.soulspawner.logic.SpawnTimer;
import dev.daishin.soulspawner.logic.TimerState;
import dev.daishin.soulspawner.logic.SpawnerSettings;
import dev.daishin.soulspawner.logic.Upgrade;
import dev.daishin.soulspawner.logic.UpgradeToggles;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import dev.daishin.soulcrystals.api.SoulData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class SoulSpawnerGameTest
{
	private static final double EPSILON = 1e-9;
	private static final SpawnerSettings DEFAULTS = SpawnerSettings.DEFAULTS;

	private static void check(GameTestHelper context, boolean condition, String message)
	{
		if (!condition) throw context.assertionException(Component.literal(message));
	}

	private static boolean near(double a, double b) { return Math.abs(a - b) < EPSILON; }

	// ========== Catch Up ==========

	@GameTest
	public void catchUpOwedRounds(GameTestHelper context)
	{
		double rounds_per_tick = 1.0 / SpawnRates.BASE_INTERVAL_TICKS;
		check(context, CatchUp.owedRounds(CatchUp.MIN_GAP_TICKS, rounds_per_tick) == 0, "a gap of 20 ticks is not away");
		check(context, CatchUp.owedRounds(0, rounds_per_tick) == 0, "no gap owes nothing");
		check(context, near(CatchUp.owedRounds(1200, rounds_per_tick), 1.0), "1200 ticks at one round per 600 owes 1 round (half speed)");
		check(context, near(CatchUp.owedRounds(21, 2.0), 21.0), "owed = gap * rate / 2");
		check(context, !CatchUp.isPayingOut(0.99) && CatchUp.isPayingOut(1.0), "pays out from one full round");
		context.succeed();
	}

	@GameTest
	public void payoutSliderFallsBackToAllowedValue(GameTestHelper context)
	{
		check(context, CatchUp.resolvePayout(5, List.of(3, 4, 5)) == 5, "allowed value stays");
		check(context, CatchUp.resolvePayout(5, List.of(3, 4)) == 4, "removed 5 falls back to the highest allowed below");
		check(context, CatchUp.resolvePayout(3, List.of(4, 5)) == 4, "nothing below falls back to the lowest allowed");
		check(context, CatchUp.resolvePayout(0, List.of(3, 4, 5)) == 3, "never chosen starts at the lowest");
		check(context, CatchUp.resolvePayout(5, List.of(5, 3, 4)) == 5, "list order does not matter");
		check(context, CatchUp.resolvePayout(4, List.of(5, 3)) == 3, "unordered fallback picks the highest below");
		check(context, CatchUp.resolvePayout(4, List.of()) == CatchUp.NO_PAYOUT, "no allowed values means no boost");
		context.succeed();
	}

	// ========== Rates And Caps ==========

	@GameTest
	public void mobsPerRoundAndCaps(GameTestHelper context)
	{
		check(context, SpawnRates.mobsPerRound(1, false) == 4, "small spawns 4");
		check(context, SpawnRates.mobsPerRound(2, false) == 6, "medium spawns 6");
		check(context, SpawnRates.mobsPerRound(3, false) == 9, "large spawns 9");
		check(context, SpawnRates.mobsPerRound(3, true) == 1, "bosses spawn 1");
		check(context, SpawnRates.mobCap(DEFAULTS, 1, false, 1) == 8, "small cap 8");
		check(context, SpawnRates.mobCap(DEFAULTS, 2, false, 1) == 16, "medium cap 16");
		check(context, SpawnRates.mobCap(DEFAULTS, 3, false, 1) == 32, "large cap 32");
		check(context, SpawnRates.mobCap(DEFAULTS, 2, true, 1) == 2, "boss cap 2");
		check(context, SpawnRates.mobCap(DEFAULTS, 3, false, 5) == 160, "large at 5x caps at 160");
		check(context, SpawnRates.mobCap(DEFAULTS, 2, true, 5) == 10, "boss at 5x caps at 10");
		SpawnerSettings doubled = new SpawnerSettings(true, List.of(3), List.of(8, 16, 32), 2.0, 0, UpgradeToggles.ALL_ENABLED);
		check(context, SpawnRates.mobCap(doubled, 1, false, 1) == 16, "mob_cap_scale doubles the cap");
		check(context, SpawnRates.mobCap(doubled, 1, true, 1) == 4, "mob_cap_scale applies to bosses");
		context.succeed();
	}

	@GameTest
	public void roundSpeed(GameTestHelper context)
	{
		double base = SpawnRates.BASE_INTERVAL_TICKS;
		check(context, near(base / SpawnRates.progressPerTick(0, false, 1), 600), "normal round every 600 ticks");
		check(context, near(base / SpawnRates.progressPerTick(1, false, 1), 400), "Speed I every 400 ticks (20 s)");
		check(context, Math.abs(base / SpawnRates.progressPerTick(2, false, 1) - 266.6667) < 0.001, "Speed II every 266.7 ticks (13.3 s)");
		check(context, near(base / SpawnRates.progressPerTick(0, true, 1), 2400), "boss round every 2400 ticks (120 s)");
		check(context, near(base / SpawnRates.progressPerTick(1, true, 1), 1600), "boss Speed I every 1600 ticks (80 s)");
		check(context, Math.abs(base / SpawnRates.progressPerTick(2, true, 1) - 1066.6667) < 0.001, "boss Speed II every 1066.7 ticks (53 s)");
		check(context, near(base / SpawnRates.progressPerTick(0, false, 3), 200), "3x payout triples the speed");
		context.succeed();
	}

	@GameTest
	public void rangeAndRadius(GameTestHelper context)
	{
		check(context, SpawnRates.ownerRange(false) == 16 && SpawnRates.ownerRange(true) == 32, "owner range 16 / 32");
		check(context, SpawnRates.spawnRadius(false, 8) == 4, "without the upgrade the radius is 4");
		check(context, SpawnRates.spawnRadius(true, 6) == 6, "chosen radius is used");
		check(context, SpawnRates.spawnRadius(true, 0) == 1 && SpawnRates.spawnRadius(true, 99) == 8, "radius is clamped to 1..8");
		context.succeed();
	}

	// ========== Slot Rules ==========

	@GameTest
	public void upgradeSlotsFollowTier(GameTestHelper context)
	{
		check(context, !SlotRules.isUpgradeSlotOpen(0, 0), "no crystal opens nothing");
		check(context, SlotRules.isUpgradeSlotOpen(0, 1) && !SlotRules.isUpgradeSlotOpen(1, 1), "small opens 1 slot");
		check(context, SlotRules.isUpgradeSlotOpen(1, 2) && !SlotRules.isUpgradeSlotOpen(2, 2), "medium opens 2 slots");
		check(context, SlotRules.isUpgradeSlotOpen(2, 3), "large opens 3 slots");
		context.succeed();
	}

	@GameTest
	public void oneOfEachUpgradeType(GameTestHelper context)
	{
		check(context, SlotRules.canAddUpgrade(List.of(), Upgrade.RANGE), "empty spawner accepts range");
		check(context, !SlotRules.canAddUpgrade(List.of(Upgrade.RANGE), Upgrade.RANGE), "second range refused");
		check(context, SlotRules.canAddUpgrade(List.of(Upgrade.RANGE, Upgrade.TIME), Upgrade.RADIUS), "different types fit");
		check(context, !SlotRules.canAddUpgrade(List.of(Upgrade.SPEED_1), Upgrade.SPEED_2), "Speed II refused next to Speed I");
		check(context, !SlotRules.canAddUpgrade(List.of(Upgrade.SPEED_2), Upgrade.SPEED_1), "Speed I refused next to Speed II");
		context.succeed();
	}

	@GameTest
	public void disabledUpgradesAreInert(GameTestHelper context)
	{
		UpgradeToggles no_speed = new UpgradeToggles(true, true, true, false, true);
		check(context, !no_speed.allows(Upgrade.SPEED_1) && !no_speed.allows(Upgrade.SPEED_2), "speed toggle covers both tiers");
		check(context, no_speed.allows(Upgrade.RANGE) && no_speed.isEnabled("silencer"), "other toggles stay on");
		context.succeed();
	}

	// ========== Game Objects ==========

	@GameTest
	public void spawnerContentsSurviveTheCodec(GameTestHelper context)
	{
		RegistryOps<Tag> ops = context.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		SpawnerContents contents = new SpawnerContents(
			ItemStack.EMPTY,
			List.of(new ItemStack(ModItems.RANGE_UPGRADE), ItemStack.EMPTY, new ItemStack(ModItems.TIME_UPGRADE)),
			6,
			4
		);
		DataResult<Tag> encoded = SpawnerContents.CODEC.encodeStart(ops, contents);
		check(context, encoded.result().isPresent(), "contents encode");
		SpawnerContents decoded = SpawnerContents.CODEC.parse(ops, encoded.result().get()).result().orElse(null);
		check(context, decoded != null, "contents decode");
		check(context, decoded.radius() == 6 && decoded.payout() == 4, "radius and payout kept");
		check(context, decoded.upgrades().size() == 3 && decoded.upgrades().get(1).isEmpty(), "empty slot position kept");
		check(context, decoded.upgrades().get(0).is(ModItems.RANGE_UPGRADE) && decoded.upgrades().get(2).is(ModItems.TIME_UPGRADE), "upgrades kept");
		context.succeed();
	}

	@GameTest
	public void redstonePulseLastsTwoTicks(GameTestHelper context)
	{
		BlockPos relative = new BlockPos(1, 1, 1);
		context.setBlock(relative, ModBlocks.SOUL_SPAWNER);
		SoulSpawnerBlock.pulse(context.getLevel(), context.absolutePos(relative));
		check(context, context.getBlockState(relative).getValue(SoulSpawnerBlock.POWERED), "powered right after the pulse");
		context.runAfterDelay(4, () ->
		{
			check(context, !context.getBlockState(relative).getValue(SoulSpawnerBlock.POWERED), "pulse ended");
			context.succeed();
		});
	}

	@GameTest
	public void emptySpawnerNeverSpawns(GameTestHelper context)
	{
		BlockPos relative = new BlockPos(1, 1, 1);
		context.setBlock(relative, ModBlocks.SOUL_SPAWNER);
		check(context, context.getBlockEntity(relative, SoulSpawnerBlockEntity.class) != null, "block entity exists");
		SoulSpawnerBlockEntity spawner = context.getBlockEntity(relative, SoulSpawnerBlockEntity.class);
		check(context, spawner.container().isEmpty() && spawner.container().tier() == 0, "starts empty");
		context.runAfterDelay(5, () ->
		{
			check(context, zombiesAround(context).isEmpty(), "nothing spawned");
			context.succeed();
		});
	}

	@GameTest
	public void brokenSpawnerKeepsItsContents(GameTestHelper context)
	{
		BlockPos relative = new BlockPos(1, 1, 1);
		context.setBlock(relative, ModBlocks.SOUL_SPAWNER);
		SoulSpawnerBlockEntity spawner = context.getBlockEntity(relative, SoulSpawnerBlockEntity.class);
		spawner.container().setItem(1, new ItemStack(ModItems.RANGE_UPGRADE));
		spawner.setRadius(7);

		BlockPos pos = context.absolutePos(relative);
		List<ItemStack> drops = Block.getDrops(context.getBlockState(relative), context.getLevel(), pos, spawner);
		check(context, drops.size() == 1 && drops.get(0).is(ModItems.SOUL_SPAWNER), "drops exactly the spawner item");
		SpawnerContents contents = drops.get(0).get(ModComponents.SPAWNER_CONTENTS);
		check(context, contents != null && contents.upgrades().get(0).is(ModItems.RANGE_UPGRADE), "the item holds the upgrade");

		BlockPos other = new BlockPos(3, 1, 1);
		context.setBlock(other, ModBlocks.SOUL_SPAWNER);
		SoulSpawnerBlockEntity placed = context.getBlockEntity(other, SoulSpawnerBlockEntity.class);
		placed.applyComponentsFromItemStack(drops.get(0));
		check(context, placed.container().getItem(1).is(ModItems.RANGE_UPGRADE), "placing restores the upgrade");
		context.succeed();
	}

	@GameTest
	public void upgradeRecipesFollowTheToggle(GameTestHelper context)
	{
		ItemStack gold = new ItemStack(Items.GOLD_BLOCK);
		ItemStack eye = new ItemStack(Items.ENDER_EYE);
		ItemStack base = new ItemStack(ModItems.UPGRADE_BASE);
		CraftingInput range_grid = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, eye, ItemStack.EMPTY, eye, base, eye, gold, gold, gold));
		Optional<RecipeHolder<CraftingRecipe>> found = context.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, range_grid, context.getLevel());
		check(context, found.isPresent() && found.get().value().assemble(range_grid).is(ModItems.RANGE_UPGRADE), "range recipe crafts a range upgrade");
		check(context, found.get().value() instanceof ToggleableShapedRecipe, "range recipe is toggleable");

		CraftingInput spawner_grid = CraftingInput.of(3, 3, List.of(
			new ItemStack(Items.IRON_BARS), new ItemStack(Items.NETHERITE_SCRAP), new ItemStack(Items.IRON_BARS),
			new ItemStack(Items.IRON_BARS), new ItemStack(Items.SOUL_SAND), new ItemStack(Items.IRON_BARS),
			new ItemStack(Items.IRON_BARS), new ItemStack(Items.NETHERITE_SCRAP), new ItemStack(Items.IRON_BARS)
		));
		Optional<RecipeHolder<CraftingRecipe>> spawner = context.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, spawner_grid, context.getLevel());
		check(context, spawner.isPresent() && spawner.get().value().assemble(spawner_grid).is(ModItems.SOUL_SPAWNER), "spawner recipe crafts a spawner");
		context.succeed();
	}

	// ========== With A Real Crystal ==========

	private static ItemStack crystal(String tier_name, EntityType<?> mob, double souls)
	{
		ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("soul_crystals", tier_name + "_crystal")));
		@SuppressWarnings("unchecked")
		DataComponentType<SoulData> soul_data = (DataComponentType<SoulData>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("soul_crystals", "soul_data"));
		stack.set(soul_data, new SoulData(mob, souls));
		return stack;
	}

	private static SpawnRound round(GameTestHelper context, BlockPos relative, EntityType<?> mob, int tier, boolean boss)
	{
		int cap = SpawnRates.mobCap(CrystalAccess.spawnerSettings(), tier, boss, 1);
		return new SpawnRound(context.getLevel(), context.absolutePos(relative), mob, SpawnRates.mobsPerRound(tier, boss), boss, 4, cap, false);
	}

	// counted around the spawner, so spots just outside the test structure count too
	private static List<net.minecraft.world.entity.Entity> zombiesAround(GameTestHelper context)
	{
		return new java.util.ArrayList<>(context.getLevel().getEntities(EntityType.ZOMBIE, new AABB(context.absolutePos(new BlockPos(4, 1, 4))).inflate(6), zombie -> true));
	}

	private static long countZombies(GameTestHelper context) { return context.getLevel().getEntities(EntityType.ZOMBIE, new AABB(context.absolutePos(new BlockPos(4, 1, 4))).inflate(6), zombie -> true).size(); }

	@GameTest
	public void crystalApiIsWired(GameTestHelper context)
	{
		ItemStack full = crystal("large", EntityType.ZOMBIE, 400);
		ItemStack half = crystal("small", EntityType.ZOMBIE, 50);
		check(context, CrystalAccess.tier(full) == 3 && CrystalAccess.tier(half) == 1 && CrystalAccess.tier(new ItemStack(Items.STICK)) == 0, "tiers read from the crystals");
		check(context, CrystalAccess.isFull(full) && !CrystalAccess.isFull(half), "full check");
		check(context, CrystalAccess.mob(full).orElse(null) == EntityType.ZOMBIE, "mob read from the crystal");
		check(context, CrystalAccess.isBoss(EntityType.WARDEN) && !CrystalAccess.isBoss(EntityType.ZOMBIE), "boss rule");
		SpawnerSettings settings = CrystalAccess.spawnerSettings();
		check(context, settings.equals(SpawnerSettings.DEFAULTS), "default crystals config matches the docs defaults");
		context.succeed();
	}

	private static final BlockPos SPAWNER_SPOT = new BlockPos(4, 1, 4);

	// stone floor under the radius, the spawner block stood in by stone
	private static void buildFloor(GameTestHelper context)
	{
		for (int x = 0; x <= 8; x++)
			for (int z = 0; z <= 8; z++)
				context.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		context.setBlock(SPAWNER_SPOT, Blocks.STONE);
	}

	private static void checkMobsFit(GameTestHelper context)
	{
		BlockPos spawner = context.absolutePos(SPAWNER_SPOT);
		for (var zombie : zombiesAround(context))
		{
			check(context, CrystalAccess.hasNoSouls(zombie), "spawned mob carries the no-souls marker");
			check(context, zombie.getBlockY() <= spawner.getY(), "mob feet at " + zombie.getBlockY() + " are above the spawner at " + spawner.getY());
			check(context, !zombie.blockPosition().equals(spawner), "no mob inside the spawner block");
			check(context, !context.getLevel().getBlockCollisions(zombie, zombie.getBoundingBox()).iterator().hasNext(), "mob box is free of blocks");
		}
	}

	private static void checkNoSharedSpots(GameTestHelper context)
	{
		List<net.minecraft.world.entity.Entity> zombies = zombiesAround(context);
		for (int i = 0; i < zombies.size(); i++)
			for (int j = i + 1; j < zombies.size(); j++)
			{
				check(context, !zombies.get(i).blockPosition().equals(zombies.get(j).blockPosition()), "two mobs share a block position");
				check(context, !zombies.get(i).getBoundingBox().intersects(zombies.get(j).getBoundingBox()), "two mob boxes intersect");
			}
	}

	@GameTest
	public void roundSpawnsExactlySixThenNineAndMarksThem(GameTestHelper context)
	{
		buildFloor(context);
		check(context, round(context, SPAWNER_SPOT, EntityType.ZOMBIE, 2, false).run(), "medium round ran");
		check(context, countZombies(context) == 6, "medium spawned " + countZombies(context) + ", expected 6");
		check(context, round(context, SPAWNER_SPOT, EntityType.ZOMBIE, 3, false).run(), "large round ran");
		check(context, countZombies(context) == 15, "large added " + (countZombies(context) - 6) + ", expected 9");
		checkMobsFit(context);
		checkNoSharedSpots(context);
		context.succeed();
	}

	@GameTest
	public void tightSpaceKeepsMobsFreeAndNotAboveTheSpawner(GameTestHelper context)
	{
		buildFloor(context);
		// a low ceiling over half the area and pillars elsewhere
		for (int x = 0; x < 4; x++)
			for (int z = 0; z <= 8; z++)
				context.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
		for (int x = 5; x <= 7; x += 2)
			for (int z = 1; z <= 7; z += 2)
				context.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
		check(context, round(context, SPAWNER_SPOT, EntityType.ZOMBIE, 3, false).run(), "round ran");
		check(context, countZombies(context) == 9, "spawned " + countZombies(context) + ", expected 9");
		checkMobsFit(context);
		checkNoSharedSpots(context);
		for (var zombie : zombiesAround(context))
		{
			double spawner_x = context.absolutePos(SPAWNER_SPOT).getX();
			boolean under_ceiling = zombie.getX() >= spawner_x - 4 && zombie.getX() < spawner_x && zombie.getY() > context.absolutePos(new BlockPos(0, 0, 0)).getY();
			check(context, !under_ceiling, "no mob under the low ceiling");
		}
		context.succeed();
	}

	@GameTest
	public void roundSpawnsOnlyIntoTheFewFreeSpots(GameTestHelper context)
	{
		for (int x = 0; x <= 8; x++)
			for (int y = -4; y <= 3; y++)
				for (int z = 0; z <= 8; z++)
					context.setBlock(new BlockPos(x, y, z), Blocks.STONE);
		BlockPos[] pockets = {new BlockPos(1, 1, 1), new BlockPos(7, 1, 1), new BlockPos(1, 1, 7)};
		for (BlockPos pocket : pockets)
		{
			context.setBlock(pocket, Blocks.AIR);
			context.setBlock(pocket.above(), Blocks.AIR);
		}
		check(context, round(context, SPAWNER_SPOT, EntityType.ZOMBIE, 3, false).run(), "round ran");
		check(context, countZombies(context) == 3, "spawned " + countZombies(context) + ", expected one per pocket");
		for (var zombie : zombiesAround(context))
		{
			boolean in_pocket = false;
			for (BlockPos pocket : pockets) if (zombie.blockPosition().equals(context.absolutePos(pocket))) in_pocket = true;
			check(context, in_pocket, "mob stands in a free pocket, got " + zombie.blockPosition());
			check(context, !context.getLevel().getBlockCollisions(zombie, zombie.getBoundingBox()).iterator().hasNext(), "mob is not inside a block");
		}
		context.succeed();
	}

	@GameTest
	public void roundWithNoFreeSpotSpawnsNothingAndKeepsBacklog(GameTestHelper context)
	{
		for (int x = 0; x <= 8; x++)
			for (int y = -4; y <= 3; y++)
				for (int z = 0; z <= 8; z++)
					context.setBlock(new BlockPos(x, y, z), Blocks.STONE);
		check(context, !round(context, SPAWNER_SPOT, EntityType.ZOMBIE, 3, false).run(), "a round with no free spot does not count: no pulse, catch-up kept");
		check(context, countZombies(context) == 0, "nothing spawned");
		context.succeed();
	}

	// radius 1 on one level has only the 8 blocks around the spawner
	@GameTest
	public void radiusOneOnOneLevelSpawnsEight(GameTestHelper context)
	{
		buildRadiusOneFloor(context);
		new SpawnRound(context.getLevel(), context.absolutePos(SPAWNER_SPOT), EntityType.ZOMBIE, 9, false, 1, 32, false).run();
		check(context, countZombies(context) == 8, "radius 1 spawned " + countZombies(context) + ", expected the 8 neighbours");
		checkMobsFit(context);
		checkNoSharedSpots(context);
		context.succeed();
	}

	// a second level under the spawner (a pit in the centre column) gives the ninth spot
	@GameTest
	public void radiusOneWithALowerLevelSpawnsNine(GameTestHelper context)
	{
		buildRadiusOneFloor(context);
		context.setBlock(new BlockPos(4, 0, 4), Blocks.AIR);
		context.setBlock(new BlockPos(4, -1, 4), Blocks.AIR);
		context.setBlock(new BlockPos(4, -2, 4), Blocks.STONE);
		new SpawnRound(context.getLevel(), context.absolutePos(SPAWNER_SPOT), EntityType.ZOMBIE, 9, false, 1, 32, false).run();
		check(context, countZombies(context) == 9, "radius 1 with a lower level spawned " + countZombies(context) + ", expected 9");
		checkMobsFit(context);
		checkNoSharedSpots(context);
		context.succeed();
	}

	// solid block around the spawner down to 6 below, only the ring floor at y 0 is open above
	private static void buildRadiusOneFloor(GameTestHelper context)
	{
		for (int x = 2; x <= 6; x++)
			for (int y = -6; y <= 0; y++)
				for (int z = 2; z <= 6; z++)
					context.setBlock(new BlockPos(x, y, z), Blocks.STONE);
		context.setBlock(SPAWNER_SPOT, Blocks.STONE);
	}

	@GameTest
	public void placingMoreUpgradesThanSlotsKeepsTheFirstOnes(GameTestHelper context)
	{
		BlockPos relative = new BlockPos(1, 1, 1);
		context.setBlock(relative, ModBlocks.SOUL_SPAWNER);
		SoulSpawnerBlockEntity spawner = context.getBlockEntity(relative, SoulSpawnerBlockEntity.class);
		List<ItemStack> upgrades = List.of(new ItemStack(ModItems.RANGE_UPGRADE), new ItemStack(ModItems.TIME_UPGRADE), new ItemStack(ModItems.RADIUS_UPGRADE), new ItemStack(ModItems.RANGE_UPGRADE), new ItemStack(ModItems.TIME_UPGRADE));
		spawner.applyComponents(net.minecraft.core.component.DataComponentMap.builder().set(ModComponents.SPAWNER_CONTENTS, new SpawnerContents(ItemStack.EMPTY, upgrades, 4, 0)).build(), net.minecraft.core.component.DataComponentPatch.EMPTY);
		check(context, spawner.container().getItem(SpawnerContainer.UPGRADE_SLOTS).is(ModItems.RADIUS_UPGRADE), "the first three upgrades were placed");
		context.succeed();
	}

	@GameTest
	public void timerStateSyncsThroughUpdateTagAndMenuData(GameTestHelper context)
	{
		BlockPos relative = new BlockPos(1, 1, 1);
		context.setBlock(relative, ModBlocks.SOUL_SPAWNER);
		SoulSpawnerBlockEntity spawner = context.getBlockEntity(relative, SoulSpawnerBlockEntity.class);
		BlockPos pos = context.absolutePos(relative);
		check(context, spawner.timer().state() == TimerState.NO_CRYSTAL, "an empty spawner reads no crystal");

		// a full crystal and an owner who is not online: waiting for the owner after one tick
		spawner.container().setItem(SpawnerContainer.CRYSTAL_SLOT, crystal("large", EntityType.ZOMBIE, 400));
		spawner.setOwner(java.util.UUID.randomUUID());
		SoulSpawnerBlockEntity.serverTick(context.getLevel(), pos, context.getBlockState(relative), spawner);
		check(context, spawner.timer().state() == TimerState.WAITING_OWNER, "owner not in range reads waiting for owner");
		check(context, spawner.menuData().get(SoulSpawnerBlockEntity.DATA_TIMER_STATE) == TimerState.WAITING_OWNER.ordinal(), "menu data holds the state");

		// the remaining ticks, with a catch-up multiplier, reach the update tag and the menu data
		spawner.syncTimer(context.getLevel(), pos, new SpawnTimer(TimerState.SPAWNING, 300, 4));
		SpawnTimer now = spawner.timer();
		check(context, now.state() == TimerState.SPAWNING && now.ticks() == 300 && now.multiplier() == 4, "server value is 300 ticks at 4x");
		var tag = spawner.getUpdateTag(context.getLevel().registryAccess());
		check(context, tag.getIntOr("timer_ticks", -1) == 300 && tag.getIntOr("timer_multiplier", -1) == 4, "update tag holds the ticks and multiplier");
		check(context, tag.getIntOr("timer_state", -1) == TimerState.SPAWNING.ordinal(), "update tag holds the state");
		check(context, spawner.getUpdatePacket() != null, "block entity sends an update packet");
		check(context, spawner.menuData().get(SoulSpawnerBlockEntity.DATA_TIMER_TICKS) == 300, "menu data holds the remaining ticks");
		check(context, spawner.menuData().get(SoulSpawnerBlockEntity.DATA_TIMER_MULTIPLIER) == 4, "menu data holds the multiplier");

		context.succeed();
	}

	@GameTest
	public void ticksUntilRoundFollowsSpeed(GameTestHelper context)
	{
		check(context, SpawnRates.ticksUntilRound(0, 1.0) == 600, "base speed from a fresh round");
		check(context, SpawnRates.ticksUntilRound(300, 1.5) == 200, "half done at 1.5x");
		check(context, SpawnRates.ticksUntilRound(599.5, 1.0) == 1, "rounds up to the next tick");
		context.succeed();
	}

	@GameTest
	public void roundStopsAtTheCapMidRound(GameTestHelper context)
	{
		buildFloor(context);
		new SpawnRound(context.getLevel(), context.absolutePos(SPAWNER_SPOT), EntityType.ZOMBIE, 9, false, 4, 5, false).run();
		check(context, countZombies(context) == 5, "stopped at the cap of 5, got " + countZombies(context));
		context.succeed();
	}

	@GameTest
	public void roundStopsAtTheCap(GameTestHelper context)
	{
		buildFloor(context);
		boolean blocked = false;
		for (int i = 0; i < 30 && !blocked; i++)
		{
			blocked = !round(context, SPAWNER_SPOT, EntityType.ZOMBIE, 1, false).run();
			check(context, countZombies(context) <= 8, "never above the small cap of 8");
		}
		check(context, blocked && countZombies(context) == 8, "rounds fill exactly to the cap, then are blocked");
		context.succeed();
	}

	@GameTest
	public void creakingCrystalDropsResinAndSpawnsNothing(GameTestHelper context)
	{
		check(context, round(context, new BlockPos(4, 1, 4), EntityType.CREAKING, 2, true).run(), "boss round ran");
		check(context, context.getEntities(EntityType.CREAKING).isEmpty(), "no creaking spawned");
		int resin = 0;
		for (var item : context.getEntities(EntityType.ITEM)) if (item.getItem().is(Items.RESIN_CLUMP)) resin += item.getItem().getCount();
		check(context, resin == 4, "4 resin clumps dropped, got " + resin);
		context.succeed();
	}

	@GameTest
	public void bossCrystalHasNoCatchUp(GameTestHelper context)
	{
		BlockPos relative = new BlockPos(1, 1, 1);
		context.setBlock(relative, ModBlocks.SOUL_SPAWNER);
		SoulSpawnerBlockEntity spawner = context.getBlockEntity(relative, SoulSpawnerBlockEntity.class);
		spawner.container().setItem(0, crystal("large", EntityType.WARDEN, 400));
		spawner.container().setItem(1, new ItemStack(ModItems.TIME_UPGRADE));
		check(context, spawner.menuData().get(SoulSpawnerBlockEntity.DATA_BOSS) == 1, "warden crystal is a boss crystal");
		spawner.setPayout(5);
		check(context, spawner.menuData().get(SoulSpawnerBlockEntity.DATA_PAYOUT) == 3, "payout cannot be set for a boss");

		spawner.container().setItem(0, crystal("medium", EntityType.ZOMBIE, 200));
		spawner.setPayout(5);
		check(context, spawner.menuData().get(SoulSpawnerBlockEntity.DATA_PAYOUT) == 5, "payout can be set for a normal crystal with Time");
		context.succeed();
	}

	@GameTest
	public void crystalTierOpensUpgradeSlots(GameTestHelper context)
	{
		SpawnerContainer container = new SpawnerContainer();
		container.setItem(0, crystal("large", EntityType.ZOMBIE, 400));
		container.setItem(3, new ItemStack(ModItems.RANGE_UPGRADE));
		check(context, container.activeUpgrades(UpgradeToggles.ALL_ENABLED).contains(Upgrade.RANGE), "large crystal opens slot 3");
		container.setItem(0, crystal("small", EntityType.ZOMBIE, 100));
		check(context, container.activeUpgrades(UpgradeToggles.ALL_ENABLED).isEmpty(), "small crystal closes slot 3");
		context.succeed();
	}
}
