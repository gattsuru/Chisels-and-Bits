package mod.chiselsandbits.chiseling;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.IChiselingManager;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public class ChiselingManager implements IChiselingManager
{
    private static final ChiselingManager INSTANCE = new ChiselingManager();

    private UUID activeInstanceId = UUID.randomUUID();

    private final ThreadLocal<UUID> activeThreadId = ThreadLocal.withInitial(() -> activeInstanceId);

    private final ThreadLocal<Table<UUID, ResourceLocation, IChiselingContext>> contexts = ThreadLocal.withInitial(HashBasedTable::create);
    private final ThreadLocal<Table<UUID, ResourceLocation, Long>> lastUsedChiselMoments = ThreadLocal.withInitial(HashBasedTable::create);

    private ChiselingManager()
    {
    }

    public static ChiselingManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Gives access to the chiseling context of the player, if it exists.
     *
     * @param playerEntity The player for which the context is looked up.
     * @param mode         The mode which the player wants to chisel in
     * @return An optional potentially containing the current context of the player.
     */
    @Override
    public Optional<IChiselingContext> get(final PlayerEntity playerEntity, final IChiselMode mode)
    {
        final UUID playerId = playerEntity.getUniqueID();
        final ResourceLocation worldId = playerEntity.getEntityWorld().getDimensionKey().getLocation();

        final IChiselingContext currentStored = contexts.get().get(playerId, worldId);
        if (currentStored == null)
            return Optional.empty();

        return Optional.of(currentStored);
    }

    @Override
    public Optional<IChiselingContext> get(final PlayerEntity playerEntity, final IChiselMode mode, final ChiselingOperation modeOfOperandus)
    {
        final UUID playerId = playerEntity.getUniqueID();
        final ResourceLocation worldId = playerEntity.getEntityWorld().getDimensionKey().getLocation();

        final IChiselingContext currentStored = contexts.get().get(playerId, worldId);
        if (currentStored == null)
            return Optional.empty();

        if (currentStored.getModeOfOperandus() == modeOfOperandus)
            return Optional.of(currentStored);

        return Optional.empty();
    }

    @Override
    public IChiselingContext create(final PlayerEntity playerEntity, final IChiselMode mode, final ChiselingOperation modeOfOperandus, final boolean simulation)
    {
        final UUID playerId = playerEntity.getUniqueID();
        final ResourceLocation worldId = playerEntity.getEntityWorld().getDimensionKey().getLocation();

        final IChiselingContext currentStored = contexts.get().get(playerId, worldId);

        //We might already have one stored.
        if (currentStored != null) {
            if (!simulation) {
                contexts.get().remove(playerId, worldId);
            }
        }

        final ChiselingContext newContext = new ChiselingContext(playerEntity.getEntityWorld(),
          mode,
          modeOfOperandus,
          simulation, () -> {
            if (simulation)
                return;

            this.lastUsedChiselMoments.get().put(playerId, worldId, (long) playerEntity.ticksExisted);
            contexts.get().remove(playerId, worldId);
        });

        if (!simulation)
        {
            contexts.get().put(playerId, worldId, newContext);
        }

        return newContext;
    }

    public boolean canChisel(final PlayerEntity playerEntity) {
        validateOrSetup();

        final UUID playerId = playerEntity.getUniqueID();
        final ResourceLocation worldId = playerEntity.getEntityWorld().getDimensionKey().getLocation();

        final Long lastChiselTime = this.lastUsedChiselMoments.get().get(playerId, worldId);
        if (lastChiselTime == null)
            return true;

        final long time = playerEntity.ticksExisted;
        final long diffSinceLastUse = time - lastChiselTime;

        if (diffSinceLastUse > Constants.TICKS_BETWEEN_CHISEL_USAGE) {
            this.lastUsedChiselMoments.get().remove(playerId, worldId);
            return true;
        }

        return false;
    }

    public void onServerStarting()
    {
        this.activeInstanceId = UUID.randomUUID();
    }

    private void validateOrSetup() {
        final UUID threadId = activeThreadId.get();
        if (threadId != activeInstanceId) {
            this.contexts.get().clear();
            this.lastUsedChiselMoments.get().clear();

            activeThreadId.set(activeInstanceId);
        }
    }
}
