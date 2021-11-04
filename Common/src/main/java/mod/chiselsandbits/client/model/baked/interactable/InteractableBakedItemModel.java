package mod.chiselsandbits.client.model.baked.interactable;

import com.google.common.collect.Lists;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class InteractableBakedItemModel implements BakedModel
{

    private final BakedModel innerModel;

    public InteractableBakedItemModel(final BakedModel innerModel) {this.innerModel = innerModel;}

    @Override
    public @NotNull List<BakedQuad> getQuads(
      @Nullable final BlockState state, @Nullable final Direction side, final @NotNull Random rand)
    {
        return innerModel.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return innerModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return innerModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight()
    {
        return innerModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer()
    {
        return true;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon()
    {
        return innerModel.getParticleIcon();
    }

    @Override
    public @NotNull ItemOverrides getOverrides()
    {
        return innerModel.getOverrides();
    }

    public BakedModel getInnerModel()
    {
        return innerModel;
    }
}
