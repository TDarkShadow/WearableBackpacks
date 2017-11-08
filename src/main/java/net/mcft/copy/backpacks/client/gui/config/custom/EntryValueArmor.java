package net.mcft.copy.backpacks.client.gui.config.custom;

import net.minecraft.client.gui.Gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.mcft.copy.backpacks.client.gui.config.EntryValueSlider;

@SideOnly(Side.CLIENT)
public class EntryValueArmor extends EntryValueSlider.RangeInteger {
	
	@Override
	protected void drawSliderForeground(boolean isHighlighted, float partialTicks) {
		int armor = (int)getSliderValue();
		int x = getWidth() / 2 - 5 * 8;
		int y = getHeight() / 2 - 8 / 2;
		
		float v = (isEnabled() ? 1.0F : 0.5F);
		setRenderColor(v, v, v);
		bindTexture(Gui.ICONS);
		
		for (int i = 0; i < 10; i++, x += 8) {
			int tx = (i * 2 + 1 < armor)  ? 34
					: (i * 2 + 1 == armor) ? 25
											: 16;
			drawRect(x, y, tx, 9, 9, 9, 256);
		}
	}
	
}
