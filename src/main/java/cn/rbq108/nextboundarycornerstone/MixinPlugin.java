package cn.rbq108.nextboundarycornerstone;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static boolean taczLoaded = false;

    // 检查class是否存在
    private static boolean isClassAvailable(String className) {
        String classPath = className.replace('.', '/') + ".class";
        return MixinPlugin.class.getClassLoader().getResource(classPath) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
        if (isClassAvailable("com.tacz.guns.entity.EntityKineticBullet") &&
            isClassAvailable("com.tacz.guns.client.model.functional.ShellRender")) {
            taczLoaded = true;
            System.out.println("[NextBoundaryCornerstone] 检测到 TACZ 模组已安装，正在启用相关Mixin···");
        } else {
            taczLoaded = false;
            System.out.println("[NextBoundaryCornerstone] 没有找到 TACZ 模组，相关Mixin已禁用。");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // TACZ 相关的 Mixin 仅在 TACZ 已安装时启用
        if (mixinClassName.equals("cn.rbq108.nextboundarycornerstone.Mixin.FixTACZ") ||
            mixinClassName.equals("cn.rbq108.nextboundarycornerstone.Mixin.MixinShellRender")) {
            return taczLoaded;
        }
        // 其他 Mixin 正常加载
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
