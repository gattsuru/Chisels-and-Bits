package mod.chiselsandbits.registrars;

import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.platforms.core.util.constants.Constants;
import mod.chiselsandbits.pattern.placement.*;
import mod.chiselsandbits.platforms.core.registries.IChiselsAndBitsRegistry;
import mod.chiselsandbits.platforms.core.registries.deferred.ICustomRegistrar;
import mod.chiselsandbits.platforms.core.registries.deferred.IRegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public final class ModPatternPlacementTypes
{
    private static final Logger                                  LOGGER         = LogManager.getLogger();
    private static final ICustomRegistrar<IPatternPlacementType> TYPE_REGISTRAR = ICustomRegistrar.create(IPatternPlacementType.class, Constants.MOD_ID);

    public static final Supplier<IChiselsAndBitsRegistry<IPatternPlacementType>>
      REGISTRY_SUPPLIER = TYPE_REGISTRAR.makeRegistry(IChiselsAndBitsRegistry.Builder::simple);

    public static final IRegistryObject<IPatternPlacementType> PLACEMENT  = TYPE_REGISTRAR.register(
      "placement", PlacePatternPlacementType::new
    );
    public static final IRegistryObject<IPatternPlacementType> REMOVAL    = TYPE_REGISTRAR.register(
      "removal", RemovalPatternPlacementType::new
    );
    public static final IRegistryObject<IPatternPlacementType> IMPOSEMENT = TYPE_REGISTRAR.register(
      "imposement", ImposePatternPlacementType::new
    );
    public static final IRegistryObject<IPatternPlacementType> MERGE      = TYPE_REGISTRAR.register(
      "merge", MergePatternPlacementType::new
    );
    public static final IRegistryObject<IPatternPlacementType> CARVING    = TYPE_REGISTRAR.register(
      "carving", CarvePatternPlacementType::new
    );

    private ModPatternPlacementTypes()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ModPatternPlacementTypes. This is a utility class");
    }

    public static void onModConstruction()
    {
        LOGGER.info("Loaded pattern placement type configuration.");
    }

}
