package com.agastronix.stormlight.highstorm;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** The one global highstorm: schedule plus in-progress phase, saved with the overworld. */
public class HighstormData extends SavedData {
    private static final String NAME = StormlightArchiveMOD.MODID + "_highstorm";

    public HighstormPhase phase = HighstormPhase.NONE;
    public int phaseTicks = 0;
    public long nextStormDayTime = -1;  // overworld day time the next WARNING starts; -1 = not rolled yet
    long lastDayTime = -1;              // not saved: only used to spot /time set jumping backwards

    public static HighstormData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(HighstormData::new, HighstormData::load, null), NAME);
    }

    private static HighstormData load(CompoundTag tag, HolderLookup.Provider registries) {
        HighstormData data = new HighstormData();
        data.phase = HighstormPhase.byName(tag.getString("phase"));
        data.phaseTicks = tag.getInt("phase_ticks");
        data.nextStormDayTime = tag.contains("next_storm") ? tag.getLong("next_storm") : -1;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("phase", phase.name());
        tag.putInt("phase_ticks", phaseTicks);
        tag.putLong("next_storm", nextStormDayTime);
        return tag;
    }
}
