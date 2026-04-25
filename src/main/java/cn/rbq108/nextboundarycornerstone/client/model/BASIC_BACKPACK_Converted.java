package cn.rbq108.nextboundarycornerstone.client.model;// Made with Blockbench 5.1.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

// 导入LivingEntity喵
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.model.HumanoidModel;

import net.minecraft.client.model.geom.ModelLayerLocation; // 这行引入治好了两个报错，加个注释做个纪念（
import net.minecraft.resources.ResourceLocation;         // 这行顺便把 ResourceLocation 也治了喵！
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart; // 治治治治治治治治
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*; // 这一行能治好下面那堆 LayerDefinitipn的报错喵


//把 EntityModel 换成 HumanoidModel，把 Entity 换成 LivingEntity 绕飞了
public class BASIC_BACKPACK_Converted<T extends LivingEntity> extends HumanoidModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    //这行不知道从哪抄来的，英文注释我怕删了到时候看不懂（虽然不删也看不懂）
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation("next_boundary_cornerstone", "basic_backpack"), "main"
    );
    /*这个是1.21neof的代码，这版本用不了
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("next_boundary_cornerstone", "basic_backpack"), "main"
    );*/
	private final ModelPart root2;
	private final ModelPart waist2;
	private final ModelPart body2;
	private final ModelPart cape;
	private final ModelPart head;
	private final ModelPart helmet;
	private final ModelPart rightArm;
	private final ModelPart rightItem;
	private final ModelPart leftArm;
	private final ModelPart leftItem;
	private final ModelPart bag2;
	private final ModelPart bag3;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart root;
	private final ModelPart waist;
	private final ModelPart body;
	private final ModelPart bag;

	public BASIC_BACKPACK_Converted(ModelPart root) {
        super(root); // 必须加上这一句，骨架交给人型模型父类
		this.root2 = root.getChild("root2");
		this.waist2 = this.root2.getChild("waist2");
		this.body2 = this.waist2.getChild("body2");
		this.cape = this.body2.getChild("cape");
		this.head = this.body2.getChild("head");
		this.helmet = this.head.getChild("helmet");
		this.rightArm = this.body2.getChild("rightArm");
		this.rightItem = this.rightArm.getChild("rightItem");
		this.leftArm = this.body2.getChild("leftArm");
		this.leftItem = this.leftArm.getChild("leftItem");
		this.bag2 = this.body2.getChild("bag2");
		this.bag3 = this.body2.getChild("bag3");
		this.rightLeg = this.root2.getChild("rightLeg");
		this.leftLeg = this.root2.getChild("leftLeg");
		this.root = root.getChild("root");
		this.waist = this.root.getChild("waist");
		this.body = this.waist.getChild("body");
		this.bag = this.body.getChild("bag");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();


        // 补齐人型模型必须的7个东西
        // 哪怕它们里面没方块，也必须得声明，不然 HumanoidModel 会直接罢工喵
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

		PartDefinition root2 = partdefinition.addOrReplaceChild("root2", CubeListBuilder.create(), PartPose.offset(-3.0F, 24.0F, 0F));//(8.0F, 好像是高度？24.0F, 好像是前后-8.0F));

		PartDefinition waist2 = root2.addOrReplaceChild("waist2", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition body2 = waist2.addOrReplaceChild("body2", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition cape = body2.addOrReplaceChild("cape", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 2.0F));

		PartDefinition head = body2.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition helmet = head.addOrReplaceChild("helmet", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition rightArm = body2.addOrReplaceChild("rightArm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));

		PartDefinition rightItem = rightArm.addOrReplaceChild("rightItem", CubeListBuilder.create(), PartPose.offset(-1.0F, 7.0F, 1.0F));

		PartDefinition leftArm = body2.addOrReplaceChild("leftArm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));

		PartDefinition leftItem = leftArm.addOrReplaceChild("leftItem", CubeListBuilder.create(), PartPose.offset(1.0F, 7.0F, 1.0F));

		PartDefinition bag2 = body2.addOrReplaceChild("bag2", CubeListBuilder.create().texOffs(7, 1).addBox(-1.25F, -0.25F, -2.5F, 1.0F, 1.0F, 4.75F, new CubeDeformation(0.0F))
		.texOffs(13, 1).addBox(-1.25F, -0.25F, 2.0F, 1.0F, 7.25F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(19, 1).addBox(-1.25F, 0.75F, 2.0F, 4.0F, 2.25F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(25, 1).addBox(-1.25F, 6.75F, 2.0F, 5.0F, 2.25F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(5, 2).addBox(-1.25F, 5.75F, 1.7F, 1.0F, 7.25F, 0.8F, new CubeDeformation(0.0F))
		.texOffs(11, 2).addBox(-4.0F, 14.75F, -5.3F, 1.0F, 1.0F, 10.8F, new CubeDeformation(0.0F))
		.texOffs(9, 6).addBox(-4.25F, -2.5F, -5.3F, 1.0F, 1.0F, 10.8F, new CubeDeformation(0.0F))
		.texOffs(27, 6).addBox(-4.1F, -3.0F, -4.8F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(1, 7).addBox(-3.85F, -3.0F, -5.05F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(7, 7).addBox(-4.1F, -3.0F, -5.05F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(13, 7).addBox(-4.1F, -2.5F, -5.05F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(15, 9).addBox(-4.1F, -2.5F, 4.95F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(21, 9).addBox(-4.1F, -3.0F, 4.95F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(27, 9).addBox(-3.85F, -3.0F, 4.95F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(1, 10).addBox(-4.1F, -3.0F, 5.2F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(7, 10).addBox(-4.1F, -3.0F, 4.95F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(7, 13).addBox(-4.1F, -3.0F, -5.05F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(2, 14).mirror().addBox(-1.25F, -0.25F, -2.5F, 1.0F, 7.25F, 0.5F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(5, 14).addBox(-1.25F, 6.5F, -2.5F, 1.0F, 0.5F, 4.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r1 = bag2.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(25, 13).addBox(-0.15F, 0.075F, -0.15F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(19, 13).addBox(-0.15F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(13, 13).addBox(-0.15F, -0.425F, -0.15F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(21, 6).addBox(-0.15F, -0.425F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(0.1F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.9F, -2.05F, -5.475F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r2 = bag2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(1, 13).addBox(-0.15F, -0.55F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(27, 12).addBox(-0.15F, -0.55F, 0.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(21, 12).addBox(0.1F, -0.55F, 0.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(9, 12).addBox(-0.15F, -0.05F, 0.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(7, 4).addBox(-0.15F, -0.55F, 10.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(1, 4).addBox(-0.15F, -0.55F, 10.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(27, 3).addBox(0.1F, -0.55F, 10.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(15, 3).addBox(-0.15F, -0.05F, 10.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.65F, 15.75F, -5.15F, 0.0F, 0.0F, -3.1416F));

		PartDefinition cube_r3 = bag2.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(15, 12).addBox(0.1F, -0.55F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(21, 3).addBox(0.1F, -0.55F, 9.85F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.4F, 15.75F, -4.9F, 0.0F, 0.0F, -3.1416F));

		PartDefinition cube_r4 = bag2.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(3, 12).addBox(-0.15F, -0.55F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(29, 12).addBox(-0.15F, -0.55F, 0.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(23, 11).addBox(0.1F, -0.55F, 0.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(11, 11).addBox(-0.15F, -0.05F, 0.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(9, 3).addBox(-0.15F, -0.55F, 10.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(3, 3).addBox(-0.15F, -0.55F, 10.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(29, 3).addBox(0.1F, -0.55F, 10.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(17, 2).addBox(-0.15F, -0.05F, 10.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.9F, 15.2F, -5.15F, 0.0F, 0.0F, -1.5708F));

		PartDefinition cube_r5 = bag2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(17, 11).addBox(0.1F, -0.55F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(23, 2).addBox(0.1F, -0.55F, 9.85F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.9F, 15.45F, -4.9F, 0.0F, 0.0F, -1.5708F));

		PartDefinition cube_r6 = bag2.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(5, 11).addBox(-0.15F, 0.075F, -0.15F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(2, 11).mirror().addBox(-0.15F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(25, 10).addBox(-0.15F, -0.425F, -0.15F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(19, 10).addBox(0.1F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(13, 10).addBox(-0.15F, -0.425F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.9F, -2.05F, 5.675F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r7 = bag2.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(9, 9).addBox(-0.15F, -0.55F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(3, 9).addBox(-0.15F, -0.55F, 0.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(29, 9).addBox(0.1F, -0.55F, 0.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(17, 8).addBox(-0.15F, -0.05F, 0.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(11, 8).addBox(-0.15F, -0.05F, -9.9F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(2, 8).mirror().addBox(0.1F, -0.55F, -9.9F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(25, 7).addBox(-0.15F, -0.55F, -9.65F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(19, 7).addBox(-0.15F, -0.55F, -9.9F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.15F, -2.05F, 4.85F, 0.0F, 0.0F, -1.5708F));

		PartDefinition cube_r8 = bag2.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(23, 8).addBox(0.1F, -0.55F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(5, 8).addBox(0.1F, -0.55F, -10.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.15F, -1.8F, 5.1F, 0.0F, 0.0F, -1.5708F));

		PartDefinition cube_r9 = bag2.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(15, 6).addBox(-0.5F, -0.4F, -0.4F, 1.0F, 4.0F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 12.65F, 2.1F, 0.0F, 0.0F, 0.7418F));

		PartDefinition cube_r10 = bag2.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(3, 6).addBox(-0.15F, -0.425F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(29, 6).addBox(0.1F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(23, 5).addBox(-0.15F, -0.425F, -0.15F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(17, 5).addBox(-0.15F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(11, 5).addBox(-0.15F, 0.075F, -0.15F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.65F, 15.2F, -5.475F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r11 = bag2.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(5, 5).addBox(-0.15F, 0.075F, -0.15F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(2, 5).mirror().addBox(-0.15F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(25, 4).addBox(-0.15F, -0.425F, -0.15F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(19, 4).addBox(0.1F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(13, 4).addBox(-0.15F, -0.425F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.65F, 15.2F, 5.675F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r12 = bag2.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(2, 2).mirror().addBox(-0.5F, -3.75F, -0.5F, 1.0F, 4.25F, 0.5F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 0.5F, 2.5F, 0.0F, 0.0F, -0.8727F));

		PartDefinition bag3 = body2.addOrReplaceChild("bag3", CubeListBuilder.create().texOffs(17, 14).addBox(6.25F, -0.25F, -2.5F, 1.0F, 1.0F, 4.75F, new CubeDeformation(0.0F))
		.texOffs(23, 14).addBox(6.25F, -0.25F, 2.0F, 1.0F, 7.25F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(29, 15).addBox(2.25F, 0.75F, 2.0F, 5.0F, 2.25F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(3, 15).addBox(3.25F, 6.75F, 2.0F, 4.0F, 2.25F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(15, 15).addBox(6.25F, 5.75F, 1.7F, 1.0F, 7.25F, 0.8F, new CubeDeformation(0.0F))
		.texOffs(21, 15).addBox(9.0F, 14.75F, -5.3F, 1.0F, 1.0F, 10.8F, new CubeDeformation(0.0F))
		.texOffs(19, 19).addBox(9.25F, -2.5F, -5.3F, 1.0F, 1.0F, 10.8F, new CubeDeformation(0.0F))
		.texOffs(5, 20).addBox(9.8F, -3.0F, -4.8F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(11, 20).addBox(9.8F, -3.0F, -5.05F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(17, 20).addBox(10.05F, -3.0F, -5.05F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(23, 20).addBox(9.8F, -2.5F, -5.05F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(25, 22).addBox(9.8F, -2.5F, 4.95F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(2, 23).mirror().addBox(10.05F, -3.0F, 4.95F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(5, 23).addBox(9.8F, -3.0F, 4.95F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(11, 23).addBox(9.8F, -3.0F, 5.2F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(17, 23).addBox(9.8F, -3.0F, 4.95F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(17, 26).addBox(9.8F, -3.0F, -5.05F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(9, 27).addBox(6.25F, -0.25F, -2.5F, 1.0F, 7.25F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(15, 27).addBox(6.25F, 6.5F, -2.5F, 1.0F, 0.5F, 4.5F, new CubeDeformation(0.0F))
		.texOffs(21, 27).addBox(7.25F, 6.5F, 0.0F, 0.75F, 0.5F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(27, 27).addBox(7.25F, 12.0F, -6.0F, 0.75F, 0.5F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(1, 28).addBox(7.25F, 12.0F, -6.5F, 1.75F, 1.1F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(7, 28).addBox(7.825F, 11.0F, -6.025F, 0.5F, 1.0F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(13, 28).addBox(7.75F, 9.75F, -6.125F, 0.65F, 1.25F, 0.675F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r13 = bag3.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(19, 28).addBox(1.25F, 4.75F, -0.75F, 0.75F, 0.5F, 6.75F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 9.0F, -6.25F, 1.0036F, 0.0F, 0.0F));

		PartDefinition cube_r14 = bag3.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(3, 27).addBox(-0.15F, 0.075F, -0.15F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(29, 27).addBox(0.1F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(23, 26).addBox(-0.15F, -0.425F, -0.15F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(2, 20).mirror().addBox(-0.15F, -0.425F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(11, 14).addBox(-0.15F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.9F, -2.05F, -5.475F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r15 = bag3.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(11, 26).addBox(-0.15F, -0.55F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(5, 26).addBox(-0.15F, -0.55F, 0.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(2, 26).mirror().addBox(-0.15F, -0.55F, 0.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(19, 25).addBox(-0.15F, -0.05F, 0.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(17, 17).addBox(-0.15F, -0.55F, 10.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(11, 17).addBox(-0.15F, -0.55F, 10.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(5, 17).addBox(-0.15F, -0.55F, 10.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(25, 16).addBox(-0.15F, -0.05F, 10.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.65F, 15.75F, -5.15F, 0.0F, 0.0F, 3.1416F));

		PartDefinition cube_r16 = bag3.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(25, 25).addBox(-0.15F, -0.55F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(2, 17).mirror().addBox(-0.15F, -0.55F, 9.85F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(9.4F, 15.75F, -4.9F, 0.0F, 0.0F, 3.1416F));

		PartDefinition cube_r17 = bag3.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(13, 25).addBox(-0.15F, -0.55F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(7, 25).addBox(-0.15F, -0.55F, 0.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(1, 25).addBox(-0.15F, -0.55F, 0.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(21, 24).addBox(-0.15F, -0.05F, 0.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(19, 16).addBox(-0.15F, -0.55F, 10.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(13, 16).addBox(-0.15F, -0.55F, 10.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(7, 16).addBox(-0.15F, -0.55F, 10.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(27, 15).addBox(-0.15F, -0.05F, 10.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.9F, 15.2F, -5.15F, 0.0F, 0.0F, 1.5708F));

		PartDefinition cube_r18 = bag3.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(27, 24).addBox(-0.15F, -0.55F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(1, 16).addBox(-0.15F, -0.55F, 9.85F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.9F, 15.45F, -4.9F, 0.0F, 0.0F, 1.5708F));

		PartDefinition cube_r19 = bag3.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(15, 24).addBox(-0.15F, 0.075F, -0.15F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(9, 24).addBox(0.1F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(3, 24).addBox(-0.15F, -0.425F, -0.15F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(29, 24).addBox(-0.15F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(23, 23).addBox(-0.15F, -0.425F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.9F, -2.05F, 5.675F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r20 = bag3.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(19, 22).addBox(-0.15F, -0.55F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(13, 22).addBox(-0.15F, -0.55F, 0.35F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(7, 22).addBox(-0.15F, -0.55F, 0.1F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(27, 21).addBox(-0.15F, -0.05F, 0.1F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(21, 21).addBox(-0.15F, -0.05F, -9.9F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(9, 21).addBox(-0.15F, -0.55F, -9.9F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(3, 21).addBox(-0.15F, -0.55F, -9.65F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(29, 21).addBox(-0.15F, -0.55F, -9.9F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.15F, -2.05F, 4.85F, 0.0F, 0.0F, 1.5708F));

		PartDefinition cube_r21 = bag3.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(1, 22).addBox(-0.15F, -0.55F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(15, 21).addBox(-0.15F, -0.55F, -10.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.15F, -1.8F, 5.1F, 0.0F, 0.0F, 1.5708F));

		PartDefinition cube_r22 = bag3.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(25, 19).addBox(-0.5F, -0.4F, -0.4F, 1.0F, 4.0F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 12.65F, 2.1F, 0.0F, 0.0F, -0.7418F));

		PartDefinition cube_r23 = bag3.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(13, 19).addBox(-0.15F, -0.425F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(7, 19).addBox(-0.15F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(1, 19).addBox(-0.15F, -0.425F, -0.15F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(27, 18).addBox(0.1F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(21, 18).addBox(-0.15F, 0.075F, -0.15F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.65F, 15.2F, -5.475F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r24 = bag3.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(15, 18).addBox(-0.15F, 0.075F, -0.15F, 0.3F, 0.1F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(9, 18).addBox(0.1F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(3, 18).addBox(-0.15F, -0.425F, -0.15F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(29, 18).addBox(-0.15F, -0.425F, -0.15F, 0.05F, 0.6F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(23, 17).addBox(-0.15F, -0.425F, 0.1F, 0.3F, 0.6F, 0.05F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.65F, 15.2F, 5.675F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r25 = bag3.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(9, 15).addBox(-0.5F, -3.75F, -0.5F, 1.0F, 4.25F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 0.5F, 2.5F, 0.0F, 0.0F, 0.8727F));

		PartDefinition rightLeg = root2.addOrReplaceChild("rightLeg", CubeListBuilder.create(), PartPose.offset(-1.9F, -12.0F, 0.0F));

		PartDefinition leftLeg = root2.addOrReplaceChild("leftLeg", CubeListBuilder.create(), PartPose.offset(1.9F, -12.0F, 0.0F));

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(-8.0F, 24.0F, -8.0F));

		PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition bag = body.addOrReplaceChild("bag", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        // 1.20.1 需要把四个颜色浮点数全传进去
        root2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}