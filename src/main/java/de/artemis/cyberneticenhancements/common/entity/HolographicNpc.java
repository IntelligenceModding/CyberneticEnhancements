package de.artemis.cyberneticenhancements.common.entity;

import net.minecraft.network.syncher.EntityDataAccessor;

public interface HolographicNpc {
    EntityDataAccessor<Long> hologramArrival();
    EntityDataAccessor<Long> hologramDeparture();
}
