package airtraffic.iterator;

public interface ListAccumulator<T> {
   boolean filter(T source);
   void forEach(T source);
}