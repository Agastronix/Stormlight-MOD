package com.agastronix.stormlight.item;

import com.agastronix.stormlight.StormlightArchiveMOD; // <-- match your generated main class name
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(StormlightArchiveMOD.MODID);

    // The "material" a Shardblade is made of. Values are deliberately extreme.
    public static final Tier SHARDBLADE_TIER = new SimpleTier(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, // blocks it can't properly harvest
            Integer.MAX_VALUE,        // durability -> effectively unbreakable
            9.0F,                     // mining speed
            8.0F,                     // tier damage bonus (real damage set in attributes below)
            22,                       // enchantability
            () -> Ingredient.EMPTY    // not repairable at an anvil
    );

    public static final DeferredItem<ShardbladeItem> SHARDBLADE = ITEMS.registerItem(
            "shardblade",
            props -> new ShardbladeItem(SHARDBLADE_TIER, props),
            new Item.Properties()
                    .fireResistant()
                    // attackDamage, attackSpeed, extraReach (in blocks)
                    .attributes(ShardbladeItem.createShardbladeAttributes(100.0, -2.0, 3.0))
    );

    public static final DeferredItem<FullSphereItem> FULL_SPHERE = ITEMS.registerItem(
            "full_sphere",
            FullSphereItem::new,
            new Item.Properties()
    );

    public static final DeferredItem<EmptySphereItem> EMPTY_SPHERE = ITEMS.registerItem(
            "empty_sphere",
            EmptySphereItem::new,
            new Item.Properties()
    );

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}