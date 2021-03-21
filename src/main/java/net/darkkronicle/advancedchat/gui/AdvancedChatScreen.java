package net.darkkronicle.advancedchat.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import net.darkkronicle.advancedchat.AdvancedChat;
import net.darkkronicle.advancedchat.storage.ConfigStorage;
import net.darkkronicle.advancedchat.config.GuiConfig;
import net.darkkronicle.advancedchat.gui.elements.CleanButton;
import net.darkkronicle.advancedchat.gui.tabs.AbstractChatTab;
import net.darkkronicle.advancedchat.gui.tabs.CustomChatTab;
import net.darkkronicle.advancedchat.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CommandSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;

public class AdvancedChatScreen extends Screen {
    private String field_2389 = "";
    private int messageHistorySize = -1;
    protected TextFieldWidget chatField;
    private String originalChatText = "";
    private CommandSuggestor commandSuggestor;

    public AdvancedChatScreen(String originalChatText) {
        super(NarratorManager.EMPTY);
        this.originalChatText = originalChatText;
        if (this.originalChatText.isEmpty()) {
            AbstractChatTab tab = AdvancedChat.getAdvancedChatHud().getCurrentTab();
            if (tab instanceof CustomChatTab) {
                CustomChatTab customTab = (CustomChatTab) tab;
                this.originalChatText = customTab.getStartingMessage();
            }
        }
    }

    protected void init() {
        this.client.keyboard.setRepeatEvents(true);
        this.messageHistorySize = this.client.inGameHud.getChatHud().getMessageHistory().size();
        this.chatField = new TextFieldWidget(this.textRenderer, 4, this.height - 12, this.width - 4, 12, new TranslatableText("chat.editBox")) {
            protected MutableText getNarrationMessage() {
                return super.getNarrationMessage().append(AdvancedChatScreen.this.commandSuggestor.getNarration());
            }
        };
        this.chatField.setMaxLength(256);
        this.chatField.setDrawsBackground(false);
        this.chatField.setText(this.originalChatText);
        this.chatField.setChangedListener(this::onChatFieldUpdate);

        AdvancedChatHud hud = AdvancedChat.getAdvancedChatHud();

        int width = ConfigStorage.ChatScreen.WIDTH.config.getIntegerValue() + 4;
        if (ConfigStorage.General.CHAT_HEADS.config.getBooleanValue()) {
            width += 10;
        }
        int height = 11;
        int bottomOffset = ConfigStorage.ChatScreen.Y.config.getIntegerValue() + ConfigStorage.ChatScreen.HEIGHT.config.getIntegerValue() + 5 + height;
        int y = client.getWindow().getScaledHeight() - bottomOffset;
        ColorUtil.SimpleColor baseColor = ConfigStorage.ChatScreen.HUD_BACKGROUND_COLOR.config.getSimpleColor();
        CleanButton tabButton = new CleanButton(ConfigStorage.ChatScreen.X.config.getIntegerValue(), y, width, height, baseColor, new LiteralText(hud.getCurrentTab().getName()), button -> {
            hud.cycleTab();
            button.setText(new LiteralText(hud.getCurrentTab().getName()));
        });

        CleanButton chatLogButton = new CleanButton(client.getWindow().getScaledWidth() - 60, client.getWindow().getScaledHeight() - 27, 50, 11, baseColor, new LiteralText("Chat Log"), button -> client.openScreen(new ChatLogScreen()));
        CleanButton settingsButton = new CleanButton(client.getWindow().getScaledWidth() - 114, client.getWindow().getScaledHeight() - 27, 50, 11, baseColor, new LiteralText("Settings"), button -> GuiBase.openGui(new GuiConfig()));

        this.addButton(chatLogButton);
        this.addButton(settingsButton);
        this.addButton(tabButton);
        this.children.add(this.chatField);

        if (ConfigStorage.ChatScreen.SHOW_TABS.config.getBooleanValue()) {
            int xadd = width + 2;
            int yadd = client.getWindow().getScaledHeight() - 11 - ConfigStorage.ChatScreen.Y.config.getIntegerValue();
            int orgYadd = yadd;
            int tabchar = ConfigStorage.ChatScreen.TAB_SIDE_CHARS.config.getIntegerValue();
            int bwidth = tabchar * 10 + 2;
            int relativeHeight = 13;
            for (AbstractChatTab tab : AdvancedChat.chatTab.getAllChatTabs()) {
                if (relativeHeight >= ConfigStorage.ChatScreen.HEIGHT.config.getIntegerValue()) {
                    xadd = xadd + bwidth + 2;
                    yadd = orgYadd;
                    relativeHeight = 13;
                }
                String abrev;
                if (tab.getAbreviation().equals("") || tab.getAbreviation() == null) {
                    abrev = tab.getName();
                } else {
                    abrev = tab.getAbreviation();
                }
                if (abrev.length() >= tabchar) {
                    abrev = abrev.substring(0, tabchar);
                }
                CleanButton buttonTab = new CleanButton(xadd, yadd, bwidth, 11, baseColor, new LiteralText(abrev), button -> {
                    hud.setCurrentTab(tab);
                    tabButton.setText(new LiteralText(tab.getName()));
                });
                yadd = yadd - 13;
                addButton(buttonTab);
                relativeHeight = relativeHeight + 13;
            }
        }


        this.commandSuggestor = new CommandSuggestor(this.client, this, this.chatField, this.textRenderer, false, false, 1, 10, true, -805306368);
        this.commandSuggestor.refresh();
        this.setInitialFocus(this.chatField);

    }

    public void resize(MinecraftClient client, int width, int height) {
        String string = this.chatField.getText();
        this.init(client, width, height);
        this.setText(string);
        this.commandSuggestor.refresh();
    }

    public void removed() {
        this.client.keyboard.setRepeatEvents(false);
        AdvancedChat.getAdvancedChatHud().resetScroll();
    }

    public void tick() {
        this.chatField.tick();
    }

    private void onChatFieldUpdate(String chatText) {
        String string = this.chatField.getText();
        this.commandSuggestor.setWindowActive(!string.equals(this.originalChatText));
        this.commandSuggestor.refresh();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandSuggestor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode == 256) {
            this.client.openScreen(null);
            return true;
        } else if (keyCode != 257 && keyCode != 335) {
            if (keyCode == 265) {
                this.setChatFromHistory(-1);
                return true;
            } else if (keyCode == 264) {
                this.setChatFromHistory(1);
                return true;
            } else if (keyCode == 266) {
                AdvancedChat.getAdvancedChatHud().scroll((double)(this.client.inGameHud.getChatHud().getVisibleLineCount() - 1));
                return true;
            } else if (keyCode == 267) {
                AdvancedChat.getAdvancedChatHud().scroll((double)(-this.client.inGameHud.getChatHud().getVisibleLineCount() + 1));
                return true;
            } else {
                return false;
            }
        } else {
            String string = this.chatField.getText().trim();
            if (!string.isEmpty()) {
                this.sendMessage(string);
            }

            this.client.openScreen((Screen)null);
            return true;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount > 1.0D) {
            amount = 1.0D;
        }

        if (amount < -1.0D) {
            amount = -1.0D;
        }

        if (this.commandSuggestor.mouseScrolled(amount)) {
            return true;
        } else {
            if (!hasShiftDown()) {
                amount *= 7.0D;
            }

            AdvancedChat.getAdvancedChatHud().scroll(amount);
            return true;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.commandSuggestor.mouseClicked((double)((int)mouseX), (double)((int)mouseY), button)) {
            return true;
        } else {
            if (button == 0) {
                AdvancedChatHud chatHud = AdvancedChat.getAdvancedChatHud();

                Style style = chatHud.getText(mouseX, mouseY);
                if (style != null && this.handleTextClick(style)) {
                    return true;
                }
            }
            return this.chatField.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        }
    }

    protected void insertText(String text, boolean override) {
        if (override) {
            this.chatField.setText(text);
        } else {
            this.chatField.write(text);
        }

    }

    public void setChatFromHistory(int i) {
        int j = this.messageHistorySize + i;
        int k = this.client.inGameHud.getChatHud().getMessageHistory().size();
        j = MathHelper.clamp(j, 0, k);
        if (j != this.messageHistorySize) {
            if (j == k) {
                this.messageHistorySize = k;
                this.chatField.setText(this.field_2389);
            } else {
                if (this.messageHistorySize == k) {
                    this.field_2389 = this.chatField.getText();
                }

                this.chatField.setText((String)this.client.inGameHud.getChatHud().getMessageHistory().get(j));
                this.commandSuggestor.setWindowActive(false);
                this.messageHistorySize = j;
            }
        }
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        AdvancedChatHud hud = AdvancedChat.getAdvancedChatHud();
        this.setFocused(this.chatField);
        this.chatField.setTextFieldFocused(true);
        fill(matrices, 2, this.height - 14, this.width - 2, this.height - 2, ConfigStorage.ChatScreen.HUD_BACKGROUND_COLOR.config.getSimpleColor().color());
        this.chatField.render(matrices, mouseX, mouseY, delta);
        this.commandSuggestor.render(matrices, mouseX, mouseY);
        Style style = hud.getText(mouseX, mouseY);
        if (style != null && style.getHoverEvent() != null) {
            this.renderTextHoverEffect(matrices, style, mouseX, mouseY);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }


    public boolean isPauseScreen() {
        return false;
    }


    private void setText(String text) {
        this.chatField.setText(text);
    }

}
