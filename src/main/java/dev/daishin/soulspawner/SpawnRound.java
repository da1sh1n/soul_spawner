package dev.daishin.soulspawner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// one spawn round: picks free spots in the radius where the mob fits, never above the spawner, without the light and spawn-rule checks
record SpawnRound(ServerLevel level, BlockPos pos, EntityType<?> mob, int mobs_per_round, boolean boss, int radius, int cap, boolean silent)
{
	private static final int RESIN_PER_BOSS_ROUND = 4;
	private static final int SPAWN_EVENT = 2004;
	private static final int SEARCH_DEPTH = 4; // blocks below the spawner searched for floor, whatever the radius
	// these need no floor; swimmers need water instead
	private static final Set<EntityType<?>> FLYERS = Set.of(EntityType.GHAST, EntityType.PHANTOM, EntityType.VEX, EntityType.BAT, EntityType.BEE,
		EntityType.PARROT, EntityType.ALLAY, EntityType.WITHER, EntityType.ENDER_DRAGON);
	private static final Set<MobCategory> WATER_CATEGORIES = Set.of(MobCategory.WATER_CREATURE, MobCategory.WATER_AMBIENT,
		MobCategory.UNDERGROUND_WATER_CREATURE, MobCategory.AXOLOTLS);

	// false when nothing could spawn (cap reached or no free spot): no pulse, and an owed catch-up round is kept
	boolean run()
	{
		if (boss && mob == EntityType.CREAKING)
		{
			Block.popResourceFromFace(level, pos, Direction.UP, new ItemStack(Items.RESIN_CLUMP, RESIN_PER_BOSS_ROUND));
			SoulSpawnerBlock.pulse(level, pos);
			return true;
		}

		int room = cap - countNearby();
		if (room <= 0 || spawnAtRandomCandidates(Math.min(mobs_per_round, room)) == 0) return false;
		SoulSpawnerBlock.pulse(level, pos);
		return true;
	}

	private AABB spawnArea() { return new AABB(pos).inflate(radius); }

	// only mobs of this spawner's type that carry the no-souls marker count towards the cap
	private int countNearby() { return level.getEntities(mob, spawnArea(), CrystalAccess::hasNoSouls).size(); }

	// each spawn takes a different random spot from the list; fewer when the list or the cap runs out; returns how many spawned
	private int spawnAtRandomCandidates(int wanted)
	{
		if (level.getDifficulty() == Difficulty.PEACEFUL && !mob.getCategory().isFriendly()) return 0;

		List<BlockPos> candidates = findCandidates();
		RandomSource random = level.getRandom();
		int spawned = 0;
		while (spawned < wanted && !candidates.isEmpty())
		{
			int pick = random.nextInt(candidates.size());
			BlockPos feet = candidates.get(pick);
			candidates.set(pick, candidates.get(candidates.size() - 1));
			candidates.remove(candidates.size() - 1);
			if (!trySpawnAt(feet)) continue;
			spawned++;
			// the new mob's box is taken: drop every spot that would overlap it
			AABB placed = spawnBox(feet);
			candidates.removeIf(other -> spawnBox(other).intersects(placed));
		}
		return spawned;
	}

	private AABB spawnBox(BlockPos feet) { return mob.getSpawnAABB(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5); }

	// feet spots in the radius, from the spawner's own level down to SEARCH_DEPTH below it, never above
	private List<BlockPos> findCandidates()
	{
		List<BlockPos> candidates = new ArrayList<>();
		for (int down = 0; down <= SEARCH_DEPTH; down++)
			for (int dx = -radius; dx <= radius; dx++)
				for (int dz = -radius; dz <= radius; dz++)
				{
					BlockPos feet = pos.offset(dx, -down, dz);
					if (!feet.equals(pos) && fits(feet)) candidates.add(feet);
				}
		return candidates;
	}

	private boolean fits(BlockPos feet)
	{
		// entities added in a chunk that is not entity-ticking yet would not be live mobs
		if (!level.isPositionEntityTicking(feet)) return false;
		boolean swimmer = WATER_CATEGORIES.contains(mob.getCategory());
		boolean needs_floor = !swimmer && !FLYERS.contains(mob);
		if (needs_floor && !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) return false;

		AABB box = spawnBox(feet);
		if (!level.noCollision(box)) return false;
		if (swimmer) return level.getFluidState(feet).is(FluidTags.WATER);
		return !level.containsAnyLiquid(box);
	}

	private boolean trySpawnAt(BlockPos feet)
	{
		RandomSource random = level.getRandom();
		double x = feet.getX() + 0.5;
		double y = feet.getY();
		double z = feet.getZ() + 0.5;
		if (!level.noCollision(mob.getSpawnAABB(x, y, z))) return false;

		Entity entity = mob.create(level, EntitySpawnReason.SPAWNER);
		if (entity == null) return false;
		entity.snapTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
		if (entity instanceof Mob spawned_mob)
		{
			if (!spawned_mob.checkSpawnObstruction(level)) return false;
			spawned_mob.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), EntitySpawnReason.SPAWNER, null);
		}

		// riders too, like a spider jockey's skeleton
		entity.getPassengersAndSelf().forEach(CrystalAccess::markNoSouls);
		if (silent) entity.setSilent(true);
		if (!level.tryAddFreshEntityWithPassengers(entity)) return false;

		level.levelEvent(SPAWN_EVENT, pos, 0);
		level.gameEvent(entity, GameEvent.ENTITY_PLACE, feet);
		if (entity instanceof Mob spawned_mob) spawned_mob.spawnAnim();
		return true;
	}
}
