package net.borisshoes.borislib.datastorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Interface for data objects that can be serialized/deserialized.
 * Reading uses ValueInput for partial decode resilience - if one field fails, others can still load.
 * Writing uses CompoundTag directly for simplicity and compatibility.
 * <p>
 * Implementations that hold mutable state (e.g. inventories, counters) should call
 * {@link #markDirty()} whenever their contents change so the parent {@code SavedData}
 * is flushed on the next autosave — even if the caller cached the reference long-term.
 */
public interface StorableData {
   /**
    * Reads data from the given ValueInput view.
    * Each field should be read independently with appropriate defaults for missing/corrupted values.
    *
    * @param view The ValueInput to read from
    */
   void read(ValueInput view);
   
   /**
    * Writes data to the given CompoundTag.
    *
    * @param tag The CompoundTag to write to
    */
   void writeNbt(CompoundTag tag);
   
   /**
    * Called by BorisLib internals immediately after a data object is created or decoded,
    * before the object is returned to any caller. Implementations should store this runnable
    * and invoke it inside {@link #markDirty()} to propagate dirtiness to the owning
    * {@code SavedData}. The default no-op is safe — it is replaced before any caller
    * can hold a reference to the object.
    *
    * @param callback Runnable that marks the owning SavedData dirty
    */
   default void setDirtyCallback(Runnable callback){ }
   
   /**
    * Signals that this object's state has changed and should be persisted on the next save.
    * <p>
    * Call this from any mutation method (e.g. after modifying an inventory slot, updating a counter).
    * To use this correctly, override <b>both</b> {@link #setDirtyCallback} and {@code markDirty}
    * so the injected callback is stored and invoked:
    * <pre>{@code
    *    private Runnable dirtyCallback = () -> {};   // safe no-op until BorisLib injects the real one
    *
    *    @Override
    *    public void setDirtyCallback(Runnable callback) {
    *       this.dirtyCallback = callback;            // BorisLib calls this before returning the object
    *    }
    *
    *    @Override
    *    public void markDirty() {
    *       dirtyCallback.run();                      // propagates to SavedData immediately
    *    }
    *
    *    public void setCount(int count) {
    *       this.count = count;
    *       markDirty();
    *    }
    * }</pre>
    * The default implementation is a no-op and is sufficient for read-only or short-lived
    * objects that do not cache a reference beyond a single operation.
    */
   default void markDirty(){ }
}
