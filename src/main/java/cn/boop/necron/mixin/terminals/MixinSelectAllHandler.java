package cn.boop.necron.mixin.terminals;

import cn.boop.necron.features.impl.necron.AutoTerms;
import com.odtheking.odin.utils.skyblock.dungeon.terminals.terminalhandler.SelectAllHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(SelectAllHandler.class)
public class MixinSelectAllHandler {
    @Inject(method = "solve(Ljava/util/List;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void onSolve(List<ItemStack> items, CallbackInfoReturnable<List<Integer>> cir) {
        if (AutoTerms.INSTANCE.shouldShuffleSolution()) {
            List<Integer> originalSolution = cir.getReturnValue();
            List<Integer> shuffledSolution = new ArrayList<>(originalSolution);
            Collections.shuffle(shuffledSolution);

            cir.setReturnValue(shuffledSolution);
        }
    }
}
