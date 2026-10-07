import sys

with open('source/mc/src/client/java/dev/rehan/passthrough/client/HostLink.java', 'r') as f:
    content = f.read()

replacement = """				case "glide" -> WorldBridge.glide(!m.has("on") || m.get("on").getAsBoolean(), m.has("speed") ? m.get("speed").getAsDouble() : 1.2);
				case "skin" -> PassthroughClient.toggleSkin(m.get("id").getAsString().equals("herobrine"));"""

content = content.replace('				case "glide" -> WorldBridge.glide(!m.has("on") || m.get("on").getAsBoolean(), m.has("speed") ? m.get("speed").getAsDouble() : 1.2);', replacement)

with open('source/mc/src/client/java/dev/rehan/passthrough/client/HostLink.java', 'w') as f:
    f.write(content)
