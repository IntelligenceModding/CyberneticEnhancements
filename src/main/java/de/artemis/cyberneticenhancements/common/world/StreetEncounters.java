package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.entity.GangMemberEntity;
import de.artemis.cyberneticenhancements.common.entity.StreetRipperdocEntity;
import de.artemis.cyberneticenhancements.common.feedback.GameplayMessages;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
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
    public static final int ANSWER_TICKS = 20 * 30, GANG_SIZE = 3;
    private static final String NEXT = "cyberneticenhancements.next_street_encounter";
    private static final int MIN_GAP = 20 * 60 * 20, MAX_GAP = 20 * 60 * 40, MIN_BALANCE = 50;
    private static final Map<UUID, Shakedown> ACTIVE = new HashMap<>();

    private record Shakedown(int demand, long deadline, List<UUID> members) {}

    private StreetEncounters() {}

    /** The toll: a tenth of the player's money, within a range that stays a nuisance rather than a ruin. */
    public static int demand(int balance) { return Math.clamp(balance / 10, 25, 400); }

    /** Chance that a refusal is taken seriously: every implant on show counts, up to three in four. */
    public static double bluffChance(int installedCount) { return Math.clamp(installedCount * 0.12, 0, 0.75); }

    public static boolean pending(UUID player) { return ACTIVE.containsKey(player); }

    public static void register() {} // Forces the game rule to register before any world loads.

    public static void stopped(ServerStoppedEvent event) { ACTIVE.clear(); }

    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        for (var entry : new ArrayList<>(ACTIVE.entrySet())) {
            if (server.overworld().getGameTime() < entry.getValue().deadline()) continue;
            var player = server.getPlayerList().getPlayer(entry.getKey());
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
        boolean shakedown = level.random.nextFloat() < 0.4F && level.getDifficulty() != Difficulty.PEACEFUL
                && PlayerEurodollarManager.balance(player) >= MIN_BALANCE;
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
        var members = new ArrayList<UUID>();
        var pos = spot(level, player, ModEntityTypes.GANG_MEMBER.get(), 10, 16);
        if (pos == null) return false;
        for (int slot = 0; slot < GANG_SIZE; slot++) {
            // Spread them along the line, falling back to the base spot where the neighbour is blocked.
            var at = pos.offset(slot - 1, 0, 0);
            GangMemberEntity thug = place(level, ModEntityTypes.GANG_MEMBER.get(), level.noCollision(
                    ModEntityTypes.GANG_MEMBER.get().getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5)) ? at : pos);
            if (thug == null) continue;
            thug.deploy(player, slot, thug.blockPosition());
            if (level.addFreshEntity(thug)) members.add(thug.getUUID());
        }
        if (members.isEmpty()) return false;
        int demand = demand(PlayerEurodollarManager.balance(player));
        ACTIVE.put(player.getUUID(), new Shakedown(demand, level.getGameTime() + ANSWER_TICKS, members));
        var pay = Component.translatable("message.cyberneticenhancements.encounter.shakedown.pay", demand).withStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cyberneticenhancements encounter pay")));
        var refuse = Component.translatable("message.cyberneticenhancements.encounter.shakedown.refuse").withStyle(Style.EMPTY
                .withColor(ChatFormatting.RED).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cyberneticenhancements encounter refuse")));
        GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown", demand, ANSWER_TICKS / 20)
                .append(Component.literal("\n")).append(pay).append(Component.literal("   ")).append(refuse));
        return true;
    }

    /** Settles a pending shakedown by paying. Without enough money the gang takes offence instead. */
    public static boolean pay(ServerPlayer player) {
        var shakedown = ACTIVE.get(player.getUUID());
        if (shakedown == null) return false;
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
        if (!ACTIVE.containsKey(player.getUUID())) return false;
        boolean bluffed = player.getRandom().nextDouble() < bluffChance(new PlayerCyberwareInventory(player).getInstalledCount());
        GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown." + (bluffed ? "bluffed" : "refused"))
                .withStyle(bluffed ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (bluffed) leave(player.server, player.getUUID()); else provoked(player.server, player.getUUID());
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
