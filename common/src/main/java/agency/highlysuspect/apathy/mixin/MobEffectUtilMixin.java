package agency.highlysuspect.apathy.mixin;

import agency.highlysuspect.apathy.Apathy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(MobEffectUtil.class)
public class MobEffectUtilMixin {
	@Inject(method = "addEffectToPlayersAround", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"), locals = LocalCapture.PRINT)
	private static void whenAddingEffectToPlayersAround(ServerLevel level, @Nullable Entity provoker, Vec3 what, double huh, MobEffectInstance effect, int hmm, CallbackInfoReturnable<List<ServerPlayer>> cir, int hrmm, MobEffect whar, List<ServerPlayer> original) {
		Apathy.INSTANCE.filterMobEffectUtilCall(level, provoker, original);
	}
}
