package cn.rbq108.nextboundarycornerstone.core;

// 似乎，这里是注册按键用的

import cn.rbq108.nextboundarycornerstone.main;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class Keybinds {

    // 统一的分类名，确保所有按键都排在一起
    public static final String KEY_CATEGORY = "key.category.test.space_flight";

    // 基础移动按键
    public static final KeyMapping B_UP = new KeyMapping("key.test.up", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, KEY_CATEGORY);
    public static final KeyMapping B_DOWN = new KeyMapping("key.test.down", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, KEY_CATEGORY);
    public static final KeyMapping B_LEFT = new KeyMapping("key.test.left", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_A, KEY_CATEGORY);
    public static final KeyMapping B_RIGHT = new KeyMapping("key.test.right", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_D, KEY_CATEGORY);
    public static final KeyMapping B_FORWARD = new KeyMapping("key.test.forward", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_W, KEY_CATEGORY);
    public static final KeyMapping B_BACK = new KeyMapping("key.test.back", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_S, KEY_CATEGORY);

    // 旋转按键
    public static final KeyMapping B_ROLL_LEFT = new KeyMapping("key.test.roll_left", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, KEY_CATEGORY);
    public static final KeyMapping B_ROLL_RIGHT = new KeyMapping("key.test.roll_right", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, KEY_CATEGORY);

    // 专用冲刺按键
    public static final KeyMapping B_RUSH_BUTTON = new KeyMapping(
            "key.test.rush_button",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL, // 默认还是 Ctrl，虽然会和 B_DOWN 冲突变红（      不过好歹能改了不是
            KEY_CATEGORY
    );

    // 制动
    public static final KeyMapping B_MANUAL_BRAKE = new KeyMapping(
            "key.test.manual_brake",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KEY_CATEGORY
    );

    // 按住此键，再按原版背包/丢弃键；Tab 不是 NeoForge 内置的 KeyModifier。
    public static final KeyMapping B_ACTION_MODIFIER = new KeyMapping(
            "key.test.action_modifier",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            KEY_CATEGORY
    );

    // 自由旋转视角
    public static final KeyMapping B_FREE_CAMERA = new KeyMapping(
            "key.test.free_camera",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KEY_CATEGORY
    );

    // 固定自由旋转视角
    public static final KeyMapping B_FIXED_CAMERA = new KeyMapping(
            "key.test.fixed_camera",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            KEY_CATEGORY
    );

    // 固定自由视角左桶滚 (滚轮向上)
    public static final KeyMapping B_FIXED_CAMERA_LEFT = new KeyMapping(
            "key.test.fixed_camera_left",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    // 固定自由视角右桶滚 (滚轮向下)
    public static final KeyMapping B_FIXED_CAMERA_ROLL = new KeyMapping(
            "key.test.fixed_camera_roll",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    // 按键设置中的显示顺序：在这里调整，不依赖翻译名称或注册先后。
    // KeyMappingOrderMixin 仅对本分类使用此顺序，其他分类仍走原版排序。
    private static final List<KeyMapping> DISPLAY_ORDER = List.of(
            B_FORWARD, B_BACK, B_LEFT, B_RIGHT, B_UP, B_DOWN,
            B_ROLL_LEFT, B_ROLL_RIGHT, B_RUSH_BUTTON, B_MANUAL_BRAKE,
            B_ACTION_MODIFIER,
            B_FREE_CAMERA, B_FIXED_CAMERA, B_FIXED_CAMERA_LEFT, B_FIXED_CAMERA_ROLL
    );

    public static int displayOrder(KeyMapping mapping) {
        int index = DISPLAY_ORDER.indexOf(mapping);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        DISPLAY_ORDER.forEach(event::register);
    }
}
