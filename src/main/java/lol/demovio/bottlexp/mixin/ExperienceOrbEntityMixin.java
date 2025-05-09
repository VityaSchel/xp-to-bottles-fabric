package lol.demovio.bottlexp.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {
	@Shadow protected abstract int repairPlayerGears(PlayerEntity player, int amount);

	@Inject(at = @At("HEAD"), method = "repairPlayerGears", cancellable = true)
	private void bottleXp(PlayerEntity player, int amount, CallbackInfoReturnable<Integer> cir) {
		int expBottleCost = 7;

		Map<EquipmentSlot, ItemStack> inventory = Maps.newEnumMap(EquipmentSlot.class);
		for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
			ItemStack itemStack = player.getEquippedStack(equipmentSlot);
			if (!itemStack.isEmpty()) {
				inventory.put(equipmentSlot, itemStack);
			}
		}

		List<Map.Entry<EquipmentSlot, ItemStack>> list = Lists.newArrayList();
		for (Map.Entry<EquipmentSlot, ItemStack> entry : inventory.entrySet()) {
			ItemStack itemStack = entry.getValue();
			Item item = itemStack.getItem();
			if (!itemStack.isEmpty() && item == Items.GLASS_BOTTLE) {
				list.add(entry);
			}
		}

		if(!list.isEmpty()) {
			ItemStack bottleStack = list.get(player.getRandom().nextInt(list.size())).getValue();
			NbtCompound nbt = bottleStack.getOrCreateNbt();
			int accumulated = nbt.getInt("accumulated") + amount;
			int remaining = 0;
			if(accumulated > expBottleCost) {
				accumulated = 0;
				bottleStack.decrement(1);
				player.giveItemStack(new ItemStack(Items.EXPERIENCE_BOTTLE));
				remaining = amount - expBottleCost;
			}
			nbt.putInt("accumulated", accumulated);
			bottleStack.setNbt(nbt);
			bottleStack.setCustomName(
					Text.literal("XP: " + accumulated + "/" + expBottleCost)
							.formatted(Formatting.RESET)
							.formatted(Formatting.GOLD)
			);
			cir.setReturnValue(remaining > 0 ? this.repairPlayerGears(player, remaining) : 0);
			cir.cancel();
		}
	}
}