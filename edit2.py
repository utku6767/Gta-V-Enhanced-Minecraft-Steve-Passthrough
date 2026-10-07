import sys

with open('source/mc/src/client/java/dev/rehan/passthrough/client/mixin/AbstractClientPlayerMixin.java', 'r') as f:
    content = f.read()

replacement = """	@Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
	private void passthrough$steve(final CallbackInfoReturnable<PlayerSkin> cir) {
		if (dev.rehan.passthrough.client.PassthroughClient.useHerobrine && dev.rehan.passthrough.client.PassthroughClient.herobrineSkin != null) {
			cir.setReturnValue(dev.rehan.passthrough.client.PassthroughClient.herobrineSkin);
			return;
		}
		if (Passthrough.active && !Boolean.getBoolean("passthrough.ownSkin")) {
			cir.setReturnValue(DefaultPlayerSkin.get(CLASSIC_STEVE));
		}
	}"""

content = content.replace("""	@Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
	private void passthrough$steve(final CallbackInfoReturnable<PlayerSkin> cir) {
		if (Passthrough.active && !Boolean.getBoolean("passthrough.ownSkin")) {
			cir.setReturnValue(DefaultPlayerSkin.get(CLASSIC_STEVE));
		}
	}""", replacement)

with open('source/mc/src/client/java/dev/rehan/passthrough/client/mixin/AbstractClientPlayerMixin.java', 'w') as f:
    f.write(content)
