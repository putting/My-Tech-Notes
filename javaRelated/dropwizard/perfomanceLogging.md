# Enabling timers to be added to Dropwizard

- Ensure the timer is **Managed**. eg SlowTaskMonitor implements Managed
- Use a timer: private ScheduledExecutorService timer
- Running tasks: Map<Callable<?>, TaskData> runningTasks = new ConcurrentHashMap<>() to hold tasks while running
- poll the runnig tasks and check: if (taskData.alertTimeMillis <= now) then alert or LOG
- Run tasks using monitor:
``` java
private ReportDbo get() {
            try {
                return queryMonitor.runTask(
                        taskName,
                        () -> re.extractPortfolioReport(eod.id(), eod.asOfDate(), eod.portfolio()),
                        QUERY_ALERT_TIME_MILLIS);
            } catch (Exception e) {
                throw new GiantException("Unable to load " + re.getReportType() + " report for " + eod, e);
            }
        }
```        
