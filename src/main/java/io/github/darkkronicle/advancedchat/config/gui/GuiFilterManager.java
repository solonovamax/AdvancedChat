package io.github.darkkronicle.advancedchat.config.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchat.config.ConfigStorage;
import io.github.darkkronicle.advancedchat.config.Filter;
import io.github.darkkronicle.advancedchat.gui.SharingScreen;
import io.github.darkkronicle.advancedchat.config.gui.widgets.WidgetFilterEntry;
import io.github.darkkronicle.advancedchat.config.gui.widgets.WidgetListFilters;

import javax.annotation.Nullable;
import java.util.Collections;

public class GuiFilterManager extends GuiListBase<Filter, WidgetFilterEntry, WidgetListFilters> implements ISelectionListener<Filter> {

    protected GuiFilterManager() {
        super(10, 60);
        this.title = StringUtils.translate("advancedchat.screen.main");
    }

    @Override
    protected WidgetListFilters createListWidget(int listX, int listY) {
        return new WidgetListFilters(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this, null, this);
    }

    @Override
    protected int getBrowserWidth() {
        return this.width - 20;
    }

    @Override
    public void initGui() {
        super.initGui();

        int x = 10;
        int y = 26;

        int rows = 1;

        for (GuiConfig.ConfigGuiTab tab : GuiConfig.ConfigGuiTab.values()) {
            int width = this.getStringWidth(tab.getDisplayName()) + 10;

            if (x >= this.width - width - 10)
            {
                x = 10;
                y += 22;
                rows++;
            }

            x += this.createTabButton(x, y, width, tab);
        }

        this.setListPosition(this.getListX(), 68 + (rows - 1) * 22);
        this.reCreateListWidget();

        this.getListWidget().refreshEntries();

        y += 24;
        x = this.width - 10;
        x -= this.addButton(x, y, ButtonListener.Type.ADD_FILTER) + 2;
        this.addButton(x, y, ButtonListener.Type.IMPORT);
    }

    protected int addButton(int x, int y, ButtonListener.Type type) {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, true, type.getDisplayName());
        this.addButton(button, new ButtonListener(type, this));

        return button.getWidth();
    }

    private int createTabButton(int x, int y, int width, GuiConfig.ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(GuiConfig.tab != tab);
        this.addButton(button, new ButtonListenerTab(tab));

        return button.getWidth() + 2;
    }

    private static class ButtonListener implements IButtonActionListener {

        private final Type type;
        private final GuiFilterManager gui;

        public ButtonListener(Type type, GuiFilterManager gui) {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            if (this.type == Type.ADD_FILTER) {
                ConfigStorage.FILTERS.add(Filter.getRandomFilter());
                Collections.sort(ConfigStorage.FILTERS);
                this.gui.getListWidget().refreshEntries();
            } else if (this.type == Type.IMPORT) {
                GuiBase.openGui(new SharingScreen(null, gui));
            }
        }

        public enum Type {
            ADD_FILTER("addfilter"),
            IMPORT("import")
            ;

            private static String translate(String key) {
                return "advancedchat.gui.button." + key;
            }

            private final String translationKey;

            Type(String translationKey) {
                this.translationKey = translate(translationKey);
            }

            public String getDisplayName() {
                return StringUtils.translate(this.translationKey);
            }
        }
    }

    private static class ButtonListenerTab implements IButtonActionListener {
        private final GuiConfig.ConfigGuiTab tab;

        public ButtonListenerTab(GuiConfig.ConfigGuiTab tab) {
            this.tab = tab;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            GuiConfig.tab = this.tab;
            GuiBase.openGui(new GuiConfig());
        }
    }

    @Override
    protected int getBrowserHeight() {
        return this.height - 6 - this.getListY();
    }

    @Override
    public void onSelectionChange(@Nullable Filter entry) {

    }
}
