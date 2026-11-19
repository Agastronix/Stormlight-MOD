package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.resource.StormlightAttachments;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID)
public class RadiantEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        if (RadiantAttachments.isRadiantActive(sp)) {
            if (player.tickCount % 20 == 0 && !StormlightAttachments.consume(sp, RadiantMode.DRAIN_PER_SECOND)) {
                RadiantMode.deactivate(sp);
            }
            if (RadiantAttachments.getOath(sp) < 1) {
                RadiantMode.deactivate(sp);
            }
            // Stormlight sustains the body: no need to breathe.
            sp.setAirSupply(sp.getMaxAirSupply());
        }

        if (player.tickCount % 20 == 0) {
            RadiantNetwork.sendToClient(sp);
        }

        RadiantProgression.tick(sp);
    }

    // Radiants can't drown or suffocate inside blocks.
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && RadiantAttachments.isRadiantActive(player)
                && (event.getSource().is(DamageTypes.DROWN) || event.getSource().is(DamageTypes.IN_WALL))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("radiant")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("oath")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 5))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            RadiantProgression.swearOath(p, IntegerArgumentType.getInteger(ctx, "level"));
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("Oath set to " + IntegerArgumentType.getInteger(ctx, "level")), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("order")
                                .then(Commands.literal("windrunner").executes(ctx -> setOrder(ctx, RadiantOrder.WINDRUNNER)))
                                .then(Commands.literal("edgedancer").executes(ctx -> setOrder(ctx, RadiantOrder.EDGEDANCER)))
                                .then(Commands.literal("none").executes(ctx -> setOrder(ctx, RadiantOrder.NONE))))
                        .then(Commands.literal("progress")
                                .then(Commands.literal("complete").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    OathProgress.completeNext(p);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Oath tasks completed"), false);
                                    return 1;
                                }))
                                .then(Commands.literal("reset").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    OathProgress.reset(p);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Oath tasks reset"), false);
                                    return 1;
                                })))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    RadiantMode.deactivate(p);
                                    RadiantAttachments.setOath(p, 0);
                                    RadiantAttachments.setOrder(p, RadiantOrder.NONE);
                                    OathProgress.reset(p);
                                    RadiantNetwork.sendToClient(p);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Radiant progress reset"), false);
                                    return 1;
                                }))
        );
    }

    private static int setOrder(CommandContext<CommandSourceStack> ctx, RadiantOrder order) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        RadiantAttachments.setOrder(p, order);
        RadiantNetwork.sendToClient(p);
        ctx.getSource().sendSuccess(() -> Component.literal("Order set to " + order.getSerializedName()), false);
        return 1;
    }
}