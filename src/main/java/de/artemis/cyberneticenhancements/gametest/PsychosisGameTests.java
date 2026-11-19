package de.artemis.cyberneticenhancements.gametest;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PsychosisGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "fabrication-test");
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        return player;
    }


    @GameTest(template="sandevistan_test", batch="psychosis_priority", timeoutTicks=40)
    public static void prioritizesPlayersThenNpcsPetsHostilesAndPassiveMobs(GameTestHelper h) {
        var hunter=player(h); var other=player(h);
        try {
            hunter.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var origin=h.absolutePos(new BlockPos(2,1,2));
            for(int x=-2;x<=14;x++) for(int z=-3;z<=3;z++) { h.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState()); for(int y=0;y<5;y++) h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState()); }
            hunter.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
            other.setPos(hunter.getX()+10,hunter.getY(),hunter.getZ());
            var npc=ModEntityTypes.FIXER.get().create(h.getLevel());
            var pet=EntityType.WOLF.create(h.getLevel()); pet.tame(hunter);
            var hostile=EntityType.ZOMBIE.create(h.getLevel());
            var passive=EntityType.COW.create(h.getLevel());
            var order=List.of(npc,pet,hostile,passive);
            for(var mob:order) { mob.setNoAi(true); mob.moveTo(hunter.getX()+2,hunter.getY(),hunter.getZ());h.getLevel().addFreshEntity(mob); }
            var probe = new net.minecraft.world.entity.monster.Zombie(h.getLevel()); probe.moveTo(hunter.getX(),hunter.getY(),hunter.getZ());probe.setOnGround(true);
            var path = probe.getNavigation().createPath(other,0);
            h.assertTrue(PsychosisPursuitController.findTarget(hunter)==other,"Player priority: selected="+PsychosisPursuitController.findTarget(hunter)+" path="+path+" reachable="+(path!=null&&path.canReach())+" los="+hunter.hasLineOfSight(other)+" alive="+other.isAlive());
            de.artemis.cyberneticenhancements.common.world.DeathBodySync.downed(other,true);
            h.assertTrue(PsychosisPursuitController.findTarget(hunter)!=other,"Cached downed player remained a target");
            de.artemis.cyberneticenhancements.common.world.DeathBodySync.downed(other,false);
            other.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            for(var expected:order) {
                PsychosisPursuitController.clear(hunter);
                h.assertTrue(PsychosisPursuitController.findTarget(hunter)==expected,"Incorrect priority for "+expected.getType());
                if(expected==npc) {
                    de.artemis.cyberneticenhancements.common.world.DeathBodySync.downed(npc,true);
                    h.assertTrue(!PsychosisPursuitController.isValidTarget(npc),"Falling NPC remained attackable");
                    h.assertTrue(PsychosisPursuitController.findTarget(hunter)==pet,"Cached falling NPC was not immediately replaced");
                    de.artemis.cyberneticenhancements.common.world.DeathBodySync.downed(npc,false);
                }
                expected.discard();
            }
            PsychosisPursuitController.clear(hunter);
            h.assertTrue(PsychosisPursuitController.findTarget(hunter)==null,"Spectator became a target");
            h.succeed();
        } finally {PsychosisPursuitController.clear(hunter);hunter.server.getPlayerList().remove(hunter);other.server.getPlayerList().remove(other);}
    }
    @GameTest(template="sandevistan_test", batch="psychosis_combat", timeoutTicks=40)
    public static void forcedEpisodeDrivesAttacksAndClearsControlState(GameTestHelper h) {
        var hunter=player(h);
        try {
            hunter.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var victim=EntityType.COW.create(h.getLevel());victim.setNoAi(true);
            victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
            victim.setHealth(100);
            victim.moveTo(hunter.getX()+2,hunter.getY(),hunter.getZ());h.getLevel().addFreshEntity(victim);
            CyberwareEffects.refreshPlayerCyberware(hunter);
            float baselineHealth = hunter.getMaxHealth();
            double baselineDamage = hunter.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            CyberstrainManager.forcePsychosisEpisode(hunter,15);
            h.assertTrue(PsychosisIntro.active(hunter) && !hunter.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "Intro was skipped or blindness started before the look-up");
            hunter.getPersistentData().putLong(PsychosisIntro.START,h.getLevel().getGameTime()-PsychosisIntroMotion.DURATION);
            hunter.getPersistentData().putLong(PsychosisIntro.READY,h.getLevel().getGameTime()-1);
            CyberstrainManager.onPlayerTick(hunter);
            h.assertTrue(hunter.getMaxHealth() >= baselineHealth + 20 && hunter.getHealth() > baselineHealth, "Episode did not grant its temporary health buffer");
            h.assertTrue(hunter.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) >= baselineDamage + 9, "Episode did not grant its damage bonus");
            h.assertTrue(CyberstrainManager.isPsychosisActive(hunter),"Forced episode did not activate");
            h.assertTrue(!hunter.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS), "Episode still applies Darkness instead of Blindness");
            h.assertTrue(hunter.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "Episode did not apply vanilla Blindness");
            float health=victim.getHealth();
            victim.invulnerableTime=0;
            hunter.getPersistentData().putLong("cyberneticenhancements.psychosis_last_attack",h.getLevel().getGameTime()-20);
            CyberstrainManager.onPlayerTick(hunter);
            h.assertTrue(victim.getHealth()<health,"Autonomous episode did not attack a visible reachable target");
            h.assertTrue(hunter.getDeltaMovement().horizontalDistanceSqr()>0,"Episode did not drive pursuit movement");
            victim.invulnerableTime = 0;
            for (int y=0;y<3;y++) for (int z=-2;z<=2;z++) h.getLevel().setBlockAndUpdate(hunter.blockPosition().offset(1,y,z),Blocks.STONE.defaultBlockState());
            hunter.getPersistentData().putLong("cyberneticenhancements.psychosis_last_attack",h.getLevel().getGameTime()-20);
            health = victim.getHealth();
            PsychosisPursuitController.clear(hunter);
            CyberstrainManager.onPlayerTick(hunter);
            h.assertTrue(victim.getHealth()==health,"Autonomous attack passed through a solid wall");
            CyberstrainManager.clearPsychosis(hunter);
            h.assertTrue(!CyberstrainManager.isPsychosisActive(hunter),"Clearing the episode did not restore control state");
            h.assertTrue(!hunter.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "Episode blindness survived clearing");
            h.assertTrue(hunter.getMaxHealth()==baselineHealth && hunter.getHealth()<=baselineHealth, "Temporary health survived the episode");
            h.assertTrue(hunter.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==baselineDamage, "Temporary damage survived the episode");
            h.succeed();
        }finally{CyberstrainManager.clearPsychosis(hunter);hunter.server.getPlayerList().remove(hunter);}
    }
    @GameTest(template="sandevistan_test", batch="psychosis_death", timeoutTicks=40)
    public static void psychoticDeathCannotUseCoverageAndDoesNotSurviveRespawn(GameTestHelper h) {
        var subject=player(h);
        try {
            subject.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            subject.getPersistentData().putBoolean(de.artemis.cyberneticenhancements.common.world.TraumaCoverage.COVERAGE,true);
            CyberstrainManager.forcePsychosisEpisode(subject,30);
            // Mock players retain spawn protection; invoke the real lethal-damage death path directly.
            subject.setHealth(0);
            subject.die(subject.damageSources().generic());
            h.assertTrue(!subject.isAlive() && subject.getHealth()<=0,"MedTec prevented a psychotic death");
            h.assertTrue(!de.artemis.cyberneticenhancements.common.world.TraumaCoverage.downed(subject),"Psychotic death entered a revival state");
            h.assertTrue(!CyberstrainManager.isPsychosisActive(subject),"Death left psychosis and controls locked");
            h.assertTrue(!subject.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "Death retained episode blindness");
            CyberwareEffects.refreshPlayerCyberware(subject);
            CyberstrainManager.onPlayerTick(subject);
            h.assertTrue(!subject.isAlive(),"Attribute refresh resurrected a dead cyberpsycho");
            de.artemis.cyberneticenhancements.common.world.TraumaCoverage.callMedtec(subject);
            h.assertTrue(!subject.isAlive(),"Call MedTec revived a dead cyberpsycho");
            h.assertTrue(subject.getPersistentData().getBoolean(de.artemis.cyberneticenhancements.common.world.TraumaCoverage.COVERAGE),"Rejected rescue consumed coverage");
            var replacement = new ServerPlayer(subject.server,h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"replacement"),net.minecraft.server.level.ClientInformation.createDefault());
            CyberstrainManager.copyState(subject,replacement);
            h.assertTrue(!CyberstrainManager.isPsychosisActive(replacement),"Respawn inherited the fatal episode");
            h.succeed();
        } finally {subject.server.getPlayerList().remove(subject);}
    }

    @GameTest(template="sandevistan_test", batch="psychosis_stall", timeoutTicks=100)
    public static void stalledPlayerTargetYieldsToAnotherReachableTarget(GameTestHelper h) {
        var hunter=player(h); var other=player(h);
        var origin=h.absolutePos(new BlockPos(2,1,2));
        for(int x=-2;x<=14;x++) for(int z=-3;z<=3;z++) {
            h.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<5;y++) h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        hunter.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        hunter.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
        other.setPos(hunter.getX()+10,hunter.getY(),hunter.getZ());
        var alternative=EntityType.COW.create(h.getLevel()); alternative.setNoAi(true);
        alternative.moveTo(hunter.getX()+2,hunter.getY(),hunter.getZ());h.getLevel().addFreshEntity(alternative);
        h.assertTrue(PsychosisPursuitController.findTarget(hunter)==other,"Pursuit did not begin with the higher-priority player");
        h.runAfterDelay(65,()->{
            try {
                h.assertTrue(PsychosisPursuitController.findTarget(hunter)==alternative,"Stationary pursuit did not switch to another target");
                h.assertTrue(PsychosisPursuitController.findTarget(hunter)==alternative,"Skipped player was immediately selected again");
                h.succeed();
            }finally{
                PsychosisPursuitController.clear(hunter);
                hunter.server.getPlayerList().remove(hunter); other.server.getPlayerList().remove(other);
            }
        });
    }

    @GameTest(template="sandevistan_test", batch="psychosis_intro", timeoutTicks=180)
    public static void introHoldsCombatUntilFinalLookUp(GameTestHelper h) {
        var subject=player(h);
        var victim=EntityType.COW.create(h.getLevel());victim.setNoAi(true);
        victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);victim.setHealth(100);
        victim.moveTo(subject.getX()+2,subject.getY(),subject.getZ());h.getLevel().addFreshEntity(victim);
        CyberstrainManager.forcePsychosisEpisode(subject,15);
        h.assertTrue(PsychosisIntro.active(subject),"Intro did not begin");
        CyberstrainManager.onPlayerTick(subject);
        h.assertTrue(victim.getHealth()==100&&subject.getEffect(net.minecraft.world.effect.MobEffects.BLINDNESS).getDuration()>=40,"Intro began combat immediately");
        h.runAfterDelay(80,()->{
            CyberstrainManager.onPlayerTick(subject);
            h.assertTrue(PsychosisIntro.active(subject)&&victim.getHealth()==100,"Combat began before the final look-up");
            var blindness=subject.getEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
            h.assertTrue(blindness!=null&&blindness.getDuration()>=40,"Blindness countdown dropped into the flickering short-duration range");
        });
        h.runAfterDelay(105,()->{
            CyberstrainManager.onPlayerTick(subject);
            h.assertTrue(!PsychosisIntro.active(subject)&&subject.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),"Look-up did not begin the real episode");
            h.assertTrue(subject.getMaxHealth()>=40,"Combat bonuses were not activated after the intro");
            h.assertTrue(PsychosisIntro.settling(subject)&&victim.getHealth()==100&&subject.getDeltaMovement().horizontalDistanceSqr()==0,"Pursuit did not pause after the intro");
        });
        h.runAfterDelay(115,()->{
            CyberstrainManager.onPlayerTick(subject);
            h.assertTrue(victim.getHealth()==100&&PsychosisIntro.settling(subject),"Pursuit started before the one-second hold ended");
        });
        h.runAfterDelay(125,()->{
            try{
                CyberstrainManager.onPlayerTick(subject);
                h.assertTrue(!PsychosisIntro.settling(subject)&&victim.getHealth()<100,"Pursuit did not resume after the hold");
                h.succeed();
            }finally{CyberstrainManager.clearPsychosis(subject);subject.server.getPlayerList().remove(subject);}
        });
    }

    @GameTest(template="sandevistan_test", batch="psychosis_recovery", timeoutTicks=40)
    public static void recoveryStopsCombatUntilStandingAndDeathCancelsIt(GameTestHelper h) {
        var subject=player(h);
        try {
            subject.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            CyberstrainManager.forcePsychosisEpisode(subject,15);
            subject.getPersistentData().putLong(PsychosisIntro.START,h.getLevel().getGameTime()-PsychosisIntroMotion.DURATION);
            subject.getPersistentData().putLong(PsychosisIntro.READY,h.getLevel().getGameTime()-1);
            CyberstrainManager.onPlayerTick(subject);
            var victim=EntityType.COW.create(h.getLevel());victim.setNoAi(true);
            victim.moveTo(subject.getX()+2,subject.getY(),subject.getZ());h.getLevel().addFreshEntity(victim);
            float health=victim.getHealth();
            subject.getPersistentData().putLong("cyberneticenhancements.psychosis_until",h.getLevel().getGameTime());
            CyberstrainManager.onPlayerTick(subject);
            h.assertTrue(!CyberstrainManager.isPsychosisActive(subject)&&PsychosisRecovery.active(subject),"Natural expiry did not enter recovery");
            h.assertTrue(subject.getMaxHealth()==20&&subject.getDeltaMovement().horizontalDistanceSqr()==0,"Recovery retained combat bonuses or movement");
            subject.getPersistentData().putLong(PsychosisRecovery.START,h.getLevel().getGameTime()-100);
            CyberstrainManager.onPlayerTick(subject);
            h.assertTrue(victim.getHealth()==health&&PsychosisRecovery.active(subject),"Recovery attacked or ended before lowering the arm");
            subject.getPersistentData().putLong(PsychosisRecovery.START,h.getLevel().getGameTime()-PsychosisRecoveryMotion.DURATION);
            CyberstrainManager.onPlayerTick(subject);
            h.assertTrue(!PsychosisRecovery.pending(subject)&&subject.getXRot()==0,"Standing pose did not release the recovery lock");
            h.assertTrue(!subject.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),"Recovery retained blindness");
            CyberstrainManager.forcePsychosisEpisode(subject,15);
            CyberstrainManager.endPsychosisEpisode(subject);
            h.assertTrue(PsychosisRecovery.active(subject),"Manual ending skipped recovery");
            subject.setHealth(0);subject.die(subject.damageSources().generic());
            h.assertTrue(!PsychosisRecovery.pending(subject)&&!subject.isAlive(),"Death did not cancel the recovery animation");
            h.succeed();
        } finally {CyberstrainManager.clearPsychosis(subject);subject.server.getPlayerList().remove(subject);}
    }

    @GameTest(template="sandevistan_test", batch="psychosis_hunger", timeoutTicks=40)
    public static void hungerIsPausedWithoutRefillingFoodOrDeferringExhaustion(GameTestHelper h) {
        var subject=player(h);
        try {
            subject.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var food=subject.getFoodData();food.setFoodLevel(1);food.setSaturation(0);food.setExhaustion(5);
            CyberstrainManager.forcePsychosisEpisode(subject,15);
            for(int phase=0;phase<3;phase++) {
                if(phase==1) {
                    subject.getPersistentData().putLong(PsychosisIntro.START,h.getLevel().getGameTime()-PsychosisIntroMotion.DURATION);
                    subject.getPersistentData().putLong(PsychosisIntro.READY,h.getLevel().getGameTime()-1);
                    CyberstrainManager.onPlayerTick(subject);
                }
                if(phase==2)CyberstrainManager.endPsychosisEpisode(subject);
                h.assertTrue(CyberstrainManager.ignoresHunger(subject),"Hunger restriction returned during a locked phase");
                subject.causeFoodExhaustion(10);
                for(int tick=0;tick<100;tick++)food.tick(subject);
                h.assertTrue(food.getFoodLevel()==1&&food.getSaturationLevel()==0,"Psychosis consumed or refilled hunger");
                h.assertTrue(food.getExhaustionLevel()==5,"Psychosis accumulated or processed exhaustion");
            }
            CyberstrainManager.clearPsychosis(subject);
            h.assertTrue(!CyberstrainManager.ignoresHunger(subject),"Normal hunger rules did not resume");
            subject.causeFoodExhaustion(2);
            h.assertTrue(food.getExhaustionLevel()==7,"Normal exhaustion did not resume");
            food.tick(subject);
            h.assertTrue(food.getExhaustionLevel()<7,"Normal hunger processing remained disabled");
            h.succeed();
        } finally {CyberstrainManager.clearPsychosis(subject);subject.server.getPlayerList().remove(subject);}
    }

    @GameTest(template="sandevistan_test", batch="psychosis_risk_only", timeoutTicks=40)
    public static void strainOnlyChangesRiskAndHasNoPreEpisodePenalties(GameTestHelper h) {
        var subject=player(h);
        try {
            var inventory=new PlayerCyberwareInventory(subject);
            for(int strain:new int[]{0,9,10,19,20,34,35,49,50,75}) {
                subject.getPersistentData().putInt("cyberneticenhancements.cyberstrain_suppression",-strain);
                subject.getPersistentData().putLong("cyberneticenhancements.cyberstrain_suppression_until",h.getLevel().getGameTime()+1000);
                subject.getPersistentData().putLong("cyberneticenhancements.psychosis_cooldown_until",h.getLevel().getGameTime()+1000);
                var totals=new java.util.EnumMap<CyberwareEffectType,Double>(CyberwareEffectType.class);
                CyberstrainManager.applyPenalties(subject,inventory,totals);
                h.assertTrue(totals.isEmpty(),"Risk-only cyberstrain still changed stats at "+strain);
                h.assertTrue(CyberstrainManager.getHealingMultiplier(subject)==1&&CyberstrainManager.getAbilityCooldownMultiplier(subject,inventory)==1,"Strain still changed healing or cooldowns at "+strain);
                h.assertTrue(CyberstrainManager.getPsychosisStateLabel(subject,inventory).equals(strain<50?"STABLE":"EPISODE_RISK"),"Removed stage still appears at "+strain);
                CyberstrainManager.onPlayerTick(subject);
                h.assertTrue(!subject.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)&&!subject.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)&&!subject.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION),"Risk-only state still applied negative effects");
            }
            h.succeed();
        }finally{CyberstrainManager.clearTemporaryStatuses(subject);subject.server.getPlayerList().remove(subject);}
    }

}
