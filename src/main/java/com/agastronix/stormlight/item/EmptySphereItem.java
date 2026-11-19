package com.agastronix.stormlight.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmptySphereItem extends Item {
    public EmptySphereItem(Item.Properties properties) {
        super(properties);
    }

    // Spheres only recharge in a highstorm (see HighstormManager); right-click just says so.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.displayClientMessage(
                    Component.literal("Leave it out under open sky in a highstorm to recharge it."), true);
        }
        return InteractionResultHolder.consume(held);
    }
}
