package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class SegmentedHitboxDebugMixin {
    @Inject(method = "renderHitbox", at = @At("TAIL"))
    private static void nextboundary$pieces(PoseStack stack, VertexConsumer lines, Entity entity, float partialTick,
                                            float red, float green, float blue, CallbackInfo ci) {
        if (!SegmentedHitboxes.enabled(entity)) return;
        var hitbox = (SegmentedAABB) SegmentedHitboxes.forHitQuery(entity);
        for (int i = 0; i < hitbox.pieces().size(); i++) {
            var box = hitbox.pieces().get(i).move(-entity.getX(), -entity.getY(), -entity.getZ());
            LevelRenderer.renderLineBox(stack, lines, box, i == 0 ? 1 : .2f, 1, i == 0 ? .2f : 1, 1);
        }
    }
}
