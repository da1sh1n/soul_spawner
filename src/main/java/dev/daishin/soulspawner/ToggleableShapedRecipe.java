package dev.daishin.soulspawner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

// a shaped recipe that stops matching while its upgrade is turned off in the config
public class ToggleableShapedRecipe extends NormalCraftingRecipe
{
	public static final MapCodec<ToggleableShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		ShapedRecipe.MAP_CODEC.forGetter(recipe -> recipe._shaped),
		Codec.STRING.fieldOf("upgrade").forGetter(recipe -> recipe._upgrade)
	).apply(i, ToggleableShapedRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ToggleableShapedRecipe> STREAM_CODEC = StreamCodec.composite(
		ShapedRecipe.STREAM_CODEC, recipe -> recipe._shaped,
		ByteBufCodecs.STRING_UTF8, recipe -> recipe._upgrade,
		ToggleableShapedRecipe::new
	);

	public static final RecipeSerializer<ToggleableShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final ShapedRecipe _shaped;
	private final String _upgrade;

	public ToggleableShapedRecipe(ShapedRecipe shaped, String upgrade)
	{
		super(new Recipe.CommonInfo(shaped.showNotification()), new CraftingRecipe.CraftingBookInfo(shaped.category(), shaped.group()));
		_shaped = shaped;
		_upgrade = upgrade;
	}

	@Override
	public RecipeSerializer<ToggleableShapedRecipe> getSerializer() { return SERIALIZER; }

	@Override
	protected PlacementInfo createPlacementInfo() { return _shaped.placementInfo(); }

	// the client has no world config, so it always matches; the server decides
	@Override
	public boolean matches(CraftingInput input, Level level)
	{
		if (!level.isClientSide() && !CrystalAccess.spawnerSettings().upgrades_enabled().isEnabled(_upgrade)) return false;
		return _shaped.matches(input, level);
	}

	@Override
	public ItemStack assemble(CraftingInput input) { return _shaped.assemble(input); }

	@Override
	public List<RecipeDisplay> display() { return _shaped.display(); }
}
