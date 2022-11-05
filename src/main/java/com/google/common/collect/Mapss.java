package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;

import java.util.*;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkNotNull;

// Maps accessor and reimplementation.
public class Mapss {
    /**
     * {@code AbstractMap} extension that makes it easy to cache customized keySet, values,
     * and entrySet views.
     */
    @GwtCompatible
    abstract static class ViewCachingAbstractMap<K, V> extends AbstractMap<K, V> {
        /**
         * Creates the entry set to be returned by {@link #entrySet()}. This method
         * is invoked at most once on a given map, at the time when {@code entrySet}
         * is first called.
         */
        abstract Set<Entry<K, V>> createEntrySet();

        private transient Set<Entry<K, V>> entrySet;

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> result = entrySet;
            return (result == null) ? entrySet = createEntrySet() : result;
        }

        private transient Set<K> keySet;

        @Override
        public Set<K> keySet() {
            Set<K> result = keySet;
            return (result == null) ? keySet = createKeySet() : result;
        }

        Set<K> createKeySet() {
            return new Maps.KeySet<K, V>(this);
        }

        private transient Collection<V> values;

        @Override
        public Collection<V> values() {
            Collection<V> result = values;
            return (result == null) ? values = createValues() : result;
        }

        Collection<V> createValues() {
            return new Maps.Values<K, V>(this);
        }
    }

    private static class AsMapView<K, V> extends ViewCachingAbstractMap<K, V> {

        private final Set<K> set;
        final Function<? super K, V> function;

        Set<K> backingSet() {
            return set;
        }

        AsMapView(Set<K> set, Function<? super K, V> function) {
            this.set = checkNotNull(set);
            this.function = checkNotNull(function);
        }

        @Override
        public Set<K> createKeySet() {
            return removeOnlySet(backingSet());
        }

        @Override
        Collection<V> createValues() {
            return Collections2.transform(set, function);
        }

        @Override
        public int size() {
            return backingSet().size();
        }

        @Override
        public boolean containsKey(Object key) {
            return backingSet().contains(key);
        }

        @Override
        public V get(Object key) {
            return getOrDefault(key, null);
        }

        @Override
        public V getOrDefault(Object key, V defaultValue) {
            if (Collections2.safeContains(backingSet(), key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                K k = (K) key;
                return function.apply(k);
            } else {
                return defaultValue;
            }
        }

        @Override
        public V remove(Object key) {
            if (backingSet().remove(key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                K k = (K) key;
                return function.apply(k);
            } else {
                return null;
            }
        }

        @Override
        public void clear() {
            backingSet().clear();
        }

        @Override
        protected Set<Entry<K, V>> createEntrySet() {
            class EntrySetImpl extends Maps.EntrySet<K, V> {
                @Override
                Map<K, V> map() {
                    return AsMapView.this;
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return asMapEntryIterator(backingSet(), function);
                }
            }
            return new EntrySetImpl();
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            checkNotNull(action);
            // avoids allocation of entries
            backingSet().forEach(k -> action.accept(k, function.apply(k)));
        }
    }

    private static <E> Set<E> removeOnlySet(final Set<E> set) {
        return new ForwardingSet<E>() {
            @Override
            protected Set<E> delegate() {
                return set;
            }

            @Override
            public boolean add(E element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends E> es) {
                throw new UnsupportedOperationException();
            }
        };
    }

    static <K, V> Iterator<Map.Entry<K, V>> asMapEntryIterator(
            Set<K> set, final Function<? super K, V> function) {
        return new TransformedIterator<K, Map.Entry<K, V>>(set.iterator()) {
            @Override
            Map.Entry<K, V> transform(final K key) {
                return immutableEntry(key, function.apply(key));
            }
        };
    }

    /**
     * Returns an immutable map entry with the specified key and value. The {@link
     * Map.Entry#setValue} operation throws an {@link UnsupportedOperationException}.
     *
     * <p>The returned entry is serializable.
     *
     * @param key the key to be associated with the returned entry
     * @param value the value to be associated with the returned entry
     */
    @GwtCompatible(serializable = true)
    public static <K, V> Map.Entry<K, V> immutableEntry(K key, V value) {
        return new ImmutableEntry<K, V>(key, value);
    }

    /**
     * Returns a live {@link Map} view whose keys are the contents of {@code set}
     * and whose values are computed on demand using {@code function}. To get an
     * immutable <i>copy</i> instead, use .
     *
     * <p>Specifically, for each {@code k} in the backing set, the returned map
     * has an entry mapping {@code k} to {@code function.apply(k)}. The {@code
     * keySet}, {@code values}, and {@code entrySet} views of the returned map
     * iterate in the same order as the backing set.
     *
     * <p>Modifications to the backing set are read through to the returned map.
     * The returned map supports removal operations if the backing set does.
     * Removal operations write through to the backing set.  The returned map
     * does not support put operations.
     *
     * <p><b>Warning:</b> If the function rejects {@code null}, caution is
     * required to make sure the set does not contain {@code null}, because the
     * view cannot stop {@code null} from being added to the set.
     *
     * <p><b>Warning:</b> This method assumes that for any instance {@code k} of
     * key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also
     * of type {@code K}. Using a key type for which this may not hold, such as
     * {@code ArrayList}, may risk a {@code ClassCastException} when calling
     * methods on the resulting map view.
     *
     * @since 14.0
     */
    public static <K, V> Map<K, V> asMap(Set<K> set, Function<? super K, V> function) {
        return new AsMapView<K, V>(set, function);
    }
}
