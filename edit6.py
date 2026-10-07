import sys

with open('source/mc/src/client/java/dev/rehan/passthrough/client/PassthroughClient.java', 'r') as f:
    content = f.read()

content = content.replace('new net.minecraft.resources.Identifier("passthrough", "herobrine");', 'new net.minecraft.resources.Identifier("passthrough:herobrine");')

with open('source/mc/src/client/java/dev/rehan/passthrough/client/PassthroughClient.java', 'w') as f:
    f.write(content)
