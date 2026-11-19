package com.agastronix.stormlight.entity;

import com.agastronix.stormlight.radiant.RadiantOrder;
import net.minecraft.world.entity.player.Player;

/**
 * A spren capable of bonding a player into a Radiant order.
 *
 * <p>The oath pipeline (swearing, breaking, Shardblade summon/dismiss) talks to this
 * interface rather than to a concrete entity, so adding another spren only means adding
 * another implementor — the spren itself decides which order the bond grants.
 */
public interface BondingSpren {

    boolean isBonded();

    boolean isOwnedBy(Player player);

    void setOwner(Player player);

    void clearOwner();

    /** The owning player, or null when unbonded, offline, or called on the client. */
    Player getOwnerPlayer();

    /** The order a player joins by swearing the First Ideal to this spren. */
    RadiantOrder getGrantedOrder();
}
