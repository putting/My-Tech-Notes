# 2025-02-21 Data Team Interviews

## Walid AL AKHRAS 2025-02-21

Higher-level / Architecture
- Canonical Model (owner)
- Kafka (not sure if he has experience)
- Devops: CI/CD kubernets - Deployment Strategy

We are on-prem not cloud:
- Dagster
- Canonical Model - Proto definitions

## From ChatGPT
### Data Architecture & Pipeline Design

Intro
1) On his cv he said he designed a scalable... So walk us through the main tech/design decisions taken and whether 
there are better alternatives.

### Considerations
Please explain how you would build a scalable pipeline for Mercuria's trade data. Consider:
- Ingestion, Transformation and Storage, Optimization & Monitoring:
- What Technologies at each stage?
- What were the challenges, what tech or different design decisions you would take retrospectively?

✔ How would you design a scalable data pipeline for ingesting, transforming, and storing data daily?
Answer:

    Ingestion:
        Use distributed tools like Kafka, Kinesis, or Pulsar for streaming data.
        For batch ingestion, leverage AWS Glue, Apache NiFi, or custom Spark jobs.
        Optimize storage with Parquet/ORC to reduce I/O overhead.

    Transformation (ETL/ELT):
        Use a Dagster-based pipeline for orchestrating tasks.
        Utilize Spark or Dask for large-scale data processing.
        Implement incremental loading to avoid redundant processing.

    Storage:
        Store raw data in S3/Data Lake (Lakehouse Architecture).
        Transform and load data into a Data Warehouse (Snowflake, Redshift, or BigQuery).
        Partition large datasets to improve query performance.

    Optimization & Monitoring:
        Implement data validation checks with Great Expectations or dbt tests.
        Use Data Lineage tracking (OpenLineage, DataHub).
        Monitor pipeline performance via Prometheus, Datadog, or ELK stack.

✔ What factors do you consider when choosing between batch vs. streaming ETL?
Answer:

    Data Latency:
        Use batch processing for daily/hourly reports (e.g., sales trends).
        Use streaming for real-time alerts (e.g., fraud detection).

    Data Volume & Velocity:
        Large, historical datasets → Batch ETL (Spark, Databricks, Airflow).
        High-frequency, event-driven data → Streaming ETL (Flink, Kafka Streams).

    Cost & Infrastructure:
        Batch jobs can be cheaper as they run on a schedule.
        Streaming requires always-on compute (higher costs).

    Use Case Example:
        Batch: Monthly revenue reports, marketing analytics.
        Streaming: Live stock prices, IoT sensor data.
		
✔ How do you ensure data quality and reliability in a data pipeline?
Answer:

    Data Validation & Schema Enforcement:
        Use Great Expectations or dbt to validate data before processing.
        Enforce schemas in tools like Apache Avro, Iceberg, or Delta Lake.

    Error Handling & Retries:
        Implement idempotent processing to avoid duplicate records.
        Use Dagster’s retry policies for transient failures.

    Data Lineage & Observability:
        Track data lineage with OpenLineage or DataHub.
        Monitor data drift with Monte Carlo or Soda Core.

    Pipeline Testing:
        Unit test SQL transformations.
        Implement CI/CD for ETL pipelines with GitLab and dbt tests.
		
✔ Can you explain the difference between a data warehouse, data lake, and lakehouse?	
Answer:
Feature	Data 	Warehouse						Data Lake						Lakehouse
Structure		Highly structured				Raw, unstructured & structured	Mix of both
Tech Examples	Snowflake, Redshift, BigQuery	S3, ADLS, HDFS					Delta Lake, Iceberg, Hudi
Use Case		BI reports, dashboards			ML, raw log storage				Hybrid workloads
Performance		Fast SQL queries				Slower query performance		Optimized query performance
Schema		Predefined schema (Schema-on-Write)	Schema-on-Read					Schema evolution

✔ When to use what?

    Use a Data Warehouse for structured analytics & BI.
    Use a Data Lake for unstructured data & ML workloads.
    Use a Lakehouse when you need structured querying on a Data Lake (e.g., Delta Lake on Databricks).	

ETL & Dagster

✔ What are the key components of Dagster, and how does it compare to Airflow?
Answer:
Dagster has a more modular and data-centric approach to orchestration compared to Airflow, which is more task-based.
Key Components of Dagster:

    Assets: Represent data objects in a pipeline (e.g., tables, ML models).
    Ops (Operations): Small, reusable computation units (similar to Airflow tasks).
    Graphs: Combine multiple ops into workflows.
    Jobs: Define execution configurations for graphs.
    Sensors & Schedules: Trigger pipeline executions.
    Dagster Daemon: Runs background tasks like scheduling and monitoring.
    Dagit (UI): Provides observability and debugging features.

Comparison with Airflow:
Feature						Dagster							Airflow
Pipeline Structure			Asset-based						Task-based
Execution					Declarative						Imperative
Orchestration				Event-driven					Task DAGs
Testing						Strong local testing support	Harder to test locally
UI & Debugging				Dagit UI with lineage tracking	Airflow UI (less interactive)

✔ Dagster is better for modern data pipelines because of its data-driven orchestration and strong observability.
✔ Airflow is better for general workflow automation and legacy pipelines.


✔ How would you implement a dependency-aware ETL pipeline in Dagster?
✔ How do you handle failures and retries in Dagster?
Retries:

    Use the @op decorator with a retry policy.
Error Handling:

    Wrap logic in try/except and log errors.
    Use Dagster’s built-in failure hooks:
Checkpointing & Idempotency:

    Store intermediate results (e.g., in S3, Delta Lake).
    Ensure retries don’t reprocess already ingested data.	
✔ How do you test and deploy Dagster pipelines in a CI/CD environment?
✔ How would you orchestrate an ETL pipeline that pulls data from multiple sources (e.g., APIs, databases, S3) and loads it into a warehouse?
1. Extract:

    Fetch data from APIs, databases, and S3 using Dagster ops.
    Use Dask or Spark for distributed extraction if handling large volumes.
2. Transform:

    Normalize, clean, and enrich data using Pandas, Spark, or dbt.	
3. Load:

    Write transformed data into Redshift, Snowflake, or BigQuery.	
4. Orchestration with Dagster Graphs:
5. Scheduling & Monitoring:

    Use Dagster schedules or sensors for automation.
    Monitor using Dagit & Prometheus/Grafana.	
