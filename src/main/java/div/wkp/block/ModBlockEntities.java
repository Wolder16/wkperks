package div.wkp.block;

import div.wkp.WKPerks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
    private ModBlockEntities() {
    }

    public static final BlockEntityType<AltarBlockEntity> RHO_ALTAR =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(WKPerks.MOD_ID, "rho_altar"),
                    BlockEntityType.Builder.create(
                            AltarBlockEntity::new,
                            ModBlocks.RHO_ALTAR
                    ).build(null)
            );

    public static void initialize() {
        WKPerks.LOGGER.info("Registering WKPerks block entities");
    }
}