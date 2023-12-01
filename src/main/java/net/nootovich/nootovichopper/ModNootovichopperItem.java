package net.nootovich.nootovichopper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModNootovichopperItem extends AxeItem {

    private static final int RECURSIVE_LIMIT = 1000;

    private static int logsBroken = 0;

    public ModNootovichopperItem(Tier pTier, float pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }


    public boolean mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pMiningEntity) {
        if (pLevel.isClientSide() || pMiningEntity.isShiftKeyDown()) return super.mineBlock(pStack, pLevel, pState, pPos, pMiningEntity);

        logsBroken = 0;
        mineAndCheckNeighbours((ServerLevel) pLevel, pPos, (ServerPlayer) pMiningEntity, pStack);
        if (logsBroken == RECURSIVE_LIMIT) pMiningEntity.sendSystemMessage(Component.literal("You have broken %d logs!".formatted(logsBroken)));

        return logsBroken > 0 || super.mineBlock(pStack, pLevel, pState, pPos, pMiningEntity);
    }

    private void mineAndCheckNeighbours(ServerLevel pLevel, BlockPos pPos, ServerPlayer pMiningEntity, ItemStack pStack) {
        if (pStack.getMaxDamage()-pStack.getDamageValue() <= 0
            || logsBroken >= RECURSIVE_LIMIT
            || pPos.getY() >= 320
            || pPos.getY() < -64
            || !isLog(pLevel.getBlockState(pPos))
        ) return;

        destroyBlock(pLevel, pPos, pMiningEntity.position(), pMiningEntity);
        pStack.hurt(1, pMiningEntity.getRandom(), pMiningEntity);
        logsBroken++;

        for (int y = 1; y >= -1; y--) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos newPos = pPos.offset(x, y, z);
                    mineAndCheckNeighbours(pLevel, newPos, pMiningEntity, pStack);
                    if (pStack.getMaxDamage()-pStack.getDamageValue() <= 0) return;
                }
            }
        }
    }

    private void destroyBlock(ServerLevel pLevel, BlockPos pPos, Vec3 pOrigin, ServerPlayer pEntity) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        if (blockstate.isAir()) return;

        BlockEntity blockentity = blockstate.hasBlockEntity() ? pLevel.getBlockEntity(pPos) : null;
        ItemStack   tool        = pEntity.getMainHandItem();

        if (pLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !pLevel.restoringBlockSnapshots) {
            Block.getDrops(blockstate, pLevel, pPos, blockentity, pEntity, tool)
                 .forEach(item -> pLevel.addFreshEntity(new ItemEntity(pLevel, pOrigin.x, pOrigin.y, pOrigin.z, item, 0, 0, 0)));
            blockstate.spawnAfterBreak(pLevel, pPos, tool, true);
        }

        boolean flag = pLevel.setBlock(pPos, pLevel.getFluidState(pPos).createLegacyBlock(), 3, 512);
        if (flag) pLevel.gameEvent(GameEvent.BLOCK_DESTROY, pPos, GameEvent.Context.of(pEntity, blockstate));
    }

    private boolean isLog(BlockState blockState) {
        return blockState.getTags().filter(tag -> tag.equals(BlockTags.MINEABLE_WITH_AXE) || tag.equals(BlockTags.LOGS) || tag.equals(BlockTags.BAMBOO_BLOCKS)).count() == 2L;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§7You can hold §e§lSHIFT§r§7 to break"));
        pTooltipComponents.add(Component.literal("§7just a single block."));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
