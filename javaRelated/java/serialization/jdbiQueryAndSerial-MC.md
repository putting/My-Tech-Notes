# How to query sql, read a result set and serialize to json

```java
public abstract class JdbiReportAdapter implements ReportExtractor {

    private static final Logger logger = LoggerFactory.getLogger(JdbiReportAdapter.class);

    /**
     * Defaults to 128 with MS SQL server. Increase by an order of magnitude to reduce chattiness.
     */
    private static final int FETCH_SIZE = 1280;

    private final DBI dbi;
    private final ObjectMapper objectMapper;
    private final ICTSRootResource.ReportType reportType;
    private final String querySql;
    private final String sanitySql;

    public JdbiReportAdapter(DBI dbi, ObjectMapper objectMapper, ICTSRootResource.ReportType reportType, String querySql, String sanitySql) {
        this.dbi = dbi;
        this.objectMapper = objectMapper;
        this.reportType = reportType;
        this.querySql = querySql;
        this.sanitySql = sanitySql;

        checkSql(querySql);
        if (sanitySql != null) {
            checkSql(sanitySql);
        }

    }

    private void checkSql(String sql ) {
        //This is a dirty check to make sure the query looks close to being valid:
        Preconditions.checkArgument(sql.contains(":portnum"), "Unable to find portnum as a parameter for " + reportType + " sql");
    }

    @Override
    public ReportDbo extractFullReport(LocalDate date) {
        throw new RuntimeException("Full report is an inventory specific abberation that should be removed from the interface");
    }

    @Override
    public ReportDbo extractPortfolioReport(long portfolioEodId, LocalDate localDate, int portfolio) {
        logger.info(String.format("Extracting %s report %d on %s ", this.getClass().getName(), portfolioEodId, localDate));
        Instant startTime = Instant.now();
        try(Handle h = dbi.open()) {
            Iterator<ObjectNode> it = h.createQuery(querySql)
                    .bind("portnum", portfolio)
                    .bind("date", localDate)
                    .setFetchSize(FETCH_SIZE)
                    .map(new JdbiJsonResultSetMapper(objectMapper))
                    .iterator();

            StringWriter buffer = new StringWriter(1024);

            try {
                int count = serializeToWriter(it, buffer, portfolioEodId);
                if (sanitySql != null) {
                    int expected = h.createQuery(sanitySql)
                            .bind("portnum", portfolio)
                            .bind("date", localDate)
                            .mapTo(Integer.class)
                            .first();

                    Preconditions.checkState(count <= expected, String.format("Expected at most %d rows but found %d", expected, count));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Instant endTime = Instant.now();
            String data = buffer.toString();

            logger.debug("{} report extract for portfolio {} date {} took {} ms for {} bytes", reportType, portfolio, localDate,
                    (Duration.between(startTime, endTime)).toMillis(), data.length());
            return ReportDbo.createReport(portfolioEodId, localDate, reportType, data, startTime, endTime);
        }
    }

    private int serializeToWriter(Iterator<ObjectNode> it, Writer buffer, long portfolioEodId) throws IOException {
        JsonGenerator generator = objectMapper.getFactory().createGenerator(buffer);
        int count = 0;
        generator.writeStartArray();
        while (it.hasNext()) {
            ObjectNode on = it.next();
            //Enriching ReportDbo data to track the lineage of the object through the system.
            on.put("portfolioEodId", portfolioEodId);
            generator.writeObject(on);
            count++;
        }
        generator.writeEndArray();
        generator.close();
        return count;
    }

    @Override
    public ICTSRootResource.ReportType getReportType() {
        return reportType;
    }

}
```
