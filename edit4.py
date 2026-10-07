import sys

with open('source/gta/src/script.cpp', 'r') as f:
    content = f.read()

# Add g_skin_toggle
content = content.replace('	std::atomic<bool> g_toggle{false};', '	std::atomic<bool> g_toggle{false};\n\tstd::atomic<bool> g_skin_toggle{false};')

# Add to on_keyboard
kbd_replacement = """		else if (key == VK_END)
			g_arm_flight = true;
		else if (key == VK_NUMLOCK)
			g_skin_toggle = true;"""
content = content.replace("""		else if (key == VK_END)
			g_arm_flight = true;""", kbd_replacement)

# Add to tick()
tick_replacement = """		if (g_toggle.exchange(false))
		{
			g_enabled = !g_enabled;
			natives::Notify(g_enabled ? "Minecraft passthrough ~g~on" : "Minecraft passthrough ~r~off");
		}
		if (g_skin_toggle.exchange(false))
		{
			static bool isHerobrine = false;
			isHerobrine = !isHerobrine;
			sendf("{\\"t\\":\\"skin\\",\\"id\\":\\"%s\\"}", isHerobrine ? "herobrine" : "steve");
			natives::Notify(isHerobrine ? "Skin set to ~r~Herobrine" : "Skin set to ~g~Steve");
		}"""
content = content.replace("""		if (g_toggle.exchange(false))
		{
			g_enabled = !g_enabled;
			natives::Notify(g_enabled ? "Minecraft passthrough ~g~on" : "Minecraft passthrough ~r~off");
		}""", tick_replacement)

with open('source/gta/src/script.cpp', 'w') as f:
    f.write(content)
