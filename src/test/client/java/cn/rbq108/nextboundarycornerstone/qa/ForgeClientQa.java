package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.api.client.PreferencePages;
import cn.rbq108.nextboundarycornerstone.client.preferences.ModSettingsScreen;
import cn.rbq108.nextboundarycornerstone.core.Keybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Opt-in test mod, excluded from the distributable JAR. */
@Mod("cornerstone_forge_qa")
public final class ForgeClientQa {
    private static final boolean PAUSE_ONLY = Boolean.getBoolean("cornerstone.qa.pauseOnly");
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private int phase, ticks, created;
    private long started;
    private java.util.concurrent.CompletableFuture<?> reload;
    private final TitleScreen parent = new TitleScreen();

    public ForgeClientQa() { MinecraftForge.EVENT_BUS.addListener(this::tick); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void click(ModSettingsScreen screen, String label) {
        var button = screen.children().stream().filter(child -> child instanceof Button b && b.getMessage().getString().equals(label))
                .map(Button.class::cast).findFirst().orElseThrow();
        screen.mouseClicked(button.getX()+3, button.getY()+3, 0);
        screen.mouseReleased(button.getX()+3, button.getY()+3, 0);
    }
    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == 99) return;
        var mc = Minecraft.getInstance();
        try {
            if (started == 0) {
                started = System.currentTimeMillis();
                mc.options.guiScale().set(3);
                mc.options.languageCode="zh_cn";
                mc.options.pauseOnLostFocus=false;
                mc.getLanguageManager().setSelected("zh_cn");
                reload = mc.reloadResourcePacks();
            }
            if (System.currentTimeMillis()-started > 150000) throw new AssertionError("client QA timeout");
            if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("LoadingErrorScreen") && mc.getOverlay() == null) {
                throw new AssertionError("Forge loading warnings/errors: " + net.minecraftforge.fml.ModLoader.get().getWarnings()
                        .stream().map(net.minecraftforge.fml.ModLoadingWarning::formatToString).toList());
            }
            if (phase == 0 && ++ticks % 100 == 0) LOG.info("FORGE_CLIENT_QA_WAIT screen={} overlay={} reloadDone={}",
                    mc.screen == null ? "null" : mc.screen.getClass().getSimpleName(),
                    mc.getOverlay() == null ? "null" : mc.getOverlay().getClass().getSimpleName(), reload.isDone());
            if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(parent);
            if (phase == 0 && reload.isDone() && mc.screen instanceof TitleScreen && mc.getOverlay()==null) {
                check(!ModList.get().isLoaded("ldlib2"), "Native settings need no LDLib2");
                check(Arrays.asList(mc.options.keyMappings).contains(Keybinds.B_FREE_CAMERA), "flight keys registered");
                var mod = ModList.get().getModContainerById("next_boundary_cornerstone").orElseThrow();
                var factory = mod.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).orElseThrow();
                mc.setScreen(factory.screenFunction().apply(mc, parent));
                check(mc.screen instanceof ModSettingsScreen, "Forge config factory opens native preferences");
                phase=1; ticks=0;
            } else if (phase==1 && ++ticks>20) {
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../settings-standalone.png"));}
                var screen=(ModSettingsScreen)mc.screen;
                check(screen.isPauseScreen(), "settings pause single player");
                screen.onClose(); check(mc.screen==parent,"Esc returns parent");
                var id=new ResourceLocation("qa", "extension");
                PreferencePages.register(id,Component.literal("扩展验证"),10,()->{
                    created++;
                    var widgets=new ArrayList<net.minecraft.client.gui.components.AbstractWidget>();
                    for(int i=0;i<15;i++)widgets.add(Button.builder(Component.literal("设置项 "+i),b->{}).bounds(0,i*24,260,20).build());
                    return widgets;
                });
                boolean duplicate=false;
                try{PreferencePages.register(id,Component.empty(),0,List::of);}catch(IllegalArgumentException expected){duplicate=true;}
                check(duplicate,"duplicate page IDs rejected");
                var extended=new ModSettingsScreen(parent);mc.setScreen(extended);
                check(created==1,"new window creates widgets");
                int left=(extended.width-320)/2, top=Math.max(4,(extended.height-218)/2);
                var first=extended.children().stream().filter(c->c instanceof Button b && b.getMessage().getString().equals("设置项 0")).map(Button.class::cast).findFirst().orElseThrow();
                int initialY=first.getY();
                check(extended.mouseScrolled(left+30,top+80,-4),"page consumes scroll");
                check(first.getY()<initialY,"scroll moves content");
                click(extended,Component.translatable("ui.next_boundary_cornerstone.settings.ui").getString());
                click(extended,"扩展验证");
                check(created==1,"tab switch retains widgets and edits");
                check(first.getY()<initialY,"page retains independent scroll");
                phase=2;ticks=0;
            } else if(phase==2 && ++ticks>20) {
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../settings-extension.png"));}
                var screen=(ModSettingsScreen)mc.screen;
                click(screen,Component.translatable("gui.done").getString());
                check(mc.screen==parent,"Done returns parent");
                mc.setScreen(new ModSettingsScreen(parent));check(created==2,"reopening makes fresh widgets");
                LOG.info("FORGE_PREFERENCES_QA_PASS");
                mc.setScreen(parent);
                if (PAUSE_ONLY) { phase=8;ticks=0;return; }
                phase=3;ticks=0;
                var rules = new net.minecraft.world.level.GameRules();
                rules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, null);
                mc.createWorldOpenFlows().createFreshLevel("forge-qa-"+started,
                        new net.minecraft.world.level.LevelSettings("Forge QA", net.minecraft.world.level.GameType.CREATIVE, false,
                                net.minecraft.world.Difficulty.PEACEFUL, true, rules, net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                        new net.minecraft.world.level.levelgen.WorldOptions(20261115L, false, false),
                        registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                                .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
            } else if (phase==3 && mc.player!=null && mc.getSingleplayerServer()!=null && mc.screen==null) {
                var id=mc.player.getUUID();
                mc.getSingleplayerServer().execute(()->{
                    var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    player.teleportTo(0,100,0);
                    player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                            new net.minecraft.world.item.ItemStack(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get()));
                });
                phase=4;ticks=0;
            } else if (phase==4 && ++ticks>60 && mc.player!=null && mc.screen==null) {
                check(mc.player.isNoGravity(),"backpack activates zero gravity on client");
                check(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_LowGravity,"flight tick activated");
                var pose=cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(mc.player).getHitboxPose();
                check(pose!=null,"client samples physical skeleton");
                check(mc.player.getBoundingBox() instanceof cn.rbq108.nextboundarycornerstone.body.collision.PhysicsBox,"client uses compound movement box");
                cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat.rotationXYZ(.4f,.7f,1.1f);
                cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.prevQuat.set(
                        cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat);
                var forward = new org.joml.Vector3f(0,0,1).rotate(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat);
                mc.player.setYRot((float)Math.toDegrees(-Math.atan2(forward.x,forward.z)));
                mc.player.setXRot((float)Math.toDegrees(-Math.asin(forward.y)));
                mc.player.yRotO=mc.player.getYRot();mc.player.xRotO=mc.player.getXRot();
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                mc.getEntityRenderDispatcher().setRenderHitBoxes(true);
                phase=5;ticks=0;
            } else if (phase==5 && ++ticks>30) {
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../flight-third-person.png"));}
                var point=mc.player.getEyePosition().add(mc.player.getViewVector(1).scale(3));
                mc.level.addParticle(cn.rbq108.nextboundarycornerstone.api.ExplosionEffects.highExplosive(),true,
                        point.x,point.y,point.z,0,0,0);
                phase=6;ticks=0;
            } else if (phase==6 && ++ticks>3) {
                check(Integer.parseInt(mc.particleEngine.countParticles())>0,"native blast preset emits textured particles");
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../flight-explosion.png"));}
                phase=8;ticks=0;
            } else if (phase==8) {
                var pause=new net.minecraft.client.gui.screens.PauseScreen(true);mc.setScreen(pause);
                for (int[] size : new int[][] {{320,240}, {480,270}, {640,360}}) {
                    var sized = new net.minecraft.client.gui.screens.PauseScreen(true);
                    sized.init(mc, size[0], size[1]);
                    var buttons = sized.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
                    for (var button : buttons) check(button.getX() >= 0 && button.getX() + button.getWidth() <= size[0]
                            && button.getY() >= 50 && button.getY() + button.getHeight() <= size[1] - 16,
                            "pause menu retains title/bottom margins at " + size[0] + "x" + size[1]);
                    for (var a : buttons) for (var b : buttons) if (a != b) check(
                            a.getX() + a.getWidth() <= b.getX() || b.getX() + b.getWidth() <= a.getX()
                            || a.getY() + a.getHeight() <= b.getY() || b.getY() + b.getHeight() <= a.getY(), "pause buttons never overlap");
                }
                var overlay = new net.minecraft.client.gui.screens.PauseScreen(false);
                overlay.init(mc, pause.width, pause.height);
                check(overlay.children().stream().noneMatch(Button.class::isInstance), "minimal pause overlay has no extra settings row");
                phase=7;ticks=0;
            } else if (phase==7 && ++ticks>10) {
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../pause-menu.png"));}
                var pause=(net.minecraft.client.gui.screens.PauseScreen)mc.screen;
                String label=Component.translatable("ui.next_boundary_cornerstone.settings.button").getString();
                var entries=pause.children().stream().filter(c->c instanceof Button b && b.getMessage().getString().equals(label)).map(Button.class::cast).toList();
                check(entries.size()==1,"one native preferences entry in pause menu");
                var button=entries.get(0);pause.mouseClicked(button.getX()+3,button.getY()+3,0);
                check(mc.screen instanceof ModSettingsScreen,"pause entry opens native settings");
                ((ModSettingsScreen)mc.screen).onClose();check(mc.screen==pause,"settings return to pause menu");
                Files.writeString(Path.of("../client-report.txt"), PAUSE_ONLY
                        ? "PASS native settings, Forge factory, keys, pages, scrolling, tab state, Done/Esc, pause layout at 320x240/480x270/640x360, minimal pause overlay, pause entry\n"
                        : "PASS native settings, Forge factory, keys, pages, scrolling, tab state, Done/Esc, integrated login, zero gravity, physical skeleton, third-person armor/debug rendering, native blast particles, pause entry\n");
                LOG.info("FORGE_CLIENT_QA_PASS");phase=99;mc.stop();
            }
        } catch(Throwable failure) {
            LOG.error("FORGE_CLIENT_QA_FAILED",failure);
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../client-failure.png"));}catch(Exception ignored){}
            try{Files.writeString(Path.of("../client-report.txt"),"FAIL "+failure);}catch(Exception ignored){}
            phase=99;mc.stop();
        }
    }
}
