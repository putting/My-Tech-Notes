import lombok.*;
import lombok.experimental.*;
import org.apache.commons.lang3.tuple.*;

import javax.validation.*;
import javax.xml.crypto.dsig.keyinfo.*;
import java.util.*;
import java.util.stream.*;

/**
 * Kind of exception that accumulates multiple problems to raise in one go.
 */
public class MultiFailureException extends RuntimeException {

    /**
     * Class encapsulating a single problem that needs to be reported together with other problems as part of a failure.
     */
    @Data
    public static class Failure {
        private final String content;

        @Override
        public String toString() {
            return content;
        }

        /**
         * Builds a problem with the given content.
         *
         * @param content text containing the problem to raise.
         *
         * @return instance of {@link Failure}.
         */
        public static Failure of(@NonNull String content) {
            return new Failure(content);
        }

        /**
         * Converts a collection of constraint violations into a collection of failures.
         *
         * @param <T>        type of entity failure to satisfy the constraints.
         * @param violations collection of {@link ConstraintViolation} to convert.
         *
         * @return list of failures.
         */
        public static <T> List<Failure> of(@NonNull Collection<ConstraintViolation<T>> violations) {
            final var groupedByMessage = violations.stream()
                                                   .collect(Collectors.groupingBy(ConstraintViolation::getMessage));
            final var aggregated = groupedByMessage.entrySet()
                                                   .stream()
                                                   .map(kv -> Pair.of(kv.getKey(),
                                                                      kv.getValue()
                                                                        .stream()
                                                                        .map(v -> String.format("%s.%s (%s)",
                                                                                                v.getRootBean().getClass().getSimpleName(),
                                                                                                v.getPropertyPath(),
                                                                                                v.getInvalidValue()))
                                                                        .sorted()
                                                                        .collect(Collectors.joining(", "))))
                                                   .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
            final var result = aggregated.entrySet()
                                         .stream()
                                         .sorted(Comparator.comparing(Map.Entry::getKey))
                                         .map(kv -> Failure.of(String.format("%s: %s", kv.getKey(), kv.getValue())))
                                         .collect(Collectors.toList());
            return result;
        }
    }

    /**
     * Utility class that allows accumulation of failure problems within the current thread.
     */
    @UtilityClass
    public static class Accumulator {

        private static final int NO_ERROR_CODE = Integer.MIN_VALUE;

        private static final ThreadLocal<LinkedList<Failure>> threadLocalFailures = new ThreadLocal<>();

        /**
         * Adds the given problem to the list of failures to be thrown from the current thread.
         *
         * @param failure
         */
        public static void addFailure(@NonNull MultiFailureException.Failure failure) {
            var local = threadLocalFailures.get();
            if (local == null) {
                threadLocalFailures.set(local = new LinkedList<>());
            }
            local.addLast(failure);
            Eventing.raise(new Eventing.FailureAddedEvent(failure));
        }

        /**
         * Raises a failure exception based on the problems accumulated so far within the current thread.
         * <p></p>
         * If no problems accumulated then this method doesn't throw.
         *
         * @throws MultiFailureException failure containing all the accumulated problems.
         */
        public static void raise() throws MultiFailureException {
            raise(NO_ERROR_CODE, false);
        }

        /**
         * Raises a failure exception based on the problems accumulated so far within the current thread.
         * <p></p>
         * If no problems accumulated then this method doesn't throw.
         *
         * @throws MultiFailureException failure containing all the accumulated problems.
         */
        public static void raise(int errorCode) throws MultiFailureException {
            raise(errorCode, true);
        }

        /**
         * Utility class that allows to delimit the problems to raise in one go within the current thread.
         * <p></p>
         * Use <em>try-with-resources</em> to delimit the region that accumulates the errors.
         */
        public static class MultiFailureThrower implements AutoCloseable {

            @Getter
            private final int errorCode;

            /**
             * Builds a failure thrower with no error code.
             */
            public MultiFailureThrower() {
                this(NO_ERROR_CODE, false);
            }

            /**
             * Builds a failure thrower with the given error code.
             *
             * @param errorCode numeric code associated with the error thrown.
             */
            public MultiFailureThrower(int errorCode) {
                this(errorCode, true);
            }

            private MultiFailureThrower(int errorCode, boolean mustHaveErrorCode) {
                if (errorCode == NO_ERROR_CODE && mustHaveErrorCode) {
                    throw new IllegalArgumentException("Illegal error code: " + errorCode);
                }
                this.errorCode = errorCode;
            }

            @Override
            public void close() throws MultiFailureException {
                if (NO_ERROR_CODE != errorCode) {
                    raise(errorCode);
                } else {
                    raise();
                }
            }
        }

        private static void raise(int errorCode, boolean mustHaveErrorCode) throws MultiFailureException {
            final boolean noErrorCode;
            if (errorCode == NO_ERROR_CODE) {
                if (mustHaveErrorCode) {
                    throw new IllegalArgumentException("Illegal error code: " + errorCode);
                }
                noErrorCode = true;
            } else {
                noErrorCode = false;
            }
            final var             local = threadLocalFailures.get();
            MultiFailureException error = null;
            try {
                if (local != null && !local.isEmpty()) {
                    throw (error = noErrorCode ? new MultiFailureException(local) : new MultiFailureException(local, errorCode));
                }
            } finally {
                threadLocalFailures.set(null);
                if (error != null) {
                    Eventing.raise(new Eventing.MultiFailureRaisedEvent(error));
                }
            }
        }
    }

    /**
     * Utility class hosting the tools for raising events when failures are being registered or raised.
     * <p></p>
     * This class uses {@link SharedEventBus} to event passing.
     */
    @UtilityClass
    public static class Eventing {

        /**
         * Generic event raised when a failure is being registered or raised.
         */
        public static class MultiFailureEvent {
        }

        /**
         * Event raised when a new error is being added to be raised in the current thread.
         */
        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class FailureAddedEvent extends MultiFailureEvent {
            private final Failure failure;
        }

        /**
         * Event raised when all the failures accumultated so far in the current thread have been raised.
         */
        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class MultiFailureRaisedEvent extends MultiFailureEvent {
            private final MultiFailureException exception;
        }

//        private final EventBus                      eventBus  = new EventBus();
//        private final ConcurrentHashMap<Integer, ?> observers = new ConcurrentHashMap<>();
//        private final AtomicInteger                 idGen     = new AtomicInteger(0);

        /**
         * Subscribes the given listener to a multi-error event.
         *
         * @param <L>      type of listener to subscribe.
         * @param listener instance of listener to subscribe.
         *
         * @return subscription id.
         */
        public <L> int subscribe(@NonNull L listener) {
            return eventBroker().subscribe(listener);
        }

        /**
         * Unsubscribes from multi-error events and returns the subscribed listener, if any.
         *
         * @param <L> type of subscribed listener.
         * @param id  subscription id.
         *
         * @return subscribed listener corresponding to the id or null if none found.
         */
        public <L> L unsubscribe(int id) {
            return eventBroker().unsubscribe(id);
        }

        private <E extends MultiFailureEvent> void raise(@NonNull E event) {
            SharedEventBus.instance().ifPresent(b -> b.raiseEvent(event));
        }

        private SharedEventBus eventBroker() {
            final var result = SharedEventBus.instance()
                                             .orElseThrow(() -> new UnsupportedOperationException(SharedEventBus.class.getName() + " not initialised"));
            return result;
        }
    }

    private final LinkedList<Failure> failures;
    private final int                 errorCode;

    /**
     * Builds a failure exception based on the given list of problems.
     */
    protected MultiFailureException(@NonNull LinkedList<Failure> failures) {
        this(failures, Accumulator.NO_ERROR_CODE, false);
    }

    /**
     * Builds a failure exception based on the given list of problems and error code.
     *
     * @param failures  list of problems to raise together.
     * @param errorCode numeric code for the failure.
     */
    protected MultiFailureException(@NonNull LinkedList<Failure> failures, int errorCode) {
        this(failures, errorCode, true);
    }

    private MultiFailureException(LinkedList<Failure> failures, int errorCode, boolean mustHaveErrorCode) {
        if (errorCode == Accumulator.NO_ERROR_CODE && mustHaveErrorCode) {
            throw new IllegalArgumentException("Illegal error code: " + errorCode);
        }
        this.failures  = failures;
        this.errorCode = errorCode;
    }

    /**
     * Gets whether the current failure does not contain any problem.
     *
     * @return true if the failure has problems, false otherwise.
     */
    public boolean isEmpty() {
        return failures.isEmpty();
    }

    /**
     * Gets whether the failures have an associated error code.
     *
     * @return true if error code, false otherwise.
     */
    public boolean hasErrorCode() {
        return errorCode != Accumulator.NO_ERROR_CODE;
    }

    /**
     * Gets the associated error code, if any.
     *
     * @return the error code associated with the failures.
     *
     * @throws UnsupportedOperationException there is no error code associated with the failures, {@link #hasErrorCode()} returns false.
     */
    public int getErrorCode() {
        if (!hasErrorCode()) {
            throw new UnsupportedOperationException("No error code associated with the failures");
        }
        return errorCode;
    }

    @Override
    public String getMessage() {
        final var result = isEmpty()
                           ? ""
                           : String.format("Application failure:%s" +
                                           "%s",
                                           System.lineSeparator(),
                                           failures.stream()
                                                   .map(Failure::toString)
                                                   .map(t -> "  " + t)
                                                   .collect(Collectors.joining(System.lineSeparator())));
        return result;
    }
}
