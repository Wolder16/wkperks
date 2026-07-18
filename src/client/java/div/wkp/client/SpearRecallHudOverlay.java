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
    private static final int MARKER_RADIUS = 12;
    private static final int TARGET_RADIUS = 14;
    private static final int FADE_RADIUS = 42;
    private static final double MIN_RECALL_DISTANCE = 2.0D;
    private static final double MARKER_OFFSET = 0.25D;

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

        Vec3d markerWorldPos = spear.getPos().add(getMarkerOffsetVector(spear).multiply(MARKER_OFFSET));
        ScreenPoint marker = projectToScreen(client, markerWorldPos, tickDelta);
        if (marker == null) {
            return;
        }

        recallHand = findRecallHand(player);

        int centerX = context.getScaledWindowWidth() / 2;
        int centerY = context.getScaledWindowHeight() / 2;
        int dx = marker.x - centerX;
        int dy = marker.y - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > FADE_RADIUS) {
            return;
        }

        markerVisible = true;
        markerTargeted = distance <= TARGET_RADIUS;

        float fade = 1.0F - (float) Math.min(distance / FADE_RADIUS, 1.0D);
        int alpha = Math.max(0x20, Math.min(0x80, (int) (fade * 0x80)));

        int markerColor = recallHand == null
                ? (alpha << 24) | 0x880000
                : markerTargeted
                ? (alpha << 24) | 0xFF7070
                : (alpha << 24) | 0xC04040;

        drawMarker(context, marker.x, marker.y, markerColor);
    }

    private static Hand findRecallHand(PlayerEntity player) {
        if (player.getMainHandStack().isEmpty()) {
            return Hand.MAIN_HAND;
        }

        if (player.getOffHandStack().isEmpty()) {
            return Hand.OFF_HAND;
        }

        return Hand.MAIN_HAND;
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

    private static Vec3d getMarkerOffsetVector(SpearProjectileEntity spear) {
        float yaw = spear.getEmbeddedYaw();
        float pitch = spear.getEmbeddedPitch();
        return Vec3d.fromPolar(pitch, yaw).normalize();
    }

    private static void drawMarker(DrawContext context, int x, int y, int color) {
        for (int row = 0; row < MARKER_RADIUS; row++) {
            int halfWidth = Math.max(1, row / 2 + 1);
            int yPos = y + row - MARKER_RADIUS / 2;
            context.fill(x - halfWidth, yPos, x + halfWidth + 1, yPos + 1, color);
        }
    }

    private record ScreenPoint(int x, int y) {
    }
}
