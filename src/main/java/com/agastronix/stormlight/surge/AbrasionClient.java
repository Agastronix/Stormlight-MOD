package com.agastronix.stormlight.surge;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.radiant.ClientRadiantData;
import com.agastronix.stormlight.radiant.RadiantOrder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, value = Dist.CLIENT)
public class AbrasionClient {
    private static boolean active = false;

    private static double vx = 0.0;
    private static double vz = 0.0;

    private static final double GLIDE = 1;
    private static final double ACCEL = 0.1;
    private static final double MAX_SPEED = 1;
    private static final double STOP_THRESHOLD = 0.005;

    private static Boolean savedBobbing = null;

    public static void toggle() {
        active = !active;
        LocalPlayer p = Minecraft.getInstance().player;
        if (active && p != null) {
            Vec3 dm = p.getDeltaMovement();
            vx = dm.x; vz = dm.z;
        } else {
            vx = 0; vz = 0;
            if (savedBobbing != null) {                       // restore view bob on disengage
                Minecraft.getInstance().options.bobView().set(savedBobbing);
                savedBobbing = null;
            }
        }
        if (p != null) {
            p.displayClientMessage(Component.literal(active ? "Abrasion engaged." : "Abrasion released."), true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (ClientRadiantData.order != RadiantOrder.EDGEDANCER.ordinal()
                || ClientRadiantData.oath < 2 || !ClientRadiantData.active) {
            active = false; vx = 0; vz = 0;
            return;
        }

        // Sliding into a block kills all momentum.
        if (player.horizontalCollision) {
            vx = 0; vz = 0;
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            return;
        }

        float yawRad = player.getYRot() * ((float) Math.PI / 180F);
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);
        double wishX = player.xxa * cos - player.zza * sin;
        double wishZ = player.zza * cos + player.xxa * sin;
        double wishLen = Math.sqrt(wishX * wishX + wishZ * wishZ);
        boolean hasInput = wishLen > 0.01;
        if (hasInput) { wishX /= wishLen; wishZ /= wishLen; }

        vx = vx * GLIDE + wishX * ACCEL;
        vz = vz * GLIDE + wishZ * ACCEL;

        double speed = Math.sqrt(vx * vx + vz * vz);
        if (speed > MAX_SPEED) { double f = MAX_SPEED / speed; vx *= f; vz *= f; }
        if (!hasInput && speed < STOP_THRESHOLD) { vx = 0; vz = 0; }

        Vec3 dm = player.getDeltaMovement();
        player.setDeltaMovement(vx, dm.y, vz);

        // Not stepping — just gliding. Freeze the limb-swing so the model stops "walking".
        // (Two calls zero both the current and interpolated swing so there's no residual twitch.)
        if (!hasInput) {
            player.walkAnimation.update(0.0F, 1.0F);
            player.walkAnimation.update(0.0F, 1.0F);
        }

        // First-person view bob is driven by distance moved, so gliding triggers it too.
        // Force it off while coasting; restore the player's real setting otherwise.
        Minecraft mc = Minecraft.getInstance();
        if (!hasInput) {
            if (savedBobbing == null) savedBobbing = mc.options.bobView().get();
            mc.options.bobView().set(false);
        } else if (savedBobbing != null) {
            mc.options.bobView().set(savedBobbing);
            savedBobbing = null;
        }
    }
}