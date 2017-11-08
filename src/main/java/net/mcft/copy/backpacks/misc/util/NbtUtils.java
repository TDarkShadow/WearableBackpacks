package net.mcft.copy.backpacks.misc.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.stream.Collector.Characteristics;

import com.google.common.collect.Iterables;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraftforge.common.util.INBTSerializable;

/** Contains NBT related methods for manipulating NBT tags and item stacks. */
public final class NbtUtils {
	
	private NbtUtils() {  }
	
	
	// TODO: Update to use enum instead and make methods "safer" by checking the type before casting.
	public static final class NbtType {
		private NbtType() {  }
		public static final int END = 0;
		public static final int BYTE = 1;
		public static final int SHORT = 2;
		public static final int INT = 3;
		public static final int LONG = 4;
		public static final int FLOAT = 5;
		public static final int DOUBLE = 6;
		public static final int BYTE_ARRAY = 7;
		public static final int STRING = 8;
		public static final int LIST = 9;
		public static final int COMPOUND = 10;
		public static final int INT_ARRAY = 11;
	}
	
	
	public static final String TAG_INDEX = "index";
	public static final String TAG_STACK = "stack";
	
	
	// Utility ItemStack / NBT manipulation methods
	
	/** Gets an NBT tag of the specified item stack, or null if it doesn't exist.
	 *  Example: <pre>{@code StackUtils.get(stack, "display", "color") }</pre> */
	public static NBTBase get(ItemStack stack, String... tags) {
		return get(stack.getTagCompound(), tags);
	}
	/** Gets a child NBT tag of the specified compound tag, or null if it doesn't exist.
	 *  Example: <pre>{@code StackUtils.get(compound, "display", "color") }</pre> */
	public static NBTBase get(NBTTagCompound compound, String... tags) {
		if (compound == null) return null;
		String tag = null;
		for (int i = 0; i < tags.length; i++) {
			tag = tags[i];
			if (!compound.hasKey(tag)) return null;
			if (i == tags.length - 1) break;
			compound = compound.getCompoundTag(tag);
		}
		return compound.getTag(tag);
	}
	
	/** Gets a value from the specified item stack's compound tag, or the default if it doesn't exist.
	 *  Example: <pre>{@code StackUtils.get(stack, -1, "display", "color") }</pre> */
	public static <T> T get(ItemStack stack, T defaultValue, String... tags) {
		return get(stack.getTagCompound(), defaultValue, tags);
	}
	/** Gets a value from the specified compound tag, or the default if it doesn't exist.
	 *  Example: <pre>{@code StackUtils.get(compound, -1, "display", "color") }</pre> */
	public static <T> T get(NBTTagCompound compound, T defaultValue, String... tags) {
		NBTBase tag = get(compound, tags);
		return ((tag != null) ? getTagValue(tag) : defaultValue);
	}
	
	/** Returns if the specified item stack's compound tag has a certain NBT tag.
	 *  Example: <pre>{@code StackUtils.has(stack, "display", "color") }</pre> */
	public static boolean has(ItemStack stack, String... tags) {
		return has(stack.getTagCompound(), tags);
	}
	/** Returns if the specified compound tag has a certain child NBT tag.
	 *  Example: <pre>{@code StackUtils.has(compound, "display", "color") }</pre> */
	public static boolean has(NBTTagCompound compound, String... tags) {
		return (get(compound, tags) != null);
	}
	
	/** Adds or replaces a tag on the specified item stack's compound tag, creating it and any parent compound tags if necessary.
	 *  Example: <pre>{@code StackUtils.set(stack, new NBTTagInt(0xFF0000), "display", "color") }</pre> */
	public static void set(ItemStack stack, NBTBase nbtTag, String... tags) {
		if (stack.isEmpty())
			throw new IllegalArgumentException("stack is empty");
		NBTTagCompound compound = stack.getTagCompound();
		if (compound == null) stack.setTagCompound(compound = new NBTTagCompound());
		set(compound, nbtTag, tags);
	}
	/** Adds or replaces a tag on the specified compound tag, creating any parent compound tags if necessary.
	 *  Example: <pre>{@code StackUtils.set(compound, new NBTTagInt(0xFF0000), "display", "color") }</pre> */
	public static void set(NBTTagCompound compound, NBTBase nbtTag, String... tags) {
		if (compound == null)
			throw new IllegalArgumentException("compound is null");
		String tag = null;
		for (int i = 0; i < tags.length; i++) {
			tag = tags[i];
			if (i == tags.length - 1) break;
			if (!compound.hasKey(tag)) {
				NBTTagCompound child = new NBTTagCompound();
				compound.setTag(tag, child);
				compound = child;
			} else compound = compound.getCompoundTag(tag);
		}
		compound.setTag(tag, nbtTag);
	}
	
	/** Adds or replaces a value on the specified item stack's compound tag, creating it and any parent compound tags if necessary.
	 *  Example: <pre>{@code StackUtils.set(stack, 0xFF0000, "display", "color") }</pre> */
	public static <T> void set(ItemStack stack, T value, String... tags) {
		set(stack, createTag(value), tags);
	}
	/** Adds or replaces a value on the specified compound tag, creating any parent compound tags if necessary.
	 *  Example: <pre>{@code StackUtils.set(compound, 0xFF0000, "display", "color") }</pre> */
	public static <T> void set(NBTTagCompound compound, T value, String... tags) {
		set(compound, createTag(value), tags);
	}
	
	/** Removes a certain NBT tag from the specified item stack's compound tag.
	 *  Example: <pre>{@code StackUtils.remove(stack, "display", "color") }</pre> */
	public static void remove(ItemStack stack, String... tags) {
		if (tags.length == 0) throw new IllegalArgumentException(
			"tags should have at least one element");
		if (!stack.hasTagCompound()) return;
		
		NBTTagCompound compound = stack.getTagCompound();
		remove(compound, tags);
		// If compound is empty, remove it from the stack.
		if (compound.hasNoTags())
			stack.setTagCompound(null);
	}
	/** Removes a certain NBT tag from the specified compound tag.
	 *  Example: <pre>{@code StackUtils.remove(compound, "display", "color") }</pre> */
	public static void remove(NBTTagCompound compound, String... tags) {
		if (tags.length == 0) throw new IllegalArgumentException(
			"tags should have at least one element");
		if (compound == null) return;
		
		if (tags.length > 1) {
			NBTBase tag = compound.getTag(tags[0]);
			if (!(tag instanceof NBTTagCompound)) return;
			NBTTagCompound subCompound = (NBTTagCompound)tag;
			remove(subCompound, (String[])Arrays.copyOfRange(tags, 1, tags.length));
			// If subCompound is empty, remove it from the parent compound.
			if (!subCompound.hasNoTags()) return;
		}
		compound.removeTag(tags[0]);
	}
	
	
	// Compound / List creation
	
	/** Creates an NBT compound from the name-value pairs in the parameters.
	 *  Doesn't add any null values to the resulting compound tag.
	 *  Example: <pre>{@code NbtUtils.createCompound("id", 1, "name", "copygirl") }</pre> */
	public static NBTTagCompound createCompound(Object... nameValuePairs) {
		return addToCompound(new NBTTagCompound(), nameValuePairs);
	}
	/** Adds entries to an ItemStack's NBT data from the name-value pairs in the parameters.
	 *  Doesn't add any null values to the ItemStack's compound tag.
	 *  Example: <pre>{@code NbtUtils.createCompound("id", 1, "name", "copygirl") }</pre> */
	public static NBTTagCompound add(ItemStack stack, Object... nameValuePairs) {
		if (stack.isEmpty()) throw new IllegalArgumentException("stack is empty");
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		return addToCompound(stack.getTagCompound(), nameValuePairs);
	}
	/** Adds entries to an NBT compound from the name-value pairs in the parameters.
	 *  Doesn't add any null values to the specified compound tag.
	 *  Example: <pre>{@code NbtUtils.addToCompound(compound, "id", 1, "name", "copygirl") }</pre> */
	public static NBTTagCompound addToCompound(NBTTagCompound compound, Object... nameValuePairs) {
		if (compound == null) throw new IllegalArgumentException("compound is null");
		for (int i = 0; i < nameValuePairs.length; i += 2) {
			String name = (String)nameValuePairs[i];
			Object value = nameValuePairs[i + 1];
			if (value == null) continue;
			compound.setTag(name, createTag(value));
		}
		return compound;
	}
	
	/** Creates an NBT list with the values, all of the single type.
	 *  Doesn't add any null values to the resulting list tag. */
	public static NBTTagList createList(Object... values) {
		return addToList(new NBTTagList(), values);
	}
	/** Adds values to an NBT list. Doesn't add any null values to the specified list tag. */
	public static NBTTagList addToList(NBTTagList list, Object... values) {
		if (list == null) throw new IllegalArgumentException("list is null");
		for (Object value : values) {
			if (value == null) continue;
			list.appendTag(createTag(value));
		}
		return list;
	}
	
	
	// Reading / writing ItemStacks
	
	/** Writes an item stack to an NBT compound. */
	public static NBTTagCompound writeItem(ItemStack item) {
		return writeItem(item, true);
	}
	/** Writes an item stack to an NBT compound. */
	public static NBTTagCompound writeItem(ItemStack item, boolean writeNullAsEmptyCompound) {
		return (!item.isEmpty()) ? item.writeToNBT(new NBTTagCompound())
			: (writeNullAsEmptyCompound ? new NBTTagCompound() : null);
	}
	
	/** Reads an item stack from an NBT compound. */
	public static ItemStack readItem(NBTTagCompound compound) {
		return ((compound != null) && !compound.hasNoTags())
				? new ItemStack(compound) : ItemStack.EMPTY;
	}
	
	
	/** Writes an item stack array to an NBT list. */
	public static NBTTagList writeItems(ItemStack[] items) {
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < items.length; i++)
			if (items[i] != null)
				list.appendTag(createCompound(
						TAG_INDEX, (short)i,
						TAG_STACK, writeItem(items[i])));
		return list;
	}
	
	/** Reads items from an NBT list to an item stack array. */
	public static ItemStack[] readItems(NBTTagList list, ItemStack[] items) {
		return readItems(list, items, null);
	}
	/** Reads items from an NBT list to an item stack array.
	 *  Any items falling outside the range of the items array
	 *  will get added to the invalid list if that's non-null. */
	public static ItemStack[] readItems(NBTTagList list, ItemStack[] items, List<ItemStack> invalid) {
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound compound = list.getCompoundTagAt(i);
			int index = compound.getShort(TAG_INDEX);
			ItemStack stack = readItem(compound.getCompoundTag(TAG_STACK));
			if ((index >= 0) || (index < items.length))
				items[index] = stack;
			else if (invalid != null)
				invalid.add(stack);
		}
		return items;
	}
	
	
	// Other utility functions
	
	/** Returns the primitive value of a tag, casted to the return type. */
	@SuppressWarnings("unchecked")
	public static <T> T getTagValue(NBTBase tag) {
		if (tag == null) throw new IllegalArgumentException("tag is null");
		
		if (tag instanceof NBTTagByte)      return (T)(Object)((NBTTagByte)tag).getByte();
		if (tag instanceof NBTTagShort)     return (T)(Object)((NBTTagShort)tag).getShort();
		if (tag instanceof NBTTagInt)       return (T)(Object)((NBTTagInt)tag).getInt();
		if (tag instanceof NBTTagLong)      return (T)(Object)((NBTTagLong)tag).getLong();
		if (tag instanceof NBTTagFloat)     return (T)(Object)((NBTTagFloat)tag).getFloat();
		if (tag instanceof NBTTagDouble)    return (T)(Object)((NBTTagDouble)tag).getDouble();
		if (tag instanceof NBTTagString)    return (T)(Object)((NBTTagString)tag).getString();
		if (tag instanceof NBTTagByteArray) return (T)((NBTTagByteArray)tag).getByteArray();
		if (tag instanceof NBTTagIntArray)  return (T)((NBTTagIntArray)tag).getIntArray();
		
		throw new IllegalArgumentException(NBTBase.NBT_TYPES[tag.getId()] + " isn't a primitive NBT tag");
	}
	
	/** Creates and returns a primitive NBT tag from a value.
	 *  If the value already is an NBT tag, it is returned instead. */
	public static NBTBase createTag(Object value) {
		if (value == null)
			throw new IllegalArgumentException("value is null");
		
		if (value instanceof NBTBase) return (NBTBase)value;
		if (value instanceof INBTSerializable)
			return ((INBTSerializable<?>)value).serializeNBT();
		if (value instanceof Collection) return ((Collection<?>)value).stream()
			.map(NbtUtils::createTag).collect(toList());
		
		if (value instanceof Byte)    return new NBTTagByte((Byte)value);
		if (value instanceof Short)   return new NBTTagShort((Short)value);
		if (value instanceof Integer) return new NBTTagInt((Integer)value);
		if (value instanceof Long)    return new NBTTagLong((Long)value);
		if (value instanceof Float)   return new NBTTagFloat((Float)value);
		if (value instanceof Double)  return new NBTTagDouble((Double)value);
		if (value instanceof String)  return new NBTTagString((String)value);
		if (value instanceof byte[])  return new NBTTagByteArray((byte[])value);
		if (value instanceof int[])   return new NBTTagIntArray((int[])value);
		
		throw new IllegalArgumentException("Can't create an NBT tag of value: " + value);
	}
	
	
	/** Returns the specified NBT serializable value
	 *  instance deserialized from the specified NBT tag. */
	public static <N extends NBTBase, T extends INBTSerializable<N>> T getTagValue(N tag, T value) {
		if (tag == null) throw new IllegalArgumentException("tag is null");
		if (value == null) throw new IllegalArgumentException("value is null");
		value.deserializeNBT(tag);
		return value;
	}
	
	/** Returns a list of NBT serializable values instantiated
	 *  using the value supplier from the specified NBT list. */
	@SuppressWarnings("unchecked")
	public static <N extends NBTBase, T extends INBTSerializable<N>> List<T> getTagList(
		NBTTagList list, Supplier<T> valueSupplier) {
		return stream(list)
			.map(tag -> getTagValue((N)tag, valueSupplier.get()))
			.collect(Collectors.toList());
	}
	
	
	// Iterable / Stream related functions
	
	/** Returns an iterable of NBT tags in the specified NBT list. */
	@SuppressWarnings("unchecked")
	public static <T extends NBTBase> Iterable<T> iterate(NBTTagList list) {
		return new Iterable<T>() {
			@Override public Iterator<T> iterator() {
				return new Iterator<T>() {
					private int _index = 0;
					@Override public boolean hasNext() { return (_index < list.tagCount()); }
					@Override public T next() { return (T)list.get(_index++); }
				};
			}
		};
	}
	/** Returns an iterable of entries in the specified NBT compound. */
	public static Iterable<CompoundEntry> iterate(NBTTagCompound compound) {
		return Iterables.transform(compound.getKeySet(),
			key -> new CompoundEntry(key, compound.getTag(key)));
	}
	
	/** Returns a stream of NBT tags in the specified NBT list. */
	public static <T extends NBTBase> Stream<T> stream(NBTTagList list) {
		return StreamSupport.stream(NbtUtils.<T>iterate(list).spliterator(), false);
	}
	/** Returns a stream of entries in the specified NBT compound. */
	public static Stream<CompoundEntry> stream(NBTTagCompound compound) {
		return StreamSupport.stream(NbtUtils.iterate(compound).spliterator(), false);
	}
	
	/** Returns a collector that accumulates the the input elements into a new NBT list. */
	public static <T> Collector<T, NBTTagList, NBTTagList> toList() {
		return Collector.of(NBTTagList::new,
			(list, element) ->
				list.appendTag(createTag(element)),
			(left, right) -> {
				for (NBTBase tag : iterate(right))
					left.appendTag(tag);
				return left;
			}, Characteristics.IDENTITY_FINISH);
	}
	
	/** Returns a collector that accumulates the the input NBT tags into a new NBT list. */
	public static <T> Collector<T, NBTTagCompound, NBTTagCompound> toCompound(
		Function<T, String> keyMapper, Function<T, NBTBase> tagMapper) {
		return Collector.of(NBTTagCompound::new,
			(compound, element) ->
				compound.setTag(keyMapper.apply(element), tagMapper.apply(element)),
			(left, right) -> {
				for (String key : right.getKeySet())
					left.setTag(key, right.getTag(key));
				return left;
			}, Characteristics.IDENTITY_FINISH);
	}
	
	public static class CompoundEntry {
		public final String key;
		public final NBTBase tag;
		public CompoundEntry(String key, NBTBase tag)
			{ this.key = key; this.tag = tag; }
	}
	
}
