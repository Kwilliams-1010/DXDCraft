package com.dxdcraft.block;

import com.dxdcraft.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Angelic Ward: a defensive beacon. Every second it smites hostile mobs within 16 blocks
 * (double damage to undead and devils), pushes them out, and makes them glow.
 */
public class AngelicWardBlock extends BaseEntityBlock {
    public static final MapCodec<AngelicWardBlock> CODEC = simpleCodec(AngelicWardBlock::new);

    public AngelicWardBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AngelicWardBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.ANGELIC_WARD_BE.get(), AngelicWardBlockEntity::serverTick);
    }
}
