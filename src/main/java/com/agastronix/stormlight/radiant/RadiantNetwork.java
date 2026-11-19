package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.client.DeadeyeClientHandler;
import com.agastronix.stormlight.client.HonorSprenClientHandler;
import com.agastronix.stormlight.entity.BondingSpren;
import com.agastronix.stormlight.shardblade.BondDeadeyePayload;
import com.agastronix.stormlight.shardblade.DeadeyeManager;
import com.agastronix.stormlight.shardblade.OpenDeadeyeBondPayload;
import com.agastronix.stormlight.shardblade.ShardbladeManager;
import com.agastronix.stormlight.shardblade.ToggleShardbladePayload;
import com.agastronix.stormlight.surge.Surge;
import com.agastronix.stormlight.surge.SurgeHandler;
import com.agastronix.stormlight.surge.UseSurgePayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, bus = EventBusSubscriber.Bus.MOD)
public class RadiantNetwork {

    public static final int MAX_SPREN_OATH = 3;

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                RadiantSyncPayload.TYPE,
                RadiantSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientRadiantData.set(payload.order(), payload.oath(), payload.active()))
        );

        registrar.playToClient(
                OathProgressPayload.TYPE,
                OathProgressPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientOathProgress.set(payload.values()))
        );

        registrar.playToClient(
                OpenSprenDialoguePayload.TYPE,
                OpenSprenDialoguePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> HonorSprenClientHandler.open(payload))
        );

        registrar.playToServer(
                ToggleRadiantPayload.TYPE,
                ToggleRadiantPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        RadiantMode.toggle(sp);
                    }
                })
        );

        registrar.playToServer(
                SwearWordsPayload.TYPE,
                SwearWordsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer sp)) return;
                    Entity entity = sp.level().getEntity(payload.entityId());
                    if (!(entity instanceof BondingSpren spren)) return;
                    if (entity.distanceToSqr(sp) > 100.0) return;

                    int oath = RadiantAttachments.getOath(sp);
                    if (oath == 0) {
                        // First Ideal: bond this spren to the player.
                        if (spren.isBonded()) return;         // already taken
                        spren.setOwner(sp);
                        // The spren decides the order, and it must be set BEFORE the oath is
                        // sworn so every ideal from here on reads from the right order.
                        RadiantAttachments.setOrder(sp, spren.getGrantedOrder());
                        RadiantProgression.swearOath(sp, 1);
                    } else if (oath < MAX_SPREN_OATH) {
                        // Advancing: must be your own spren.
                        if (!spren.isOwnedBy(sp)) return;
                        // Belt and braces: an order lost somehow must never drift to the default.
                        if (RadiantAttachments.getOrder(sp) == RadiantOrder.NONE) {
                            RadiantAttachments.setOrder(sp, spren.getGrantedOrder());
                        }
                        // Server authority: the client's button state is never trusted.
                        if (!OathProgress.isMet(sp, oath + 1)) {
                            sp.displayClientMessage(Component.literal("Requirements not met"), true);
                            return;
                        }
                        RadiantProgression.swearOath(sp, oath + 1);
                    }
                })
        );

        registrar.playToServer(
                BreakBondPayload.TYPE,
                BreakBondPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer sp)) return;
                    Entity entity = sp.level().getEntity(payload.entityId());
                    if (!(entity instanceof BondingSpren spren)) return;
                    if (!spren.isOwnedBy(sp)) return;

                    RadiantProgression.breakBond(sp);       // reset player to non-radiant
                    if (sp.level() instanceof ServerLevel server) {
                        server.sendParticles(ParticleTypes.END_ROD,
                                entity.getX(), entity.getY() + 0.3, entity.getZ(), 25, 0.2, 0.3, 0.2, 0.02);
                    }
                    entity.discard();                       // returns to Shadesmar
                })
        );

        registrar.playToServer(
                UseSurgePayload.TYPE,
                UseSurgePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SurgeHandler.activate(sp, payload.slot());
                    }
                })
        );

        registrar.playToServer(
                ToggleShardbladePayload.TYPE,
                ToggleShardbladePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        ShardbladeManager.toggle(sp);
                    }
                })
        );

        registrar.playToClient(
                OpenDeadeyeBondPayload.TYPE,
                OpenDeadeyeBondPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> DeadeyeClientHandler.open(payload))
        );

        registrar.playToServer(
                BondDeadeyePayload.TYPE,
                BondDeadeyePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        DeadeyeManager.bond(sp, payload.hand());
                    }
                })
        );
    }

    public static void sendToClient(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new RadiantSyncPayload(
                RadiantAttachments.getOrder(player).ordinal(),
                RadiantAttachments.getOath(player),
                RadiantAttachments.isRadiantActive(player)
        ));
        OathProgress.sendToClient(player);
    }

    public static void sendOpenDialogue(ServerPlayer player, int entityId, boolean manage) {
        PacketDistributor.sendToPlayer(player,
                new OpenSprenDialoguePayload(entityId, RadiantAttachments.getOath(player), MAX_SPREN_OATH, manage));
    }
}