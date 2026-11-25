package com.tacz.guns.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public abstract class AbstractGunSmithTableBlock extends Block {
    public static final DirectionProperty FACING = null;

    protected AbstractGunSmithTableBlock(Properties props) { super(props); }
    public boolean isRoot(BlockState state) { return true; }
    public int parseRotation(Direction facing) { return 0; }
}
