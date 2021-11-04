package mod.chiselsandbits.client.model.baked.simple;

import mod.chiselsandbits.client.model.baked.base.BaseBakedBlockModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SuppressWarnings("deprecation")
public class CombinedModel extends BaseBakedBlockModel
{

    private static final Random COMBINED_RANDOM_MODEL = new Random();

    BakedModel[] merged;

    List<BakedQuad>[] face;
    List<BakedQuad>   generic;

    boolean isSideLit;

    @SuppressWarnings( "unchecked" )
    public CombinedModel(
      final BakedModel... args )
    {
        face = new ArrayList[Direction.values().length];

        generic = new ArrayList<>();
        for ( final Direction f : Direction.values() )
        {
            face[f.ordinal()] = new ArrayList<>();
        }

        merged = args;

        for ( final BakedModel m : merged )
        {
            generic.addAll( m.getQuads( null, null, COMBINED_RANDOM_MODEL ) );
            for ( final Direction f : Direction.values() )
            {
                face[f.ordinal()].addAll( m.getQuads( null, f, COMBINED_RANDOM_MODEL ) );
            }
        }

        isSideLit = Arrays.stream(args).anyMatch(BakedModel::usesBlockLight);
    }

    @NotNull
    @Override
    public TextureAtlasSprite getParticleIcon()
    {
        for ( final BakedModel a : merged )
        {
            return a.getParticleIcon();
        }

        return Minecraft.getInstance().getTextureAtlas(
          InventoryMenu.BLOCK_ATLAS
        ).apply(MissingTextureAtlasSprite.getLocation());
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(
      @Nullable final BlockState state, @Nullable final Direction side, @NotNull final Random rand, @NotNull final IModelData extraData)
    {
        if ( side != null )
        {
            return face[side.ordinal()];
        }

        return generic;
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, @NotNull final Random rand)
    {
        if ( side != null )
        {
            return face[side.ordinal()];
        }

        return generic;
    }

    @Override
    public boolean usesBlockLight()
    {
        return isSideLit;
    }
}