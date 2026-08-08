package dta.sfmflow.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class ProgramDiskItem extends Item {
	public ProgramDiskItem(Properties properties) {
		super(properties);
	}

	public static boolean isProgrammed(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		return customData != null && customData.copyTag().contains("flowchart");
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
		if (!isProgrammed(stack)) {
			tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.GRAY));
			tooltip.add(Component.literal("Use on a Machine Inventory Manager to copy layout").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			tooltip.add(Component.literal("Programmed").withStyle(ChatFormatting.GOLD));
			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData != null) {
				CompoundTag tag = customData.copyTag();
				int components = 0;
				int connections = 0;
				if (tag.contains("flowchart")) {
					CompoundTag fc = tag.getCompound("flowchart");
					if (fc.contains("components")) {
						components = fc.getList("components", 10).size();
					}
					if (fc.contains("connections")) {
						connections = fc.getList("connections", 10).size();
					}
				}
				tooltip.add(Component.literal(" - Nodes: " + components).withStyle(ChatFormatting.GRAY));
				tooltip.add(Component.literal(" - Connections: " + connections).withStyle(ChatFormatting.GRAY));
			}
			tooltip.add(Component.literal("Use on another Manager to paste layout").withStyle(ChatFormatting.DARK_GRAY));
		}
		super.appendHoverText(stack, context, tooltip, tooltipFlag);
	}
}