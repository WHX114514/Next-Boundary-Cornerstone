package cn.rbq108.nextboundarycornerstone.client;

import com.tacz.guns.client.model.BedrockAmmoModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpaceShellProxy {
    public Vec3 startPosition;
    public Quaternionf startRotation;

    public Vector3f velocity = new Vector3f();
    public Vector3f angularVelocity = new Vector3f();

    public BedrockAmmoModel model;
    public ResourceLocation texture;
    public long spawnTime;
}