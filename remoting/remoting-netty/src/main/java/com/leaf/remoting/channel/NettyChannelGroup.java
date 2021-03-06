package com.leaf.remoting.channel;

import com.leaf.common.UnresolvedAddress;
import com.leaf.common.constants.Constants;
import com.leaf.common.model.Directory;
import com.leaf.remoting.api.channel.ChannelGroup;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yefei
 */
public class NettyChannelGroup implements ChannelGroup {

    private static final Logger logger = LoggerFactory.getLogger(NettyChannelGroup.class);

    private final CopyOnWriteArrayList<Channel> channels = new CopyOnWriteArrayList<>();

    private AtomicInteger index = new AtomicInteger(0);

    private final UnresolvedAddress address;

    private static final int DEFAULT_WEIGHT = Constants.SERVICE_WEIGHT;

    private final ConcurrentMap<String, Integer> weights = new ConcurrentHashMap<>();

    public NettyChannelGroup(UnresolvedAddress address) {
        this.address = address;
    }

    @Override
    public UnresolvedAddress remoteAddress() {
        return address;
    }

    @Override
    public Channel next() {
        for (; ; ) {
            int length = channels.size();
            if (length == 0) {
                throw new IllegalStateException("no channel");
            }
            if (length == 1) {
                return channels.get(0);
            }
            int offset = Math.abs(index.incrementAndGet() % length);
            return channels.get(offset);
        }
    }

    @Override
    public boolean addChannel(Channel channel) {
        channels.add(channel);
        //连接断开时自动被移除
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info("channel close , remove channel: {}", future.channel());
                removeChannel(future.channel());
            }
        });
        return true;
    }

    @Override
    public boolean removeChannel(Channel channel) {
        return channels.remove(channel);
    }

    @Override
    public void setWeight(Directory directory, int weight) {
        if (weight == DEFAULT_WEIGHT) {
            return;
        }
        weights.put(directory.directory(), weight);
    }

    @Override
    public int getWeight(Directory directory) {
        Integer weight = weights.get(directory.directory());
        if (weight == null) {
            weight = DEFAULT_WEIGHT;
        }
        return weight;
    }

    @Override
    public void removeWeight(Directory directory) {
        weights.remove(directory.directory());
    }

    @Override
    public boolean isAvailable() {
        return !channels.isEmpty();
    }

    @Override
    public int size() {
        return channels.size();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("NettyChannelGroup{");
        sb.append("channels=").append(channels);
        sb.append(", address=").append(address);
        // weights =  {} DEFAULT_WEIGHT
        sb.append(", weights=").append(weights);
        sb.append('}');
        return sb.toString();
    }
}
