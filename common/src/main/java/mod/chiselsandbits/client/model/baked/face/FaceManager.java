package mod.chiselsandbits.client.model.baked.face;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.client.color.IBlockInformationColorManager;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.client.model.baked.face.model.ModelQuadLayer;
import mod.chiselsandbits.client.model.baked.face.model.ModelVertexRange;
import mod.chiselsandbits.client.model.baked.simple.SimpleGeneratedModel;
import mod.chiselsandbits.platforms.core.client.rendering.IRenderingManager;
import mod.chiselsandbits.platforms.core.client.rendering.type.IRenderTypeManager;
import mod.chiselsandbits.platforms.core.fluid.FluidInformation;
import mod.chiselsandbits.platforms.core.fluid.IFluidManager;
import mod.chiselsandbits.platforms.core.registries.IPlatformRegistryManager;
import mod.chiselsandbits.utils.ItemStackUtils;
import mod.chiselsandbits.utils.LightUtil;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class FaceManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    private static final FaceManager INSTANCE = new FaceManager();

    private final SimpleMaxSizedCache<Key, ModelQuadLayer[]> cache = new SimpleMaxSizedCache<>(
            IClientConfiguration.getInstance().getFaceLayerCacheSize()::get
    );
    private final SimpleMaxSizedCache<BlockInformation, Integer> colorCache = new SimpleMaxSizedCache<>(
            () -> IPlatformRegistryManager.getInstance().getBlockStateIdMap().size() == 0 ? 1000 : IPlatformRegistryManager.getInstance().getBlockStateIdMap().size()
    );


    private FaceManager() {
    }

    public static FaceManager getInstance() {
        return INSTANCE;
    }

    public void clearCache() {
        cache.clear();
        colorCache.clear();
    }

    public ModelQuadLayer[] getCachedFace(
            final BlockInformation state,
            final Direction face,
            final RenderType layer,
            final long primaryStateRenderSeed) {
        if (layer == null) {
            return null;
        }

        final Key key = new Key(state, layer, face, primaryStateRenderSeed);

        return cache.get(key, () -> {
            final Optional<RenderType> original = IRenderTypeManager.getInstance().getCurrentRenderType();
            try {
                IRenderTypeManager.getInstance().setCurrentRenderType(layer);
                return buildFaceQuadLayers(state, face, primaryStateRenderSeed);
            } finally {
                IRenderTypeManager.getInstance().setCurrentRenderType(original.orElse(null));
            }
        });
    }

    private ModelQuadLayer[] buildFaceQuadLayers(
            final BlockInformation state,
            final Direction face,
            final long primaryStateRenderSeed) {
        final BakedModel model = solveModel(state, Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state.getBlockState()), primaryStateRenderSeed);
        final int lv = IClientConfiguration.getInstance().getUseGetLightValue().get() ?
                state.getBlockState().getLightEmission() : 0;

        final Fluid fluid = state.getBlockState().getFluidState().getType();
        if (fluid != Fluids.EMPTY) {
            final ModelQuadLayer[] mp = new ModelQuadLayer[1];
            mp[0] = new ModelQuadLayer();
            mp[0].setColor(IFluidManager.getInstance().getFluidColor(new FluidInformation(fluid)));
            mp[0].setLight(lv);

            final float V = 0.5f;
            final float Uf = 1.0f;
            final float U = 0.5f;
            final float Vf = 1.0f;

            if (face.getAxis() == Direction.Axis.Y) {
                mp[0].setSprite(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(IRenderingManager.getInstance().getStillFluidTexture(fluid)));
                mp[0].setUvs(new float[]{Uf, Vf, 0, Vf, Uf, 0, 0, 0});
            } else if (face.getAxis() == Direction.Axis.X) {
                mp[0].setSprite(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(IRenderingManager.getInstance().getFlowingFluidTexture(fluid)));
                mp[0].setUvs(new float[]{U, 0, U, V, 0, 0, 0, V});
            } else {
                mp[0].setSprite(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(IRenderingManager.getInstance().getFlowingFluidTexture(fluid)));
                mp[0].setUvs(new float[]{U, 0, 0, 0, U, V, 0, V});
            }

            mp[0].setLight(0);
            return mp;
        }

        final List<ModelQuadLayer.ModelQuadLayerBuilder> layers = Lists.newArrayList();
        final int color = getColorFor(state);

        if (model != null) {
            final List<BakedQuad> quads = getModelQuads(model, state, face, primaryStateRenderSeed);
            processFaces(layers, face, quads);
        }

        final ModelQuadLayer[] quadLayers = new ModelQuadLayer[layers.size()];
        for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
            quadLayers[layerIndex] = layers.get(layerIndex).build(state, color, lv);
        }

        return quadLayers;
    }

    private static void processFaces(
            final List<ModelQuadLayer.ModelQuadLayerBuilder> layers,
            final Direction face,
            final List<BakedQuad> quads) {
        for (final BakedQuad quad : quads) {
            if (quad.getDirection() != face)
                return;

            try {
                final TextureAtlasSprite sprite = findQuadTexture(quad);

                ModelQuadLayer.ModelQuadLayerBuilder layerBuilder = null;
                for (final ModelQuadLayer.ModelQuadLayerBuilder builder : layers) {
                    if (builder.getCache().getSprite() == sprite) {
                        layerBuilder = builder;
                        break;
                    }
                }

                if (layerBuilder == null) {
                    // top/bottom
                    int uCoord = 0;
                    int vCoord = 2;

                    switch (face) {
                        case NORTH, SOUTH -> vCoord = 1;
                        case EAST, WEST -> uCoord = 1;
                        default -> {
                        }
                    }

                    layerBuilder = new ModelQuadLayer.ModelQuadLayerBuilder(sprite, uCoord, vCoord, quad.isShade(), face);
                    layerBuilder.getCache().setTint(quad.getTintIndex());
                    layers.add(layerBuilder);
                }

                LightUtil.put(layerBuilder.getUvExtractor(), quad);

                if (IClientConfiguration.getInstance().getEnableFaceLightmapExtraction().get()) {
                    layerBuilder.getLightValueExtractor().setVertexFormat(DefaultVertexFormat.BLOCK);
                    LightUtil.put(layerBuilder.getLightValueExtractor(), quad);
                }
            } catch (final Exception ex) {
                LOGGER.error("Failed to process quad: " + quad, ex);
            }
        }
    }

    private static BakedModel solveModel(
            final BlockInformation state,
            final BakedModel originalModel,
            final long primaryStateRenderSeed
    ) {
        boolean hasFaces;
        try {
            hasFaces = hasFaces(originalModel, state, null, primaryStateRenderSeed);

            for (final Direction f : Direction.values()) {
                hasFaces = hasFaces || hasFaces(originalModel, state, f, primaryStateRenderSeed);
            }
        } catch (final Exception e) {
            // an exception was thrown.. use the item model and hope...
            hasFaces = false;
        }

        if (!hasFaces) {
            // if the model is empty then lets grab an item and try that...
            final ItemStack is = ItemStackUtils.getItemStackFromBlockState(state);
            if (!is.isEmpty()) {
                final BakedModel itemModel =
                        Minecraft.getInstance().getItemRenderer().getModel(is, Minecraft.getInstance().level, Minecraft.getInstance().player, 0);

                try {
                    hasFaces = hasFaces(originalModel, state, null, primaryStateRenderSeed);

                    for (final Direction f : Direction.values()) {
                        hasFaces = hasFaces || hasFaces(originalModel, state, f, primaryStateRenderSeed);
                    }
                } catch (final Exception e) {
                    // an exception was thrown.. use the item model and hope...
                    hasFaces = false;
                }

                if (hasFaces) {
                    return itemModel;
                } else {
                    return new SimpleGeneratedModel(findTexture(state, originalModel, Direction.UP, primaryStateRenderSeed));
                }
            }
        }

        return originalModel;
    }

    private static boolean hasFaces(
            final BakedModel model,
            final BlockInformation state,
            final Direction f,
            final long primaryStateRenderSeed) {
        final List<BakedQuad> quads = getModelQuads(model, state, f, primaryStateRenderSeed);
        if (quads == null || quads.isEmpty()) {
            return false;
        }

        TextureAtlasSprite texture = null;

        try {
            texture = findTexture(null, quads, f);
        } catch (final Exception ignored) {
        }

        final ModelVertexRange vertexRangeExtractor = new ModelVertexRange();

        for (final BakedQuad quad : quads) {
            LightUtil.put(vertexRangeExtractor, quad);
        }

        return vertexRangeExtractor.getLargestRange() > 0 && !isMissingTexture(texture);
    }

    public static TextureAtlasSprite findTexture(
            final BlockInformation state,
            final BakedModel model,
            final Direction myFace,
            final long primaryStateRenderSeed) {
        TextureAtlasSprite texture = null;

        if (model != null) {
            try {
                texture = findTexture(null, getModelQuads(model, state, myFace, primaryStateRenderSeed), myFace);

                if (texture == null) {
                    for (final Direction side : Direction.values()) {
                        texture = findTexture(texture, getModelQuads(model, state, side, primaryStateRenderSeed), side);
                    }

                    texture = findTexture(texture, getModelQuads(model, state, null, primaryStateRenderSeed), null);
                }
            } catch (final Exception ignored) {
            }
        }

        // who knows if that worked.. now lets try to get a texture...
        if (isMissingTexture(texture)) {
            try {
                if (model != null) {
                    texture = model.getParticleIcon();
                }
            } catch (final Exception ignored) {
            }
        }

        if (isMissingTexture(texture)) {
            try {
                texture = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state.getBlockState());
            } catch (final Exception ignored) {
            }
        }

        if (texture == null) {
            texture = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("missingno"));
        }

        return texture;
    }

    private static TextureAtlasSprite findTexture(
            TextureAtlasSprite texture,
            final List<BakedQuad> faceQuads,
            final Direction myFace) throws IllegalArgumentException, NullPointerException {
        for (final BakedQuad q : faceQuads) {
            if (q.getDirection() == myFace) {
                texture = findQuadTexture(q);
            }
        }

        return texture;
    }

    @SuppressWarnings("ConstantConditions")
    private static TextureAtlasSprite findQuadTexture(
            final BakedQuad q
    ) throws IllegalArgumentException, NullPointerException {
        if (q.getSprite() == null)
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
        return q.getSprite();
    }

    private static boolean isMissingTexture(final TextureAtlasSprite sprite) {
        if (sprite == null)
            return true;

        return sprite.getName().equals(MissingTextureAtlasSprite.getLocation());
    }

    private static List<BakedQuad> getModelQuads(
            final BakedModel model,
            final BlockInformation state,
            final Direction f,
            final long primaryStateRenderSeed) {
        try {
            // try to get block model...
            RANDOM.setSeed(primaryStateRenderSeed);
            return model.getQuads(state.getBlockState(), f, RANDOM);
        } catch (final Throwable ignored) {
        }

        try {
            // try to get item model?
            return model.getQuads(null, f, RANDOM);
        } catch (final Throwable ignored) {
        }

        final ItemStack is = ItemStackUtils.getItemStackFromBlockState(state);
        if (!is.isEmpty()) {
            final BakedModel secondModel = getOverrides(model).resolve(model, is, Minecraft.getInstance().level, Minecraft.getInstance().player, 0);

            if (secondModel != null) {
                try {
                    return secondModel.getQuads(null, f, RANDOM);
                } catch (final Throwable ignored) {
                }
            }
        }

        // try to not crash...
        return Collections.emptyList();
    }

    private static ItemOverrides getOverrides(
            final BakedModel model) {
        if (model != null) {
            return model.getOverrides();
        }
        return ItemOverrides.EMPTY;
    }

    private int getColorFor(
            final BlockInformation state) {
        return colorCache.get(state, () -> {
            final Optional<Integer> dynamicColor = IBlockInformationColorManager.getInstance()
                                                     .getColor(state);

            if (dynamicColor.isPresent()) {
                return dynamicColor.get();
            }

            int out;
            final Fluid fluid = state.getBlockState().getFluidState().getType();
            if (fluid != Fluids.EMPTY) {
                out = IFluidManager.getInstance().getFluidColor(fluid);
            } else {
                final ItemStack target = ItemStackUtils.getItemStackFromBlockState(state);

                if (target.isEmpty()) {
                    out = 0xffffff;
                } else {
                    out =  Minecraft.getInstance().itemColors.getColor(target, 0);
                }
            }

            return out;
        });
    }

    private record Key(BlockInformation blockState, RenderType renderType, Direction direction,
                       long primaryStateSeed) {

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final Key key)) {
                return false;
            }

            if (primaryStateSeed != key.primaryStateSeed) {
                return false;
            }
            if (!Objects.equals(blockState, key.blockState)) {
                return false;
            }
            if (!Objects.equals(renderType, key.renderType)) {
                return false;
            }
            return direction == key.direction;
        }

        @Override
        public int hashCode() {
            int result = blockState != null ? blockState.hashCode() : 0;
            result = 31 * result + (renderType != null ? renderType.hashCode() : 0);
            result = 31 * result + (direction != null ? direction.hashCode() : 0);
            result = 31 * result + (int) (primaryStateSeed ^ (primaryStateSeed >>> 32));
            return result;
        }
    }
}
