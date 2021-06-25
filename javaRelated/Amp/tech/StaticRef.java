import lombok.*;

import java.util.*;
import java.util.function.*;

/**
 * Encapsulate the use of a 'singleton' Spring Object from a static context.
 * <p></p>
 * This class holds a value which should be set with setValue in a post constructor
 * method and cleared with clear() in a pre destroy method.
 * <p></p>
 * It can then only be accessed by passing a consumer to ifPresent which can safely
 * be called from a static content, since if the value is not present the consumer
 * will not be invoked.
 * <p></p>
 * (Whilst this class is intended for 'singleton' Spring Objects the code does not
 * make use of any functionality that would prevent it's reuse to safeguard the use
 * of other singleton objects from a static context.)
 * <p></p>
 * The no-args constructor should be called from the static context on the line which
 * defines the variable (and not delayed until the instance constructor or the Post
 * Constructor method).
 *
 * @param <T> The type of the value (i.e. the 'singleton' Spring Object).
 */
public class StaticRef<T> {

    private volatile T value;

    /**
     * Set the value. Intended for use in a Post Construct method.
     *
     * @param value Value of singleton object.
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * Clear the value. Intended for use in a Pre Destroy method.
     */
    public void clear() {
        this.value = null;
    }

    /**
     * Execute the supplied consumer, if the value is present.
     *
     * @param consumer Consumer to receive the current value. Not called if value is null.
     */
    public void ifPresentDo(@NonNull Consumer<? super T> consumer) {
        final T localValue = value; // Subsequent access in this method must all be to localValue (not value).
        if (localValue != null) {
            consumer.accept(localValue);
        }
    }

    /**
     * Execute the supplied converter, if the value is present.
     *
     * @param <U>    type of converted value.
     * @param mapper converting method.
     *
     * @return converted value, if any.
     */
    public <U> Optional<U> ifPresentGet(@NonNull Function<? super T, ? extends U> mapper) {
        return Optional.ofNullable(value).map(mapper);
    }
