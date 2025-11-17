# Phase 13.2: 实时计算平台实施指南

## 📋 概述

本指南介绍如何构建企业级实时计算平台，实现Flink流处理、实时数仓、实时报表和CEP（复杂事件处理），构建低延迟、高吞吐的数据处理能力。

---

## 🏗️ 实时计算平台架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                      实时计算平台架构                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   Flink 作业   │  │   实时数仓    │  │   实时报表    │           │
│  │              │  │              │  │              │           │
│  │ • DataStream  │  │ • ODS 层      │  │ • 大屏展示     │           │
│  │ • 流式SQL     │  │ • DWD 层      │  │ • 自助查询     │           │
│  │ • CEP 引擎     │  │ • DWS 层      │  │ • 告警推送     │           │
│  │ • 窗口计算     │  │ • ADS 层      │  │ • 移动端       │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   数据采集     │  │   数据存储   │  │   数据服务   │           │
│  │              │  │              │  │              │           │
│  │ • CDC 采集     │  │ • Redis      │  │ • API 接口    │           │
│  │ • 日志采集     │  │ • ClickHouse │  │ • 数据订阅     │           │
│  │ • 消息队列     │  │ • Hudi       │  │ • 数据查询     │           │
│  │ • 爬虫采集     │  │ • Elasticsearch│  │ • 数据导出     │           │
│  └──────────────┘  └─────────────┘  └─────────────┘           │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    计算资源层                                 │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • Flink Cluster (Standalone/YARN/Kubernetes)                │ │
│  │ • Kafka Cluster                                              │ │
│  │ • Redis Cluster                                              │ │
│  │ • ClickHouse Cluster                                         │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    监控运维层                                  │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • Flink UI                                                    │ │
│  │ • Prometheus + Grafana                                        │ │
│  │ • 作业监控告警                                                 │ │
│  │ • 性能分析工具                                                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 技术栈选型

| 层次 | 技术组件 | 版本 | 用途 |
|------|----------|------|------|
| **流处理引擎** | Apache Flink | 1.18.0 | 实时计算 |
| **流式SQL** | Flink SQL | 1.18.0 | SQL开发 |
| **复杂事件** | Flink CEP | 1.18.0 | 模式匹配 |
| **消息队列** | Apache Kafka | 3.6.0 | 数据缓冲 |
| **数据采集** | Flink CDC | 3.0.0 | 变更捕获 |
| **实时存储** | ClickHouse | 23.12.0 | OLAP数据库 |
| **缓存层** | Redis | 7.2.0 | 热数据缓存 |
| **实时数仓** | Apache Hudi | 0.14.0 | 增量数据湖 |

---

## 🚀 Flink 流处理开发

### 1. 环境配置

#### Flink 集群配置

```yaml
# flink-conf.yaml
# =============================================================================
# Flink Configuration File
# =============================================================================

#==============================================================================
# Common
#==============================================================================

# The external address of the host on which the JobManager runs and can be
# reached by the TaskManagers and any clients which want to connect.
jobmanager.rpc.address: flink-jobmanager

jobmanager.rpc.port: 6123

# The heap size for the JobManager JVM
jobmanager.memory.process.size: 2048m

# The heap size for the TaskManager JVM
taskmanager.memory.process.size: 2048m

# The number of slots for each TaskManager
taskmanager.numberOfTaskSlots: 4

# The parallelism used for programs that did not specify parallelism explicitly
parallelism.default: 2

#==============================================================================
# Checkpointing
#==============================================================================

# The backend that will be used to store operator state checkpoints if
# checkpointing is enabled
state.backend: rocksdb

# Directory for checkpoints filesystem
state.checkpoints.dir: hdfs://hadoop:9000/flink/checkpoints

# Default checkpoint interval (milliseconds)
execution.checkpointing.interval: 10000

# Checkpoint timeout (milliseconds)
execution.checkpointing.timeout: 600000

# Minimum time between checkpoints
execution.checkpointing.min-pause: 5000

# Number of concurrent checkpoints
execution.checkpointing.max-concurrent-checkpoints: 1

# Enable unaligned checkpoints
execution.checkpointing.unaligned: true

#==============================================================================
# Rest & web frontend
#==============================================================================

# Port of the web interface
rest.address: 0.0.0.0
rest.port: 8081

# Flag to specify whether job submission is enabled from the web UI
web.submit.enable: true

# Timeout for web requests
web.timeout: 100000

# Timeout after which an idle job is automatically terminated
web.timeout: 3600000

# Timeout for a savepoint to be created without actually triggering the operation
web.savepoint-timeout: 600000

# Directory to which downloaded files are stored
web.downloads.dir: /tmp/flink-web

# Address of the web interface
web.address: 0.0.0.0

# Timeout after which an idle job is automatically terminated
web.timeout: 3600000

# Flag to specify whether job submission is enabled from the web UI
web.submit.enable: true

# Flag to specify whether job cancellation is enabled from the web UI
web.cancel.enable: true

# Timeout for a savepoint to be created without actually triggering the operation
web.savepoint-timeout: 600000

# Directory to which downloaded files are stored
web.downloads.dir: /tmp/flink-web

# Timeout after which an idle job is automatically terminated
web.timeout: 3600000

# Flag to specify whether job submission is enabled from the web UI
web.submit.enable: true

# Flag to specify whether job cancellation is enabled from the web UI
web.cancel.enable: true

# Timeout for a savepoint to be created without actually triggering the operation
web.savepoint-timeout: 600000

# Directory to which downloaded files are stored
web.downloads.dir: /tmp/flink-web

# Timeout after which an idle job is automatically terminated
web.timeout: 3600000

# Flag to specify whether job submission is enabled from the web UI
web.submit.enable: true

# Flag to specify whether job cancellation is enabled from the web UI
web.cancel.enable: true

# Timeout for a savepoint to be created without actually triggering the operation
web.savepoint-timeout: 600000

# Directory to which downloaded files are stored
web.downloads.dir: /tmp/flink-web
```

#### Docker Compose 部署

```yaml
# docker-compose-flink.yml
version: '3.8'

services:
  jobmanager:
    image: flink:1.18.0-scala_2.12-java11
    ports:
      - "8081:8081"
    command: jobmanager
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        state.checkpoints.dir: file:///opt/flink/checkpoints
        execution.checkpointing.interval: 10000
    volumes:
      - flink_checkpoints:/opt/flink/checkpoints
    networks:
      - flink-network

  taskmanager:
    image: flink:1.18.0-scala_2.12-java11
    depends_on:
      - jobmanager
    command: taskmanager
    scale: 2
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 4
        state.checkpoints.dir: file:///opt/flink/checkpoints
        execution.checkpointing.interval: 10000
    volumes:
      - flink_checkpoints:/opt/flink/checkpoints
    networks:
      - flink-network

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    networks:
      - flink-network

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - flink-network

  redis:
    image: redis:7.2-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes --requirepass redis123
    networks:
      - flink-network

  clickhouse:
    image: clickhouse/clickhouse-server:23.12-alpine
    ports:
      - "8123:8123"
      - "9000:9000"
    environment:
      CLICKHOUSE_DB: basebackend
      CLICKHOUSE_USER: admin
      CLICKHOUSE_PASSWORD: password123
    volumes:
      - clickhouse_data:/var/lib/clickhouse
      - clickhouse_logs:/var/log/clickhouse-server
    networks:
      - flink-network

volumes:
  flink_checkpoints:
  clickhouse_data:
  clickhouse_logs:

networks:
  flink-network:
    driver: bridge
```

### 2. DataStream API 开发

#### 基础数据流处理

```java
/**
 * 实时数据处理作业
 */
public class RealTimeDataProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeDataProcessingJob.class);

    public static void main(String[] args) throws Exception {
        // 创建流执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 配置检查点
        env.enableCheckpointing(10000);
        env.getCheckpointConfig().setCheckpointTimeout(60000);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5000);
        env.getCheckpointConfig().enableUnalignedCheckpoints();

        // 配置状态后端
        env.setStateBackend(new FsStateBackend("hdfs://hadoop:9000/flink/checkpoints"));

        // 数据源配置
        String kafkaBootstrapServers = "kafka:9092";
        String inputTopic = "basebackend-data";
        String outputTopic = "basebackend-realtime";
        String groupId = "realtime-processing";

        // 从Kafka读取数据
        FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>(
            inputTopic,
            new SimpleStringSchema(),
            getKafkaProperties(kafkaBootstrapServers, groupId)
        );

        DataStream<String> inputStream = env.addSource(kafkaConsumer);

        // 数据处理流水线
        DataStream<ProcessedEvent> processedStream = inputStream
            .map(new JsonToEventMapper())
            .filter(new EventFilter())
            .keyBy(event -> event.getUserId())
            .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
            .aggregate(new EventAggregator());

        // 实时统计
        DataStream<StatisticsResult> statisticsStream = processedStream
            .keyBy(event -> event.getCategory())
            .timeWindow(Time.minutes(1))
            .aggregate(new StatisticsAggregator());

        // 输出到Kafka
        FlinkKafkaProducer<String> kafkaProducer = new FlinkKafkaProducer<>(
            outputTopic,
            new EventSerializationSchema(),
            getKafkaProperties(kafkaBootstrapServers, "realtime-output")
        );

        processedStream.addSink(kafkaProducer);
        statisticsStream.addSink(new RedisSink<>());

        // 输出到ClickHouse
        processedStream.addSink(new ClickHouseSink<>());

        // 执行作业
        env.execute("BaseBackend Real-Time Data Processing");
    }

    private static Properties getKafkaProperties(String bootstrapServers, String groupId) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", bootstrapServers);
        props.setProperty("group.id", groupId);
        props.setProperty("auto.offset.reset", "earliest");
        props.setProperty("enable.auto.commit", "false");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }
}

/**
 * JSON转事件对象
 */
public class JsonToEventMapper implements MapFunction<String, Event> {

    private transient ObjectMapper objectMapper;

    @Override
    public Event map(String json) throws Exception {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        try {
            return objectMapper.readValue(json, Event.class);
        } catch (Exception e) {
            logger.error("Failed to parse JSON: {}", json, e);
            return null;
        }
    }
}

/**
 * 事件过滤
 */
public class EventFilter implements FilterFunction<Event> {

    @Override
    public boolean filter(Event event) throws Exception {
        // 过滤条件
        return event != null &&
               event.getUserId() != null &&
               event.getEventType() != null &&
               !"test".equals(event.getEventType());
    }
}

/**
 * 事件聚合器
 */
public class EventAggregator implements
    AggregateFunction<Event, EventAggregate, ProcessedEvent> {

    @Override
    public EventAggregate createAccumulator() {
        return new EventAggregate();
    }

    @Override
    public EventAggregate add(Event event, EventAggregate accumulator) {
        accumulator.setUserId(event.getUserId());
        accumulator.setCategory(event.getCategory());
        accumulator.setCount(accumulator.getCount() + 1);
        accumulator.setTotalAmount(accumulator.getTotalAmount().add(event.getAmount()));
        accumulator.setMaxAmount(accumulator.getMaxAmount().max(event.getAmount()));
        accumulator.setMinAmount(accumulator.getMinAmount().min(event.getAmount()));
        accumulator.setFirstTime(event.getTimestamp());
        accumulator.setLastTime(event.getTimestamp());
        return accumulator;
    }

    @Override
    public ProcessedEvent getResult(EventAggregate accumulator) {
        ProcessedEvent result = new ProcessedEvent();
        result.setUserId(accumulator.getUserId());
        result.setCategory(accumulator.getCategory());
        result.setCount(accumulator.getCount());
        result.setAvgAmount(accumulator.getTotalAmount().divide(
            BigDecimal.valueOf(accumulator.getCount()), 2, RoundingMode.HALF_UP));
        result.setMaxAmount(accumulator.getMaxAmount());
        result.setMinAmount(accumulator.getMinAmount());
        result.setStartTime(accumulator.getFirstTime());
        result.setEndTime(accumulator.getLastTime());
        result.setProcessTime(System.currentTimeMillis());
        return result;
    }

    @Override
    public EventAggregate merge(EventAggregate a, EventAggregate b) {
        a.setCount(a.getCount() + b.getCount());
        a.setTotalAmount(a.getTotalAmount().add(b.getTotalAmount()));
        a.setMaxAmount(a.getMaxAmount().max(b.getMaxAmount()));
        a.setMinAmount(a.getMinAmount().min(b.getMinAmount()));
        if (a.getFirstTime() == null || (b.getFirstTime() != null && b.getFirstTime().before(a.getFirstTime()))) {
            a.setFirstTime(b.getFirstTime());
        }
        if (a.getLastTime() == null || (b.getLastTime() != null && b.getLastTime().after(a.getLastTime()))) {
            a.setLastTime(b.getLastTime());
        }
        return a;
    }
}

/**
 * 实时统计聚合器
 */
public class StatisticsAggregator implements
    AggregateFunction<ProcessedEvent, StatisticsAggregate, StatisticsResult> {

    @Override
    public StatisticsAggregate createAccumulator() {
        return new StatisticsAggregate();
    }

    @Override
    public StatisticsAggregate add(ProcessedEvent event, StatisticsAggregate accumulator) {
        accumulator.setCategory(event.getCategory());
        accumulator.setEventCount(accumulator.getEventCount() + 1);
        accumulator.setUserCount(accumulator.getUserCount() + 1);
        accumulator.setTotalAmount(accumulator.getTotalAmount().add(event.getAvgAmount()));
        return accumulator;
    }

    @Override
    public StatisticsResult getResult(StatisticsAggregate accumulator) {
        StatisticsResult result = new StatisticsResult();
        result.setCategory(accumulator.getCategory());
        result.setEventCount(accumulator.getEventCount());
        result.setUserCount(accumulator.getUserCount());
        result.setAvgEventCountPerUser(BigDecimal.valueOf(
            accumulator.getEventCount()).divide(BigDecimal.valueOf(accumulator.getUserCount()), 2, RoundingMode.HALF_UP));
        result.setTotalAmount(accumulator.getTotalAmount());
        result.setAvgAmount(accumulator.getTotalAmount().divide(BigDecimal.valueOf(accumulator.getUserCount()), 2, RoundingMode.HALF_UP));
        result.setWindowStart(System.currentTimeMillis() - 60000); // 1分钟窗口
        result.setWindowEnd(System.currentTimeMillis());
        return result;
    }

    @Override
    public StatisticsAggregate merge(StatisticsAggregate a, StatisticsAggregate b) {
        a.setEventCount(a.getEventCount() + b.getEventCount());
        a.setUserCount(a.getUserCount() + b.getUserCount());
        a.setTotalAmount(a.getTotalAmount().add(b.getTotalAmount()));
        return a;
    }
}
```

#### 窗口计算实现

```java
/**
 * 窗口计算示例
 */
public class WindowComputationJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 时间窗口计算
        DataStream<SensorReading> sensorStream = env
            .addSource(new SensorSource())
            .assignTimestampsAndWatermarks(
                WatermarkStrategy.<SensorReading>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                    .withTimestampAssigner((event, timestamp) -> event.getTimestamp())
            );

        // 1. 滚动窗口 (Tumbling Window)
        DataStream<TumblingWindowResult> tumblingWindowResult = sensorStream
            .keyBy(SensorReading::getSensorId)
            .window(TumblingEventTimeWindows.of(Time.minutes(5)))
            .aggregate(new SensorAggregator());

        // 2. 滑动窗口 (Sliding Window)
        DataStream<SlidingWindowResult> slidingWindowResult = sensorStream
            .keyBy(SensorReading::getSensorId)
            .window(SlidingEventTimeWindows.of(Time.minutes(15), Time.minutes(5)))
            .aggregate(new SensorAggregator());

        // 3. 会话窗口 (Session Window)
        DataStream<SessionWindowResult> sessionWindowResult = sensorStream
            .keyBy(SensorReading::getSensorId)
            .window(EventTimeSessionWindows.withGap(Time.minutes(10)))
            .aggregate(new SensorAggregator());

        // 4. 计数窗口 (Count Window)
        DataStream<CountWindowResult> countWindowResult = sensorStream
            .keyBy(SensorReading::getSensorId)
            .countWindow(100)
            .aggregate(new SensorAggregator());

        // 5. 全局窗口 (Global Window)
        DataStream<GlobalWindowResult> globalWindowResult = sensorStream
            .window(GlobalWindows.create())
            .trigger(CountTrigger.of(1000))
            .aggregate(new SensorAggregator());

        // 6. 窗口函数
        sensorStream
            .keyBy(SensorReading::getSensorId)
            .timeWindow(Time.minutes(5))
            .process(new CustomProcessWindowFunction());

        // 7. 侧输出流 (Side Output)
        OutputTag<SensorReading> coldSensorTag = new OutputTag<>("cold-sensor");
        DataStream<SensorReading> normalStream = sensorStream
            .keyBy(SensorReading::getSensorId)
            .timeWindow(Time.minutes(5))
            .process(new TemperatureAlertFunction(coldSensorTag));

        DataStream<SensorReading> coldStream = normalStream
            .getSideOutput(coldSensorTag);

        env.execute("Window Computation Job");
    }
}

/**
 * 自定义处理窗口函数
 */
public class CustomProcessWindowFunction
    extends ProcessWindowFunction<SensorReading, WindowResult, String, TimeWindow> {

    @Override
    public void process(String key, ProcessWindowFunction<SensorReading, WindowResult, String, TimeWindow>.Context context,
                       Iterable<SensorReading> elements, Collector<WindowResult> out) throws Exception {

        long count = 0;
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (SensorReading element : elements) {
            count++;
            sum += element.getTemperature();
            min = Math.min(min, element.getTemperature());
            max = Math.max(max, element.getTemperature());
        }

        WindowResult result = new WindowResult();
        result.setSensorId(key);
        result.setCount(count);
        result.setAvgTemperature(sum / count);
        result.setMinTemperature(min);
        result.setMaxTemperature(max);
        result.setWindowStart(context.window().getStart());
        result.setWindowEnd(context.window().getEnd());

        out.collect(result);
    }
}

/**
 * 温度告警窗口函数
 */
public class TemperatureAlertFunction
    extends ProcessWindowFunction<SensorReading, SensorReading, String, TimeWindow> {

    private final OutputTag<SensorReading> coldSensorTag;

    public TemperatureAlertFunction(OutputTag<SensorReading> coldSensorTag) {
        this.coldSensorTag = coldSensorTag;
    }

    @Override
    public void process(String key,
                       ProcessWindowFunction<SensorReading, SensorReading, String, TimeWindow>.Context context,
                       Iterable<SensorReading> elements,
                       Collector<SensorReading> out) throws Exception {

        for (SensorReading element : elements) {
            if (element.getTemperature() < 10.0) {
                // 输出到侧输出流
                context.output(coldSensorTag, element);
            } else {
                // 正常数据输出
                out.collect(element);
            }
        }
    }
}
```

### 3. Flink SQL 开发

#### 流式SQL示例

```java
/**
 * 流式SQL作业
 */
public class StreamingSQLJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 启用Flink SQL
        EnvironmentSettings settings = EnvironmentSettings
            .newInstance()
            .useBlinkPlanner()
            .inStreamingMode()
            .build();

        TableEnvironment tableEnv = TableEnvironment.create(settings);

        // 创建Kafka源表
        tableEnv.executeSql("CREATE TABLE user_behavior (\n" +
            "  user_id STRING,\n" +
            "  event_type STRING,\n" +
            "  event_time TIMESTAMP(3),\n" +
            "  amount DECIMAL(10,2),\n" +
            "  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND\n" +
            ") WITH (\n" +
            "  'connector' = 'kafka',\n" +
            "  'topic' = 'user_behavior',\n" +
            "  'properties.bootstrap.servers' = 'kafka:9092',\n" +
            "  'properties.group.id' = 'flink-sql',\n" +
            "  'format' = 'json',\n" +
            "  'scan.startup.mode' = 'earliest-offset'\n" +
            ")");

        // 创建ClickHouse结果表
        tableEnv.executeSql("CREATE TABLE user_stats (\n" +
            "  window_start TIMESTAMP(3),\n" +
            "  window_end TIMESTAMP(3),\n" +
            "  user_id STRING,\n" +
            "  event_count BIGINT,\n" +
            "  total_amount DECIMAL(10,2),\n" +
            "  avg_amount DECIMAL(10,2)\n" +
            ") WITH (\n" +
            "  'connector' = 'clickhouse',\n" +
            "  'url' = 'jdbc:clickhouse://clickhouse:8123/basebackend',\n" +
            "  'table-name' = 'user_stats',\n" +
            "  'username' = 'admin',\n" +
            "  'password' = 'password123'\n" +
            ")");

        // 创建Redis结果表
        tableEnv.executeSql("CREATE TABLE user_realtime_metrics (\n" +
            "  user_id STRING,\n" +
            "  last_event_time TIMESTAMP(3),\n" +
            "  event_count BIGINT,\n" +
            "  total_amount DECIMAL(10,2)\n" +
            ") WITH (\n" +
            "  'connector' = 'redis',\n" +
            "  'host' = 'redis',\n" +
            "  'port' = '6379',\n" +
            "  'password' = 'redis123',\n" +
            "  'key-pattern' = 'user_metrics:{user_id}'\n" +
            ")");

        // 实时统计查询
        Table resultTable = tableEnv.sqlQuery(
            "SELECT\n" +
            "  TUMBLE_START(event_time, INTERVAL '5' MINUTE) as window_start,\n" +
            "  TUMBLE_END(event_time, INTERVAL '5' MINUTE) as window_end,\n" +
            "  user_id,\n" +
            "  COUNT(*) as event_count,\n" +
            "  SUM(amount) as total_amount,\n" +
            "  AVG(amount) as avg_amount\n" +
            "FROM user_behavior\n" +
            "GROUP BY TUMBLE(event_time, INTERVAL '5' MINUTE), user_id");

        // 输出到ClickHouse
        TableResult clickhouseResult = tableEnv.executeSql(
            "INSERT INTO user_stats " +
            "SELECT * FROM (" + resultTable.toString() + ")");

        // 输出到Redis (使用维表Join)
        Table realtimeMetrics = tableEnv.sqlQuery(
            "SELECT\n" +
            "  user_id,\n" +
            "  MAX(event_time) as last_event_time,\n" +
            "  COUNT(*) as event_count,\n" +
            "  SUM(amount) as total_amount\n" +
            "FROM user_behavior\n" +
            "GROUP BY user_id");

        TableResult redisResult = tableEnv.executeSql(
            "INSERT INTO user_realtime_metrics " +
            "SELECT * FROM (" + realtimeMetrics.toString() + ")");

        // 等待查询完成
        clickhouseResult.print();
        redisResult.print();

        // 实时Top-N查询
        Table topUsersTable = tableEnv.sqlQuery(
            "SELECT *\n" +
            "FROM (\n" +
            "  SELECT\n" +
            "    user_id,\n" +
            "    COUNT(*) as event_count,\n" +
            "    ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC) as rank\n" +
            "  FROM user_behavior\n" +
            "  GROUP BY user_id\n" +
            ")\n" +
            "WHERE rank <= 10");

        tableEnv.createTemporaryView("top_users", topUsersTable);

        // 实时会话分析
        Table sessionTable = tableEnv.sqlQuery(
            "SELECT\n" +
            "  user_id,\n" +
            "  SESSION_START(event_time, INTERVAL '30' MINUTE) as session_start,\n" +
            "  SESSION_END(event_time, INTERVAL '30' MINUTE) as session_end,\n" +
            "  COUNT(*) as event_count,\n" +
            "  SUM(amount) as total_amount\n" +
            "FROM user_behavior\n" +
            "GROUP BY SESSION(event_time, INTERVAL '30' MINUTE), user_id");

        env.execute("Streaming SQL Job");
    }
}

/**
 * 流式维表Join
 */
public class StreamingJoinJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        EnvironmentSettings settings = EnvironmentSettings
            .newInstance()
            .useBlinkPlanner()
            .inStreamingMode()
            .build();

        TableEnvironment tableEnv = TableEnvironment.create(settings);

        // 创建事实表 (行为数据)
        tableEnv.executeSql("CREATE TABLE fact_behavior (\n" +
            "  user_id STRING,\n" +
            "  event_type STRING,\n" +
            "  event_time TIMESTAMP(3),\n" +
            "  amount DECIMAL(10,2),\n" +
            "  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND\n" +
            ") WITH (\n" +
            "  'connector' = 'kafka',\n" +
            "  'topic' = 'behavior',\n" +
            "  'properties.bootstrap.servers' = 'kafka:9092',\n" +
            "  'format' = 'json'\n" +
            ")");

        // 创建维表 (用户信息)
        tableEnv.executeSql("CREATE TABLE dim_user (\n" +
            "  user_id STRING,\n" +
            "  user_name STRING,\n" +
            "  age INT,\n" +
            "  gender STRING,\n" +
            "  city STRING,\n" +
            "  update_time TIMESTAMP(3)\n" +
            ") WITH (\n" +
            "  'connector' = 'redis',\n" +
            "  'host' = 'redis',\n" +
            "  'port' = '6379',\n" +
            "  'password' = 'redis123',\n" +
            "  'key-pattern' = 'user:{user_id}'\n" +
            ")");

        // 维表Join查询
        Table joinedTable = tableEnv.sqlQuery(
            "SELECT\n" +
            "  f.user_id,\n" +
            "  u.user_name,\n" +
            "  u.age,\n" +
            "  u.gender,\n" +
            "  u.city,\n" +
            "  f.event_type,\n" +
            "  f.event_time,\n" +
            "  f.amount\n" +
            "FROM fact_behavior f\n" +
            "JOIN dim_user FOR SYSTEM_TIME AS OF f.event_time AS u\n" +
            "  ON f.user_id = u.user_id");

        // 实时聚合分析
        Table analysisTable = tableEnv.sqlQuery(
            "SELECT\n" +
            "  u.city,\n" +
            "  u.gender,\n" +
            "  COUNT(*) as event_count,\n" +
            "  SUM(f.amount) as total_amount,\n" +
            "  AVG(f.amount) as avg_amount,\n" +
            "  TUMBLE_START(f.event_time, INTERVAL '1' HOUR) as window_start\n" +
            "FROM fact_behavior f\n" +
            "JOIN dim_user FOR SYSTEM_TIME AS OF f.event_time AS u\n" +
            "  ON f.user_id = u.user_id\n" +
            "GROUP BY\n" +
            "  u.city,\n" +
            "  u.gender,\n" +
            "  TUMBLE(f.event_time, INTERVAL '1' HOUR)");

        tableEnv.createTemporaryView("city_gender_analysis", analysisTable);

        // 城市消费排行
        Table cityRanking = tableEnv.sqlQuery(
            "SELECT\n" +
            "  city,\n" +
            "  SUM(total_amount) as total_consumption,\n" +
            "  ROW_NUMBER() OVER (ORDER BY SUM(total_amount) DESC) as rank\n" +
            "FROM city_gender_analysis\n" +
            "GROUP BY city\n" +
            "ORDER BY total_consumption DESC\n" +
            "LIMIT 20");

        env.execute("Streaming Join Job");
    }
}
```

---

## 🔍 CEP 复杂事件处理

### 模式定义与匹配

```java
/**
 * CEP 复杂事件处理示例
 */
public class CEPProcessingJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 从Kafka读取事件流
        DataStream<Event> eventStream = env
            .addSource(new FlinkKafkaConsumer<>(
                "user-behavior",
                new EventDeserializationSchema(),
                getKafkaProperties()
            ))
            .assignTimestampsAndWatermarks(
                WatermarkStrategy.<Event>forBoundedOutOfOrderness(Duration.ofSeconds(10))
                    .withTimestampAssigner((event, timestamp) -> event.getTimestamp())
            );

        // 定义模式序列
        Pattern<Event, ?> pattern = Pattern.<Event>begin("start")
            .where(new SimpleCondition<Event>() {
                @Override
                public boolean filter(Event event) {
                    return "login".equals(event.getEventType());
                }
            })
            .next("middle")
            .where(new SimpleCondition<Event>() {
                @Override
                public boolean filter(Event event) {
                    return "purchase".equals(event.getEventType());
                }
            })
            .within(Time.minutes(30));

        // 应用模式
        PatternStream<Event> patternStream = CEP.pattern(eventStream, pattern);

        // 处理匹配结果
        DataStream<Alert> alerts = patternStream.process(
            new PatternProcessFunction<Event, Alert>() {
                @Override
                public void processMatch(Map<String, List<Event>> match,
                                       Context ctx,
                                       Collector<Alert> out) throws Exception {
                    Event loginEvent = match.get("start").get(0);
                    Event purchaseEvent = match.get("middle").get(0);

                    Alert alert = new Alert();
                    alert.setUserId(loginEvent.getUserId());
                    alert.setAlertType("PURCHASE_AFTER_LOGIN");
                    alert.setDescription("User " + loginEvent.getUserId() +
                        " purchased within 30 minutes of login");
                    alert.setEventTime(System.currentTimeMillis());
                    alert.setSeverity("INFO");

                    out.collect(alert);
                }
            });

        // 模式增量查询
        Pattern<Event, ?> incrementalPattern = Pattern.<Event>begin("start")
            .where(new SimpleCondition<Event>() {
                @Override
                public boolean filter(Event event) {
                    return "login".equals(event.getEventType());
                }
            })
            .followedByAny("middle")
            .where(new SimpleCondition<Event>() {
                @Override
                public boolean filter(Event event) {
                    return "browse".equals(event.getEventType());
                }
            })
            .times(2, 10);

        PatternStream<Event> incrementalPatternStream = CEP.pattern(eventStream, incrementalPattern);

        DataStream<Alert> incrementalAlerts = incrementalPatternStream.flatSelect(
            new FlatteningPatternSelectFunction<Event, Alert>() {
                @Override
                public void flatSelect(Map<String, List<Event>> match,
                                     Collector<Alert> out) throws Exception {
                    List<Event> startEvents = match.get("start");
                    List<Event> middleEvents = match.get("middle");

                    if (startEvents != null && !startEvents.isEmpty()) {
                        Event startEvent = startEvents.get(0);

                        Alert alert = new Alert();
                        alert.setUserId(startEvent.getUserId());
                        alert.setAlertType("MULTIPLE_BROWSE_AFTER_LOGIN");
                        alert.setDescription("User " + startEvent.getUserId() +
                            " browsed " + middleEvents.size() + " times after login");
                        alert.setEventTime(System.currentTimeMillis());
                        alert.setSeverity("WARNING");

                        out.collect(alert);
                    }
                }
            });

        // 组合所有告警
        DataStream<Alert> allAlerts = alerts.union(incrementalAlerts);

        // 输出告警
        allAlerts.addSink(new AlertSink());

        env.execute("CEP Processing Job");
    }

    private static Properties getKafkaProperties() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "kafka:9092");
        props.setProperty("group.id", "cep-processing");
        return props;
    }
}

/**
 * 交易异常检测CEP
 */
public class TransactionAnomalyCEP {

    public static DataStream<Alert> detectTransactionAnomalies(
            DataStream<Transaction> transactionStream) {

        // 定义交易异常模式
        Pattern<Transaction, ?> anomalyPattern = Pattern.<Transaction>begin("first")
            .where(new SimpleCondition<Transaction>() {
                @Override
                public boolean filter(Transaction transaction) {
                    return transaction.getAmount().compareTo(new BigDecimal("10000")) > 0;
                }
            })
            .next("second")
            .where(new SimpleCondition<Transaction>() {
                @Override
                public boolean filter(Transaction transaction) {
                    return transaction.getAmount().compareTo(new BigDecimal("10000")) > 0;
                }
            })
            .within(Time.minutes(5));

        PatternStream<Transaction> anomalyPatternStream =
            CEP.pattern(transactionStream.keyBy(Transaction::getUserId), anomalyPattern);

        return anomalyPatternStream.process(new PatternProcessFunction<Transaction, Alert>() {
            @Override
            public void processMatch(Map<String, List<Transaction>> match,
                                   Context ctx,
                                   Collector<Alert> out) throws Exception {

                Transaction first = match.get("first").get(0);
                Transaction second = match.get("second").get(0);

                Alert alert = new Alert();
                alert.setUserId(first.getUserId());
                alert.setAlertType("HIGH_FREQUENCY_LARGE_TRANSACTION");
                alert.setDescription("用户 " + first.getUserId() +
                    " 在5分钟内进行了两笔大额交易: " +
                    first.getAmount() + " 和 " + second.getAmount());
                alert.setEventTime(System.currentTimeMillis());
                alert.setSeverity("CRITICAL");

                out.collect(alert);
            }
        });
    }

    /**
     * 检测用户行为异常
     */
    public static DataStream<Alert> detectBehaviorAnomalies(
            DataStream<UserEvent> eventStream) {

        // 登录失败次数异常
        Pattern<UserEvent, ?> loginFailPattern = Pattern.<UserEvent>begin("failures")
            .where(new SimpleCondition<UserEvent>() {
                @Override
                public boolean filter(UserEvent event) {
                    return "login_failed".equals(event.getEventType());
                }
            })
            .times(5)
            .consecutive()
            .within(Time.minutes(10));

        PatternStream<UserEvent> loginFailPatternStream =
            CEP.pattern(eventStream.keyBy(UserEvent::getUserId), loginFailPattern);

        DataStream<Alert> loginAlerts = loginFailPatternStream.process(
            new PatternProcessFunction<UserEvent, Alert>() {
                @Override
                public void processMatch(Map<String, List<UserEvent>> match,
                                       Context ctx,
                                       Collector<Alert> out) throws Exception {

                    List<UserEvent> failures = match.get("failures");
                    String userId = failures.get(0).getUserId();

                    Alert alert = new Alert();
                    alert.setUserId(userId);
                    alert.setAlertType("EXCESSIVE_LOGIN_FAILURES");
                    alert.setDescription("用户 " + userId +
                        " 在10分钟内连续登录失败 " + failures.size() + " 次");
                    alert.setEventTime(System.currentTimeMillis());
                    alert.setSeverity("CRITICAL");

                    out.collect(alert);
                }
            });

        return loginAlerts;
    }
}
```

---

## 📊 实时数仓构建

### 1. 分层架构设计

```java
/**
 * 实时数仓分层服务
 */
@Service
public class RealTimeDataWarehouseService {

    @Autowired
    private FlinkTableService flinkTableService;

    @Autowired
    private ClickHouseService clickHouseService;

    @Autowired
    private HudiService hudiService;

    /**
     * 构建ODS层 (Operational Data Store)
     */
    @Transactional
    public void buildODSLayer() {
        // 创建原始数据表
        flinkTableService.executeSQL(
            "CREATE TABLE ods_user_behavior (\n" +
            "  user_id STRING,\n" +
            "  event_type STRING,\n" +
            "  event_time TIMESTAMP(3),\n" +
            "  event_timestamp AS TO_TIMESTAMP(event_time),\n" +
            "  amount DECIMAL(10,2),\n" +
            "  properties MAP<STRING, STRING>,\n" +
            "  WATERMARK FOR event_timestamp AS event_timestamp - INTERVAL '5' SECOND\n" +
            ") WITH (\n" +
            "  'connector' = 'kafka',\n" +
            "  'topic' = 'user_behavior_raw',\n" +
            "  'properties.bootstrap.servers' = 'kafka:9092',\n" +
            "  'format' = 'json',\n" +
            "  'json.timestamp-format' = 'yyyy-MM-dd HH:mm:ss',\n" +
            "  'scan.startup.mode' = 'earliest-offset'\n" +
            ")"
        );

        // 写入Hudi表
        flinkTableService.executeSQL(
            "CREATE TABLE ods_user_behavior_hudi (\n" +
            "  user_id STRING,\n" +
            "  event_type STRING,\n" +
            "  event_time TIMESTAMP(3),\n" +
            "  amount DECIMAL(10,2),\n" +
            "  properties MAP<STRING, STRING>,\n" +
            "  ts TIMESTAMP(3),\n" +
            "  PRIMARY KEY (user_id) NOT ENFORCED\n" +
            ") WITH (\n" +
            "  'connector' = 'hudi',\n" +
            "  'path' = 'hdfs://hadoop:9000/hudi/ods/user_behavior',\n" +
            "  'table.type' = 'COPY_ON_WRITE',\n" +
            "  'compaction.tasks' = '4',\n" +
            "  'compaction.async.enabled' = 'true'\n" +
            ")"
        );

        // ODS到Hudi的实时同步
        flinkTableService.executeSQL(
            "INSERT INTO ods_user_behavior_hudi\n" +
            "SELECT\n" +
            "  user_id,\n" +
            "  event_type,\n" +
            "  event_time,\n" +
            "  amount,\n" +
            "  properties,\n" +
            "  event_timestamp as ts\n" +
            "FROM ods_user_behavior"
        );
    }

    /**
     * 构建DWD层 (Data Warehouse Detail)
     */
    @Transactional
    public void buildDWDLayer() {
        // 创建明细层事实表
        flinkTableService.executeSQL(
            "CREATE TABLE dwd_user_behavior_detail (\n" +
            "  user_id STRING,\n" +
            "  session_id STRING,\n" +
            "  event_type STRING,\n" +
            "  event_time TIMESTAMP(3),\n" +
            "  page_id STRING,\n" +
            "  page_name STRING,\n" +
            "  action_id STRING,\n" +
            "  action_name STRING,\n" +
            "  amount DECIMAL(10,2),\n" +
            "  currency STRING,\n" +
            "  traffic_source STRING,\n" +
            "  device_type STRING,\n" +
            "  os STRING,\n" +
            "  browser STRING,\n" +
            "  ip STRING,\n" +
            "  province STRING,\n" +
            "  city STRING,\n" +
            "  ts TIMESTAMP(3),\n" +
            "  event_date AS DATE_FORMAT(event_time, 'yyyy-MM-dd'),\n" +
            "  event_hour AS DATE_FORMAT(event_time, 'HH'),\n" +
            "  WATERMARK FOR ts AS ts - INTERVAL '5' SECOND\n" +
            ") WITH (\n" +
            "  'connector' = 'clickhouse',\n" +
            "  'url' = 'jdbc:clickhouse://clickhouse:8123/basebackend',\n" +
            "  'table-name' = 'dwd_user_behavior_detail',\n" +
            "  'username' = 'admin',\n" +
            "  'password' = 'password123'\n" +
            ")"
        );

        // 创建维表
        flinkTableService.executeSQL(
            "CREATE TABLE dwd_dim_user (\n" +
            "  user_id STRING,\n" +
            "  register_time TIMESTAMP(3),\n" +
            "  register_date STRING,\n" +
            "  user_name STRING,\n" +
            "  phone STRING,\n" +
            "  email STRING,\n" +
            "  gender STRING,\n" +
            "  birthday DATE,\n" +
            "  age INT,\n" +
            "  city STRING,\n" +
            "  province STRING,\n" +
            "  user_level STRING,\n" +
            "  user_status STRING,\n" +
            "  ts TIMESTAMP(3),\n" +
            "  event_date AS DATE_FORMAT(ts, 'yyyy-MM-dd'),\n" +
            "  WATERMARK FOR ts AS ts - INTERVAL '5' SECOND\n" +
            ") WITH (\n" +
            "  'connector' = 'clickhouse',\n" +
            "  'url' = 'jdbc:clickhouse://clickhouse:8123/basebackend',\n" +
            "  'table-name' = 'dwd_dim_user',\n" +
            "  'username' = 'admin',\n" +
            "  'password' = 'password123'\n" +
            ")"
        );

        // ODS到DWD的数据处理
        flinkTableService.executeSQL(
            "INSERT INTO dwd_user_behavior_detail\n" +
            "SELECT\n" +
            "  user_id,\n" +
            "  CAST(properties['session_id'] AS STRING) as session_id,\n" +
            "  event_type,\n" +
            "  event_time,\n" +
            "  CAST(properties['page_id'] AS STRING) as page_id,\n" +
            "  CAST(properties['page_name'] AS STRING) as page_name,\n" +
            "  CAST(properties['action_id'] AS STRING) as action_id,\n" +
            "  CAST(properties['action_name'] AS STRING) as action_name,\n" +
            "  amount,\n" +
            "  CAST(properties['currency'] AS STRING) as currency,\n" +
            "  CAST(properties['traffic_source'] AS STRING) as traffic_source,\n" +
            "  CAST(properties['device_type'] AS STRING) as device_type,\n" +
            "  CAST(properties['os'] AS STRING) as os,\n" +
            "  CAST(properties['browser'] AS STRING) as browser,\n" +
            "  CAST(properties['ip'] AS STRING) as ip,\n" +
            "  CAST(properties['province'] AS STRING) as province,\n" +
            "  CAST(properties['city'] AS STRING) as city,\n" +
            "  event_timestamp as ts\n" +
            "FROM ods_user_behavior\n" +
            "WHERE event_type IS NOT NULL"
        );
    }

    /**
     * 构建DWS层 (Data Warehouse Service)
     */
    @Transactional
    public void buildDWSLayer() {
        // 创建服务层主题宽表
        flinkTableService.executeSQL(
            "CREATE TABLE dws_user_behavior_summary (\n" +
            "  user_id STRING,\n" +
            "  event_date STRING,\n" +
            "  event_hour STRING,\n" +
            "  pv_count BIGINT,\n" +
            "  uv_count BIGINT,\n" +
            "  click_count BIGINT,\n" +
            "  purchase_count BIGINT,\n" +
            "  purchase_amount DECIMAL(15,2),\n" +
            "  bounce_rate DECIMAL(5,2),\n" +
            "  avg_session_duration INTERVAL SECOND,\n" +
            "  ts TIMESTAMP(3)\n" +
            ") WITH (\n" +
            "  'connector' = 'clickhouse',\n" +
            "  'url' = 'jdbc:clickhouse://clickhouse:8123/basebackend',\n" +
            "  'table-name' = 'dws_user_behavior_summary',\n" +
            "  'username' = 'admin',\n" +
            "  'password' = 'password123'\n" +
            ")"
        );

        // 宽表Join和聚合
        flinkTableService.executeSQL(
            "INSERT INTO dws_user_behavior_summary\n" +
            "SELECT\n" +
            "  d.user_id,\n" +
            "  d.event_date,\n" +
            "  d.event_hour,\n" +
            "  COUNT(CASE WHEN d.event_type = 'page_view' THEN 1 END) as pv_count,\n" +
            "  COUNT(DISTINCT d.user_id) as uv_count,\n" +
            "  COUNT(CASE WHEN d.event_type = 'click' THEN 1 END) as click_count,\n" +
            "  COUNT(CASE WHEN d.event_type = 'purchase' THEN 1 END) as purchase_count,\n" +
            "  SUM(CASE WHEN d.event_type = 'purchase' THEN d.amount ELSE 0 END) as purchase_amount,\n" +
            "  0.0 as bounce_rate, -- 需要会话计算\n" +
            "  INTERVAL '0' SECOND as avg_session_duration, -- 需要会话分析\n" +
            "  MAX(d.ts) as ts\n" +
            "FROM dwd_user_behavior_detail d\n" +
            "GROUP BY\n" +
            "  d.user_id,\n" +
            "  d.event_date,\n" +
            "  d.event_hour"
        );

        // 创建地区主题表
        flinkTableService.executeSQL(
            "CREATE TABLE dws_region_statistics (\n" +
            "  province STRING,\n" +
            "  city STRING,\n" +
            "  event_date STRING,\n" +
            "  event_hour STRING,\n" +
            "  pv_count BIGINT,\n" +
            "  uv_count BIGINT,\n" +
            "  purchase_count BIGINT,\n" +
            "  purchase_amount DECIMAL(15,2),\n" +
            "  conversion_rate DECIMAL(5,2),\n" +
            "  ts TIMESTAMP(3)\n" +
            ") WITH (\n" +
            "  'connector' = 'clickhouse',\n" +
            "  'url' = 'jdbc:clickhouse://clickhouse:8123/basebackend',\n" +
            "  'table-name' = 'dws_region_statistics',\n" +
            "  'username' = 'admin',\n" +
            "  'password' = 'password123'\n" +
            ")"
        );

        flinkTableService.executeSQL(
            "INSERT INTO dws_region_statistics\n" +
            "SELECT\n" +
            "  d.province,\n" +
            "  d.city,\n" +
            "  d.event_date,\n" +
            "  d.event_hour,\n" +
            "  COUNT(CASE WHEN d.event_type = 'page_view' THEN 1 END) as pv_count,\n" +
            "  COUNT(DISTINCT d.user_id) as uv_count,\n" +
            "  COUNT(CASE WHEN d.event_type = 'purchase' THEN 1 END) as purchase_count,\n" +
            "  SUM(CASE WHEN d.event_type = 'purchase' THEN d.amount ELSE 0 END) as purchase_amount,\n" +
            "  CASE WHEN COUNT(CASE WHEN d.event_type = 'page_view' THEN 1 END) > 0\n" +
            "    THEN ROUND(COUNT(CASE WHEN d.event_type = 'purchase' THEN 1 END) * 100.0 / \n" +
            "               COUNT(CASE WHEN d.event_type = 'page_view' THEN 1 END), 2)\n" +
            "    ELSE 0 END as conversion_rate,\n" +
            "  MAX(d.ts) as ts\n" +
            "FROM dwd_user_behavior_detail d\n" +
            "GROUP BY\n" +
            "  d.province,\n" +
            "  d.city,\n" +
            "  d.event_date,\n" +
            "  d.event_hour"
        );
    }

    /**
     * 构建ADS层 (Application Data Service)
     */
    @Transactional
    public void buildADSLayer() {
        // 创建应用服务层聚合表
        flinkTableService.executeSQL(
            "CREATE TABLE ads_user_portrait (\n" +
            "  user_id STRING PRIMARY KEY,\n" +
            "  total_pv BIGINT,\n" +
            "  total_purchase_amount DECIMAL(15,2),\n" +
            "  avg_purchase_amount DECIMAL(10,2),\n" +
            "  last_purchase_time TIMESTAMP(3),\n" +
            "  purchase_frequency DOUBLE,\n" +
            "  favorite_category STRING,\n" +
            "  favorite_brand STRING,\n" +
            "  rfm_score STRING,\n" +
            "  user_level STRING,\n" +
            "  churn_risk DOUBLE,\n" +
            "  update_time TIMESTAMP(3)\n" +
            ") WITH (\n" +
            "  'connector' = 'redis',\n" +
            "  'host' = 'redis',\n" +
            "  'port' = '6379',\n" +
            "  'password' = 'redis123',\n" +
            "  'key-pattern' = 'user_portrait:{user_id}'\n" +
            ")"
        );

        // RFM分析
        flinkTableService.executeSQL(
            "INSERT INTO ads_user_portrait\n" +
            "SELECT\n" +
            "  d.user_id,\n" +
            "  COUNT(CASE WHEN d.event_type = 'page_view' THEN 1 END) as total_pv,\n" +
            "  SUM(CASE WHEN d.event_type = 'purchase' THEN d.amount ELSE 0 END) as total_purchase_amount,\n" +
            "  CASE WHEN COUNT(CASE WHEN d.event_type = 'purchase' THEN 1 END) > 0\n" +
            "    THEN ROUND(SUM(CASE WHEN d.event_type = 'purchase' THEN d.amount ELSE 0 END) / \n" +
            "               COUNT(CASE WHEN d.event_type = 'purchase' THEN 1 END), 2)\n" +
            "    ELSE 0 END as avg_purchase_amount,\n" +
            "  MAX(CASE WHEN d.event_type = 'purchase' THEN d.event_time END) as last_purchase_time,\n" +
            "  COUNT(CASE WHEN d.event_type = 'purchase' THEN 1 END) as purchase_frequency,\n" +
            "  'category' as favorite_category, -- 需要分析计算\n" +
            "  'brand' as favorite_brand, -- 需要分析计算\n" +
            "  'R5F5M5' as rfm_score, -- 需要RFM计算\n" +
            "  'level_1' as user_level, -- 需要分级计算\n" +
            "  0.1 as churn_risk, -- 需要预测模型\n" +
            "  CURRENT_TIMESTAMP as update_time\n" +
            "FROM dwd_user_behavior_detail d\n" +
            "GROUP BY d.user_id"
        );
    }
}
```

### 2. ClickHouse 优化配置

```xml
<!-- clickhouse-server配置 -->
<?xml version="1.0" encoding="UTF-8"?>
<yandex>
    <remote_servers>
        <basebackend_cluster>
            <shard>
                <replica>
                    <host>clickhouse-1</host>
                    <port>9000</port>
                </replica>
                <replica>
                    <host>clickhouse-2</host>
                    <port>9000</port>
                </replica>
            </shard>
        </basebackend_cluster>
    </remote_servers>

    <zookeeper>
        <node>
            <host>zookeeper-1</host>
            <port>2181</port>
        </node>
        <node>
            <host>zookeeper-2</host>
            <port>2181</port>
        </node>
        <node>
            <host>zookeeper-3</host>
            <port>2181</port>
        </node>
    </zookeeper>

    <macros>
        <layer>02</layer>
        <shard>01</shard>
        <replica>clickhouse-01</replica>
    </macros>

    <networks>
        <ip>127.0.0.1</ip>
        <ip>10.0.0.0/8</ip>
    </networks>

    <clickhouse>
        <path>/var/lib/clickhouse/</path>
        <tmp_path>/var/lib/clickhouse/tmp/</path>
        <user_files_path>/var/lib/clickhouse/user_files/</path>
        <access_control_path>/var/lib/clickhouse/access/</path>
        <users_config>users.xml</users_config>
        <default_profile>default</default_profile>
        <custom_settings_prefixes></custom_settings_prefixes>
        <display_name>BaseBackend ClickHouse</display_name>
        <description>Real-time Analytics Database</description>
        <remote_url_allow_hosts></remote_url_allow_hosts>
    </clickhouse>

    <default_database>basebackend</default_database>

    <format_schema_path>/var/lib/clickhouse/format_schemas/</format_schema_path>

    <!-- Query Log配置 -->
    <query_log>
        <database>system</database>
        <table>query_log</table>
        <engine>Engine = MergeTree\nPARTITION BY toYYYYMM(event_date)\nORDER BY (event_date, query_start_time_microseconds)\nSAMPLE BY query_start_time_microseconds\nTTL event_date + INTERVAL 2 WEEK DELETE</engine>
        <flush_interval_milliseconds>7500</flush_interval_milliseconds>
    </query_log>

    <!-- 慢查询日志 -->
    <query_thread_log>
        <database>system</database>
        <table>query_thread_log</table>
        <engine>Engine = MergeTree\nPARTITION BY toYYYYMM(event_date)\nORDER BY (event_date, query_start_time_microseconds)\nSAMPLE BY query_start_time_microseconds\nTTL event_date + INTERVAL 2 WEEK DELETE</engine>
        <flush_interval_milliseconds>7500</flush_interval_milliseconds>
    </query_thread_log>

    <!-- Part Log配置 -->
    <part_log>
        <database>system</database>
        <table>part_log</table>
        <engine>Engine = MergeTree\nPARTITION BY toYYYYMM(event_date)\nORDER BY (event_date, event_time)\nSAMPLE BY event_time\nTTL event_date + INTERVAL 2 WEEK DELETE</engine>
        <flush_interval_milliseconds>7500</flush_interval_milliseconds>
    </part_log>

    <!-- 性能配置 -->
    <max_connections>4096</max_connections>
    <max_concurrent_queries>100</max_concurrent_queries>
    <max_server_memory_usage>0</max_server_memory_usage>
    <max_server_memory_usage_to_ram_ratio>0.8</max_server_memory_usage_to_ram_ratio>

    <!-- 缓存配置 -->
    < uncompressed_cache_size>8589934592</uncompressed_cache_size>
    < mark_cache_size>5368709120</mark_cache_size>
    <max_result_cache_size>0</max_result_cache_size>

    <!-- 查询限制 -->
    <max_query_size>1000000</max_query_size>
    <max_result_rows>100000</max_result_rows>
    <max_result_bytes>104857600</max_result_bytes>
    <result_overflow_mode=break>break</result_overflow_mode>

    <!-- 分布式查询 -->
    <max_execution_time>300</max_execution_time>
    <max_execution_time_overflow_mode=break>break</max_execution_time_overflow_mode>

    <max_memory_usage>10000000000</max_memory_usage>
    <max_memory_usage_for_user>0</max_memory_usage_for_user>
    <max_memory_usage_for_all_queries>0</max_memory_usage_for_all_queries>

    <!-- MergeTree引擎配置 -->
    <merge_tree>
        <parts_to_delay_insert>300</parts_to_delay_insert>
        <parts_to_throw_insert>600</parts_to_throw_insert>
        <max_suspicious_broken_parts>10</max_suspicious_broken_parts>
        <max_suspicious_broken_parts_bytes>1073741824</max_suspicious_broken_parts_bytes>
        <parts_to_merge_at_once>10</parts_to_merge_at_once>
        <max_files_to_merge_at_once>50</max_files_to_merge_at_once>
        <merge_with_ttl_timeout>86400</merge_with_ttl_timeout>
        <ttl_only_drop_parts>0</ttl_only_drop_parts>
        <remote_fs_expiration_time>3600</remote_fs_expiration_time>
        <max_bytes_to_merge_at_max_space_in_pool>107374182400</max_bytes_to_merge_at_max_space_in_pool>
    </merge_tree>

    <!-- 异步写入配置 -->
    <async_insert>
        <wait_for_async_insert>1</wait_for_async_insert>
        <wait_for_async_insert_timeout>600</wait_for_async_insert_timeout>
    </async_insert>

    <!-- HTTP服务器配置 -->
    <http_server_max_request_size>100000000</http_server_max_request_size>

    <!-- Kafka配置 -->
    <kafka>
        <auto_offset_reset>earliest</auto_offset_reset>
        <retry_backoff_ms>250</retry_backoff_ms>
        <error_on_eof>1</error_on_eof>
        <compression_codec>gzip</compression_codec>
        <max_poll_interval_ms>300000</max_poll_interval_ms>
        <session_timeout_ms>10000</session_timeout_ms>
        <receive_buffer_size>262144</receive_buffer_size>
        <max_in_flight_requests>10</max_in_flight_requests>
    </kafka>
</yandex>
```

---

## 📈 实时报表系统

### 1. 大屏展示

```vue
<template>
  <div class="realtime-dashboard">
    <!-- 顶部KPI卡片 -->
    <el-row :gutter="20" class="kpi-cards">
      <el-col :span="6">
        <div class="kpi-card">
          <div class="kpi-icon realtime-icon">
            <i class="el-icon-odometer"></i>
          </div>
          <div class="kpi-content">
            <h3>{{ realtimeMetrics.totalPV }}</h3>
            <p>实时PV</p>
            <span class="kpi-trend up">
              <i class="el-icon-top"></i>
              {{ realtimeMetrics.pvGrowth }}%
            </span>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="kpi-card">
          <div class="kpi-icon user-icon">
            <i class="el-icon-user"></i>
          </div>
          <div class="kpi-content">
            <h3>{{ realtimeMetrics.totalUV }}</h3>
            <p>实时UV</p>
            <span class="kpi-trend up">
              <i class="el-icon-top"></i>
              {{ realtimeMetrics.uvGrowth }}%
            </span>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="kpi-card">
          <div class="kpi-icon revenue-icon">
            <i class="el-icon-money"></i>
          </div>
          <div class="kpi-content">
            <h3>¥{{ realtimeMetrics.totalRevenue }}</h3>
            <p>实时营收</p>
            <span class="kpi-trend up">
              <i class="el-icon-top"></i>
              {{ realtimeMetrics.revenueGrowth }}%
            </span>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="kpi-card">
          <div class="kpi-icon conversion-icon">
            <i class="el-icon-data-analysis"></i>
          </div>
          <div class="kpi-content">
            <h3>{{ realtimeMetrics.conversionRate }}%</h3>
            <p>转化率</p>
            <span class="kpi-trend down">
              <i class="el-icon-bottom"></i>
              {{ realtimeMetrics.conversionChange }}%
            </span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 实时趋势图表 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card>
          <div slot="header">
            <span>实时流量趋势</span>
            <el-button-group style="float: right;">
              <el-button :type="timeRange === '1h' ? 'primary' : ''" @click="changeTimeRange('1h')">1小时</el-button>
              <el-button :type="timeRange === '24h' ? 'primary' : ''" @click="changeTimeRange('24h')">24小时</el-button>
              <el-button :type="timeRange === '7d' ? 'primary' : ''" @click="changeTimeRange('7d')">7天</el-button>
            </el-button-group>
          </div>
          <div id="realtimeTrendChart" style="height: 350px;"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <div slot="header">
            <span>地区分布TOP5</span>
          </div>
          <div id="regionChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时排行榜 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card>
          <div slot="header">
            <span>实时销量TOP10</span>
            <el-button type="text" @click="refreshRanking">刷新</el-button>
          </div>
          <el-table :data="salesRanking" height="300">
            <el-table-column prop="rank" label="排名" width="60">
              <template slot-scope="scope">
                <span class="ranking" :class="getRankClass(scope.row.rank)">
                  {{ scope.row.rank }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="商品名称"></el-table-column>
            <el-table-column prop="salesCount" label="销量" width="100"></el-table-column>
            <el-table-column prop="salesAmount" label="销售额" width="120">
              <template slot-scope="scope">
                ¥{{ scope.row.salesAmount.toLocaleString() }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <div slot="header">
            <span>实时流量来源</span>
          </div>
          <div id="trafficSourceChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时告警 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <el-card>
          <div slot="header">
            <span>实时告警</span>
            <el-badge :value="alerts.length" class="item">
              <el-button size="small">告警数量</el-button>
            </el-badge>
          </div>

          <div class="alerts-container">
            <div v-for="alert in alerts" :key="alert.id" class="alert-item" :class="alert.severity">
              <div class="alert-icon">
                <i :class="getAlertIcon(alert.severity)"></i>
              </div>
              <div class="alert-content">
                <h4>{{ alert.title }}</h4>
                <p>{{ alert.description }}</p>
                <span class="alert-time">{{ formatTime(alert.timestamp) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapActions, mapGetters } from 'vuex'
import * as echarts from 'echarts'

export default {
  name: 'RealtimeDashboard',

  data() {
    return {
      timeRange: '1h',
      realtimeTrendChart: null,
      regionChart: null,
      trafficSourceChart: null,
      updateInterval: null
    }
  },

  computed: {
    ...mapGetters('dashboard', [
      'realtimeMetrics',
      'salesRanking',
      'alerts'
    ])
  },

  mounted() {
    this.initCharts()
    this.startRealtimeUpdate()

    // 窗口大小改变时重绘图表
    window.addEventListener('resize', this.handleResize)
  },

  beforeDestroy() {
    this.stopRealtimeUpdate()
    window.removeEventListener('resize', this.handleResize)

    if (this.realtimeTrendChart) {
      this.realtimeTrendChart.dispose()
    }
    if (this.regionChart) {
      this.regionChart.dispose()
    }
    if (this.trafficSourceChart) {
      this.trafficSourceChart.dispose()
    }
  },

  methods: {
    ...mapActions('dashboard', [
      'fetchRealtimeMetrics',
      'fetchSalesRanking',
      'fetchAlerts',
      'updateTimeRange'
    ]),

    // 初始化图表
    initCharts() {
      this.realtimeTrendChart = echarts.init(document.getElementById('realtimeTrendChart'))
      this.regionChart = echarts.init(document.getElementById('regionChart'))
      this.trafficSourceChart = echarts.init(document.getElementById('trafficSourceChart'))

      this.loadRealtimeTrendData()
      this.loadRegionData()
      this.loadTrafficSourceData()
    },

    // 加载实时趋势数据
    async loadRealtimeTrendData() {
      try {
        const data = await this.$http.get('/api/realtime/trend', {
          params: { timeRange: this.timeRange }
        })

        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'cross'
            }
          },
          legend: {
            data: ['PV', 'UV', '营收']
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            boundaryGap: false,
            data: data.timeLabels
          },
          yAxis: [
            {
              type: 'value',
              name: '访问量',
              axisLabel: {
                formatter: '{value}'
              }
            },
            {
              type: 'value',
              name: '营收',
              axisLabel: {
                formatter: '¥{value}'
              }
            }
          ],
          series: [
            {
              name: 'PV',
              type: 'line',
              smooth: true,
              data: data.pvData,
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
                  { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
                ])
              },
              lineStyle: {
                color: '#409EFF'
              }
            },
            {
              name: 'UV',
              type: 'line',
              smooth: true,
              data: data.uvData,
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
                  { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
                ])
              },
              lineStyle: {
                color: '#67C23A'
              }
            },
            {
              name: '营收',
              type: 'line',
              yAxisIndex: 1,
              smooth: true,
              data: data.revenueData,
              lineStyle: {
                color: '#E6A23C'
              }
            }
          ]
        }

        this.realtimeTrendChart.setOption(option)
      } catch (error) {
        console.error('加载实时趋势数据失败', error)
      }
    },

    // 加载地区数据
    async loadRegionData() {
      try {
        const data = await this.$http.get('/api/realtime/region')

        const option = {
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
          },
          series: [
            {
              name: '地区分布',
              type: 'pie',
              radius: ['40%', '70%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#fff',
                borderWidth: 2
              },
              label: {
                show: true,
                formatter: '{b}\n{d}%'
              },
              data: data.map((item, index) => ({
                value: item.pv,
                name: item.region,
                itemStyle: {
                  color: this.getRegionColor(index)
                }
              }))
            }
          ]
        }

        this.regionChart.setOption(option)
      } catch (error) {
        console.error('加载地区数据失败', error)
      }
    },

    // 加载流量来源数据
    async loadTrafficSourceData() {
      try {
        const data = await this.$http.get('/api/realtime/traffic-source')

        const option = {
          tooltip: {
            trigger: 'item'
          },
          legend: {
            orient: 'vertical',
            left: 'left'
          },
          series: [
            {
              name: '流量来源',
              type: 'pie',
              radius: '50%',
              data: data.map(item => ({
                value: item.pv,
                name: item.source
              })),
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
              }
            }
          ]
        }

        this.trafficSourceChart.setOption(option)
      } catch (error) {
        console.error('加载流量来源数据失败', error)
      }
    },

    // 开始实时更新
    startRealtimeUpdate() {
      this.updateInterval = setInterval(() => {
        this.fetchRealtimeMetrics()
        this.fetchSalesRanking()
        this.fetchAlerts()

        // 更新图表数据
        this.loadRealtimeTrendData()
        this.loadRegionData()
        this.loadTrafficSourceData()
      }, 5000) // 每5秒更新一次
    },

    // 停止实时更新
    stopRealtimeUpdate() {
      if (this.updateInterval) {
        clearInterval(this.updateInterval)
        this.updateInterval = null
      }
    },

    // 改变时间范围
    changeTimeRange(range) {
      this.timeRange = range
      this.updateTimeRange(range)
      this.loadRealtimeTrendData()
    },

    // 刷新排行榜
    refreshRanking() {
      this.fetchSalesRanking()
    },

    // 处理窗口大小改变
    handleResize() {
      if (this.realtimeTrendChart) {
        this.realtimeTrendChart.resize()
      }
      if (this.regionChart) {
        this.regionChart.resize()
      }
      if (this.trafficSourceChart) {
        this.trafficSourceChart.resize()
      }
    },

    // 获取排名样式类
    getRankClass(rank) {
      if (rank <= 3) return 'top-three'
      return ''
    },

    // 获取地区颜色
    getRegionColor(index) {
      const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
      return colors[index % colors.length]
    },

    // 获取告警图标
    getAlertIcon(severity) {
      const icons = {
        CRITICAL: 'el-icon-warning',
        WARNING: 'el-icon-info',
        INFO: 'el-icon-question'
      }
      return icons[severity] || 'el-icon-info'
    },

    // 格式化时间
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleTimeString()
    }
  }
}
</script>

<style scoped>
.realtime-dashboard {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.kpi-cards {
  margin-bottom: 20px;
}

.kpi-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  color: white;
}

.kpi-card .kpi-icon {
  font-size: 48px;
  margin-right: 20px;
}

.kpi-card .kpi-content h3 {
  margin: 0;
  font-size: 32px;
  font-weight: bold;
}

.kpi-card .kpi-content p {
  margin: 5px 0;
  font-size: 14px;
  opacity: 0.9;
}

.kpi-trend {
  font-size: 14px;
  font-weight: bold;
}

.kpi-trend.up {
  color: #67C23A;
}

.kpi-trend.down {
  color: #F56C6C;
}

.charts-row {
  margin-bottom: 20px;
}

.ranking {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: #909399;
  color: white;
  border-radius: 50%;
  font-weight: bold;
}

.ranking.top-three {
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);
}

.alerts-container {
  max-height: 300px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 8px;
  border-left: 4px solid #909399;
  background: white;
}

.alert-item.CRITICAL {
  border-left-color: #F56C6C;
  background: #FEF0F0;
}

.alert-item.WARNING {
  border-left-color: #E6A23C;
  background: #FDF6EC;
}

.alert-item.INFO {
  border-left-color: #409EFF;
  background: #ECF5FF;
}

.alert-icon {
  font-size: 24px;
  margin-right: 15px;
}

.alert-content h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
}

.alert-content p {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #606266;
}

.alert-time {
  font-size: 12px;
  color: #909399;
}
</style>
```

---

## 📚 参考资料

1. [Apache Flink 官方文档](https://flink.apache.org/)
2. [Flink SQL 开发指南](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/)
3. [ClickHouse 官方文档](https://clickhouse.com/)
4. [Apache Hudi 官方文档](https://hudi.apache.org/)
5. [Flink CDC 文档](https://ververica.github.io/flink-cdc-connectors/)

---

## 📋 实施检查清单

### Flink开发
- [ ] Flink集群部署完成
- [ ] DataStream API作业开发
- [ ] Flink SQL作业开发
- [ ] CEP模式匹配实现
- [ ] 窗口计算实现

### 实时数仓
- [ ] ODS层建设完成
- [ ] DWD层明细数据
- [ ] DWS层主题宽表
- [ ] ADS层应用服务
- [ ] ClickHouse优化配置

### 实时报表
- [ ] 实时大屏开发
- [ ] KPI指标监控
- [ ] 实时排行榜
- [ ] 告警推送
- [ ] 移动端适配

### 数据采集
- [ ] Kafka消息队列
- [ ] Flink CDC采集
- [ ] 日志采集
- [ ] 数据清洗

### 存储优化
- [ ] ClickHouse集群
- [ ] Redis缓存
- [ ] Hudi数据湖
- [ ] 索引优化

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-15
**状态：** 📋 指南完成，准备实施

**加油喵～ 实时计算平台即将完成！** ฅ'ω'ฅ
