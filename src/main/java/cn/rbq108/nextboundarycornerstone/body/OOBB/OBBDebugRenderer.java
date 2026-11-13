package cn.rbq108.nextboundarycornerstone.body.OOBB;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** Client-only caller, deliberately separate from server geometry/configuration. */
public final class OBBDebugRenderer {
    private OBBDebugRenderer() {}

    public static void render(PoseStack stack, VertexConsumer lines, OBBHitbox hitbox, Vec3 entityPosition) {
        for (int i = 0; i < hitbox.orientedPieces().size(); i++) {
            OrientedBox box = hitbox.orientedPieces().get(i);
            Vec3 center = box.center().subtract(entityPosition), half = box.halfSize();
            var basis = new Matrix3f(
                    (float) box.axisX().x, (float) box.axisX().y, (float) box.axisX().z,
                    (float) box.axisY().x, (float) box.axisY().y, (float) box.axisY().z,
                    (float) box.axisZ().x, (float) box.axisZ().y, (float) box.axisZ().z);
            stack.pushPose();
            stack.translate(center.x, center.y, center.z);
            stack.mulPose(new Quaternionf().setFromNormalized(basis));
            LevelRenderer.renderLineBox(stack, lines, new AABB(-half.x, -half.y, -half.z, half.x, half.y, half.z),
                    i == 0 ? 1 : .2f, 1, i == 0 ? .2f : 1, 1);
            stack.popPose();
        }
    }
}
