package dev.daishin.soulspawner;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipes
{
	public static final RecipeSerializer<ToggleableShapedRecipe> TOGGLEABLE_SHAPED = Registry.register(
		BuiltInRegistries.RECIPE_SERIALIZER,
		SoulSpawnerMod.id("toggleable_shaped"),
		ToggleableShapedRecipe.SERIALIZER
	);

	private ModRecipes()
	{
	}

	// calling this loads the class, which runs the static registrations
	static void register()
	{
	}
}
