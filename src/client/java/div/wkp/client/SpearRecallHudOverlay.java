package div.wkp.client;

import div.wkp.ArtifactComponents;
import div.wkp.component.ArtifactStateComponent;
import div.wkp.entity.SpearProjectileEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SpearRecallHudOverlay {
    private static final int MARKER_RADIUS = 8;
    private static final int TARGET_RADIUS = 10;
    private static final double MIN_RECALL_DISTANCE = 3.0D;

    private static boolean markerVisible = false;
    private static boolean markerTargeted = false;
    private static Hand recallHand = null;

    private SpearRecallHudOverlay() {
    }

    public static boolean isMarkerTargeted() {
        return markerVisible && markerTargeted;
    }

    public static Hand getRecallHand() {
        return recallHand;
    }

    public static void render(DrawContext context, RenderTickCounter tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        markerVisible = false;
        markerTargeted = false;
        recallHand = null;

        if (client.player == null || client.world == null || client.currentScreen != null) {
            return;
        }

        PlayerEntity player = client.player;
        ArtifactStateComponent component = ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        if (!component.hasActiveSpear()) {
            return;
        }

        Entity entity = client.world.getEntityById(component.getActiveSpearEntityId());
        if (!(entity instanceof SpearProjectileEntity spear) || !spear.isEmbedded() || spear.isRecalling()) {
            return;
        }

        if (player.squaredDistanceTo(spear) < MIN_RECALL_DISTANCE * MIN_RECALL_DISTANCE) {
            return;
        }

        ScreenPoint marker = projectToScreen(client, spear.getPos(), tickDelta);
        if (marker == null) {
            return;
        }

        recallHand = findRecallHand(player);
        markerVisible = true;

        int centerX = context.getScaledWindowWidth() / 2;
        int centerY = context.getScaledWindowHeight() / 2;
        markerTargeted = Math.abs(marker.x - centerX) <= TARGET_RADIUS
                && Math.abs(marker.y - centerY) <= TARGET_RADIUS;

        int ringColor = recallHand == null
                ? 0x66880000
                : markerTargeted ? 0xFFFF6060 : 0xFFAA3030;

        drawMarker(context, marker.x, marker.y, 0xFFB0B0B0, ringColor);
    }

    private static Hand findRecallHand(PlayerEntity player) {
        if (player.getMainHandStack().isEmpty()) {
            return Hand.MAIN_HAND;
        }

        if (player.getOffHandStack().isEmpty()) {
            return Hand.OFF_HAND;
        }

        return null;
    }

    private static ScreenPoint projectToScreen(MinecraftClient client, Vec3d worldPos, RenderTickCounter tickDelta) {
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Vec3d relative = worldPos.subtract(camPos);

        Quaternionf inverseRotation = new Quaternionf(camera.getRotation()).conjugate();
        Vector3f transformed = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);
        transformed.rotate(inverseRotation);

        if (transformed.z() >= 0.0F) {
            return null;
        }

        double fov = client.options.getFov().getValue();
        double scale = client.getWindow().getScaledHeight()
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));

        int x = (int) Math.round(client.getWindow().getScaledWidth() / 2.0D
                + transformed.x() * scale / -transformed.z());
        int y = (int) Math.round(client.getWindow().getScaledHeight() / 2.0D
                - transformed.y() * scale / -transformed.z());

        if (x < -32 || x > client.getWindow().getScaledWidth() + 32
                || y < -32 || y > client.getWindow().getScaledHeight() + 32) {
            return null;
        }

        return new ScreenPoint(x, y);
    }

    private static void drawMarker(DrawContext context, int x, int y, int crossColor, int ringColor) {
        context.fill(x - 1, y - 6, x + 1, y - 1, crossColor);
        context.fill(x - 1, y + 1, x + 1, y + 6, crossColor);
        context.fill(x - 6, y - 1, x - 1, y + 1, crossColor);
        context.fill(x + 1, y - 1, x + 6, y + 1, crossColor);

        context.fill(x - MARKER_RADIUS, y - MARKER_RADIUS, x - MARKER_RADIUS + 2, y + MARKER_RADIUS, ringColor);
        context.fill(x + MARKER_RADIUS - 2, y - MARKER_RADIUS, x + MARKER_RADIUS, y + MARKER_RADIUS, ringColor);
        context.fill(x - MARKER_RADIUS, y - MARKER_RADIUS, x + MARKER_RADIUS, y - MARKER_RADIUS + 2, ringColor);
        context.fill(x - MARKER_RADIUS, y + MARKER_RADIUS - 2, x + MARKER_RADIUS, y + MARKER_RADIUS, ringColor);
    }

    private record ScreenPoint(int x, int y) {
    }
}
