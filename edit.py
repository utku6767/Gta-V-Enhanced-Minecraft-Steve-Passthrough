import sys

with open('source/mc/src/client/java/dev/rehan/passthrough/client/PassthroughClient.java', 'r') as f:
    content = f.read()

replacement = """private static int respawnIn;

	public static net.minecraft.world.entity.player.PlayerSkin herobrineSkin;
	public static boolean useHerobrine;

	public static void toggleSkin(boolean herobrine) {
		Minecraft.getInstance().execute(() -> {
			useHerobrine = herobrine;
			if (herobrine && herobrineSkin == null) {
				try {
					net.minecraft.client.renderer.texture.DynamicTexture tex = new net.minecraft.client.renderer.texture.DynamicTexture(
						com.mojang.blaze3d.platform.NativeImage.read(new java.io.FileInputStream("E:\\\\Openclawdx\\\\herobrine skin.png"))
					);
					net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.of("passthrough", "herobrine");
					Minecraft.getInstance().getTextureManager().register(id, tex);
					net.minecraft.world.entity.player.PlayerSkin base = net.minecraft.client.resources.DefaultPlayerSkin.get(new java.util.UUID(0L, 15L));
					Object[] args = new Object[base.getClass().getRecordComponents().length];
					for (int i = 0; i < args.length; i++) {
						args[i] = base.getClass().getRecordComponents()[i].getAccessor().invoke(base);
					}
					args[0] = id;
					herobrineSkin = (net.minecraft.world.entity.player.PlayerSkin) base.getClass().getConstructors()[0].newInstance(args);
				} catch (Exception e) {
					Passthrough.LOG.error("Failed to load skin", e);
				}
			}
		});
	}"""

content = content.replace("private static int respawnIn;", replacement)

with open('source/mc/src/client/java/dev/rehan/passthrough/client/PassthroughClient.java', 'w') as f:
    f.write(content)
