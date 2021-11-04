package mod.chiselsandbits.api.multistate.accessor;

import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A special {@link IAreaAccessor} that is capable of determining its own voxelshape.
 */
public interface IAreaAccessorWithVoxelShape extends IAreaAccessor
{
    VoxelShape provideShape(final Function<IAreaAccessor, Predicate<IStateEntryInfo>> selectablePredicateBuilder, final BlockPos offset, final boolean simplify);
}
