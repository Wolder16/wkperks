package div.wkp.client.renderer.entity;

import div.wkp.client.model.SpearProjectileGeoModel;
import div.wkp.entity.SpearProjectileEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SpearProjectileRenderer extends GeoEntityRenderer<SpearProjectileEntity> {
    public SpearProjectileRenderer(EntityRendererFactory.Context context) {
        super(context, new SpearProjectileGeoModel());
    }

    @Override
    protected void applyRotations(
            SpearProjectileEntity animatable,
            MatrixStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick,
            float nativeScale
    ) {
        float yaw = animatable.isEmbedded()
                ? animatable.getEmbeddedYaw()
                : MathHelper.lerp(partialTick, animatable.prevYaw, animatable.getYaw());

        float pitch = animatable.isEmbedded()
                ? animatable.getEmbeddedPitch()
                : MathHelper.lerp(partialTick, animatable.prevPitch, animatable.getPitch());

        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw - 90.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pitch + 90.0F));
    }
}
