fix grafana dashboards, run app and test e2e

Below is a clean, production-grade approach using Micrometer → Prometheus → Grafana, with annotations and MQ-specific caveats (IBM MQ).

I’ll keep it practical, not theoretical.

1. The right stack (Spring Boot standard)

Spring Boot observability stack:

Spring Boot App
└─ Micrometer (metrics API)
└─ Prometheus registry
└─ Prometheus
└─ Grafana


You do not write Prometheus code directly.
You instrument with Micrometer.

2. Dependencies
   Maven
   <dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>


That’s enough.

3. Actuator & Prometheus endpoint
   management:
   endpoints:
   web:
   exposure:
   include: health,info,prometheus
   metrics:
   tags:
   application: order-service


Prometheus will scrape:

/actuator/prometheus

4. Metrics you want for MQ consumers

For TPS + latency, you want:

Metric	Micrometer type	Purpose
Messages received	Counter	TPS
Processing time	Timer	Latency
Failures	Counter	Error rate
In-flight messages	Gauge (optional)	Backpressure
5. Annotation-based metrics (recommended)
   TPS + latency with @Timed
   @Component
   public class MqMessageListener {

   @Timed(
   value = "mq.message.processing",
   description = "Time spent processing MQ messages",
   histogram = true
   )
   @Counted(
   value = "mq.message.received",
   description = "Number of MQ messages received"
   )
   public void onMessage(Message message) {
   process(message);
   }
   }


What you get:

mq_message_processing_seconds_*

mq_message_received_total

Prometheus-friendly by default.

6. Labeling (VERY important)

Always add tags, otherwise metrics are useless in Grafana.

@Timed(
value = "mq.message.processing",
extraTags = {
"queue", "ORDER.IN",
"system", "ibm-mq"
},
histogram = true
)


Now you can slice by:

queue

system

app

instance

7. IBM MQ specifics (listener container)

If you’re using Spring JMS with IBM MQ:

@JmsListener(destination = "ORDER.IN")
@Timed(
value = "mq.message.processing",
extraTags = {"queue", "ORDER.IN"},
histogram = true
)
public void onMessage(String payload) {
...
}


✔ This measures business processing, not JMS internals
✔ Exactly what you want

8. Handling concurrency correctly

If you have concurrent consumers:

spring:
jms:
listener:
concurrency: 5


Micrometer:

Aggregates across threads

Prometheus calculates TPS correctly

No extra work needed.

9. Manual metrics (when annotations are not enough)

For advanced cases (ack timing, partial failures):

@Component
public class MqMetrics {

    private final Counter received;
    private final Timer processing;

    public MqMetrics(MeterRegistry registry) {
        this.received = Counter.builder("mq.message.received")
            .tag("queue", "ORDER.IN")
            .register(registry);

        this.processing = Timer.builder("mq.message.processing")
            .tag("queue", "ORDER.IN")
            .publishPercentileHistogram()
            .register(registry);
    }

    public <T> T record(Supplier<T> supplier) {
        received.increment();
        return processing.record(supplier);
    }
}


Usage:

metrics.record(() -> process(message));

10. TPS in Prometheus (query)
    sum(
    rate(mq_message_received_total[1m])
    )


Per queue:

sum by (queue)(
rate(mq_message_received_total[1m])
)

11. Latency in Prometheus

P95:

histogram_quantile(
0.95,
sum by (le) (
rate(mq_message_processing_seconds_bucket[5m])
)
)

12. Grafana dashboard panels (minimum)

You want at least:

Messages/sec

P50 / P95 / P99 latency

Error rate

In-flight (optional)

This is enough to:

Detect slowdowns

Spot backpressure

Correlate with MQ depth

13. Common mistakes (please avoid)

❌ Measuring JMS receive instead of processing
❌ No histogram enabled (histogram = true)
❌ No tags (queue, app, instance)
❌ Using Gauges for TPS
❌ Measuring outside the listener method

14. Bonus: MQ depth correlation (advanced)

IBM MQ exposes queue depth via:

MQ exporter

Custom JMX

Combine:

MQ depth ↑

TPS flat

Latency ↑

→ consumer bottleneck

15. Bottom line

Micrometer annotations are enough for 90% of MQ observability

✔ TPS → @Counted
✔ Latency → @Timed(histogram=true)
✔ Prometheus → /actuator/prometheus
✔ Grafana → rate + histogram_quantile

This is battle-tested in high-throughput MQ/Kafka systems.

1️⃣ What you are deploying (mental model)

With Helm you deploy one exporter pod that:

Connects to IBM MQ Queue Manager

Periodically queries MQ (PCF/MQI)

Exposes metrics at /metrics

Prometheus scrapes it

[ MQ Exporter Pod ] ───► [ IBM MQ QM ]
│
▼
/metrics (HTTP)
│
▼
Prometheus

2️⃣ Helm chart source

IBM provides an official Helm chart via GitHub / IBM Container Registry.

You typically use:

Image: icr.io/ibm-messaging/mq-metric-samples or

Newer: icr.io/ibm-messaging/mq-exporter

(Exact image depends on MQ version; Helm values stay the same.)

3️⃣ Basic Helm install (minimal, works)
helm repo add ibm-mq https://ibm-messaging.github.io/mq-helm
helm repo update

helm install mq-exporter ibm-mq/mq-metric-samples \
--namespace mq-monitoring \
--create-namespace


⚠️ This alone is not enough — it won’t connect to MQ yet.

4️⃣ Core configuration concepts (IMPORTANT)

You must configure three things:

How exporter connects to MQ

Which queues / objects to monitor

How Prometheus discovers it

5️⃣ Connection to IBM MQ (values.yaml)
Example values.yaml (realistic)
image:
repository: icr.io/ibm-messaging/mq-exporter
tag: latest
pullPolicy: IfNotPresent

mq:
queueManager: QM1
connectionName: mq-qm1(1414)
channel: PROMETHEUS.CHANNEL
user: mqmonitor
password: mqpassword


🔑 Notes:

connectionName = host(port)

Channel must be SVRCONN

User needs inquire permissions only

Required MQ permissions (minimal)

User needs:

+inq qmgr
+inq queue
+inq channel


❌ No put/get required
✔ Read-only access

6️⃣ TLS configuration (very common in prod)
values.yaml (TLS)
mq:
tls:
enabled: true
secretName: mq-tls-secret
cipherSpec: TLS_RSA_WITH_AES_256_CBC_SHA256


Kubernetes secret:

kubectl create secret generic mq-tls-secret \
--from-file=key.jks \
--from-file=trust.jks


✔ Strongly recommended in production
✔ Works well with OpenShift too

7️⃣ What metrics are collected (filters)
Queue filters (VERY IMPORTANT)

By default exporter may collect everything → bad idea.

metrics:
queue:
include:
- ORDER.*
- PAYMENT.*
exclude:
- SYSTEM.*


This limits:

Queue depth

Oldest message age

Open handles

👉 Prevents metric cardinality explosion

8️⃣ Prometheus scraping (Prometheus Operator)
Enable ServiceMonitor
serviceMonitor:
enabled: true
interval: 30s
scrapeTimeout: 10s
labels:
release: prometheus


Prometheus Operator will auto-discover it.

Check:

kubectl get servicemonitor -n mq-monitoring

If you do NOT use Prometheus Operator

Expose service:

service:
type: ClusterIP
port: 9157


Then add manual scrape config in Prometheus:

scrape_configs:
- job_name: ibm-mq
  static_configs:
   - targets:
      - mq-exporter.mq-monitoring:9157

9️⃣ Validate metrics
kubectl port-forward svc/mq-exporter 9157:9157


Open:

http://localhost:9157/metrics


You should see:

ibmmq_queue_depth{qmgr="QM1",queue="ORDER.IN"} 42

🔟 Most important metrics (what to graph)
Metric	Meaning
ibmmq_queue_depth	Backlog
ibmmq_oldest_msg_age	SLA breach
ibmmq_open_input_count	Active consumers
ibmmq_open_output_count	Active producers
11️⃣ Grafana example queries
Queue depth
ibmmq_queue_depth{queue="ORDER.IN"}

Oldest message age
ibmmq_oldest_msg_age{queue="ORDER.IN"}

Detect backlog growth
deriv(ibmmq_queue_depth[5m]) > 0

12️⃣ Common mistakes (seen many times)

❌ Monitoring SYSTEM.* queues
❌ One exporter per app (wrong!)
❌ Using app credentials instead of monitor user
❌ Forgetting TLS in prod
❌ Not filtering queues

13️⃣ Recommended production topology
1 MQ Queue Manager
└─ 1 MQ Exporter
└─ Monitors selected queues


Apps:

Do NOT monitor depth

Only expose TPS / latency

Can IBM MQ Prometheus Exporter measure TPS?
✅ YES — MQ-level TPS (PUT / GET rates)

The exporter exposes metrics like:

Message put rate

Message get rate

Examples (names may vary slightly by MQ version):

ibmmq_put_rate{queue="ORDER.IN"}
ibmmq_get_rate{queue="ORDER.IN"}


These are:

Calculated from MQ statistics

Aggregated at the queue manager / queue level

Independent of any application

What this TPS means

PUT rate → how fast producers write to MQ

GET rate → how fast consumers read from MQ

This is true throughput at the broker.

✔ Very useful for capacity planning
✔ Good for detecting producer/consumer imbalance