package cn.rbq108.nextboundarycornerstone.item.equipment;

import cn.rbq108.nextboundarycornerstone.client.model.BASIC_BACKPACK_Converted;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class BasicBackpack extends ArmorItem {

    // 1.20.1 专属改法：直接实现 ArmorMaterial 接口，手动填入所有属性
    private static final ArmorMaterial BACKPACK_MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return 0; // 基础耐久度
        }

        @Override
        public int getDefenseForType(Type type) {
            return type == Type.CHESTPLATE ? 1 : 0; // 防御力喵
        }

        @Override
        public int getEnchantmentValue() {
            return 0; // 附魔等级
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER; // 穿戴声效
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY; // 修复材料喵
        }

        @Override
        public String getName() {
            // 1.20.1 是靠这个名字去找贴图的！
            // 游戏会自动去 assets/next_boundary_cornerstone/textures/models/armor/ 下找 basic_backpack_layer_1.png
            return "next_boundary_cornerstone:basic_backpack";
        }

        @Override
        public float getToughness() {
            return 0.0F; // 韧性
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F; // 击退抗性
        }
    };

    public BasicBackpack() {
        // 直接传入刚才写好的材质，不再需要 1.21 的 Holder
        super(BACKPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
    }

    // 1.20.1 的模型注册接口
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    net.minecraft.world.entity.LivingEntity entity,
                    ItemStack stack,
                    net.minecraft.world.entity.EquipmentSlot slot,
                    net.minecraft.client.model.HumanoidModel<?> _default
            ) {
                // 返回你转换好的那个背包 3D 模型
                return new BASIC_BACKPACK_Converted(
                        Minecraft.getInstance().getEntityModels().bakeLayer(BASIC_BACKPACK_Converted.LAYER_LOCATION)
                );
            }
        });
    }
}