package com.leaf.spring.init.bean;

import com.leaf.rpc.GenericProxyFactory;
import com.leaf.rpc.balancer.LoadBalancerType;
import com.leaf.rpc.consumer.InvokeType;
import com.leaf.rpc.consumer.cluster.ClusterInvoker;
import com.leaf.rpc.consumer.dispatcher.DispatchType;
import com.leaf.rpc.consumer.invoke.GenericInvoke;
import com.leaf.serialization.api.SerializerType;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import static com.google.common.base.Preconditions.checkNotNull;

public class GenericReferenceFactoryBean<T> implements FactoryBean<GenericInvoke>, InitializingBean {

    //----------------------
    private String group;

    private String serviceProviderName;

    private String version;
    // -----------------------

    private Class<T> interfaceClass;

    private ConsumerFactory consumer;

    private GenericInvoke genericInvoke;

    // ---------
    private long timeout;

    private LoadBalancerType loadBalancerType;

    private DispatchType dispatchType;

    private InvokeType invokeType;

    private SerializerType serializerType;

    private ClusterInvoker.Strategy strategy;

    private int retries;
    // ---------

    @Override
    public void afterPropertiesSet() throws Exception {
        checkNotNull(consumer, "consumer");

        GenericProxyFactory factory = GenericProxyFactory.factory();
        factory.consumer(consumer.getLeafClient())
                .group(group)
                .version(version)
                .timeMillis(timeout);

        if (serviceProviderName != null) {
            factory.serviceProviderName(serviceProviderName);
        }

        if (loadBalancerType != null) {
            factory.loadBalancerType(loadBalancerType);
        }

        if (strategy != null) {
            factory.strategy(strategy);
            if (strategy == ClusterInvoker.Strategy.FAIL_OVER && retries > 0) {
                factory.retries(this.retries);
            }
        }

        if (invokeType != null) {
            factory.invokeType(invokeType);
        }

        if (dispatchType != null) {
            factory.dispatchType(dispatchType);
        }

        if (serializerType != null) {
            factory.serializerType(serializerType);
        }

        genericInvoke = factory.newProxy();
    }

    @Override
    public GenericInvoke getObject() throws Exception {
        return genericInvoke;
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setConsumer(ConsumerFactory consumer) {
        this.consumer = consumer;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setLoadBalancerType(String loadBalancerType) {
        this.loadBalancerType = LoadBalancerType.parse(loadBalancerType);
        if (this.loadBalancerType == null) {
            throw new IllegalArgumentException("loadBalancerType:" + loadBalancerType);
        }
    }

    public void setStrategy(String strategy) {
        this.strategy = ClusterInvoker.Strategy.parse(strategy);
        if (this.strategy == null) {
            throw new IllegalArgumentException("strategy:" + strategy);
        }
    }

    public void setInvokeType(String invokeType) {
        this.invokeType = InvokeType.parse(invokeType);
        if (this.invokeType == null) {
            throw new IllegalArgumentException("invokeType" + invokeType);
        }
    }

    public void setSerializerType(String serializerType) {
        this.serializerType = SerializerType.parse(serializerType);
        if (this.serializerType == null) {
            throw new IllegalArgumentException("serializerType" + serializerType);
        }
    }

    public void setDispatchType(String dispatchType) {
        this.dispatchType = DispatchType.parse(dispatchType);
        if (this.dispatchType == null) {
            throw new IllegalArgumentException("dispatchType" + dispatchType);
        }
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public String getServiceProviderName() {
        return serviceProviderName;
    }

    public void setServiceProviderName(String serviceProviderName) {
        this.serviceProviderName = serviceProviderName;
    }
}