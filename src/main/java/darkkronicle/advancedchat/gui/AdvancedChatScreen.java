/* AdvancedChat: A Minecraft Mod to modify the chat.
Copyright (C) 2020 DarkKronicle

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.*/

package darkkronicle.advancedchat.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CommandSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class AdvancedChatScreen extends Screen {

    private String messHist = "";
    private int messageHistorySize = -1;
    protected TextFieldWidget chatField;
    private String originalChatText = "";
    private CommandSuggestor commandSuggestor;

    public AdvancedChatScreen(String originalChatText) {
        super(NarratorManager.EMPTY);
        this.originalChatText = originalChatText;

    }

    protected void init() {
        this.minecraft.keyboard.enableRepeatEvents(true);
        this.messageHistorySize = this.minecraft.inGameHud.getChatHud().getMessageHistory().size();
        this.chatField = new TextFieldWidget(this.font, 4, this.height - 12, this.width - 4, 12, I18n.translate("chat.editBox")) {
            protected String getNarrationMessage() {
                return super.getNarrationMessage() + AdvancedChatScreen.this.commandSuggestor.method_23958();
            }
        };
        this.chatField.setMaxLength(256);
        this.chatField.setHasBorder(false);
        this.chatField.setText(this.originalChatText);
        this.chatField.setChangedListener(this::onChatFieldUpdate);
        this.children.add(this.chatField);

        addButton(new ButtonWidget(minecraft.getWindow().getScaledWidth() - 60, 10, 50, 20, "Chat Log", button -> {
            minecraft.openScreen(new ChatLogScreen());
        }));

        this.commandSuggestor = new CommandSuggestor(this.minecraft, this, this.chatField, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestor.refresh();
        this.setInitialFocus(this.chatField);

        if (minecraft.player.isSleeping()) {
            // Prevents softlocks from sleeping.
            this.addButton(new ButtonWidget(this.width / 2 - 100, this.height - 40, 200, 20, I18n.translate("multiplayer.stopSleeping"), (buttonWidget) -> {
                this.stopSleeping();
            }));
        }
    }

    public void resize(MinecraftClient client, int width, int height) {
        String string = this.chatField.getText();
        this.init(client, width, height);
        this.setText(string);
        this.commandSuggestor.refresh();
    }

    public void removed() {
        this.minecraft.keyboard.enableRepeatEvents(false);
        this.minecraft.inGameHud.getChatHud().resetScroll();
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
            if (!minecraft.player.isSleeping()) {
                this.minecraft.openScreen((Screen) null);
            } else {
                this.stopSleeping();
            }
            return true;
        } else if (keyCode != 257 && keyCode != 335) {
            if (keyCode == 265) {
                this.setChatFromHistory(-1);
                return true;
            } else if (keyCode == 264) {
                this.setChatFromHistory(1);
                return true;
            } else if (keyCode == 266) {
                this.minecraft.inGameHud.getChatHud().scroll((double) (this.minecraft.inGameHud.getChatHud().getVisibleLineCount() - 1));
                return true;
            } else if (keyCode == 267) {
                this.minecraft.inGameHud.getChatHud().scroll((double) (-this.minecraft.inGameHud.getChatHud().getVisibleLineCount() + 1));
                return true;
            } else {
                return false;
            }
        } else {
            String string = this.chatField.getText().trim();
            if (!string.isEmpty()) {
                this.sendMessage(string);
            }

            this.minecraft.openScreen((Screen) null);
            return true;
        }
    }

    public boolean mouseScrolled(double d, double e, double amount) {
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

            this.minecraft.inGameHud.getChatHud().scroll(amount);
            return true;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.commandSuggestor.mouseClicked((double) ((int) mouseX), (double) ((int) mouseY), button)) {
            return true;
        } else {
            if (button == 0) {
                Text text = this.minecraft.inGameHud.getChatHud().getText(mouseX, mouseY);
                if (text != null && this.handleComponentClicked(text)) {
                    return true;
                }
            }

            return this.chatField.mouseClicked(mouseX, mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
        }
    }

    protected void insertText(String string, boolean bl) {
        if (bl) {
            this.chatField.setText(string);
        } else {
            this.chatField.write(string);
        }

    }

    public void setChatFromHistory(int i) {
        int j = this.messageHistorySize + i;
        int k = this.minecraft.inGameHud.getChatHud().getMessageHistory().size();
        j = MathHelper.clamp(j, 0, k);
        if (j != this.messageHistorySize) {
            if (j == k) {
                this.messageHistorySize = k;
                this.chatField.setText(this.messHist);
            } else {
                if (this.messageHistorySize == k) {
                    this.messHist = this.chatField.getText();
                }

                this.chatField.setText((String) this.minecraft.inGameHud.getChatHud().getMessageHistory().get(j));
                this.commandSuggestor.setWindowActive(false);
                this.messageHistorySize = j;
            }
        }
    }

    public void render(int mouseX, int mouseY, float delta) {
        this.setFocused(this.chatField);
        this.chatField.setSelected(true);
        fill(2, this.height - 14, this.width - 2, this.height - 2, this.minecraft.options.getTextBackgroundColor(Integer.MIN_VALUE));
        this.chatField.render(mouseX, mouseY, delta);
        this.commandSuggestor.render(mouseX, mouseY);
        Text text = this.minecraft.inGameHud.getChatHud().getText((double) mouseX, (double) mouseY);
        if (text != null && text.getStyle().getHoverEvent() != null) {
            this.renderComponentHoverEffect(text, mouseX, mouseY);
        }

        super.render(mouseX, mouseY, delta);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private void setText(String text) {
        this.chatField.setText(text);
    }


    public String getMessHist() {
        return messHist;
    }

    public void setMessHist(String messHist) {
        this.messHist = messHist;
    }

    public int getMessageHistorySize() {
        return messageHistorySize;
    }

    public void setMessageHistorySize(int messageHistorySize) {
        this.messageHistorySize = messageHistorySize;
    }

    public void onClose() {
        if (minecraft.player.isSleeping()) {
            this.stopSleeping();
        }
        minecraft.openScreen(null);

    }

    private void stopSleeping() {
        ClientPlayNetworkHandler clientPlayNetworkHandler = this.minecraft.player.networkHandler;
        clientPlayNetworkHandler.sendPacket(new ClientCommandC2SPacket(this.minecraft.player, ClientCommandC2SPacket.Mode.STOP_SLEEPING));
        this.minecraft.openScreen((Screen) null);
    }
}