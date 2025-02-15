package mod.chiselsandbits.item;

import com.google.common.collect.Lists;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.api.item.measuring.IMeasuringTapeItem;
import mod.chiselsandbits.api.measuring.MeasuringMode;
import mod.chiselsandbits.api.util.HelpTextUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.RayTracingUtils;
import mod.chiselsandbits.keys.KeyBindingManager;
import mod.chiselsandbits.measures.MeasuringManager;
import mod.chiselsandbits.network.packets.MeasurementsResetPacket;
import mod.chiselsandbits.api.util.BlockHitResultUtils;
import mod.chiselsandbits.platforms.core.dist.Dist;
import mod.chiselsandbits.platforms.core.dist.DistExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class MeasuringTapeItem extends Item implements IMeasuringTapeItem
{
    public MeasuringTapeItem(final Properties properties)
    {
        super(properties);
    }

    @NotNull
    @Override
    public MeasuringMode getMode(final ItemStack stack)
    {
        if (!stack.getOrCreateTag().contains("mode"))
            return MeasuringMode.WHITE_BIT;

        return MeasuringMode.valueOf(stack.getOrCreateTag().getString("mode"));
    }

    @Override
    public void setMode(final ItemStack stack, final MeasuringMode mode)
    {
        if (mode == null)
            return;

        stack.getOrCreateTag().putString("mode", mode.toString());
    }

    @Override
    public @NotNull Collection<MeasuringMode> getPossibleModes()
    {
        return Lists.newArrayList(MeasuringMode.values());
    }

    @Override
    public ClickProcessingState handleRightClickProcessing(
      final Player playerEntity, final InteractionHand hand, final BlockPos position, final Direction face, final ClickProcessingState currentState)
    {
        ClickProcessingState result = ClickProcessingState.DEFAULT;
        final ItemStack stack = playerEntity.getItemInHand(hand);
        if (stack.getItem() == this)
        {
            //We only check for this on the client not on the server side, the client sends the packet to the server and then performs the code.
            if (DistExecutor.unsafeRunForDist(
              () -> () -> { return KeyBindingManager.getInstance().isResetMeasuringTapeKeyPressed(); },
              () -> () -> { return false; })
            )
            {
                clear(stack);
                ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new MeasurementsResetPacket());
            }
            else
            {
                final HitResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
                if (rayTraceResult.getType() == HitResult.Type.BLOCK && rayTraceResult instanceof final BlockHitResult blockRayTraceResult)
                {
                    final Optional<Vec3> startPointHandler = getStart(stack);
                    if (startPointHandler.isEmpty())
                    {
                        setStart(stack, getMode(stack).getType().adaptClickedPosition(blockRayTraceResult));
                    }
                    else
                    {
                        final Vec3 startPoint = startPointHandler.get();
                        final Vec3 hitVector = BlockHitResultUtils.getCenterOfHitObject(blockRayTraceResult, getMode(stack).getType().getResolution() );
                        MeasuringManager.getInstance().createAndSend(
                          startPoint,
                          getMode(stack).getType().adaptClickedPosition(blockRayTraceResult),
                          blockRayTraceResult.getDirection(),
                          getMode(stack)
                        );
                        clear(stack);
                    }
                    result = ClickProcessingState.ALLOW;
                }
            }
        }

        return result;
    }

    @Override
    public void inventoryTick(final @NotNull ItemStack stack, final @NotNull Level worldIn, final @NotNull Entity entityIn, final int itemSlot, final boolean isSelected)
    {
        if (!worldIn.isClientSide())
            return;

        if (!(entityIn instanceof final Player playerEntity))
            return;

        if (stack.getItem() != this)
            return;

        final Optional<Vec3> startPointHandler = getStart(stack);
        if (startPointHandler.isEmpty()) {
            return;
        }

        if (KeyBindingManager.getInstance().isResetMeasuringTapeKeyPressed()) {
            clear(stack);
            ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new MeasurementsResetPacket());
            return;
        }

        final HitResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
        if (rayTraceResult.getType() != HitResult.Type.BLOCK || !(rayTraceResult instanceof final BlockHitResult blockRayTraceResult))
        {
            return;
        }
        final Vec3 hitVector = blockRayTraceResult.getLocation();

        final Vec3 startPoint = startPointHandler.get();

        MeasuringManager.getInstance().createAndSend(
          startPoint,
          getMode(stack).getType().adaptClickedPosition(blockRayTraceResult),
          blockRayTraceResult.getDirection(),
          getMode(stack)
        );
    }

    @Override
    public @NotNull Optional<Vec3> getStart(final @NotNull ItemStack stack)
    {
        if (!stack.getOrCreateTag().contains("start"))
            return Optional.empty();

        final CompoundTag start = stack.getOrCreateTag().getCompound("start");
        return Optional.of(
          new Vec3(
            start.getDouble("x"),
            start.getDouble("y"),
            start.getDouble("z")
          )
        );
    }

    @Override
    public void setStart(final @NotNull ItemStack stack, final @NotNull Vec3 start)
    {
        final CompoundTag compoundNBT = new CompoundTag();

        compoundNBT.putDouble("x", start.x());
        compoundNBT.putDouble("y", start.y());
        compoundNBT.putDouble("z", start.z());

        stack.getOrCreateTag().put("start", compoundNBT);
    }

    @Override
    public void clear(final @NotNull ItemStack stack)
    {
        stack.getOrCreateTag().remove("start");
    }

    @Override
    public void appendHoverText(
      final @NotNull ItemStack stack, @Nullable final Level worldIn, final @NotNull List<Component> tooltip, final @NotNull TooltipFlag flagIn)
    {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (KeyBindingManager.getInstance().hasBeenInitialized()) {
                HelpTextUtils.build(
                  LocalStrings.HelpTapeMeasure, tooltip,
                  Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage(),
                  Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage(),
                  KeyBindingManager.getInstance().getResetMeasuringTapeKeyBinding().getTranslatedKeyMessage(),
                  KeyBindingManager.getInstance().getOpenToolMenuKeybinding().getTranslatedKeyMessage()
                );
            }
          });
    }
}
