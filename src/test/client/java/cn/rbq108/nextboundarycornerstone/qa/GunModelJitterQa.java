package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.event.ClientEvents;
import cn.rbq108.nextboundarycornerstone.camera.HandSway;
import cn.rbq108.nextboundarycornerstone.main;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

/** Compare actual TaCZ sway transforms with recorded e669ea8 samples, before world-angle publication. */
public final class GunModelJitterQa {
    public static boolean probing;
    public static PoseStack pose;
    public static String capturedPath;
    private JsonArray golden;
    private double maxError;
    public static Matrix4f matrix;
    private int phase,ticks;
    private final long started=System.currentTimeMillis();
    private final List<Map<String,Object>> rows=new ArrayList<>(),summaries=new ArrayList<>();

    public GunModelJitterQa() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,this::tick);
    }
    private static double angle(Quaternionf a,Quaternionf b) {
        double max=0;
        for(var axis:new Vector3f[]{new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,1)})
            max=Math.max(max,Math.toDegrees(2*Math.asin(Math.min(1,a.transform(new Vector3f(axis)).distance(b.transform(new Vector3f(axis)))/2))));
        return max;
    }
    private void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || phase==99)return;
        var mc=Minecraft.getInstance();
        try {
            if(System.currentTimeMillis()-started>150000)throw new AssertionError("probe timeout phase="+phase);
            long window=mc.getWindow().getWindow();
            if(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_VISIBLE)!=0 || GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)!=0)
                throw new AssertionError("window visible or focused");
            if(mc.screen!=null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))mc.setScreen(new TitleScreen());
            if(phase==0 && mc.screen instanceof TitleScreen && mc.getOverlay()==null) {
                mc.options.pauseOnLostFocus=false;mc.options.framerateLimit().set(60);
                var rules=new net.minecraft.world.level.GameRules();rules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("gun-model-qa-"+started,
                        new net.minecraft.world.level.LevelSettings("Gun model probe",net.minecraft.world.level.GameType.CREATIVE,false,
                                net.minecraft.world.Difficulty.PEACEFUL,true,rules,net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                        new net.minecraft.world.level.levelgen.WorldOptions(20261117L,false,false),
                        r -> r.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                                .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
                phase=1;
            } else if(phase==1 && mc.player!=null && mc.screen==null) {
                var id=mc.player.getUUID();mc.getSingleplayerServer().submit(() -> {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);p.teleportTo(0,120,0);
                    p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(main.BASIC_BACKPACK.get()));
                    p.setItemSlot(EquipmentSlot.MAINHAND,GunItemBuilder.create().setId(new net.minecraft.resources.ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).build());
                    IGunOperator.fromLivingEntity(p).draw(p::getMainHandItem);
                }).join();phase=2;ticks=0;
            } else if(phase==2 && ++ticks>60) {
                if(!mc.player.isNoGravity())throw new AssertionError("zero gravity not active");
                try (var input=GunModelJitterQa.class.getResourceAsStream("/qa/gun-sway-legacy.json")) {
                    golden=JsonParser.parseReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("samples");
                }
                suite(false);suite(true);
                Files.writeString(Path.of("../model-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("baselineCommit","e669ea8","maxQuaternionComponentError",maxError,"summaries",summaries,"samples",rows)));
                Files.writeString(Path.of("../client-report.txt"),"PASS gun model regression: 2 real renderer paths, "+rows.size()+" samples match pre-regression baseline; max quaternion component error="+maxError+"; camera and aim unchanged; hidden=true focused=false\n");
                phase=99;mc.stop();
            }
        }catch(Throwable t){com.mojang.logging.LogUtils.getLogger().error("GUN_MODEL_QA_FAILED",t);try{Files.writeString(Path.of("../client-report.txt"),"FAIL "+t);}catch(Exception ignored){}phase=99;mc.stop();}
    }
    private void suite(boolean basePath) throws Throwable {
        var mc=Minecraft.getInstance();var p=mc.player;
        String path=basePath?"renderFirstPerson":"lambda$renderFirstPerson$5";int sample=0;
        var renderer=net.minecraftforge.client.extensions.common.IClientItemExtensions.of(p.getMainHandItem().getItem()).getCustomRenderer();
        var render=renderer.getClass().getMethod("renderFirstPerson",net.minecraft.client.player.LocalPlayer.class,ItemStack.class,ItemDisplayContext.class,PoseStack.class,net.minecraft.client.renderer.MultiBufferSource.class,int.class,float.class);
        var base=Class.forName("com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer");
        var type=MethodType.methodType(void.class,net.minecraft.client.player.LocalPlayer.class,ItemStack.class,
                ItemDisplayContext.class,PoseStack.class,net.minecraft.client.renderer.MultiBufferSource.class,int.class,float.class);
        var baseRender=MethodHandles.privateLookupIn(base,MethodHandles.lookup()).findSpecial(base,"renderFirstPerson",type,base).bindTo(renderer);
        var bob=net.minecraft.client.player.LocalPlayer.class.getDeclaredMethod("serverAiStep");bob.setAccessible(true);
        float[][] cases={{178,0,0,4,0},{30,88,0,0,4},{55,40,90,4,2},{55,40,90,0,0},{178,0,0,4,0}};
        String[] names={"yaw-wrap","pitch-pole","rolled-mixed","stationary-rolled","ordinary-yaw"};
        for(int c=0;c<cases.length;c++) {
            var v=cases[c];boolean floating=c!=4;p.setNoGravity(floating);GlobalVariables.B_LowGravity=floating;
            GlobalVariables.B_FreeCameraActive=false;GlobalVariables.B_CollisionFreeLookActive=false;
            p.setYRot(v[0]);p.setXRot(v[1]);p.yRotO=v[0];p.xRotO=v[1];p.xBobO=p.xBob=v[1];p.yBobO=p.yBob=v[0];p.setDeltaMovement(Vec3.ZERO);
            GlobalVariables.currentQuat.rotationYXZ((float)Math.toRadians(-v[0]),(float)Math.toRadians(v[1]),(float)Math.toRadians(v[2]));
            GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
            HandSway.reset(p);
            Quaternionf previousGun=null,previousCamera=null;double maxGun=0,maxCamera=0,maxOffset=0;
            for(int step=0;step<9;step++) {
                p.xRotO=p.getXRot();p.yRotO=p.getYRot();
                HandSway.tick(p);
                bob.invoke(p);GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);p.turn(v[3],v[4]);
                for(float partial:new float[]{0,.25f,.5f,.75f,1}) {
                    matrix=null;capturedPath=null;pose=new PoseStack();probing=true;
                    float aimPitch=p.getXRot(),aimYaw=p.getYRot();var aimQuat=new Quaternionf(GlobalVariables.currentQuat);
                    try {
                        if(basePath)baseRender.invoke(p,p.getMainHandItem(),ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,pose,null,15728880,partial);
                        else render.invoke(renderer,p,p.getMainHandItem(),ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,pose,null,15728880,partial);
                    }finally{probing=false;}
                    if(matrix==null || !path.equals(capturedPath))throw new AssertionError("actual TaCZ sway path not captured: "+path+" / "+capturedPath);
                    if(p.getXRot()!=aimPitch || p.getYRot()!=aimYaw || !aimQuat.equals(GlobalVariables.currentQuat))
                        throw new AssertionError("render changed logical aim");
                    var gun=matrix.getNormalizedRotation(new Quaternionf());
                    var expected=golden.get(sample++).getAsJsonObject();
                    if(!names[c].equals(expected.get("case").getAsString()) || step!=expected.get("step").getAsInt()
                            || partial!=expected.get("partial").getAsFloat())throw new AssertionError("baseline sample mismatch");
                    var q=expected.getAsJsonArray("gun");float[] actual={gun.x,gun.y,gun.z,gun.w};
                    for(int axis=0;axis<4;axis++)maxError=Math.max(maxError,Math.abs(actual[axis]-q.get(axis).getAsFloat()));
                    if(maxError>1e-6)throw new AssertionError("gun differs from legacy baseline: "+names[c]+" / "+step+" / "+partial+" / "+maxError);
                    var cameraEvent=new ViewportEvent.ComputeCameraAngles(mc.gameRenderer,mc.gameRenderer.getMainCamera(),partial,p.getViewYRot(partial),p.getViewXRot(partial),0);
                    ClientEvents.onComputeCameraAngles(cameraEvent);
                    var camera=cn.rbq108.nextboundarycornerstone.camera.CameraRotation.rotation(cameraEvent.getYaw(),cameraEvent.getPitch(),cameraEvent.getRoll());
                    if(previousGun!=null){maxGun=Math.max(maxGun,angle(previousGun,gun));maxCamera=Math.max(maxCamera,angle(previousCamera,camera));}
                    maxOffset=Math.max(maxOffset,angle(new Quaternionf(),gun));previousGun=gun;previousCamera=camera;
                    rows.add(Map.of("case",names[c],"path",path,"step",step,"partial",partial,"gun",List.of(gun.x,gun.y,gun.z,gun.w)));
                }
            }
            double[] cameraSteps={0.15000925977013652,0.15001171437147273,0.16771052578456508,0.0,0.6000063655304545};
            if(Math.abs(maxCamera-cameraSteps[c])>1e-4)throw new AssertionError("base camera changed: "+names[c]+" / "+maxCamera);
            if(maxGun>1 || (c==3 && maxGun>1e-6))throw new AssertionError("gun jitter: "+names[c]+" / "+maxGun);
            var summary=Map.<String,Object>of("case",names[c],"path",path,"maxGunStepDegrees",maxGun,"maxCameraStepDegrees",maxCamera,"maxGunOffsetDegrees",maxOffset);summaries.add(summary);
            com.mojang.logging.LogUtils.getLogger().info("GUN_MODEL_SUMMARY {}",summary);
        }
        if(sample!=golden.size())throw new AssertionError("missing baseline samples");
    }
}
