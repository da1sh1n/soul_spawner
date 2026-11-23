package dev.daishin.soulspawner;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SoulSpawnerItem extends BlockItem
{
	public SoulSpawnerItem(Block block, Item.Properties properties)
	{
		super(block, properties);
	}

	@Override
	protected boolean canPlace(BlockPlaceContext context, BlockState state)
	{
		if (!super.canPlace(context, state)) return false;
		if (!(context.getPlayer() instanceof ServerPlayer player)) return true;

		int max = CrystalAccess.spawnerSettings().maxSpawnersPerPlayer();
		if (max <= 0 || SpawnerCounts.get(player.level().getServer()).countOf(player.getUUID()) < max) return true;
		player.sendSystemMessage(Component.translatable("message.soul_spawner.spawner_limit", max));
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag)
	{
		stack.addToTooltip(ModComponents.SPAWNER_CONTENTS, context, display, builder, flag);
	}
}
