package com.leaf.rpc;

import com.google.common.base.Strings;
import com.leaf.common.UnresolvedAddress;
import com.leaf.common.annotation.ServiceInterface;
import com.leaf.common.constants.Constants;
import com.leaf.common.model.ServiceMeta;
import com.leaf.common.utils.Proxies;
import com.leaf.rpc.consumer.StrategyConfig;
import com.leaf.rpc.consumer.dispatcher.Dispatcher;
import com.leaf.rpc.consumer.invoke.DefaultInvoker;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class DefaultProxyFactory extends AbstractProxyFactory {

    private Class<?> interfaceClass;

    private DefaultProxyFactory() {
    }

    public static DefaultProxyFactory factory(Class<?> interfaceClass) {
        DefaultProxyFactory defaultProxyFactory = new DefaultProxyFactory();
        defaultProxyFactory.interfaceClass = interfaceClass;
        defaultProxyFactory.addresses = new ArrayList<>();
        return defaultProxyFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T newProxy() {

        checkNotNull(interfaceClass, "interfaceClass");
        ServiceInterface annotationInterface = interfaceClass.getAnnotation(ServiceInterface.class);
        if (annotationInterface != null) {
            checkArgument(
                    group == null,
                    interfaceClass.getName() + " has a @ServiceInterface annotation, can't set [group] again"
            );
            group = annotationInterface.group();
        }

        ServiceMeta serviceMeta = new ServiceMeta(
                Strings.isNullOrEmpty(group) ? Constants.DEFAULT_SERVICE_GROUP : group,
                Strings.isNullOrEmpty(serviceProviderName) ? interfaceClass.getName() : serviceProviderName,
                Strings.isNullOrEmpty(version) ? Constants.DEFAULT_SERVICE_VERSION : version);

        for (UnresolvedAddress address : addresses) {
            leafClient.connect(address);
            leafClient.remotingClient().addChannelGroup(serviceMeta, address);
        }

        if (leafClient.registerService() != null) {
            subscribe(serviceMeta);
        }

        Dispatcher dispatcher = dispatcher(dispatchType, leafClient, loadBalancerType, timeoutMillis);

        return (T) Proxies.getDefault().newProxy(
                interfaceClass,
                new DefaultInvoker(
                        leafClient.application(),
                        dispatcher,
                        serviceMeta,
                        new StrategyConfig(strategy, retries),
                        invokeType
                ));
    }


}
