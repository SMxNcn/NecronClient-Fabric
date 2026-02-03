package cn.boop.necron.mixin;

import cn.boop.necron.features.impl.necron.B64Chat;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class MixinChatComponent {
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Component message, CallbackInfo ci) {
        if (message == null) return;

        String text = message.getString();
        var component = Component.literal(text).getString().replaceAll("§[0-9a-fk-or]", "").trim();
        if (B64Chat.INSTANCE.getEnabled() && B64Chat.INSTANCE.getHideMessage() && component.contains("::") && component.endsWith("%]")) {
            ci.cancel();
        }
    }
}
