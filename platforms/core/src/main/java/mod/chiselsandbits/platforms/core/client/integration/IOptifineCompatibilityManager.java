package mod.chiselsandbits.platforms.core.client.integration;

import mod.chiselsandbits.platforms.core.client.IClientManager;

/**
 * Compatibility manager for Optifine.
 */
public interface IOptifineCompatibilityManager
{

    /**
     * The instance of the manager.
     *
     * @return The instance.
     */
    static IOptifineCompatibilityManager getInstance() {
        return IClientManager.getInstance().getOptifineCompatibilityManager();
    }

    /**
     * Indicates if Optifine is installed.
     *
     * @return True if Optifine is installed.
     */
    boolean isInstalled();
}
