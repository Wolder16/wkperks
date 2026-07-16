package div.wkp.client.model;

import div.wkp.WKPerks;
import div.wkp.artifact.ArtifactSpearItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class ArtifactSpearItemGeoModel extends GeoModel<ArtifactSpearItem> {
    private static final Identifier MODEL = Identifier.of(WKPerks.MOD_ID, "geo/artifact_spear.geo.json");
    private static final Identifier ANIMATION = Identifier.of(WKPerks.MOD_ID, "animations/artifact_spear.animation.json");

    @Override
    public Identifier getModelResource(ArtifactSpearItem animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(ArtifactSpearItem animatable) {
        return ArtifactSpearItem.getTextureForStack(new net.minecraft.item.ItemStack(animatable));
    }

    @Override
    public Identifier getAnimationResource(ArtifactSpearItem animatable) {
        return ANIMATION;
    }
}
