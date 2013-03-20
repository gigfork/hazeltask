package com.hazeltask.config;

import java.io.Serializable;
import java.util.concurrent.ThreadFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazeltask.Hazeltask;
import com.hazeltask.core.metrics.MetricNamer;
import com.yammer.metrics.core.MetricsRegistry;

public class HazeltaskConfig<GROUP extends Serializable> {
    private HazelcastInstance hazelcast = null; //we will pull the default hazelcast later
    private String topologyName         = Hazeltask.DEFAULT_TOPOLOGY;
//    private MetricNamer metricNamer     = new ScopeFirstMetricNamer();
//    private MetricsRegistry   metricsRegistry;
    private ExecutorConfig<GROUP> executorConfig = new ExecutorConfig<GROUP>();
    private MetricsConfig metricsConfig = new MetricsConfig();
    private ThreadFactory threadFactory;
    
    public HazeltaskConfig<GROUP> withTopologyName(String name) {
        this.topologyName = name;
        return this;
    }
    
    public HazeltaskConfig<GROUP> withExecutorConfig(ExecutorConfig<GROUP> executorConfig) {
        this.executorConfig = executorConfig;
        return this;
    }
    
    public HazeltaskConfig<GROUP> withHazelcastInstance(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
        return this;
    }
    
    public HazeltaskConfig<GROUP> withMetricsConfig(MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
        return this;
    }
    
//    public HazeltaskConfig withMetricNamer(MetricNamer metricNamer) {
//        this.metricNamer = metricNamer;
//        return this;
//    }
    
    public MetricNamer getMetricNamer() {
        return this.metricsConfig.metricNamer;
    }
    
    public MetricsConfig getMetricsConfig() {
        return this.metricsConfig;
    }
    
//    public HazeltaskConfig withMetricsRegistry(MetricsRegistry metricsRegistry) {
//        this.metricsRegistry = metricsRegistry;
//        return this;
//    }
    
    public MetricsRegistry getMetricsRegistry() {
        return this.metricsConfig.metricsRegistry;
    }

    public HazelcastInstance getHazelcast() {
        return hazelcast;
    }

    public String getTopologyName() {
        return topologyName;
    }
    
    public ExecutorConfig<GROUP> getExecutorConfig() {
        return this.executorConfig;
    }
    
    public HazeltaskConfig<GROUP> withThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }
    
    public ThreadFactory getThreadFactory() {
        return this.threadFactory;
    }
}