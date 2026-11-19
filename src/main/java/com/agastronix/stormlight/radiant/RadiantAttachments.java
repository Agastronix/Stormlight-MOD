package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;
import java.util.function.Supplier;

public class RadiantAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StormlightArchiveMOD.MODID);

    // Bonded order — persists and survives death.
    public static final Supplier<AttachmentType<RadiantOrder>> ORDER =
            ATTACHMENT_TYPES.register("order",
                    () -> AttachmentType.builder(() -> RadiantOrder.NONE)
                            .serialize(RadiantOrder.CODEC).copyOnDeath().build());

    // Highest oath sworn: 0 = unbonded, 1-5.
    public static final Supplier<AttachmentType<Integer>> OATH =
            ATTACHMENT_TYPES.register("oath",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT).copyOnDeath().build());

    // Radiant mode on/off. Transient: not saved, resets each session and on death.
    public static final Supplier<AttachmentType<Boolean>> RADIANT_ACTIVE =
            ATTACHMENT_TYPES.register("radiant_active",
                    () -> AttachmentType.builder(() -> false).build());

    // Oath task progress, keyed by OathTask id. Persists and survives death; cleared when the bond breaks.
    public static final Supplier<AttachmentType<Map<String, Integer>>> OATH_PROGRESS =
            ATTACHMENT_TYPES.register("oath_progress",
                    () -> AttachmentType.<Map<String, Integer>>builder(() -> Map.of())
                            .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT)).copyOnDeath().build());

    public static RadiantOrder getOrder(Player p) { return p.getData(ORDER.get()); }
    public static void setOrder(Player p, RadiantOrder o) { p.setData(ORDER.get(), o); }

    public static int getOath(Player p) { return p.getData(OATH.get()); }
    public static void setOath(Player p, int oath) { p.setData(OATH.get(), Math.max(0, Math.min(5, oath))); }

    public static boolean isRadiantActive(Player p) { return p.getData(RADIANT_ACTIVE.get()); }
    public static void setRadiantActive(Player p, boolean active) { p.setData(RADIANT_ACTIVE.get(), active); }

    /** True once the First Ideal is sworn. This is the Stormlight gate. */
    public static boolean isRadiant(Player p) { return getOath(p) >= 1; }

    public static final Supplier<AttachmentType<Boolean>> SHARDBLADE_SUMMONED =
            ATTACHMENT_TYPES.register("shardblade_summoned",
                    () -> AttachmentType.builder(() -> false).build()); // transient: resets on death/logout

    public static boolean isShardbladeSummoned(Player p) { return p.getData(SHARDBLADE_SUMMONED.get()); }
    public static void setShardbladeSummoned(Player p, boolean v) { p.setData(SHARDBLADE_SUMMONED.get(), v); }

    // Bonded Deadeye blade, EMPTY = no bond. Persists, but not through death (the blade drops instead).
    public static final Supplier<AttachmentType<ItemStack>> DEADEYE_BLADE =
            ATTACHMENT_TYPES.register("deadeye_blade",
                    () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                            .serialize(ItemStack.OPTIONAL_CODEC, stack -> !stack.isEmpty()).build());

    // A copy of the bonded Deadeye is out in the world. Persists across relogs, not death.
    public static final Supplier<AttachmentType<Boolean>> DEADEYE_SUMMONED =
            ATTACHMENT_TYPES.register("deadeye_summoned",
                    () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    // Ticks left on a Deadeye summon, 0 = not charging. Transient: resets on death/logout.
    public static final Supplier<AttachmentType<Integer>> DEADEYE_CHARGE =
            ATTACHMENT_TYPES.register("deadeye_charge",
                    () -> AttachmentType.builder(() -> 0).build());

    public static ItemStack getDeadeye(Player p) { return p.getData(DEADEYE_BLADE.get()); }
    public static void setDeadeye(Player p, ItemStack blade) { p.setData(DEADEYE_BLADE.get(), blade); }
    public static boolean hasDeadeye(Player p) { return !getDeadeye(p).isEmpty(); }

    public static boolean isDeadeyeSummoned(Player p) { return p.getData(DEADEYE_SUMMONED.get()); }
    public static void setDeadeyeSummoned(Player p, boolean v) { p.setData(DEADEYE_SUMMONED.get(), v); }

    public static int getDeadeyeCharge(Player p) { return p.getData(DEADEYE_CHARGE.get()); }
    public static void setDeadeyeCharge(Player p, int ticks) { p.setData(DEADEYE_CHARGE.get(), ticks); }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}