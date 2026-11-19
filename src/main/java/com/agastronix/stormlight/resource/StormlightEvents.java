package com.agastronix.stormlight.resource;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.radiant.RadiantAttachments;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID)
public class StormlightEvents {

    private static final float HEAL_AMOUNT = 1.0F;          // half a heart per heal tick
    private static final float STORMLIGHT_PER_HEAL = 5.0F;  // cost per half-heart
    private static final float HIGHSTORM_REFILL_PER_TICK = 10.0F;

    /** Ticks between heals: every 5 ticks at the First Ideal, down to every tick at the Fifth. */
    private static int healInterval(Player player) {
        return Math.max(1, 6 - RadiantAttachments.getOath(player));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Standing in a highstorm refills a Radiant to full (from empty in ~2.5 seconds).
        if (RadiantAttachments.isRadiant(player) && StormlightAttachments.isInHighstorm(player)) {
            StormlightAttachments.add(player, HIGHSTORM_REFILL_PER_TICK);
        }

        // Passive healing — faster with each Ideal sworn.
        if (RadiantAttachments.isRadiant(player)
                && StormlightAttachments.get(player) > 0
                && player.getHealth() < player.getMaxHealth()
                && player.tickCount % healInterval(player) == 0) {
            if (StormlightAttachments.consume(player, HEAL_AMOUNT * STORMLIGHT_PER_HEAL)) {
                player.heal(HEAL_AMOUNT);
            }
        }

        // Push the current value to the client HUD. (Fine to optimize to send-on-change later.)
        StormlightNetwork.sendToClient(serverPlayer, StormlightAttachments.get(player), StormlightAttachments.MAX_STORMLIGHT);
    }

    // Lets you fill up for testing: /stormlight 100
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("stormlight")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    float amount = FloatArgumentType.getFloat(ctx, "amount");
                                    StormlightAttachments.set(player, amount);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Stormlight set to " + amount), false);
                                    return 1;
                                })
                        )
        );
    }
}