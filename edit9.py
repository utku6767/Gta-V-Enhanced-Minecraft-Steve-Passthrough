import sys

with open('source/mc/src/client/java/dev/rehan/passthrough/client/PassthroughClient.java', 'r') as f:
    content = f.read()

replacement = """	public static void toggleSkin(boolean herobrine) {
		Minecraft.getInstance().execute(() -> {
			try {
				String path = herobrine ? "E:\\\\Openclawdx\\\\herobrine skin.png" : "E:\\\\Openclawdx\\\\steve skin.png";
				net.minecraft.client.renderer.texture.DynamicTexture tex = new net.minecraft.client.renderer.texture.DynamicTexture(
					() -> herobrine ? "herobrine" : "steve",
					com.mojang.blaze3d.platform.NativeImage.read(new java.io.FileInputStream(path))
				);
				net.minecraft.world.entity.player.PlayerSkin base = net.minecraft.client.resources.DefaultPlayerSkin.get(new java.util.UUID(0L, 15L));
				Object clientAssetTex = base.getClass().getRecordComponents()[0].getAccessor().invoke(base);
				java.lang.reflect.Method m = clientAssetTex.getClass().getMethod("texturePath");
				net.minecraft.resources.Identifier id = (net.minecraft.resources.Identifier) m.invoke(clientAssetTex);
				Minecraft.getInstance().getTextureManager().register(id, tex);
			} catch (Exception e) {
				dev.rehan.passthrough.Passthrough.LOG.error("Failed to load skin", e);
			}
		});
	}"""

import re
content = re.sub(r'public static void toggleSkin\(boolean herobrine\) \{.*?\n\t\}', replacement.replace('\\', '\\\\'), content, flags=re.DOTALL)

with open('source/mc/src/client/java/dev/rehan/passthrough/client/PassthroughClient.java', 'w') as f:
    f.write(content)
