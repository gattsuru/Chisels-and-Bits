package mod.chiselsandbits.forge.client.events;

import mod.chiselsandbits.client.logic.ChiseledBlockModelUpdateHandler;
import mod.chiselsandbits.platforms.core.util.constants.Constants;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChiseledBlockModelUpdateEventHandler
{

    @SubscribeEvent
    public static void onChunkLoaded(final ChunkEvent.Load event)
    {
        if (event.getChunk() instanceof LevelChunk levelChunk)
            ChiseledBlockModelUpdateHandler.updateAllModelDataInChunk(levelChunk);
    }
}
