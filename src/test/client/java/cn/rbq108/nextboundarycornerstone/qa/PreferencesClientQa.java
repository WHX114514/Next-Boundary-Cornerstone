package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.api.client.PreferencePages;
import cn.rbq108.nextboundarycornerstone.client.preferences.ModSettingsScreen;
import cn.rbq108.nextboundarycornerstone.client.preferences.PreferencesTheme;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Standalone host smoke check: no industry classes or resources in this run. */
@Mod(value = "cornerstone_surface_qa", dist = Dist.CLIENT)
public final class PreferencesClientQa {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static int phase, ticks, created;
    private static long started;
    private static CompletableFuture<?> reload;
    private static final Path OUTPUT = Path.of("../standalone-report.txt");

    public PreferencesClientQa() {
        if (Boolean.getBoolean("cornerstone.preferences.qa")) NeoForge.EVENT_BUS.addListener(PreferencesClientQa::tick);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void tick(ClientTickEvent.Post event) {
        if (phase == 99) return;
        var mc = Minecraft.getInstance();
        try {
            if (started == 0) {
                started = System.currentTimeMillis();
                mc.options.guiScale().set(3);
                mc.options.languageCode = "zh_cn";
                mc.options.pauseOnLostFocus = false;
                mc.getLanguageManager().setSelected("zh_cn");
                reload = mc.reloadResourcePacks();
            }
            if (System.currentTimeMillis() - started > 120_000) throw new AssertionError("Standalone preferences timed out");
            if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
            if (phase == 0 && reload.isDone() && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                require(!ModList.get().isLoaded("next_boundary_industry"), "Industry must be absent");
                var container = ModList.get().getModContainerById("next_boundary_cornerstone").orElseThrow();
                mc.setScreen(container.getCustomExtension(IConfigScreenFactory.class).orElseThrow().createScreen(container, mc.screen));
                phase = 1;
            } else if (phase == 1 && ++ticks > 25) {
                var screen = (ModSettingsScreen) Objects.requireNonNull(mc.screen, "Standalone preferences screen");
                require(screen.modularUI.ui.select("#next_boundary_cornerstone-ui").findFirst().orElseThrow().isDisplayed(), "Base UI page works alone");
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(Path.of("../standalone-zh-gui3.png")); }
                var id = ResourceLocation.fromNamespaceAndPath("qa_addon", "custom");
                PreferencePages.register(id, Component.literal("扩展验证"), 10, () -> {
                    created++;
                    return PreferencesTheme.label(Component.literal("附属模组页面"), 0, 3, 274);
                });
                boolean rejected = false;
                try { PreferencePages.register(id, Component.empty(), 10, UIElement::new); }
                catch (IllegalArgumentException expected) { rejected = true; }
                require(rejected, "Duplicate page IDs must fail");
                mc.setScreen(new ModSettingsScreen(new TitleScreen()));
                require(created == 1, "Host invokes registered factory");
                mc.setScreen(new ModSettingsScreen(new TitleScreen()));
                require(created == 2, "Reopen creates fresh page content");
                phase = 2; ticks = 0;
            } else if (phase == 2 && ++ticks > 25) {
                var screen = (ModSettingsScreen) Objects.requireNonNull(mc.screen, "Extension preferences screen");
                require(screen.modularUI.ui.select("#qa_addon-custom").findFirst().orElseThrow().isDisplayed(), "Registered extension is selected by order");
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(Path.of("../extension-zh-gui3.png")); }
                Files.writeString(OUTPUT, "PASS zh_cn / GUI 3 / standalone host, extension registration, duplicate ID, fresh content\n");
                LOG.info("PREFERENCES_STANDALONE_QA_PASS");
                phase = 99; mc.stop();
            }
        } catch (Throwable error) {
            LOG.error("PREFERENCES_STANDALONE_QA_FAILED", error);
            try { Files.writeString(OUTPUT, "FAIL " + error); } catch (java.io.IOException ignored) {}
            phase = 99; mc.stop();
        }
    }
}
