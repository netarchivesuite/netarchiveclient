package dk.statsbiblioteket.netarchiveclient.util;



import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Class to automatically chain generated iterators
 * Use it like {@code Iterator combined = new AutochainingIterator(i -> getIterator(i)) }
 * I.e. feed it a function (Integer -> Iterator<T>) to generate the next iterator in the set.
 * The Integer input to the function is the offset, i.e. the count of objects seen so far.
 * Whenever the current iterator is exhausted, it generates the next one and continues from there.
 * This continues until the generating function returns null or an iterator with no next element;
 *
 * @param <T> the type of object iterated over
 */
public class AutochainingIterator<K, T> implements Iterator<T> {
    
    //Offset into the overall stream
    private K offset;
    
    //The current iterator
    private Iterator<T> current;
    
    /**
     * The function to generate iterators Takes an integer as input, which is the offset
     *
     * @see #offset
     */
    private final Function<K, IteratorOffset<K, Iterator<T>>> iteratorGenerator;
    
    /**
     * The next item to return
     */
    private T currentItem;
    
    /**
     * Turn a iterator-generator into an iterator. When the previous iterator runs out, it automatically requests the
     * next one
     * This continues until either the generator returns an empty iterator or null
     *
     * @param iteratorGenerator the function to generate the next iterator. Takes the overall offset as input
     */
    public AutochainingIterator(Function<K, IteratorOffset<K, Iterator<T>>> iteratorGenerator) {
        this.iteratorGenerator = iteratorGenerator;
        init();
    }
    
    
    private void init() {
        IteratorOffset<K, Iterator<T>> pair = iteratorGenerator.apply(null);
        this.current = pair.getValue();
        this.offset = pair.getKey();
        
        if (this.current.hasNext()) {
            currentItem = this.current.next();
        } else {
            currentItem = null;
        }
    }
    
    @Override
    public boolean hasNext() {
        return currentItem != null;
        
    }
    
    @Override
    public T next() {
        if (currentItem == null) {
            throw new NoSuchElementException("No next");
        }
        T result = currentItem;
        
        if (current.hasNext()) {
            //prepare next item and return the current
            currentItem = current.next();
        } else {
            //get next iterator
            IteratorOffset<K, Iterator<T>> pair = iteratorGenerator.apply(offset);
            current = pair.getValue();
            if (offset.equals(pair.getKey())){
                currentItem = null;
            } else {
                offset = pair.getKey();
                if (current != null && current.hasNext()) {
                    currentItem = current.next();
                } else {
                    currentItem = null;
                }
            }
            
        }
        return result;
    }
    
    public static class IteratorOffset<K,I extends Iterator<?>> {
        private final K key;
        private final I value;
        
        public IteratorOffset(K key, I value) {
            this.key = key;
            this.value = value;
        }
        
        public K getKey() {
            return key;
        }
        
        public I getValue() {
            return value;
        }
        
        public static <K,I extends Iterator<?>> IteratorOffset<K,I> of(K key, I value){
            return new IteratorOffset<>(key, value);
        }
    }
    
}

