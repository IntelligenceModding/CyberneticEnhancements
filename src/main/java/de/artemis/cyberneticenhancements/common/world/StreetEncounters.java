package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.entity.GangMemberEntity;
import de.artemis.cyberneticenhancements.common.entity.StreetRipperdocEntity;
import de.artemis.cyberneticenhancements.common.feedback.GameplayMessages;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue;
import de.artemis.cyberneticenhancements.common.streetcred.StreetCred;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Random street encounters: a wandering ripperdoc with inflated prices, and a gang demanding a toll. */
public final class StreetEncounters {
    public static final GameRules.Key<GameRules.BooleanValue> RULE =
            GameRules.register("cyberneticStreetEncounters", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
    /** Points for backing a gang down, or for turning down what a gang offers: respect, not money. */
    public static final int FACE_DOWN = 3;
    private static final String NEXT = "cyberneticenhancements.next_street_encounter";
    private static final int MIN_GAP = 20 * 60 * 20, MAX_GAP = 20 * 60 * 40, MIN_BALANCE = 50;
    private static final Map<UUID, Shakedown> ACTIVE = new HashMap<>();

    private record Shakedown(int demand, long deadline, List<UUID> members, boolean tribute) {}

    private StreetEncounters() {}

    public static boolean pending(UUID player) { return ACTIVE.containsKey(player); }

    public static void register() {} // Forces the game rule to register before any world loads.

    public static void stopped(ServerStoppedEvent event) { ACTIVE.clear(); }

    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        for (var entry : new ArrayList<>(ACTIVE.entrySet())) {
            if (server.overworld().getGameTime() < entry.getValue().deadline()) continue;
            var player = server.getPlayerList().getPlayer(entry.getKey());
            if (entry.getValue().tribute()) {
                // Nothing was demanded, so there is nothing to take offence at: they simply go.
                if (player != null) GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.tribute.timeout"));
                leave(server, entry.getKey());
                continue;
            }
            if (player != null) GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown.timeout").withStyle(ChatFormatting.RED));
            provoked(server, entry.getKey());
        }
        if (server.getTickCount() % 200 != 0 || !server.getGameRules().getBoolean(RULE)
                || de.artemis.cyberneticenhancements.common.tutorial.TutorialEnvironment.applies(server)) return;
        for (var player : server.getPlayerList().getPlayers()) roll(player);
    }

    private static void roll(ServerPlayer player) {
        var level = player.serverLevel();
        long now = level.getGameTime();
        var data = player.getPersistentData();
        if (!data.contains(NEXT)) { data.putLong(NEXT, now + gap(level)); return; }
        if (now < data.getLong(NEXT) || !player.isAlive() || player.isCreative() || player.isSpectator() || player.isSleeping()
                || level.dimension() != net.minecraft.world.level.Level.OVERWORLD || !level.canSeeSky(player.blockPosition())
                || ACTIVE.containsKey(player.getUUID())) return;
        data.putLong(NEXT, now + gap(level));
        var stance = ShakedownStance.of(player); // The street picks on a nobody far more often than on a name.
        boolean shakedown = level.random.nextFloat() < stance.chance() && level.getDifficulty() != Difficulty.PEACEFUL
                && (PlayerEurodollarManager.balance(player) >= MIN_BALANCE || stance.tributeChance() > 0);
        if (shakedown) startShakedown(player); else startRipperdoc(player);
    }

    private static long gap(ServerLevel level) { return MIN_GAP + level.random.nextInt(MAX_GAP - MIN_GAP); }

    private static BlockPos spot(ServerLevel level, ServerPlayer player, EntityType<?> type, double min, double max) {
        for (int attempt = 0; attempt < 48; attempt++) {
            // Crowded terrain (forests, hills, towns) gets a second chance closer in, rather than no encounter at all.
            double reach = attempt < 24 ? 1 : 0.6, angle = level.random.nextDouble() * Math.PI * 2;
            double distance = (min + level.random.nextDouble() * (max - min)) * reach;
            var column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(column)) continue;
            int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            for (int dy = 8; dy >= -12; dy--) {
                var pos = column.offset(0, dy, 0);
                if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
                // Underground pockets are out: stay with the open surface the player can see.
                if (pos.getY() < surface - 3) continue;
                if (level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) return pos;
            }
        }
        return null;
    }

    private static <T extends Mob> T place(ServerLevel level, EntityType<T> type, BlockPos pos) {
        T mob = type.create(level);
        if (mob == null) return null;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, (float) (level.random.nextFloat() * 360), 0);
        return mob;
    }

    public static boolean startRipperdoc(ServerPlayer player) {
        var level = player.serverLevel();
        var pos = spot(level, player, ModEntityTypes.STREET_RIPPERDOC.get(), 14, 24);
        if (pos == null) return false;
        StreetRipperdocEntity doc = place(level, ModEntityTypes.STREET_RIPPERDOC.get(), pos);
        if (doc == null) return false;
        doc.serve(player);
        if (!level.addFreshEntity(doc)) return false;
        GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.ripperdoc",
                (int) Math.round(Math.sqrt(doc.distanceToSqr(player))), StreetRipperdocEntity.WAIT_TICKS / 1200));
        return true;
    }

    public static boolean startShakedown(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return false;
        var level = player.serverLevel();
        var stance = ShakedownStance.of(player);
        int playerLevel = StreetCred.level(player);
        boolean tribute = level.random.nextDouble() < stance.tributeChance();
        var members = new ArrayList<UUID>();
        var pos = spot(level, player, ModEntityTypes.GANG_MEMBER.get(), 10, 16);
        if (pos == null) return false;
        for (int slot = 0; slot < stance.size(); slot++) {
            // Spread them along the line, falling back to the base spot where the neighbour is blocked.
            var at = pos.offset(slot - stance.size() / 2, 0, 0);
            GangMemberEntity thug = place(level, ModEntityTypes.GANG_MEMBER.get(), level.noCollision(
                    ModEntityTypes.GANG_MEMBER.get().getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5)) ? at : pos);
            if (thug == null) continue;
            thug.deploy(player, slot, thug.blockPosition(), stance.power());
            if (level.addFreshEntity(thug)) members.add(thug.getUUID());
        }
        if (members.isEmpty()) return false;
        int seconds = stance.answerTicks() / 20;
        var boss = level.getEntity(members.get(0));
        var speaker = boss == null ? Component.empty() : boss.getName();
        int amount = tribute ? ShakedownStance.tribute(playerLevel) : stance.demand(PlayerEurodollarManager.balance(player));
        if (!tribute) for (var id : members) if (level.getEntity(id) instanceof GangMemberEntity member) member.demand(amount);
        ACTIVE.put(player.getUUID(), new Shakedown(amount, level.getGameTime() + stance.answerTicks(), members, tribute));
        // They speak to you where they stand; there is nothing to open, and the clock keeps running while you think.
        if (boss != null) FaceDialogue.open(player, boss, shakedownNode(player, stance, speaker, tribute, amount, seconds), true,
                answering -> ACTIVE.containsKey(answering.getUUID()));
        return true;
    }

    private static FaceDialogue.Node shakedownNode(ServerPlayer player, ShakedownStance stance, Component speaker, boolean tribute, int amount, int seconds) {
        int level = StreetCred.level(player);
        var options = new ArrayList<FaceDialogue.Option>();
        if (tribute) {
            options.add(FaceDialogue.Option.of(Component.translatable("message.cyberneticenhancements.encounter.tribute.take", amount), p -> { pay(p); return null; }));
            options.add(FaceDialogue.Option.of(Component.translatable("message.cyberneticenhancements.encounter.tribute.decline"), p -> { refuse(p); return null; }));
            return new FaceDialogue.Node(speaker, Component.translatable("message.cyberneticenhancements.encounter.tribute", amount, seconds), options);
        }
        options.add(FaceDialogue.Option.of(Component.translatable("message.cyberneticenhancements.encounter.shakedown.pay", amount), p -> { pay(p); return null; }));
        options.add(FaceDialogue.Option.of(Component.translatable("message.cyberneticenhancements.encounter.shakedown.refuse"), p -> { refuse(p); return null; }));
        options.add(level >= ShakedownStance.NAME_LEVEL
                ? FaceDialogue.Option.of(Component.translatable("dialogue.cyberneticenhancements.shakedown.name",
                        Math.round(ShakedownStance.nameDropChance(level) * 100)), p -> { nameDrop(p); return null; })
                : FaceDialogue.Option.locked(Component.translatable("dialogue.cyberneticenhancements.shakedown.name.locked"),
                        Component.translatable("dialogue.cyberneticenhancements.requires_street_cred", ShakedownStance.NAME_LEVEL)));
        options.add(level >= ShakedownStance.STARE_LEVEL
                ? FaceDialogue.Option.of(Component.translatable("dialogue.cyberneticenhancements.shakedown.stare"), p -> { stareDown(p); return null; })
                : FaceDialogue.Option.locked(Component.translatable("dialogue.cyberneticenhancements.shakedown.stare"),
                        Component.translatable("dialogue.cyberneticenhancements.requires_street_cred", ShakedownStance.STARE_LEVEL)));
        return new FaceDialogue.Node(speaker, Component.translatable("message.cyberneticenhancements.encounter.shakedown." + stance.key(), amount, seconds), options);
    }

    /** Settles a pending shakedown by paying. Without enough money the gang takes offence instead. */
    public static boolean pay(ServerPlayer player) {
        var shakedown = ACTIVE.get(player.getUUID());
        if (shakedown == null) return false;
        if (shakedown.tribute()) {
            PlayerEurodollarManager.add(player, shakedown.demand(), "tribute", "Tribute from a street gang", null, "Street gang");
            GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.tribute.taken", shakedown.demand()));
            leave(player.server, player.getUUID());
            return true;
        }
        if (!PlayerEurodollarManager.tryWithdraw(player, shakedown.demand(), "shakedown", "Street gang toll", null, "Street gang")) {
            GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown.broke").withStyle(ChatFormatting.RED));
            provoked(player.server, player.getUUID());
            return true;
        }
        GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown.paid", shakedown.demand()));
        leave(player.server, player.getUUID());
        return true;
    }

    /** Refusing is a bluff: the more chrome on show, the likelier the gang backs down. Otherwise they attack. */
    public static boolean refuse(ServerPlayer player) {
        var shakedown = ACTIVE.get(player.getUUID());
        if (shakedown == null) return false;
        if (shakedown.tribute()) {
            // Turning down what a gang offers earns more than it would have cost them.
            GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.tribute.declined").withStyle(ChatFormatting.GOLD));
            StreetCred.gain(player, FACE_DOWN);
            leave(player.server, player.getUUID());
            return true;
        }
        boolean bluffed = player.getRandom().nextDouble() < ShakedownStance.of(player).bluffChance(new PlayerCyberwareInventory(player).getInstalledCount());
        GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown." + (bluffed ? "bluffed" : "refused"))
                .withStyle(bluffed ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (bluffed) leave(player.server, player.getUUID()); else provoked(player.server, player.getUUID());
        return true;
    }

    /** For names that need no words: the gang only has to look at who is standing there. No roll, and a name that is made by it. */
    public static boolean stareDown(ServerPlayer player) {
        var shakedown = ACTIVE.get(player.getUUID());
        if (shakedown == null || shakedown.tribute() || StreetCred.level(player) < ShakedownStance.STARE_LEVEL) return false;
        GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown.stare").withStyle(ChatFormatting.GOLD));
        StreetCred.gain(player, FACE_DOWN);
        leave(player.server, player.getUUID());
        return true;
    }

    /** Answering with a name instead of money: the gang either knows who they are dealing with, or does not and takes offence. */
    public static boolean nameDrop(ServerPlayer player) {
        var shakedown = ACTIVE.get(player.getUUID());
        if (shakedown == null || shakedown.tribute()) return false;
        double chance = ShakedownStance.nameDropChance(StreetCred.level(player));
        if (chance <= 0) {
            GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown.nobody").withStyle(ChatFormatting.RED));
            provoked(player.server, player.getUUID());
            return true;
        }
        boolean known = player.getRandom().nextDouble() < chance;
        GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown." + (known ? "recognised" : "unknown"))
                .withStyle(known ? ChatFormatting.GOLD : ChatFormatting.RED));
        if (known) {
            StreetCred.gain(player, FACE_DOWN);
            leave(player.server, player.getUUID());
        } else provoked(player.server, player.getUUID());
        return true;
    }

    /** The whole gang turns on the player, and the demand is over. */
    public static void provoked(MinecraftServer server, UUID player) {
        var shakedown = ACTIVE.remove(player);
        if (shakedown != null) members(server, shakedown).forEach(GangMemberEntity::provoke);
    }

    private static void leave(MinecraftServer server, UUID player) {
        var shakedown = ACTIVE.remove(player);
        if (shakedown != null) members(server, shakedown).forEach(GangMemberEntity::withdraw);
    }

    private static List<GangMemberEntity> members(MinecraftServer server, Shakedown shakedown) {
        var found = new ArrayList<GangMemberEntity>();
        for (var level : server.getAllLevels())
            for (var id : shakedown.members()) if (level.getEntity(id) instanceof GangMemberEntity member) found.add(member);
        return found;
    }
}
