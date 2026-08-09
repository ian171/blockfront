package cn.epicmc.client.mixin;

import cn.epicmc.client.hud.HudDataManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 只记录玩家击杀玩家
        if (entity instanceof PlayerEntity victim) {
            if (damageSource.getAttacker() instanceof PlayerEntity killer) {
                String killerName = killer.getName().getString();
                String victimName = victim.getName().getString();

                // 获取武器名称
                String weapon = "未知";
                if (killer.getMainHandStack() != null && !killer.getMainHandStack().isEmpty()) {
                    weapon = killer.getMainHandStack().getName().getString();
                }

                // 判断是否是爆头（简单判断：如果伤害来源是弹射物且高度差较大）
                boolean isHeadshot = false;

                // 添加到击杀反馈（这里假设都是友军击杀，实际应根据队伍判断）
                boolean isFriendly = true;

                HudDataManager.getInstance().addKillFeedEntry(
                    killerName, victimName, weapon, isHeadshot, isFriendly
                );
            }
        }
    }
}
