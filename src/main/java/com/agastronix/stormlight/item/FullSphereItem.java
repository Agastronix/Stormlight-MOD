package com.agastronix.stormlight.item;

import com.agastronix.stormlight.radiant.RadiantAttachments;
import com.agastronix.stormlight.resource.StormlightAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FullSphereItem extends Item {
    private static final float STORMLIGHT_PER_SPHERE = 100.0F;

    public FullSphereItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // Client just plays the swing; the server does the real work and syncs back.
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(held, true);
        }

        if (!RadiantAttachments.isRadiant(player)) {
            player.displayClientMessage(Component.literal("You have no bond to hold Stormlight."), true);
            return InteractionResultHolder.pass(held);
        }

        // No room for more Stormlight -> don't waste the sphere.
        if (StormlightAttachments.get(player) >= StormlightAttachments.MAX_STORMLIGHT) {
            return InteractionResultHolder.pass(held);
        }

        StormlightAttachments.add(player, STORMLIGHT_PER_SPHERE);

        ItemStack empty = new ItemStack(ModItems.EMPTY_SPHERE.get());
        held.shrink(1);

        if (held.isEmpty()) {
            // Hand is now free -> hand the empty sphere back (vanilla puts it in the slot).
            return InteractionResultHolder.sidedSuccess(empty, false);
        }
        // Had a stack of spheres -> drop the empty into inventory, keep the rest in hand.
        if (!player.getInventory().add(empty)) {
            player.drop(empty, false);
        }
        return InteractionResultHolder.sidedSuccess(held, false);
    }
}