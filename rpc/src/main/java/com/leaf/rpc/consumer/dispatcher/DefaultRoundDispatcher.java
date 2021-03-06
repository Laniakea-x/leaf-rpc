package com.leaf.rpc.consumer.dispatcher;

import com.leaf.common.context.RpcContext;
import com.leaf.remoting.api.ProtocolHead;
import com.leaf.rpc.provider.process.RequestWrapper;
import com.leaf.remoting.api.channel.ChannelGroup;
import com.leaf.remoting.api.payload.RequestCommand;
import com.leaf.rpc.balancer.LoadBalancer;
import com.leaf.rpc.consumer.LeafClient;
import com.leaf.rpc.consumer.InvokeType;
import com.leaf.rpc.consumer.future.InvokeFuture;
import com.leaf.serialization.api.Serializer;
import com.leaf.serialization.api.SerializerType;

/**
 * @author yefei
 */
public class DefaultRoundDispatcher extends AbstractDispatcher {

    public DefaultRoundDispatcher(
            LeafClient leafClient, LoadBalancer loadBalancer, SerializerType serializerType) {
        super(leafClient, loadBalancer, serializerType);
    }

    @Override
    public <T> InvokeFuture<T> dispatch(RequestWrapper request, Class<T> returnType, InvokeType invokeType) throws Throwable {
        final RequestWrapper requestWrapper = request;
        requestWrapper.setAttachment(RpcContext.getAttachments());

        // 通过软负载均衡选择一个channel
        ChannelGroup channelGroup = select(requestWrapper.getServiceMeta());
        Serializer serializer = getSerializer();

        byte[] bytes = serializer.serialize(requestWrapper);
        RequestCommand requestCommand = new RequestCommand(ProtocolHead.RPC_REQUEST, getSerializerCode(), bytes);

        InvokeFuture<T> invoke = invoke(requestCommand, DispatchType.ROUND, returnType, invokeType, channelGroup);

        return invoke;
    }
}
