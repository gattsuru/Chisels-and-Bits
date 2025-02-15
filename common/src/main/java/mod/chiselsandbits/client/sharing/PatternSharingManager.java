package mod.chiselsandbits.client.sharing;

import com.mojang.datafixers.util.Either;
import mod.chiselsandbits.api.client.sharing.IPatternSharingManager;
import mod.chiselsandbits.api.client.sharing.PatternIOException;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.platforms.core.dist.Dist;
import mod.chiselsandbits.platforms.core.dist.DistExecutor;

public final class PatternSharingManager implements IPatternSharingManager
{
    private static final PatternSharingManager INSTANCE = new PatternSharingManager();

    public static PatternSharingManager getInstance()
    {
        return INSTANCE;
    }

    private PatternSharingManager()
    {
    }

    @Override
    public void exportPattern(final IMultiStateItemStack multiStateItemStack, final String name)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PatternSharingExecutor.doSavePattern(multiStateItemStack, name));
    }

    @Override
    public Either<IMultiStateItemStack, PatternIOException> importPattern(final String name)
    {
        return DistExecutor.unsafeRunForDist(() -> () -> PatternSharingExecutor.doImportPattern(name), () -> () -> Either.right(new PatternIOException(
          LocalStrings.PatternImportInvokedFromTheServer.getText(),
          "Pattern invoked from the server side!"
        )));
    }
}
