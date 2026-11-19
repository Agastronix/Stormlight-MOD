package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.resource.StormlightAttachments;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class RadiantMode {
    public static final ResourceLocation SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "radiant_speed");
    public static final ResourceLocation STRENGTH_ID =
            ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "radiant_strength");

    public static final float DRAIN_PER_SECOND = 1.0F;

    public static void toggle(ServerPlayer player) {
        if (RadiantAttachments.isRadiantActive(player)) {
            deactivate(player);
            return;
        }
        // Requirements to light up.
        if (RadiantAttachments.getOath(player) < 1) return;
        if (StormlightAttachments.get(player) <= 0) {
            player.displayClientMessage(Component.literal("You have no Stormlight to draw upon."), true);
            return;
        }
        activate(player);
    }

    public static void activate(ServerPlayer player) {
        RadiantAttachments.setRadiantActive(player, true);
        player.setGlowingTag(true);
        addModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(player, Attributes.ATTACK_DAMAGE, STRENGTH_ID, 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        RadiantNetwork.sendToClient(player);
    }

    public static void deactivate(ServerPlayer player) {
        RadiantAttachments.setRadiantActive(player, false);
        player.setGlowingTag(false);
        removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, STRENGTH_ID);
        RadiantNetwork.sendToClient(player);
    }

    private static void addModifier(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst != null && !inst.hasModifier(id)) {
            inst.addTransientModifier(new AttributeModifier(id, amount, op)); // transient = never saved
        }
    }

    private static void removeModifier(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst != null) inst.removeModifier(id);
    }
}