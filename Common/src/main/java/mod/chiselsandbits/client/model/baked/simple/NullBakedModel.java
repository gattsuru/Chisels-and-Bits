package mod.chiselsandbits.client.model.baked.simple;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

@SuppressWarnings("deprecation")
public class NullBakedModel implements BakedModel
{

    public static final NullBakedModel instance = new NullBakedModel();

    @NotNull
    @Override
    public List<BakedQuad> getQuads(
      @Nullable final BlockState state, @Nullable final Direction side, @NotNull final Random rand, @NotNull final IModelData extraData)
    {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, @NotNull final Random rand)
    {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return false;
    }

    @Override
    public boolean isGui3d()
    {
        return false;
    }

    @Override
    public boolean usesBlockLight()
    {
        return false;
    }

    @Override
    public boolean isCustomRenderer()
    {
        return false;
    }

    @NotNull
    @Override
    public TextureAtlasSprite getParticleIcon()
    {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
          .apply(MissingTextureAtlasSprite.getLocation());
    }

    @NotNull
    @Override
    public ItemTransforms getTransforms()
    {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @NotNull
    @Override
    public ItemOverrides getOverrides()
    {
        return ItemOverrides.EMPTY;
    }

}