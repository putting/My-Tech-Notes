# AsyncTaskService 

Eg of task submitting class. 

```java
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


public class AsyncTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncTaskService.class);

    private final ConcurrentMap<Integer, TaskState> taskStates = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Clock clock;

    public AsyncTaskService() {
        this.clock = Clock.systemUTC();
    }

    public <T> CompletableFuture<T> submit(String user, String taskName, AsyncTask<T> asyncTask) {
        return CompletableFuture.supplyAsync(() -> {
            int id = counter.getAndIncrement();
            LOGGER.info("Starting task {} for user {}", taskName, user);
            TaskStatus status = TaskStatus.RUNNING;
            taskStates.put(id, ImmutableTaskState.builder().startTime(this.clock.instant()).id(id).user(user).status(status).task(taskName).build());
            try {
                T execute = asyncTask.execute();
                status = TaskStatus.COMPLETED;
                LOGGER.info("Task {} Completed", taskName);
                return execute;
            } catch (Throwable throwable) {
                status = TaskStatus.FAILED;
                taskStates.put(id, ImmutableTaskState.copyOf(taskStates.get(id)).withError(throwable));
                LOGGER.error("Task " + taskName + " Failed", throwable);
                throw new RuntimeException(throwable);
            } finally {
                taskStates.put(id, ImmutableTaskState.copyOf(taskStates.get(id)).withStatus(status).withCompleteTime(clock.instant()));
            }
        });
    }

    public ImmutableList<TaskState> getAllStates() {
        return this.taskStates.values().stream().collect(ImmutableList.toImmutableList());
    }


}

```
