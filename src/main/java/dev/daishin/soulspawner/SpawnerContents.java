package dev.daishin.soulspawner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.daishin.soulspawner.logic.Upgrade;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.ChatFormatting;

// upgrades holds all three upgrade slots, empty stacks included, so slot positions survive
public record SpawnerContents(ItemStack crystal, List<ItemStack> upgrades, int radius, int payout) implements TooltipProvider
{
	public static final Codec<SpawnerContents> CODEC = RecordCodecBuilder.create(i -> i.group(
		ItemStack.OPTIONAL_CODEC.fieldOf("crystal").forGetter(SpawnerContents::crystal),
		ItemStack.OPTIONAL_CODEC.listOf().fieldOf("upgrades").forGetter(SpawnerContents::upgrades),
		Codec.INT.fieldOf("radius").forGetter(SpawnerContents::radius),
		Codec.INT.fieldOf("payout").forGetter(SpawnerContents::payout)
	).apply(i, SpawnerContents::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerContents> STREAM_CODEC = StreamCodec.composite(
		ItemStack.OPTIONAL_STREAM_CODEC, SpawnerContents::crystal,
		ItemStack.OPTIONAL_LIST_STREAM_CODEC, SpawnerContents::upgrades,
		ByteBufCodecs.VAR_INT, SpawnerContents::radius,
		ByteBufCodecs.VAR_INT, SpawnerContents::payout,
		SpawnerContents::new
	);

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components)
	{
		if (!crystal.isEmpty())
		{
			consumer.accept(Component.translatable("tooltip.soul_spawner.crystal", crystal.getHoverName()).withStyle(ChatFormatting.GRAY));
			CrystalAccess.mob(crystal).ifPresent(mob -> consumer.accept(Component.translatable("tooltip.soul_spawner.mob", mob.getDescription()).withStyle(ChatFormatting.GRAY)));
		}
		for (ItemStack upgrade : upgrades)
			if (!upgrade.isEmpty()) consumer.accept(Component.translatable("tooltip.soul_spawner.upgrade", upgrade.getHoverName()).withStyle(ChatFormatting.GRAY));
		if (hasUpgrade(Upgrade.RADIUS)) consumer.accept(Component.translatable("tooltip.soul_spawner.radius", radius).withStyle(ChatFormatting.DARK_GRAY));
		if (hasUpgrade(Upgrade.TIME)) consumer.accept(Component.translatable("tooltip.soul_spawner.payout", payout).withStyle(ChatFormatting.DARK_GRAY));
	}

	private boolean hasUpgrade(Upgrade wanted)
	{
		for (ItemStack stack : upgrades)
			if (stack.getItem() instanceof UpgradeItem upgrade_item && upgrade_item.upgrade() == wanted) return true;
		return false;
	}
}
