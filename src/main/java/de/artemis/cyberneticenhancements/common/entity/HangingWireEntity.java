package de.artemis.cyberneticenhancements.common.entity;

import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LanternBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A copper wire hung between two points on blocks, sagging in between. The entity sits half way along it; the points it hangs
 * from are kept, and when either block is gone the wire comes down and drops its item.
 */
public final class HangingWireEntity extends Entity {
    public static final double MAX_LENGTH = 32;
    private static final EntityDataAccessor<Vector3f> START = SynchedEntityData.defineId(HangingWireEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> END = SynchedEntityData.defineId(HangingWireEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<CompoundTag> DECORATIONS = SynchedEntityData.defineId(HangingWireEntity.class, EntityDataSerializers.COMPOUND_TAG);
    public static final int MAX_DECORATIONS = 16;
    private static final double MIN_GAP = 0.5;
    /** A banner or lantern hung from the wire, {@code t} of the way along it. */
    public record Decoration(double t, ItemStack stack) {}
    private List<Decoration> decorations = new ArrayList<>();
    private List<BlockPos> lights = new ArrayList<>();
    private BlockPos startBlock = BlockPos.ZERO, endBlock = BlockPos.ZERO;
    private int checkTicks;

    public HangingWireEntity(EntityType<? extends HangingWireEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START, new Vector3f());
        builder.define(END, new Vector3f());
        builder.define(DECORATIONS, new CompoundTag());
    }

    public Vec3 start() { return new Vec3(entityData.get(START)); }

    public Vec3 end() { return new Vec3(entityData.get(END)); }

    public List<Decoration> decorations() { return decorations; }

    /** Whether the item can be hung from a wire: any banner, lantern or soul lantern. */
    public static boolean canHang(ItemStack stack) {
        return stack.is(ItemTags.BANNERS) || WireSigns.isSign(stack) || stack.getItem() instanceof BlockItem block && block.getBlock() instanceof LanternBlock;
    }

    /** The length of the wire, as far as spacing hung things goes. */
    private double length() { return start().distanceTo(end()); }

    /** Hangs one of the stack's items {@code t} of the way along, if it is a hangable item and there is room. */
    public boolean addDecoration(double t, ItemStack stack) {
        t = Math.clamp(t, 0.03, 0.97);
        if (!canHang(stack) || decorations.size() >= MAX_DECORATIONS) return false;
        for (Decoration other : decorations) {
            double width = WireSigns.isSign(stack) || WireSigns.isSign(other.stack()) ? 1.0 : MIN_GAP;
            if (Math.abs(other.t() - t) < width / Math.max(length(), width)) return false;
        }
        decorations.add(new Decoration(t, stack.copyWithCount(1)));
        syncDecorations();
        return true;
    }

    /** Takes down and returns the decoration nearest {@code t} if it is within {@code range} of the wire's length. */
    public ItemStack removeDecoration(double t, double range) {
        Decoration best = decorationAt(t, range);
        if (best == null) return ItemStack.EMPTY;
        decorations.remove(best);
        syncDecorations();
        return WireSigns.tidy(best.stack(), registryAccess());
    }

    /** The decoration nearest {@code t} if it is within {@code range} of the wire's length. */
    public Decoration decorationAt(double t, double range) {
        Decoration best = null;
        for (Decoration d : decorations) if (Math.abs(d.t() - t) * length() <= range && (best == null || Math.abs(d.t() - t) < Math.abs(best.t() - t))) best = d;
        return best;
    }

    private void replaceDecoration(Decoration old, ItemStack stack) {
        for (int i = 0; i < decorations.size(); i++)
            if (decorations.get(i) == old) { decorations.set(i, new Decoration(old.t(), stack)); break; }
        syncDecorations();
    }

    private void syncDecorations() {
        entityData.set(DECORATIONS, decorationsTag(), true);
        updateLights();
    }

    /** Where a hung lantern's flame is: below the wire point, since it hangs by its ring. */
    public static BlockPos lanternPos(Vec3 start, Vec3 end, double t) { return BlockPos.containing(point(start, end, t).add(0, -0.55, 0)); }

    /**
     * Hung lanterns light their surroundings like placed ones: an invisible light block is set where each hangs, in air only,
     * and taken away again when the lantern or the wire goes.
     */
    private void updateLights() {
        if (!(level() instanceof ServerLevel server)) return;
        Map<BlockPos, Integer> wanted = new HashMap<>();
        Vec3 start = start(), end = end();
        for (Decoration d : decorations)
            if (d.stack().getItem() instanceof BlockItem item && item.getBlock() instanceof LanternBlock lantern)
                wanted.merge(lanternPos(start, end, d.t()), lantern.defaultBlockState().getLightEmission(), Math::max);
        List<BlockPos> kept = new ArrayList<>();
        for (BlockPos old : lights) if (!wanted.containsKey(old) && server.isLoaded(old) && server.getBlockState(old).is(Blocks.LIGHT)) server.removeBlock(old, false);
        for (Map.Entry<BlockPos, Integer> entry : wanted.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!server.isLoaded(pos)) { if (lights.contains(pos)) kept.add(pos); continue; }
            BlockState state = server.getBlockState(pos);
            BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, entry.getValue());
            if (state.isAir() || state.is(Blocks.LIGHT) && lights.contains(pos)) {
                if (!state.equals(light)) server.setBlock(pos, light, 3);
                kept.add(pos);
            }
        }
        lights = kept;
    }

    private void clearLights() {
        if (level().isClientSide) return;
        for (BlockPos pos : lights) if (level().isLoaded(pos) && level().getBlockState(pos).is(Blocks.LIGHT)) level().removeBlock(pos, false);
        lights = new ArrayList<>();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy()) clearLights();
        super.remove(reason);
    }

    private CompoundTag decorationsTag() {
        ListTag list = new ListTag();
        for (Decoration d : decorations) {
            CompoundTag entry = new CompoundTag();
            entry.putDouble("T", d.t());
            entry.put("Item", d.stack().save(registryAccess()));
            list.add(entry);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("List", list);
        return tag;
    }

    private void loadDecorations(CompoundTag tag) {
        List<Decoration> loaded = new ArrayList<>();
        for (Tag entry : tag.getList("List", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            ItemStack stack = ItemStack.parse(registryAccess(), compound.getCompound("Item")).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) loaded.add(new Decoration(compound.getDouble("T"), stack));
        }
        decorations = loaded;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DECORATIONS.equals(accessor) && level().isClientSide) loadDecorations(entityData.get(DECORATIONS));
    }

    /** What the player does to a wire: hang the held item on it, take a hung item off it, or take the whole wire down. */
    public enum Action { HANG, TAKE, BREAK, USE }

    private static net.minecraft.sounds.SoundEvent hangSound(ItemStack stack, boolean placing) {
        if (WireSigns.isSign(stack)) return placing ? SoundEvents.HANGING_SIGN_PLACE : SoundEvents.HANGING_SIGN_BREAK;
        boolean banner = stack.is(ItemTags.BANNERS);
        return placing ? (banner ? SoundEvents.WOOL_PLACE : SoundEvents.LANTERN_PLACE) : (banner ? SoundEvents.WOOL_BREAK : SoundEvents.LANTERN_BREAK);
    }

    /** The wire with the id if the player is close enough to the point {@code t} along it to work on it. */
    private static HangingWireEntity reachable(ServerPlayer player, int id, double t) {
        if (!(player.level().getEntity(id) instanceof HangingWireEntity wire) || wire.level() != player.level()) return null;
        Vec3 spot = point(wire.start(), wire.end(), Math.clamp(t, 0, 1));
        return spot.distanceToSqr(player.getEyePosition()) > Math.pow(player.blockInteractionRange() + 2, 2) ? null : wire;
    }

    /** Whether the player is on the front side of a hung sign: signs hang across the wire, their front facing the way the wire's sideways direction points. */
    private boolean facingFront(ServerPlayer player, Decoration d) {
        Vec3 start = start(), end = end(), at = point(start, end, d.t());
        double angle = Math.atan2(start.z - end.z, end.x - start.x);
        return player.getEyePosition().subtract(at).dot(new Vec3(Math.sin(angle), 0, Math.cos(angle))) >= 0;
    }

    private void play(net.minecraft.sounds.SoundEvent sound, Decoration d) {
        Vec3 at = point(start(), end(), d.t());
        level().playSound(null, at.x, at.y, at.z, sound, SoundSource.BLOCKS, 1F, 1F);
    }

    /** A sign is used: dye, ink or honeycomb change it, an axe takes the wax off, and anything else opens it for writing. */
    private void useSign(ServerPlayer player, Decoration d) {
        ItemStack held = player.getMainHandItem(), sign = d.stack();
        boolean front = facingFront(player, d), waxed = WireSigns.waxed(sign);
        SignText text = WireSigns.text(sign, front, registryAccess());
        boolean spend = !player.getAbilities().instabuild;
        if (held.is(Items.HONEYCOMB) && !waxed) {
            replaceDecoration(d, WireSigns.withWax(sign, true, registryAccess()));
            play(SoundEvents.HONEYCOMB_WAX_ON, d);
            if (spend) held.shrink(1);
        } else if (held.getItem() instanceof AxeItem && waxed) {
            replaceDecoration(d, WireSigns.withWax(sign, false, registryAccess()));
            play(SoundEvents.AXE_WAX_OFF, d);
            if (spend) held.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        } else if (waxed) {
            play(SoundEvents.WAXED_SIGN_INTERACT_FAIL, d);
            return;
        } else if (held.getItem() instanceof DyeItem dye) {
            if (text.getColor() == dye.getDyeColor()) return;
            replaceDecoration(d, WireSigns.withText(sign, front, text.setColor(dye.getDyeColor()), registryAccess()));
            play(SoundEvents.DYE_USE, d);
            if (spend) held.shrink(1);
        } else if (held.is(Items.GLOW_INK_SAC) || held.is(Items.INK_SAC)) {
            boolean glow = held.is(Items.GLOW_INK_SAC);
            if (text.hasGlowingText() == glow) return;
            replaceDecoration(d, WireSigns.withText(sign, front, text.setHasGlowingText(glow), registryAccess()));
            play(glow ? SoundEvents.GLOW_INK_SAC_USE : SoundEvents.INK_SAC_USE, d);
            if (spend) held.shrink(1);
        } else {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new de.artemis.cyberneticenhancements.common.network.OpenHangingSignPayload(getId(), d.t(), front));
            return;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    /** The player finished writing on a hung sign. */
    public static void writeSign(ServerPlayer player, int id, double t, boolean front, List<String> lines) {
        HangingWireEntity wire = reachable(player, id, t);
        if (wire == null || lines.size() != 4) return;
        Decoration d = wire.decorationAt(t, 0.6);
        if (d == null || !WireSigns.isSign(d.stack()) || WireSigns.waxed(d.stack())) return;
        SignText text = WireSigns.text(d.stack(), front, wire.registryAccess());
        for (int i = 0; i < 4; i++) text = text.setMessage(i, Component.literal(net.minecraft.util.StringUtil.filterText(lines.get(i))));
        wire.replaceDecoration(d, WireSigns.withText(d.stack(), front, text, wire.registryAccess()));
    }

    public static void act(ServerPlayer player, int id, double t, Action action) {
        HangingWireEntity wire = reachable(player, id, t);
        if (wire == null) return;
        Vec3 spot = point(wire.start(), wire.end(), Math.clamp(t, 0, 1));
        if (action == Action.USE) {
            Decoration d = wire.decorationAt(t, 0.6);
            if (d == null) return;
            if (WireSigns.isSign(d.stack())) { wire.useSign(player, d); return; }
            if (!player.getMainHandItem().isEmpty()) return;
            action = Action.TAKE;
        }
        switch (action) {
            case HANG -> {
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack held = player.getItemInHand(hand);
                    if (!canHang(held) || !wire.addDecoration(t, held)) continue;
                    wire.level().playSound(null, spot.x, spot.y, spot.z, hangSound(held, true), SoundSource.BLOCKS, 1F, 1F);
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    player.swing(hand, true);
                    return;
                }
            }
            case TAKE -> {
                ItemStack taken = wire.removeDecoration(t, 0.6);
                if (taken.isEmpty()) return;
                wire.level().playSound(null, spot.x, spot.y, spot.z, hangSound(taken, false), SoundSource.BLOCKS, 1F, 1F);
                player.swing(InteractionHand.MAIN_HAND, true);
                if (!player.getAbilities().instabuild && !player.getInventory().add(taken)) player.drop(taken, false);
            }
            case BREAK -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                wire.takeDown(!player.getAbilities().instabuild);
            }
        }
    }

    public BlockPos startBlock() { return startBlock; }

    public BlockPos endBlock() { return endBlock; }

    /** How far the wire sags at its lowest, in blocks. */
    public static double sag(Vec3 start, Vec3 end) {
        return 0.12 * Math.hypot(end.x - start.x, end.z - start.z) + 0.04 * Math.abs(end.y - start.y) + 0.05;
    }

    /** The point a fraction {@code t} of the way along the wire, sag included. */
    public static Vec3 point(Vec3 start, Vec3 end, double t) {
        return start.lerp(end, t).add(0, -sag(start, end) * 4 * t * (1 - t), 0);
    }

    public void hang(Vec3 start, BlockPos startBlock, Vec3 end, BlockPos endBlock) {
        this.startBlock = startBlock.immutable();
        this.endBlock = endBlock.immutable();
        entityData.set(START, start.toVector3f());
        entityData.set(END, end.toVector3f());
        Vec3 middle = start.add(end).scale(0.5);
        setPos(middle.x, middle.y, middle.z);
    }

    public boolean isAnchoredAt(BlockPos pos) { return startBlock.equals(pos) || endBlock.equals(pos); }

    /** Takes the wire down. */
    public void takeDown(boolean drop) {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1F, 1.2F);
        if (drop) {
            spawnAtLocation(ModItems.HANGING_WIRE.get());
            for (Decoration d : decorations) spawnAtLocation(WireSigns.tidy(d.stack(), registryAccess()));
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || ++checkTicks < 20) return;
        checkTicks = 0;
        if (!held(startBlock) || !held(endBlock)) takeDown(true);
        else updateLights();
    }

    private boolean held(BlockPos pos) {
        return !level().isLoaded(pos) || !level().getBlockState(pos).isAir();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 start = start(), end = end();
        return new AABB(start, end).inflate(0.2).expandTowards(0, -sag(start, end), 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128 * 128; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        startBlock = new BlockPos(tag.getInt("StartBlockX"), tag.getInt("StartBlockY"), tag.getInt("StartBlockZ"));
        endBlock = new BlockPos(tag.getInt("EndBlockX"), tag.getInt("EndBlockY"), tag.getInt("EndBlockZ"));
        entityData.set(START, new Vector3f(tag.getFloat("StartX"), tag.getFloat("StartY"), tag.getFloat("StartZ")));
        entityData.set(END, new Vector3f(tag.getFloat("EndX"), tag.getFloat("EndY"), tag.getFloat("EndZ")));
        loadDecorations(tag.getCompound("Decorations"));
        lights = new ArrayList<>();
        for (long packed : tag.getLongArray("Lights")) lights.add(BlockPos.of(packed));
        entityData.set(DECORATIONS, decorationsTag(), true);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("StartBlockX", startBlock.getX());
        tag.putInt("StartBlockY", startBlock.getY());
        tag.putInt("StartBlockZ", startBlock.getZ());
        tag.putInt("EndBlockX", endBlock.getX());
        tag.putInt("EndBlockY", endBlock.getY());
        tag.putInt("EndBlockZ", endBlock.getZ());
        Vector3f start = entityData.get(START), end = entityData.get(END);
        tag.putFloat("StartX", start.x);
        tag.putFloat("StartY", start.y);
        tag.putFloat("StartZ", start.z);
        tag.putFloat("EndX", end.x);
        tag.putFloat("EndY", end.y);
        tag.putFloat("EndZ", end.z);
        tag.put("Decorations", decorationsTag());
        tag.putLongArray("Lights", lights.stream().mapToLong(BlockPos::asLong).toArray());
    }
}
