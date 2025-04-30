package traben.flowing_fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public interface FFBucketItem {

    int ff$emptyContents_AndGetRemainder(@Nullable Player player, Level level, BlockPos blockPos, @Nullable BlockHitResult blockHitResult, int amount, boolean onlyModifyThatBlock);

    ItemStack ff$bucketOfAmount(ItemStack originalItemData ,int amount);

    Fluid ff$getFluid();
}
