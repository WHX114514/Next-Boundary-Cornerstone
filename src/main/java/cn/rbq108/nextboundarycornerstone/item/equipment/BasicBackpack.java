package cn.rbq108.nextboundarycornerstone.item.equipment;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

//何意味何意味何意味喵！🤤

public class BasicBackpack extends ArmorItem {

    private static final Holder<ArmorMaterial> BACKPACK_MATERIAL = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.CHESTPLATE, 1 // 防御力喵
            ),
            0, // 附魔等级
            SoundEvents.ARMOR_EQUIP_LEATHER, // 穿戴声效
            () -> Ingredient.EMPTY, // 修复材料喵
            // 指定贴图路径，注意全小写喵 它会去找 assets/test/textures/models/armor/basic_backpack_layer_1.png
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("next_boundary_cornerstone", "basic_backpack"))),
            0.0F, // 韧性
            0.0F  // 击退抗性
    ));

    public BasicBackpack() {
        super(BACKPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
    }

}
/*package cn.rbq108.test.item.equipment;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

public class BasicBackpack extends ArmorItem {
    public BasicBackpack() {
        // 借用皮革材质，胸甲类型
        super(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
    }

    //模型接口
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {

    }
}*/

/*这是一大坨一大坨超超级大的屎山！模型也塞进来了（虽然用不了）
package cn.rbq108.test.item.equipment;


import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;



@OnlyIn(Dist.CLIENT)
public class basic_backpack<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("test", "basic_backpack"), "main");

    private final ModelPart bag;

    public basic_backpack(ModelPart root) {

        this.bag = root.getChild("root2").getChild("waist2").getChild("body2").getChild("bag2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();


        PartDefinition root2 = partdefinition.addOrReplaceChild("root2", CubeListBuilder.create(), PartPose.offset(8.0F, 24.0F, -8.0F));
        PartDefinition waist2 = root2.addOrReplaceChild("waist2", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition body2 = waist2.addOrReplaceChild("body2", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));
        body2.addOrReplaceChild("bag2", CubeListBuilder.create().texOffs(7, 1).addBox(-1.25F, -0.25F, -2.5F, 1.0F, 1.0F, 4.75F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));


        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bag.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
*/
/*
// 补上这些NeoF的原生接口

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;



public class BasicBackpack extends ArmorItem {

    public BasicBackpack() {
        super(BACKPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> _default
            ) {

                return BackpackModel.INSTANCE;
            }
        });
    }
}

/*
下面是，一大坨！package cn.rbq108.test.item.equipment;


import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;



public class BasicBackpack extends ArmorItem {


    private static final Holder<ArmorMaterial> BACKPACK_MATERIAL = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.CHESTPLATE, 1
            ),
            0,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.EMPTY,
            // 关键，会去 assets/test/textures/models/armor/ 目录下找 basic_backpack_layer_1.png
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("test", "basic_backpack"))),
            0.0F,
            0.0F
    ));

    public BasicBackpack() {

        super(BACKPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()
                .stacksTo(1)
        );
    }
}*/
