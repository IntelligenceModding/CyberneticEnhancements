package de.artemis.cyberneticenhancements.common.tarot;

import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class TarotDecorations {
    public record Placement(BlockPos anchor, Direction facing) {
        public Placement { anchor = anchor.immutable(); }
    }
    private static BlockHitResult wallHit(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1).scale(Math.min(6, player.blockInteractionRange())));
        AABB ray = new AABB(eye, end);
        if (!player.level().hasChunksAt(BlockPos.containing(ray.minX, ray.minY, ray.minZ), BlockPos.containing(ray.maxX, ray.maxY, ray.maxZ))) return null;
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection().getAxis().isHorizontal() ? hit : null;
    }
    public static Placement preview(Player player) {
        BlockHitResult hit = wallHit(player); if (hit == null) return null;
        Direction face = hit.getDirection(), right = face.getCounterClockWise();
        BlockPos anchor = hit.getBlockPos().relative(face).below();
        double along = (hit.getLocation().x - hit.getBlockPos().getX() - .5) * right.getStepX()
                + (hit.getLocation().z - hit.getBlockPos().getZ() - .5) * right.getStepZ();
        if (along < 0) anchor = anchor.relative(right.getOpposite());
        return new Placement(anchor, face);
    }
    public static boolean fits(Level level, Placement placement) {
        if (placement == null || !TarotCardEntity.canAttach(level, placement.anchor(), placement.facing())) return false;
        AABB box = TarotCardEntity.attachmentBounds(placement.anchor(), placement.facing()).deflate(.001);
        return level.getEntities((net.minecraft.world.entity.Entity) null, box, entity -> entity instanceof TarotCardEntity || entity instanceof HangingEntity).isEmpty();
    }
    public static boolean place(ServerPlayer player, String id, Placement placement) {
        if (!player.isAlive() || player.isSpectator() || !player.mayBuild() || player.containerMenu != player.inventoryMenu
                || TarotCatalog.find(id) == null || !TarotKnowledge.known(player).contains(id) || !fits(player.level(), placement)) return false;
        BlockHitResult hit = wallHit(player);
        if (hit == null || hit.getDirection() != placement.facing()) return false;
        boolean lookingAtWall = false;
        for (int col = 0; col < 2; col++) for (int row = 0; row < 3; row++) {
            BlockPos air = placement.anchor().relative(placement.facing().getCounterClockWise(), col).above(row);
            BlockPos support = air.relative(placement.facing().getOpposite());
            if (!player.serverLevel().mayInteract(player, air) || !player.serverLevel().mayInteract(player, support)
                    || !player.mayUseItemAt(support, placement.facing(), ItemStack.EMPTY)) return false;
            if (hit.getBlockPos().equals(support)) lookingAtWall = true;
        }
        if (!lookingAtWall) return false;
        TarotCardEntity card = ModEntityTypes.TAROT_CARD.get().create(player.serverLevel());
        if (card == null) return false;
        card.setDecoration(true); card.attach(placement.anchor(), placement.facing(), id);
        if (!player.serverLevel().addFreshEntity(card)) return false;
        player.serverLevel().playSound(null, card.blockPosition(), net.minecraft.sounds.SoundEvents.PAINTING_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1);
        return true;
    }
    private TarotDecorations() {}
}
