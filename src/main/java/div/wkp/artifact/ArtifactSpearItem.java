package div.wkp.artifact;

import div.wkp.ArtifactComponents;
import div.wkp.WKPerks;
import div.wkp.component.ArtifactStateComponent;
import div.wkp.entity.SpearProjectileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class ArtifactSpearItem extends ArtifactItem implements GeoItem {
    public static final int MAX_HEAT = 20;
    public static final int HEAT_PER_PHASE = 5;
    public static final int COOL_INTERVAL_TICKS = 20;

    private static final Identifier PHASE1_TEXTURE = Identifier.of(WKPerks.MOD_ID, "textures/item/artifact_spear_phase1.png");
    private static final Identifier PHASE2_TEXTURE = Identifier.of(WKPerks.MOD_ID, "textures/item/artifact_spear_phase2.png");
    private static final Identifier PHASE3_TEXTURE = Identifier.of(WKPerks.MOD_ID, "textures/item/artifact_spear_phase3.png");
    private static final Identifier PHASE4_TEXTURE = Identifier.of(WKPerks.MOD_ID, "textures/item/artifact_spear_phase4.png");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public ArtifactSpearItem(Settings settings) {
        super(settings, MAX_HEAT, 0, 8, true);
    }

    public static int getHeatPhase(ItemStack stack) {
        int heat = Math.max(0, Math.min(MAX_HEAT, stack.getDamage()));

        if (heat >= 15) {
            return 4;
        }
        if (heat >= 10) {
            return 3;
        }
        if (heat >= 5) {
            return 2;
        }
        return 1;
    }

    public static boolean isOverheated(ItemStack stack) {
        return getHeatPhase(stack) >= 4;
    }

    public static void addHeatOnCatch(ItemStack stack) {
        stack.setDamage(Math.min(MAX_HEAT, stack.getDamage() + HEAT_PER_PHASE));
    }

    public static Identifier getTextureForStack(ItemStack stack) {
        return switch (getHeatPhase(stack)) {
            case 2 -> PHASE2_TEXTURE;
            case 3 -> PHASE3_TEXTURE;
            case 4 -> PHASE4_TEXTURE;
            default -> PHASE1_TEXTURE;
        };
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            World world,
            Entity entity,
            int slot,
            boolean selected
    ) {
        if (!world.isClient
                && entity instanceof ServerPlayerEntity
                && !selected
                && slot != PlayerInventory.OFF_HAND_SLOT
                && stack.getDamage() > 0
                && entity.age % COOL_INTERVAL_TICKS == 0) {
            stack.setDamage(Math.max(0, stack.getDamage() - 1));
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public boolean canStartUsing(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
        return super.canStartUsing(player, hand, stack, component)
                && !ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player).hasActiveSpear();
    }

    @Override
    public void onUseStarted(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
        int sourceSlot = hand == Hand.MAIN_HAND
                ? player.getInventory().selectedSlot
                : PlayerInventory.OFF_HAND_SLOT;

        SpearProjectileEntity spear = new SpearProjectileEntity(player, hand, sourceSlot, stack.copy());
        player.getWorld().spawnEntity(spear);
        component.setActiveSpear(spear.getUuidAsString(), sourceSlot);
        SpearProjectileEntity.applyThrowRecoil(player);

        if (!player.getAbilities().creativeMode) {
            player.setStackInHand(hand, ItemStack.EMPTY);
        }

        component.stopUsingArtifact();
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            java.util.List<Text> tooltip,
            net.minecraft.item.tooltip.TooltipType type
    ) {
        tooltip.add(
                Text.literal("ПКМ: бросок. Повторный ПКМ после броска: отзыв.")
                        .formatted(Formatting.DARK_AQUA)
        );
        tooltip.add(
                Text.literal("Фаза нагрева: " + getHeatPhase(stack) + " / 4")
                        .formatted(isOverheated(stack) ? Formatting.RED : Formatting.GOLD)
        );
        tooltip.add(
                Text.literal("Остывает в неактивном слоте по 1 ед. каждые 2 секунды.")
                        .formatted(Formatting.GRAY)
        );
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private div.wkp.client.renderer.item.ArtifactSpearItemRenderer renderer;

            @Override
            public net.minecraft.client.render.item.BuiltinModelItemRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new div.wkp.client.renderer.item.ArtifactSpearItemRenderer();
                }

                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
