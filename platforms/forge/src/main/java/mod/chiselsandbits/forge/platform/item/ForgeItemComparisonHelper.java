package mod.chiselsandbits.forge.platform.item;

import mod.chiselsandbits.platforms.core.item.IItemComparisonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class ForgeItemComparisonHelper implements IItemComparisonHelper
{
    private static final ForgeItemComparisonHelper INSTANCE = new ForgeItemComparisonHelper();

    public static ForgeItemComparisonHelper getInstance()
    {
        return INSTANCE;
    }

    private ForgeItemComparisonHelper()
    {
    }

    @Override
    public boolean canItemStacksStack(final ItemStack left, final ItemStack right)
    {
        return ItemHandlerHelper.canItemStacksStack(left, right);
    }
}
