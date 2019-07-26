# Concurreny

Examples of the Task Service which submit a number of runnables or rather AsyncTask which is Functional Interface so allows () -> runnable notation i think.
The AsyncTaskService is the overall executor for the tasks, and includes returning the result of the AsyncTask

```java
@FunctionalInterface
public interface AsyncTask<T> {
    T execute() throws Throwable;
}
```

```java
public class AsyncTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncTaskService.class);

    private final ConcurrentMap<Integer, TaskState> taskStates = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Clock clock;
    private final MetricRegistry metricRegistry;

    public AsyncTaskService(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.clock = Clock.systemUTC();
    }

    public <T> CompletableFuture<T> submit(String user, String taskName, AsyncTask<T> asyncTask) {
        return CompletableFuture.supplyAsync(() -> {
            int id = counter.getAndIncrement();
            Timer timer = metricRegistry.timer(MetricRegistry.name(AsyncTaskService.class, taskName, "duration"));
            Timer.Context context = timer.time();
            LOGGER.info("Starting task {} for user {}", taskName, user);
            TaskStatus status = TaskStatus.RUNNING;
            taskStates.put(id, ImmutableTaskState.builder().startTime(this.clock.instant()).id(id).user(user).status(status).task(taskName).build());
            try {
                T execute = asyncTask.execute();
                status = TaskStatus.COMPLETED;
                LOGGER.info("Task {} Success", taskName);
                return execute;
            } catch (DaliWebServiceClientException clientException) {
                status = TaskStatus.FAILED;
                taskStates.put(id, ImmutableTaskState.copyOf(taskStates.get(id)).withError(clientException));
                LOGGER.error("Task " + taskName + " Failed", clientException);
                int statusCode = clientException.getStatusCode() >= 400 ? clientException.getStatusCode() : 500;
                throw new WebApplicationException(clientException.getMessage(), statusCode);
            } catch (Throwable throwable) {
                status = TaskStatus.FAILED;
                taskStates.put(id, ImmutableTaskState.copyOf(taskStates.get(id)).withError(throwable));
                LOGGER.error("Task " + taskName + " Failed", throwable);
                throw new RuntimeException(throwable);
            } finally {
                TaskState finalTaskState = ImmutableTaskState.copyOf(taskStates.get(id)).withStatus(status).withCompleteTime(clock.instant());
                taskStates.put(id, finalTaskState);
                context.stop();
                LOGGER.info("Task {} completed with status {}.  Started: {}, Completed: {}, Duration {}ms.  Error message '{}'", finalTaskState.task(), finalTaskState.status(), finalTaskState.startTime(), finalTaskState.completeTime().get(), finalTaskState.durationMillis().get(), finalTaskState.errorMessage().orElse("No error"));
            }
        });
    }

    public ImmutableList<TaskState> getAllStates() {
        return this.taskStates.values().stream().sorted(Comparator.comparing(TaskState::startTime)).collect(ImmutableList.toImmutableList());
    }


}
```

```java
public class TaskServiceImpl implements TaskService {

    private final AsyncTaskService taskService;
    private final ProcessingResource processingResource;
    private final RunControlBreakResource runControlBreakResource;
    private final SubledgerEntitlements entitlements;
    private final ComparisonService comparisonService;
    private final VaultLoader vaultLoader;

    @Inject
    public TaskServiceImpl(ProcessingResource processingResource, RunControlBreakResource runControlBreakResource,
                           SubledgerEntitlements entitlements, AsyncTaskService taskService,
                           ComparisonService comparisonService, VaultLoader vaultLoader) {
        this.processingResource = processingResource;
        this.runControlBreakResource = runControlBreakResource;
        this.entitlements = entitlements;
        this.taskService = taskService;
        this.comparisonService = comparisonService;
        this.vaultLoader = vaultLoader;
    }

    @Override
    public ImmutableList<TaskState> showState() {
        return this.taskService.getAllStates();
    }

    @Override
    public void clearedDerivatives(SecurityContext securityContext, AsyncResponse asyncResponse, LocalDate businessDate) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getSubledgerPostCheck, SubledgerId.ClearedDerivatives);

        String user = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(user, "ClearedDerivatives_" + businessDate, () -> {
                    this.processingResource.journaliseEndurPipeline(businessDate, user);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }

    @Override
    public void subledger(SecurityContext securityContext, AsyncResponse asyncResponse, LocalDate businessDate, DataLoadMode loadMode) {
        assertCostInvAuditAccess(securityContext);

        String user = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(user, "All_Subledger_" + loadMode + "_" + businessDate, () -> {
                    this.processingResource.journaliseSubledger(businessDate, loadMode, user);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }

    @Override
    public void snapshotData(SecurityContext securityContext, AsyncResponse asyncResponse, LocalDate businessDate, DataLoadMode loadMode) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getCreateSnapshotDataCheck, "*");

        if (businessDate.getDayOfWeek() == DayOfWeek.SATURDAY || businessDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BadRequestException("You should not snapshot for a week-end date.  Please contact Finance Technology if you have any questions.");
        }

        String user = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(user, "SnapshotData_" + loadMode + "_" + businessDate, () -> {
                    this.processingResource.snapshotData(businessDate, loadMode, user);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }

    @Override
    public void reconcileVaultData(SecurityContext securityContext, AsyncResponse asyncResponse, LocalDate businessDate, DataLoadMode loadMode) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getCreateSnapshotDataCheck, "*");

        String user = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(user, "ReconcileVaultData_" + loadMode + "_" + businessDate, () -> {
                    this.processingResource.compareVaultWithGiantLoaderResults(businessDate, loadMode, user);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }


    @Override
    public void runControl(SecurityContext securityContext, AsyncResponse asyncResponse, String controlName, ControlMode controlMode, LocalDate businessDate) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getCreateSnapshotDataCheck, "*");

        String user = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(user, controlName + "_" + controlMode + "_" + businessDate, () -> {
                    this.runControlBreakResource.runControl(controlName, businessDate, user, controlMode);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }

    @Override
    public void runAllControl(SecurityContext securityContext, AsyncResponse asyncResponse, ControlMode controlMode, LocalDate businessDate) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getCreateSnapshotDataCheck, "*");

        String user = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(user, "All_" + controlMode + "_" + businessDate, () -> {
                    this.runControlBreakResource.runAllControl(businessDate, user, controlMode);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }

    @Override
    public void triggerWorkflow(SecurityContext securityContext, AsyncResponse asyncResponse, ControlMode controlMode, LocalDate businessDate) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getCreateSnapshotDataCheck, "*");

        complete(this.taskService
                .submit(securityContext.getUserPrincipal().getName(), "TriggerWorkflow_" + controlMode + "_" + businessDate, () -> {
                    this.runControlBreakResource.triggerControlWorkflow(businessDate, controlMode);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }


    @Override
    public void compareAndStore(SecurityContext securityContext, AsyncResponse asyncResponse, ComparisonConfig comparisonConfig) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getCreateSnapshotDataCheck, "*");

        String username = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(username, "CompareAndStore" + comparisonConfig.dataset1Page() + "_" + comparisonConfig.dataset2Page(), () -> {
                    this.comparisonService.compareAndStore(username, comparisonConfig);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }

    @Override
    public void loadEndurPnl(SecurityContext securityContext, AsyncResponse asyncResponse, LocalDate asOfDate) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getCreateSnapshotDataCheck, "*");

        String username = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(username, "Vault_loadEndurPnlDetail" + asOfDate, () -> {
                    this.vaultLoader.loadEndurPnlDetail(asOfDate);
                    this.vaultLoader.loadEndurPnlDetailMatured(asOfDate);
                    this.vaultLoader.loadEndurPAAReserves(asOfDate);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);

    }

        @Override
    public void setExecutionMode(SecurityContext securityContext, AsyncResponse asyncResponse, String executionMode) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getCreateSnapshotDataCheck, "*");
        String user = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(user, "ExecutionMode_" + executionMode, () -> {
                    this.processingResource.setExecutionMode(executionMode);
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }

    @Override
    public void restartSpark(SecurityContext securityContext, AsyncResponse asyncResponse) {
        SubledgerEntitlements.assertAccess(securityContext, entitlements::getCreateSnapshotDataCheck, "*");
        String user = securityContext.getUserPrincipal().getName();
        complete(this.taskService
                .submit(user, "RestartSpark", () -> {
                    this.processingResource.restartSparkService();
                    return "{\"status\" : \"done\"}";
                }), asyncResponse);
    }

```
