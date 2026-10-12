package dev.rehan.passthrough.client;

import com.google.gson.JsonObject;
import dev.rehan.passthrough.Passthrough;
import dev.rehan.passthrough.client.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.sdl.SDLVideo;

/** Host input, applied on the client thread: the host window has the focus, so Minecraft never sees these itself. */
final class ClientInput {
	private ClientInput() {
	}

	static void handle(final Minecraft minecraft, final JsonObject m) {
		LocalPlayer player = minecraft.player;
		switch (m.get("t").getAsString()) {
			case "key" -> {
				String k = m.get("k").getAsString();
				boolean down = !m.has("down") || m.get("down").getAsBoolean();
				if (k.equals("escape")) {
					if (down && minecraft.gui.screen() != null) {
						minecraft.gui.screen().onClose();
					}

					return;
				}

				KeyMapping key = switch (k) {
					case "use" -> minecraft.options.keyUse;
					case "attack" -> minecraft.options.keyAttack;
					case "pick" -> minecraft.options.keyPickItem;
					case "inventory" -> minecraft.options.keyInventory;
					case "drop" -> minecraft.options.keyDrop;
					case "swap" -> minecraft.options.keySwapOffhand;
					default -> null;
				};
				if (k.equals("attack") && down && player != null
					&& BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getPath().endsWith("_sword")) {
					// a sword swing: the host hits what's in front of Steve in its own world
					Passthrough.events.accept("{\"t\":\"melee\"}");
				}

				if (key != null) {
					if (down && !key.isDown()) {
						KeyMappingAccessor access = (KeyMappingAccessor)key;
						access.passthrough$setClickCount(access.passthrough$getClickCount() + 1);
					}

					key.setDown(down);
				}
			}
			case "slot" -> {
				if (player != null) {
					player.getInventory().setSelectedSlot(Math.clamp(m.get("n").getAsInt(), 0, Inventory.getSelectionSize() - 1));
				}
			}
			case "scroll" -> {
				if (player != null) {
					Inventory inventory = player.getInventory();
					int size = Inventory.getSelectionSize();
					inventory.setSelectedSlot(Math.floorMod(inventory.getSelectedSlot() - m.get("d").getAsInt(), size));
				}
			}
			case "hud" -> {
				if (minecraft.gui.hud.isHidden() != m.get("hidden").getAsBoolean()) {
					minecraft.gui.hud.toggle();
				}
			}
			case "view" -> {
				// match the host's picture exactly: un-minimize/un-maximize first (resizing a maximized window is ignored)
				int w = m.get("w").getAsInt(), h = m.get("h").getAsInt();
				long handle = minecraft.getWindow().handle();
				SDLVideo.SDL_RestoreWindow(handle);
				minecraft.getWindow().setWindowed(w, h);
				SDLVideo.SDL_SetWindowSize(handle, w, h);
				SDLVideo.SDL_SyncWindow(handle);
			}
			case "tpsnap" -> {
				PlayerSync.aimSnapEnabled = !PlayerSync.aimSnapEnabled;
				if (minecraft.player != null) {
					minecraft.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Third-person controls: " + (PlayerSync.aimSnapEnabled ? "ON" : "OFF")));
				}
			}
			case "chat_open" -> {
				minecraft.gui.setScreen(new net.minecraft.client.gui.screens.ChatScreen("", false));
			}
			case "chat_char" -> {
				if (minecraft.gui.screen() != null && m.has("c")) {
					int codepoint = m.get("c").getAsInt();
					minecraft.gui.screen().charTyped(new net.minecraft.client.input.CharacterEvent(codepoint));
				}
			}
			case "chat_key" -> {
				if (minecraft.gui.screen() != null && m.has("k")) {
					String k = m.get("k").getAsString();
					switch (k) {
						case "enter" -> minecraft.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(257, 0, 0));
						case "escape" -> {
							minecraft.gui.screen().onClose();
							minecraft.gui.setScreen(null);
						}
						case "backspace" -> minecraft.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(259, 0, 0));
						case "left" -> minecraft.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(263, 0, 0));
						case "right" -> minecraft.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(262, 0, 0));
						case "up" -> minecraft.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(265, 0, 0));
						case "down" -> minecraft.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(264, 0, 0));
						default -> {
						}
					}
				}
			}
			case "inv_open" -> {
				if (minecraft.player != null) {
					minecraft.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(minecraft.player));
				}
			}
			case "inv_close" -> {
				if (minecraft.gui.screen() != null) {
					minecraft.gui.screen().onClose();
					minecraft.gui.setScreen(null);
				}
			}
			case "inv_move" -> {
				if (m.has("x") && m.has("y")) {
					double normX = m.get("x").getAsDouble();
					double normY = m.get("y").getAsDouble();
					double screenW = minecraft.getWindow().getScreenWidth();
					double screenH = minecraft.getWindow().getScreenHeight();
					((dev.rehan.passthrough.client.mixin.MouseHandlerAccessor) minecraft.mouseHandler).passthrough$setXpos(normX * screenW);
					((dev.rehan.passthrough.client.mixin.MouseHandlerAccessor) minecraft.mouseHandler).passthrough$setYpos(normY * screenH);
					if (minecraft.gui.screen() != null) {
						double guiX = normX * minecraft.getWindow().getGuiScaledWidth();
						double guiY = normY * minecraft.getWindow().getGuiScaledHeight();
						minecraft.gui.screen().mouseMoved(guiX, guiY);
					}
				}
			}
			case "inv_click" -> {
				if (m.has("x") && m.has("y")) {
					double normX = m.get("x").getAsDouble();
					double normY = m.get("y").getAsDouble();
					int btn = m.has("btn") ? m.get("btn").getAsInt() : 0;
					double screenW = minecraft.getWindow().getScreenWidth();
					double screenH = minecraft.getWindow().getScreenHeight();
					double guiX = normX * minecraft.getWindow().getGuiScaledWidth();
					double guiY = normY * minecraft.getWindow().getGuiScaledHeight();
					((dev.rehan.passthrough.client.mixin.MouseHandlerAccessor) minecraft.mouseHandler).passthrough$setXpos(normX * screenW);
					((dev.rehan.passthrough.client.mixin.MouseHandlerAccessor) minecraft.mouseHandler).passthrough$setYpos(normY * screenH);
					if (minecraft.gui.screen() != null) {
						net.minecraft.client.input.MouseButtonInfo info = new net.minecraft.client.input.MouseButtonInfo(btn, 0);
						net.minecraft.client.input.MouseButtonEvent event = new net.minecraft.client.input.MouseButtonEvent(guiX, guiY, info);
						minecraft.gui.screen().mouseClicked(event, false);
						minecraft.gui.screen().mouseReleased(event);
					}
				}
			}
			default -> {
			}
		}
	}
}




