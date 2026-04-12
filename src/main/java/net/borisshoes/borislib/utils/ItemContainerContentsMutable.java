package net.borisshoes.borislib.utils;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemContainerContentsMutable {
   private final List<ItemStack> items;
   private int maxSlots = -1;
   private BiPredicate<ItemStack, List<ItemStack>> predicate;
   
   public ItemContainerContentsMutable(List<ItemStack> items){
      this.items = new ArrayList<>(items);
   }
   
   public ItemContainerContentsMutable(ItemStack... items){
      this.items = new ArrayList<>(Arrays.asList(items));
   }
   
   public ItemContainerContentsMutable(ItemContainerContents component){
      this.items = component.allItemsCopyStream().collect(Collectors.toCollection(ArrayList::new));
   }
   
   public ItemContainerContentsMutable(ItemContainerContents component, int size){
      NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
      component.copyInto(list);
      this.items = new ArrayList<>(list);
      this.maxSlots = size;
   }
   
   public static ItemContainerContentsMutable fromComponent(ItemContainerContents component){
      return new ItemContainerContentsMutable(component);
   }
   
   public static ItemContainerContentsMutable fromComponent(ItemContainerContents component, int size){
      return new ItemContainerContentsMutable(component, size);
   }
   
   public ItemContainerContentsMutable setMaxSlots(int maxSlots){
      this.maxSlots = maxSlots;
      return this;
   }
   
   public ItemContainerContentsMutable setPredicate(BiPredicate<ItemStack, List<ItemStack>> predicate){
      this.predicate = predicate;
      return this;
   }
   
   public int getMaxSlots(){
      return maxSlots;
   }
   
   public BiPredicate<ItemStack, List<ItemStack>> getPredicate(){
      return predicate;
   }
   
   public boolean setItem(int slot, ItemStack stack){
      if(slot < 0 || slot >= items.size()) return false;
      if(maxSlots >= 0 && slot >= maxSlots) return false;
      if(predicate != null && !predicate.test(stack, items)) return false;
      items.set(slot, stack);
      return true;
   }
   
   public boolean addItem(ItemStack stack){
      if(maxSlots >= 0 && items.size() >= maxSlots) return false;
      if(predicate != null && !predicate.test(stack, items)) return false;
      return items.add(stack);
   }
   
   public ItemStack getItem(int slot){
      if(slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
      if(maxSlots >= 0 && slot >= maxSlots) return ItemStack.EMPTY;
      ItemStack stack = items.get(slot);
      return stack != null ? stack : ItemStack.EMPTY;
   }
   
   public List<ItemStack> getItems(){
      return items;
   }
   
   public List<ItemStack> getNonEmpty(){
      return items.stream().filter(stack -> !stack.isEmpty()).toList();
   }
   
   public Stream<ItemStack> getAllCopyStream(){
      return items.stream().map(ItemStack::copy);
   }
   
   public Stream<ItemStack> getNonEmptyCopyStream(){
      return items.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy);
   }
   
   public List<ItemStack> getAllCopy(){
      return getAllCopyStream().toList();
   }
   
   public List<ItemStack> getNonEmptyCopy(){
      return getNonEmptyCopyStream().toList();
   }
   
   public ItemContainerContents toImmutable(){
      return ItemContainerContents.fromItems(items);
   }
   
   public ItemStack tryRemoveFirstNonEmpty(){
      for(ItemStack stack : items){
         if(stack != null && !stack.isEmpty()){
            return stack.copyAndClear();
         }
      }
      return ItemStack.EMPTY;
   }
   
   public ItemStack tryRemoveLastNonEmpty(){
      for(ItemStack stack : items.reversed()){
         if(stack != null && !stack.isEmpty()){
            return stack.copyAndClear();
         }
      }
      return ItemStack.EMPTY;
   }
   
   public ItemStack tryAddStackToContainerComp(ItemStack stack){
      for(ItemStack existingStack : items){
         if(existingStack == null || existingStack.isEmpty()) continue;
         if(stack.isEmpty()) break;
         int curCount = stack.getCount();
         
         boolean canCombine = ItemStack.isSameItemSameComponents(existingStack, stack) && existingStack.isStackable() && existingStack.getCount() < existingStack.getMaxStackSize();
         if(canCombine){
            int toAdd = Math.min(existingStack.getMaxStackSize() - existingStack.getCount(), curCount);
            existingStack.grow(toAdd);
            stack.setCount(curCount - toAdd);
         }
      }
      
      if(stack.isEmpty()) return stack;
      if(predicate != null && !predicate.test(stack, items)) return stack;
      
      for(int i = 0; i < items.size(); i++){
         ItemStack item = items.get(i);
         if(item == null || item.isEmpty()){
            items.set(i,stack.copyAndClear());
            return stack;
         }
      }
      
      if(maxSlots >= 0 && items.size() >= maxSlots) return stack;
      items.add(stack.copyAndClear());
      return stack;
   }
   
   public boolean canInsertPerfectly(ItemStack stack){
      if(stack.isEmpty()) return true;
      int remaining = stack.getCount();
      
      boolean hasEmptySlot = false;
      for(ItemStack existingStack : items){
         if(existingStack == null || existingStack.isEmpty()){
            hasEmptySlot = true;
            continue;
         }
         if(ItemStack.isSameItemSameComponents(existingStack, stack) && existingStack.isStackable()){
            remaining -= (existingStack.getMaxStackSize() - existingStack.getCount());
            if(remaining <= 0) return true;
         }
      }
      
      if(predicate != null && !predicate.test(stack, items)) return false;
      return hasEmptySlot || maxSlots < 0 || items.size() < maxSlots;
   }
   
   @Override
   public int hashCode(){
      return ItemStack.hashStackList(items);
   }
}
