package div.wkp.client.block;

import div.wkp.block.AltarBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.Objects;

public class AltarPedestalRenderer implements BlockEntityRenderer<AltarBlockEntity> {

    public AltarPedestalRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(AltarBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        if (entity.getStack().isEmpty()) return;

        matrices.push();

        // Позиционирование над алтарём
        matrices.translate(0.5, 1.15, 0.5);

        // Лёгкое покачивание вверх-вниз
        double bob = MathHelper.sin((Objects.requireNonNull(entity.getWorld()).getTime() + tickDelta) / 8.0f) * 0.04;
        matrices.translate(0.0, bob, 0.0);

        // Медленное вращение
        float rotation = (entity.getWorld().getTime() + tickDelta) * 2.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

        // Небольшой наклон, чтобы предмет выглядел "лежащим"
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(5.0f)); // небольшой наклон

        // Масштаб
        float scale = entity.isProcessing() ? 0.55f : 0.65f;
        matrices.scale(scale, scale, scale);

        // Разный режим рендера в зависимости от состояния
        ModelTransformationMode mode = ModelTransformationMode.GROUND;

        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

        itemRenderer.renderItem(
                entity.getStack(),
                mode,
                light,
                overlay,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                0
        );

        matrices.pop();
    }
}