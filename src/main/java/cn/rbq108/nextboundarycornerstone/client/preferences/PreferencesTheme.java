package cn.rbq108.nextboundarycornerstone.client.preferences;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.texture.*;
import net.minecraft.network.chat.Component;

/** Shared preferences styling. Never changes a global stylesheet or font setting. */
public final class PreferencesTheme {
    public static final int INK=0xFF29393E, CYAN=0xFF4CAEB3;
    public static final int SCROLL_TRACK=0xFFCBD6D2, SCROLL_THUMB=0xFF819B9C, SCROLL_PRESSED=0xFF568E91;
    private PreferencesTheme() {}
    public static Stylesheet stylesheet() {
        if(com.lowdragmc.lowdraglib2.LDLib2.isRemote()) {
            var sheet=com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager.INSTANCE.getStylesheet(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("next_boundary_cornerstone","lss/preferences.lss"));
            if(sheet!=null) return sheet;
        }
        try (var in=PreferencesTheme.class.getResourceAsStream("/assets/next_boundary_cornerstone/lss/preferences.lss")) {
            return Stylesheet.parse(new String(java.util.Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) { throw new IllegalStateException("Preferences stylesheet",e); }
    }
    public static <T extends UIElement> T at(T e,int x,int y,int w,int h) {
        e.layout(l->l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE));
        e.layout(l->l.left(x).top(y).width(w).height(h)); return e;
    }
    public static Label label(Component text,int x,int y,int w) {
        Label label=new Label(); label.setText(text); label.textStyle(s->s.font(net.minecraft.resources.ResourceLocation.withDefaultNamespace("default")).textColor(INK).textShadow(false).adaptiveWidth(false).textWrap(com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.HIDE)); return at(label,x,y,w,12);
    }
    public static Button button(Component text,int x,int y,int w) {
        Button b=new Button().setText(text); b.addClass("preference-button");
        b.text.layout(l->l.widthPercent(100).heightPercent(100).marginHorizontal(0));
        b.textStyle(s->s.font(net.minecraft.resources.ResourceLocation.withDefaultNamespace("default")).textColor(INK).textShadow(false).adaptiveWidth(false).textWrap(com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.HIDE)); return at(b,x,y,w,16);
    }
    /** Thin visual chrome with a wider hit area. Keeps LDLib2's wheel, track click and drag handling. */
    public static ScrollerView scrollView(ScrollerView view) {
        view.verticalScroller.layout(l->l.width(6).marginLeft(2).gapRow(0));
        view.horizontalScroller.layout(l->l.height(6).marginTop(2).gapColumn(0));
        scrollBar(view.verticalScroller,true);
        scrollBar(view.horizontalScroller,false);
        return view;
    }
    private static void scrollBar(Scroller scroller,boolean vertical) {
        scroller.headButton.setDisplay(false);
        scroller.tailButton.setDisplay(false);
        float trackX=vertical?1f/3:1, trackY=vertical?1:1f/3;
        float thumbX=vertical?.5f:1, thumbY=vertical?1:.5f;
        scroller.scrollContainer.style(s->s.backgroundTexture(new ColorRectTexture(SCROLL_TRACK).scale(trackX,trackY)));
        scroller.scrollBar.buttonStyle(s->s
            .baseTexture(new ColorRectTexture(SCROLL_THUMB).scale(thumbX,thumbY))
            .hoverTexture(new ColorRectTexture(CYAN).scale(thumbX,thumbY))
            .pressedTexture(new ColorRectTexture(SCROLL_PRESSED).scale(thumbX,thumbY)));
    }
}
