{{/*
Generate Kafka controller quorum voters configuration.
For KRaft mode, generates a comma-separated list like: 0@kafka-0.kafka-headless:9093,1@kafka-1.kafka-headless:9093,2@kafka-2.kafka-headless:9093
*/}}
{{- define "kafka.quorumVoters" -}}
{{- $namespace := .Values.global.namespace -}}
{{- $replicaCount := int .Values.kafka.replicaCount -}}
{{- $voters := list -}}
{{- range $i := until $replicaCount -}}
{{- $voters = append $voters (printf "%d@kafka-%d.kafka-headless.%s.svc.cluster.local:9093" $i $i $namespace) -}}
{{- end -}}
{{- join "," $voters -}}
{{- end -}}

{{/*
Generate IBM MQ cluster configuration.
*/}}
{{- define "ibmmq.clusterName" -}}
{{- printf "%s-CLUSTER" .Values.ibmmq.config.queueManager -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "common.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
