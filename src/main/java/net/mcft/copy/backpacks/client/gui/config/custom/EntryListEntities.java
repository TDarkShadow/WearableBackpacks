package net.mcft.copy.backpacks.client.gui.config.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.mcft.copy.backpacks.WearableBackpacks;
import net.mcft.copy.backpacks.api.BackpackRegistry;
import net.mcft.copy.backpacks.api.BackpackRegistry.BackpackEntityEntry;
import net.mcft.copy.backpacks.client.gui.GuiContainer;
import net.mcft.copy.backpacks.client.gui.GuiLabel;
import net.mcft.copy.backpacks.client.gui.GuiLabel.TextAlign;
import net.mcft.copy.backpacks.client.gui.config.BaseEntryList;
import net.mcft.copy.backpacks.client.gui.control.GuiButton;
import net.mcft.copy.backpacks.config.Status;
import net.mcft.copy.backpacks.config.Setting.ChangeRequiredAction;
import net.mcft.copy.backpacks.config.Status.Severity;
import net.mcft.copy.backpacks.config.custom.SettingListEntities;

@SideOnly(Side.CLIENT)
public class EntryListEntities extends BaseEntryList<BackpackEntityEntry> {
	
	public static final Status STATUS_NOT_FOUND = Status.WARN("entity", "entityNotFound");
	
	private final SettingListEntities _setting;
	
	public EntryListEntities(SettingListEntities setting) {
		// Use getDefaultEntityEntries instead of getDefault because
		// it will only be populated after CONFIG has been initialized.
		super(260, setting.getOwn(), BackpackRegistry.getDefaultEntityEntries());
		_setting = setting;
		
		GuiContainer entryLabel = new GuiContainer();
		entryLabel.setFillHorizontal();
		entryLabel.setHeight(DEFAULT_ENTRY_HEIGHT);
			GuiLabel label = new GuiLabel(I18n.format(
				"config." + WearableBackpacks.MOD_ID + ".entity.list"));
			label.setCenteredHorizontal();
			label.setBottom(2);
			entryLabel.add(label);
		insertFixed(0, entryLabel);
	}
	
	@Override
	public void setValue(List<BackpackEntityEntry> value) {
		List<BackpackEntityEntry> entityEntries = new ArrayList<>();
		BackpackRegistry.mergeEntityEntriesWithDefault(entityEntries, value);
		super.setValue(entityEntries);
	}
	
	@Override
	protected BaseEntryList.Entry<BackpackEntityEntry> createListEntry()
		{ return new Entry(this); }
	
	@Override
	protected void addButtonPressed()
		{ display(new ScreenEntityEntry(this, Optional.empty())); }
	
	
	public static Optional<EntityEntry> getEntityEntry(String entityID) {
		return Optional.ofNullable(ForgeRegistries.ENTITIES.getValue(new ResourceLocation(entityID)))
			.filter(entry -> EntityLivingBase.class.isAssignableFrom(entry.getEntityClass()));
	}
	public static String getEntityEntryName(Optional<EntityEntry> entry, String entityID)
		{ return entry.map(EntityEntry::getName).map(s -> "[" + s + "]").orElse("\"" + entityID + "\""); }
	
	
	public static class Entry extends BaseEntryList.Entry<BackpackEntityEntry> {
		
		public final GuiLabel labelName;
		public final GuiButton buttonEdit;
		private BackpackEntityEntry _value;
		private boolean _knownEntity;
		
		public Entry(EntryListEntities owningList) {
			super(owningList);
			
			labelName = new GuiLabel(0, "", TextAlign.CENTER);
			labelName.setCenteredVertical();
			
			buttonEdit = new GuiButton(100, DEFAULT_ENTRY_HEIGHT);
			buttonEdit.setAction(() -> { display(new ScreenEntityEntry(owningList, Optional.of(this))); });
			
			addFixed(buttonMove);
			addWeighted(labelName);
			addFixed(buttonEdit);
			addFixed(buttonRemove);
		}
		
		@Override
		public BackpackEntityEntry getValue() { return _value; }
		@Override
		public void setValue(BackpackEntityEntry value) {
			_value = value;
			
			Optional<EntityEntry> entry = getEntityEntry(value.entityID);
			_knownEntity      = entry.isPresent();
			Severity severity = Status.getSeverity(getStatus());
			boolean isFine    = (severity == Severity.FINE);
			
			int numEntries = value.getEntries().size();
			String entriesTextKey = "config." + WearableBackpacks.MOD_ID + ".entity.entry";
			// First we try to translate "[...].entity.entry.<num>".
			String entriesText = I18n.format(entriesTextKey + "." + numEntries);
			if (entriesText.equals(entriesTextKey + "." + numEntries))
				// If not found, use "[...].entity.entry" instead.
				entriesText = I18n.format(entriesTextKey, numEntries);
			// ... I miss C#'s ?? operator :(
			
			buttonMove.setEnabled(!value.isDefault);
			buttonRemove.setEnabled(!value.isDefault);
			
			labelName.setText(getEntityEntryName(entry, value.entityID));
			labelName.setColor(value.isDefault ? GuiUtils.getColorCode('8', true)
			                 : isFine          ? GuiUtils.getColorCode('7', true)
			                                   : severity.foregroundColor);
			
			buttonEdit.setText(entriesText);
			if (!_knownEntity) buttonEdit.setTextColor(Severity.WARN.foregroundColor);
			else buttonEdit.unsetTextColor();
		}
		
		@Override
		public List<Status> getStatus() {
			return _knownEntity ? Collections.emptyList()
			                    : Arrays.asList(STATUS_NOT_FOUND);
		}
		
	}
	
	
	// IConfigEntry implementation
	
	@Override
	public ChangeRequiredAction applyChanges() {
		if (!isChanged()) return ChangeRequiredAction.None;
		_setting.set(getValue());
		if (!_setting.requiresMinecraftRestart()) _setting.update();
		return _setting.getChangeRequiredAction();
	}
	
}
