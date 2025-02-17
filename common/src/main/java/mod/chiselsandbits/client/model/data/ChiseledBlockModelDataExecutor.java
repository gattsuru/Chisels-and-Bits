package mod.chiselsandbits.client.model.data;

import com.google.common.collect.Maps;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhoodBuilder;
import mod.chiselsandbits.api.profiling.IProfilerSection;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.client.model.baked.chiseled.ChiselRenderType;
import mod.chiselsandbits.client.model.baked.chiseled.ChiseledBlockBakedModel;
import mod.chiselsandbits.client.model.baked.chiseled.ChiseledBlockBakedModelManager;
import mod.chiselsandbits.client.model.baked.chiseled.FluidRenderingManager;
import mod.chiselsandbits.client.model.baked.simple.CombinedModel;
import mod.chiselsandbits.client.model.baked.simple.NullBakedModel;
import mod.chiselsandbits.platforms.core.client.models.data.IModelDataBuilder;
import mod.chiselsandbits.platforms.core.client.models.data.IModelDataManager;
import mod.chiselsandbits.platforms.core.client.rendering.type.IRenderTypeManager;
import mod.chiselsandbits.profiling.ProfilingManager;
import mod.chiselsandbits.registrars.ModModelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ChiseledBlockModelDataExecutor
{
    private static ExecutorService recalculationService;

    public static void updateModelDataCore(final ChiseledBlockEntity tileEntity, final Runnable onCompleteCallback)
    {
        ensureThreadPoolSetup();

        final IBlockNeighborhood neighborhood = IBlockNeighborhoodBuilder.getInstance().build(
          direction -> {
              final BlockState state = Objects.requireNonNull(tileEntity.getLevel()).getBlockState(tileEntity.getBlockPos().offset(direction.getNormal()));
              final Optional<IStateVariant> additionalStateInfo = IStateVariantManager.getInstance().getStateVariant(
                state,
                Optional.ofNullable(tileEntity.getLevel().getBlockEntity(tileEntity.getBlockPos().offset(direction.getNormal())))
              );

              return new BlockInformation(state, additionalStateInfo);
          },
          direction -> {
              final BlockEntity otherTileEntity = Objects.requireNonNull(tileEntity.getLevel()).getBlockEntity(tileEntity.getBlockPos().offset(direction.getNormal()));
              if (otherTileEntity instanceof IAreaAccessor)
              {
                  return (IAreaAccessor) otherTileEntity;
              }

              return null;
          }
        );
        CompletableFuture.supplyAsync(() -> {
              BakedModel unknownRenderTypeModel;
              Map<RenderType, BakedModel> renderTypedModels = Maps.newLinkedHashMap();

              try (IProfilerSection ignored1 = ProfilingManager.getInstance().withSection("Extract model data from data"))
              {
                  final RenderType currentType = IRenderTypeManager.getInstance().getCurrentRenderType().orElse(null);
                  IRenderTypeManager.getInstance().setCurrentRenderType(null);

                  try (IProfilerSection ignored2 = ProfilingManager.getInstance().withSection("Known render layer model building"))
                  {
                      for (final RenderType chunkBufferLayer : RenderType.chunkBufferLayers())
                      {
                          final ChiselRenderType solidType =
                            ChiselRenderType.fromLayer(chunkBufferLayer, false);
                          final ChiselRenderType fluidType =
                            ChiselRenderType.fromLayer(chunkBufferLayer, true);


                          IRenderTypeManager.getInstance().setCurrentRenderType(chunkBufferLayer);
                          if (tileEntity.getStatistics().getStateCounts().isEmpty() ||
                                (tileEntity.getStatistics().getStateCounts().size() == 1 && tileEntity.getStatistics().getStateCounts().containsKey(BlockInformation.AIR)))
                          {
                              continue;
                          }

                          boolean requiredDuringRendering = false;

                          for (final BlockInformation state : tileEntity.getStatistics().getStateCounts().keySet())
                          {
                              if (solidType.isRequiredForRendering(state) || fluidType.isRequiredForRendering(state))
                              {
                                  requiredDuringRendering = true;
                              }
                          }


                          BakedModel baked = NullBakedModel.instance;

                          if (requiredDuringRendering)
                          {

                              try (IProfilerSection ignored3 = ProfilingManager.getInstance()
                                .withSection("Known render layer model building for: " + solidType.name() + " and " + fluidType.name()))
                              {

                                  if (FluidRenderingManager.getInstance().isFluidRenderType(chunkBufferLayer))
                                  {
                                      try (IProfilerSection ignored4 = ProfilingManager.getInstance().withSection("Combined model building"))
                                      {

                                          final ChiseledBlockBakedModel solidModel;
                                          try (IProfilerSection ignored5 = ProfilingManager.getInstance().withSection("Solid"))
                                          {
                                              solidModel = ChiseledBlockBakedModelManager.getInstance().get(
                                                tileEntity,
                                                tileEntity.getStatistics().getPrimaryState(),
                                                solidType,
                                                neighborhood::getBlockInformation,
                                                neighborhood::getAreaAccessor,
                                                tileEntity.getBlockPos()
                                              );
                                          }

                                          final ChiseledBlockBakedModel fluidModel;
                                          try (IProfilerSection ignored5 = ProfilingManager.getInstance().withSection("Fluid"))
                                          {
                                              fluidModel = ChiseledBlockBakedModelManager.getInstance().get(
                                                tileEntity,
                                                tileEntity.getStatistics().getPrimaryState(),
                                                fluidType,
                                                neighborhood::getBlockInformation,
                                                neighborhood::getAreaAccessor,
                                                tileEntity.getBlockPos()
                                              );
                                          }

                                          try (IProfilerSection ignored5 = ProfilingManager.getInstance().withSection("Model combining"))
                                          {
                                              if (solidModel.isEmpty())
                                              {
                                                  baked = fluidModel;
                                              }
                                              else if (fluidModel.isEmpty())
                                              {
                                                  baked = solidModel;
                                              }
                                              else
                                              {
                                                  baked = new CombinedModel(solidModel, fluidModel);
                                              }
                                          }
                                      }
                                  }
                                  else
                                  {
                                      try (IProfilerSection ignored4 = ProfilingManager.getInstance().withSection("Singular model building"))
                                      {
                                          baked = ChiseledBlockBakedModelManager.getInstance().get(
                                            tileEntity,
                                            tileEntity.getStatistics().getPrimaryState(),
                                            ChiselRenderType.fromLayer(chunkBufferLayer, false),
                                            neighborhood::getBlockInformation,
                                            neighborhood::getAreaAccessor,
                                            tileEntity.getBlockPos()
                                          );
                                      }
                                  }
                              }
                          }

                          renderTypedModels.put(chunkBufferLayer, baked);
                      }
                  }

                  IRenderTypeManager.getInstance().setCurrentRenderType(currentType);
              }

              unknownRenderTypeModel = new CombinedModel(renderTypedModels.values().stream()
                .filter(model -> model != NullBakedModel.instance)
                .toArray(BakedModel[]::new));

              return IModelDataBuilder.create()
                .withInitial(
                  ModModelProperties.UNKNOWN_LAYER_MODEL_PROPERTY, unknownRenderTypeModel
                )
                .withInitial(
                  ModModelProperties.KNOWN_LAYER_MODEL_PROPERTY, renderTypedModels
                )
                .build();
          }, recalculationService)
          .thenAcceptAsync(tileEntity::setModelData, recalculationService)
          .thenRunAsync(onCompleteCallback, recalculationService)
          .thenRunAsync(() -> {
              if (Minecraft.getInstance().level == tileEntity.getLevel())
              {
                  IModelDataManager.getInstance().requestModelDataRefresh(tileEntity);
                  Objects.requireNonNull(Minecraft.getInstance().level).sendBlockUpdated(
                    tileEntity.getBlockPos(),
                    tileEntity.getBlockState(),
                    tileEntity.getBlockState(),
                    8
                  );
              }
          }, Minecraft.getInstance());
    }

    private static synchronized void ensureThreadPoolSetup()
    {
        if (recalculationService == null)
        {
            final ClassLoader classLoader = ChiselsAndBits.class.getClassLoader();
            final AtomicInteger genericThreadCounter = new AtomicInteger();
            recalculationService = Executors.newFixedThreadPool(
              IClientConfiguration.getInstance().getModelBuildingThreadCount().get(),
              runnable -> {
                  final Thread thread = new Thread(runnable);
                  thread.setContextClassLoader(classLoader);
                  thread.setName(String.format("Chisels and Bits Model builder #%s", genericThreadCounter.incrementAndGet()));
                  thread.setDaemon(true);
                  return thread;
              }
            );
        }
    }
}
