package airtraffic.iterator;

import java.util.Map.Entry;

/**
 * Specifies behavior needed for accumulation using Maps.
 *
 * @author tony@piazzaconsulting.com
 */
public interface MapAccumulator<T, K, V> {
   boolean filter(T source);
   K getKey(T source);
   V initializeValue(T source);
   V updateValue(T source, V value);
   void forEach(Entry<K, V> entry);
}