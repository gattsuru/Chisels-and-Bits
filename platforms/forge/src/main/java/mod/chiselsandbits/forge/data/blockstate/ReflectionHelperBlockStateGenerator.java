package mod.chiselsandbits.forge.data.blockstate;

import mod.chiselsandbits.platforms.core.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ReflectionHelperBlockStateGenerator extends BlockStateProvider implements DataProvider
{
    public ReflectionHelperBlockStateGenerator(final DataGenerator gen, final ExistingFileHelper exFileHelper)
    {
        super(gen, Constants.MOD_ID, exFileHelper);
    }

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent event)
    {
        event.getGenerator().addProvider(new ReflectionHelperBlockStateGenerator(event.getGenerator(), event.getExistingFileHelper()));
    }

    @Override
    protected void registerStatesAndModels()
    {
        this.actOnBlock(ModBlocks.REFLECTION_HELPER_BLOCK.get());
    }

    @NotNull
    @Override
    public String getName()
    {
        return "Reflection helper block blockstate generator";
    }

    public void actOnBlock(final Block block)
    {
        getVariantBuilder(block)
          .forAllStates(blockState -> ConfiguredModel.builder()
            .modelFile(models().getExistingFile(new ResourceLocation("air")))
            .build());
    }
}
