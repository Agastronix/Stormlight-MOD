package com.agastronix.stormlight.loot;

import com.agastronix.stormlight.item.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Adds one unbonded Deadeye Shardblade to the loot, with probability {@code chance}.
 * Which tables it applies to, and each chance, live in data/stormlightarchivemod/loot_modifiers/.
 */
public class AddDeadeyeModifier extends LootModifier {
    public static final MapCodec<AddDeadeyeModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst)
                    .and(Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(m -> m.chance))
                    .apply(inst, AddDeadeyeModifier::new));

    private final float chance;

    public AddDeadeyeModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < chance) {
            generatedLoot.add(new ItemStack(ModItems.SHARDBLADE.get()));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
