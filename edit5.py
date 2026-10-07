import sys

with open('source/mc/src/client/java/dev/rehan/passthrough/client/PassthroughClient.java', 'r') as f:
    content = f.read()

content = content.replace('net.minecraft.client.renderer.texture.DynamicTexture tex = new net.minecraft.client.renderer.texture.DynamicTexture(', 'net.minecraft.client.renderer.texture.DynamicTexture tex = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "herobrine",')

content = content.replace('net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.of("passthrough", "herobrine");', 'net.minecraft.resources.Identifier id = new net.minecraft.resources.Identifier("passthrough", "herobrine");')

with open('source/mc/src/client/java/dev/rehan/passthrough/client/PassthroughClient.java', 'w') as f:
    f.write(content)
