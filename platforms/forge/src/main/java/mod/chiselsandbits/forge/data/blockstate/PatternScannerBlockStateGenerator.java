package mod.chiselsandbits.forge.data.blockstate;

import mod.chiselsandbits.platforms.core.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PatternScannerBlockStateGenerator extends BlockStateProvider implements DataProvider
{
    private static final ResourceLocation PATTERN_SCANNER_BLOCK_MODEL = new ResourceLocation(Constants.MOD_ID, "block/pattern_scanner");

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent event)
    {
        event.getGenerator().addProvider(new PatternScannerBlockStateGenerator(event.getGenerator(), event.getExistingFileHelper()));
    }


    public PatternScannerBlockStateGenerator(final DataGenerator gen, final ExistingFileHelper exFileHelper)
    {
        super(gen, Constants.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels()
    {
        actOnBlock(ModBlocks.PATTERN_SCANNER.get());
    }

    public void actOnBlock(final Block block)
    {
        horizontalBlock(block, models().getExistingFile(PATTERN_SCANNER_BLOCK_MODEL));
    }

    @Override
    public @NotNull String getName()
    {
        return "Chiseled printer blockstate generator";
    }
}
