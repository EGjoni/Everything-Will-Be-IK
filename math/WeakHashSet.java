package math;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.WeakHashMap;

/*
 import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * HashSet backed by a WeakHashMap.
 */
public class WeakHashSet<T extends Object> extends AbstractSet<T> {

	private transient WeakHashMap<T, Object> map=null;

	private static final Object PRESENT=new Object();

	/**
	 * Get an instance of WeakHashSet.
	 */
	public WeakHashSet() {
		super();
		map=new WeakHashMap<T, Object>();
	}

	/**
	 * Create a WeakHashSet with the given capacity.
	 *
	 * @param n the capacity
	 */
	public WeakHashSet(int n) {
		super();
		map=new WeakHashMap<T, Object>(n);
	}

	/**
	 * Get a WeakHashSet with the contents from the given Collection.
	 *
	 * @param c the collection
	 */
	public WeakHashSet(Collection<T> c) {
		this();
		if(c == null) {
			throw new NullPointerException(
				"Null collection provided to WeakHashSet");
		}
		addAll(c);
	}

	/**
	 * Get the Iterator for the backing Map.
	 */
	@Override
	public Iterator<T> iterator() {
		return(map.keySet().iterator());
	}

	/**
	 * Get the number of keys currently contained in this Set.
	 */
	@Override
	public int size() {
		return(map.size());
	}

	/**
	 * True if this set contains no elements.
	 */
	@Override
	public boolean isEmpty() {
		return(map.isEmpty());
	}

	/**
	 * True if this Set contains the given Object.
	 */
	@Override
	public boolean contains(Object o) {
		return(map.containsKey(o));
	}

	/**
	 * Add this object to this Set if it's not already present.
	 *
	 * @param o the object to add
	 * @return true if this object was just added, false if it already existed
	 */
	@Override
	public boolean add(T o) {
		Object old=map.put(o, PRESENT);
		return(old == null);
	}

	/**
	 * Remove the given object from this Set.
	 *
	 * @param o Object to be removed
	 * @return true if the Set did contain this object (but now doesn't)
	 */
	@Override
	public boolean remove(Object o) {
		Object old=map.remove(o);
		return(old==PRESENT);
	}

	/**
	 * Remove all entries from this Set.
	 */
	@Override
	public void clear() {
		map.clear();
	}
	
	public WeakHashSet<T> clone() {
		return new WeakHashSet<T>(this);
	}

}