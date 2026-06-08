package cn.rbq108.nextboundarycornerstone.client;

import com.tacz.guns.client.model.BedrockAmmoModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Quaternionf;

public class SpaceShellProxy {
    public Vec3 worldPosition;       // 绝对世界坐标
    public Vector3f velocity;        // 飞行速度
    public Vector3f angularVelocity; // 翻滚角速度
    public Quaternionf rotation;     // 当前的三维旋转姿态

    public BedrockAmmoModel model;   // TACZ 的现成模型
    public ResourceLocation texture; // TACZ 的现成贴图

    public long spawnTime;           // 出生时间
    public float livingTime;         // 存活寿命
}