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
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class Keybinds {

    // 统一的分类名，确保所有按键都排在一起
    public static final String KEY_CATEGORY = "key.category.test.space_flight";

    // 基础移动按键
    public static final KeyMapping B_UP = new KeyMapping("key.test.up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, KEY_CATEGORY);
    public static final KeyMapping B_DOWN = new KeyMapping("key.test.down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, KEY_CATEGORY);
    public static final KeyMapping B_LEFT = new KeyMapping("key.test.left", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_A, KEY_CATEGORY);
    public static final KeyMapping B_RIGHT = new KeyMapping("key.test.right", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_D, KEY_CATEGORY);
    public static final KeyMapping B_FORWARD = new KeyMapping("key.test.forward", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_W, KEY_CATEGORY);
    public static final KeyMapping B_BACK = new KeyMapping("key.test.back", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_S, KEY_CATEGORY);

    // 旋转按键
    public static final KeyMapping B_ROLL_LEFT = new KeyMapping("key.test.roll_left", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, KEY_CATEGORY);
    public static final KeyMapping B_ROLL_RIGHT = new KeyMapping("key.test.roll_right", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, KEY_CATEGORY);

    // 专用冲刺按键
    public static final KeyMapping B_RUSH_BUTTON = new KeyMapping(
            "key.test.rush_button",
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

    // 组合键：shift+e
    public static final KeyMapping B_BAG = new KeyMapping(
            "key.test.bag",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_E,
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

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(B_UP);
        event.register(B_DOWN);
        event.register(B_LEFT);
        event.register(B_RIGHT);
        event.register(B_FORWARD);
        event.register(B_BACK);
        event.register(B_ROLL_LEFT);
        event.register(B_ROLL_RIGHT);
        event.register(B_BAG);
        event.register(B_MANUAL_BRAKE);
        event.register(B_RUSH_BUTTON); //确认登记
        event.register(B_FREE_CAMERA);
    }
}