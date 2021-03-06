package com.leaf.remoting.netty;

import com.leaf.common.constants.Constants;

public class NettyServerConfig {

    private int serverOnewaySemaphoreValue = 256;
    private int serverAsyncSemaphoreValue = 64;

    private int connCount = 4;

    private int idleAllSeconds = NettySystemConfig.IO_IDLE_ALL_TIME_SECONDS;
    private int idleReadSeconds = NettySystemConfig.IO_IDLE_READ_TIME_SECONDS;

    private int serverSocketSndBufSize = NettySystemConfig.socketSndbufSize;
    private int serverSocketRcvBufSize = NettySystemConfig.socketRcvbufSize;

    private int port = Constants.DEFAULT_PROVIDER_PORT;

    private long invokeTimeoutMillis = 3000L;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getIdleAllSeconds() {
        return idleAllSeconds;
    }

    public void setIdleAllSeconds(int idleAllSeconds) {
        this.idleAllSeconds = idleAllSeconds;
    }

    public int getServerSocketSndBufSize() {
        return serverSocketSndBufSize;
    }

    public void setServerSocketSndBufSize(int serverSocketSndBufSize) {
        this.serverSocketSndBufSize = serverSocketSndBufSize;
    }

    public int getServerSocketRcvBufSize() {
        return serverSocketRcvBufSize;
    }

    public void setServerSocketRcvBufSize(int serverSocketRcvBufSize) {
        this.serverSocketRcvBufSize = serverSocketRcvBufSize;
    }

    public int getServerOnewaySemaphoreValue() {
        return serverOnewaySemaphoreValue;
    }

    public void setServerOnewaySemaphoreValue(int serverOnewaySemaphoreValue) {
        this.serverOnewaySemaphoreValue = serverOnewaySemaphoreValue;
    }

    public int getServerAsyncSemaphoreValue() {
        return serverAsyncSemaphoreValue;
    }

    public void setServerAsyncSemaphoreValue(int serverAsyncSemaphoreValue) {
        this.serverAsyncSemaphoreValue = serverAsyncSemaphoreValue;
    }

    public int getConnCount() {
        return connCount;
    }

    public void setConnCount(int connCount) {
        this.connCount = connCount;
    }

    public long getInvokeTimeoutMillis() {
        return invokeTimeoutMillis;
    }

    public void setInvokeTimeoutMillis(long invokeTimeoutMillis) {
        this.invokeTimeoutMillis = invokeTimeoutMillis;
    }

    public int getIdleReadSeconds() {
        return idleReadSeconds;
    }

    public void setIdleReadSeconds(int idleReadSeconds) {
        this.idleReadSeconds = idleReadSeconds;
    }
}
