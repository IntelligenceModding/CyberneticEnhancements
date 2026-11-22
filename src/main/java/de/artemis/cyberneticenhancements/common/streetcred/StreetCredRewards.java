package de.artemis.cyberneticenhancements.common.streetcred;

import de.artemis.cyberneticenhancements.common.block.RelicCacheTier;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.progression.ExperienceRewards;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * What earns Street Cred. A name is made by taking work and surviving it, so contracts are the steady source and the
 * dangerous or rare things are the big ones. Everything here is paid once per thing done (a contract, a kill, a first
 * find), never for time spent or for things that can be repeated cheaply, so there is nothing to farm.
 */
public final class StreetCredRewards {
    /** A name should take a long time to make: everything pays this share of the figures it was first worked out with. */
    private static final float PACE = 0.25F;
    public static final int TAROT_CARD = paced(25);
    public static final int SCHEMATIC = paced(15);
    public static final int CHROMIUM_DEPOSIT = paced(40);
    public static final int MAX_TAC_SQUAD = paced(150);

    private StreetCredRewards() {}

    private static int paced(int points) { return Math.max(1, Math.round(points * PACE)); }

    /** A finished contract pays twice what it pays in experience, so harder work and harder types both count for more. */
    public static int contract(QuestType type, int tier) {
        return paced(ExperienceRewards.quest(type, tier) * 2);
    }

    /** Putting down a cyberpsycho, whether or not a contract asked for it. */
    public static int cyberpsycho(int tier) {
        return paced(ExperienceRewards.cyberpsycho(tier));
    }

    /** Breaking into a cache for the first time; the better the cache, the bigger the name. */
    public static int relicCache(RelicCacheTier tier) {
        return paced(ExperienceRewards.relicCache(tier));
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof CyberpsychoEntity psycho)) return;
        var killer = event.getSource().getEntity();
        ServerPlayer credited = killer instanceof ServerPlayer player ? player
                : killer instanceof TamableAnimal pet && pet.getOwner() instanceof ServerPlayer owner ? owner
                : psycho.getKillCredit() instanceof ServerPlayer player ? player : null;
        if (credited != null) StreetCred.gain(credited, cyberpsycho(psycho.tier()));
    }
}
