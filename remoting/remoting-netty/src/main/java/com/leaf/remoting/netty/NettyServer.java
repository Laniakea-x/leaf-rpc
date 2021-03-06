package com.leaf.remoting.netty;

import com.leaf.remoting.api.ChannelEventListener;
import com.leaf.remoting.api.InvokeCallback;
import com.leaf.remoting.api.RemotingServer;
import com.leaf.remoting.api.RequestCommandProcessor;
import com.leaf.remoting.api.exception.RemotingException;
import com.leaf.remoting.api.payload.ByteHolder;
import com.leaf.remoting.api.payload.RequestCommand;
import com.leaf.remoting.api.payload.ResponseCommand;
import com.leaf.remoting.netty.event.ChannelEvent;
import com.leaf.remoting.netty.event.ChannelEventType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyServer extends NettyServiceAbstract implements RemotingServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    private final NettyEncoder encoder = new NettyEncoder();

    private final NettyServerHandler nettyServerHandler = new NettyServerHandler();

    private final NettyConnectManageHandler nettyConnectManageHandler = new NettyConnectManageHandler();

    private final ServerBootstrap serverBootstrap;

    private final NioEventLoopGroup nioEventLoopGroupWorker;

    private final NioEventLoopGroup nioEventLoopGroupMain;

    private final NettyServerConfig config;

    private final ChannelEventListener channelEventListener;

    private final ExecutorService publicExecutorService;

    private final ScheduledExecutorService scanResponseTableExecutorService;

    public NettyServer(NettyServerConfig config) {
        this(config, null);
    }

    public NettyServer(NettyServerConfig config, ChannelEventListener listener) {
        super(config.getServerAsyncSemaphoreValue(), config.getServerOnewaySemaphoreValue());
        this.config = config;
        this.channelEventListener = listener;
        this.serverBootstrap = new ServerBootstrap();
        this.nioEventLoopGroupWorker = new NioEventLoopGroup();
        this.nioEventLoopGroupMain = new NioEventLoopGroup();

        this.publicExecutorService = Executors.newFixedThreadPool(AVAILABLE_PROCESSORS, new ThreadFactory() {

            AtomicInteger atomicInteger = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("PUBLIC#EXECUTOR#" + atomicInteger);
                return thread;
            }
        });

        scanResponseTableExecutorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("SCAN#PRC_RESPONSE#TABLE");
                return thread;
            }
        });
    }

    @Override
    public ResponseCommand invokeSync(Channel channel, RequestCommand request, long timeoutMillis) throws RemotingException, InterruptedException {
        return invokeSync0(channel, request, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void invokeOneWay(Channel channel, RequestCommand request, long timeoutMillis) throws RemotingException, InterruptedException {
        invokeOneWay0(channel, request, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void invokeAsync(final Channel channel, final RequestCommand request, long timeoutMillis, InvokeCallback<ResponseCommand> invokeCallback) throws RemotingException, InterruptedException {
        invokeAsync0(channel, request, timeoutMillis, TimeUnit.MILLISECONDS, invokeCallback);
    }

    @Override
    public void registerRequestProcess(RequestCommandProcessor requestCommandProcessor, ExecutorService executor) {
        defaultProcessor.setA(requestCommandProcessor);
        defaultProcessor.setB(executor);
    }

    @Override
    public void start() {

        serverBootstrap.group(nioEventLoopGroupMain, nioEventLoopGroupWorker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 32768)
                .childOption(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.SO_SNDBUF, config.getServerSocketSndBufSize())
                .childOption(ChannelOption.SO_RCVBUF, config.getServerSocketRcvBufSize())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(
                                new IdleStateHandler(config.getIdleReadSeconds(), 0, config.getIdleAllSeconds()),
                                encoder,
                                new NettyDecoder(),
                                nettyConnectManageHandler,
                                nettyServerHandler
                        );
                    }
                });
        try {
            serverBootstrap.bind(config.getPort()).sync().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info("NettyServer start complete, listen port: {}", config.getPort());
                    }
                }
            });
        } catch (InterruptedException e) {
            logger.error("NettyServer start error ", e);
        }

        this.scanResponseTableExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    scanResponseTable();
                } catch (Throwable e) {
                    logger.error("scanResponseTable exception", e);
                }
            }
        }, 1000 * 3, 1000, TimeUnit.MILLISECONDS);

        if (channelEventListener != null) {
            new Thread(channelEventExecutor).start();
        }
    }

    @ChannelHandler.Sharable
    class NettyServerHandler extends SimpleChannelInboundHandler<ByteHolder> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteHolder msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

    @ChannelHandler.Sharable
    class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.debug("NETTY SERVER PIPELINE: channelRegistered {}", remoteAddress);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.debug("NETTY SERVER PIPELINE: channelUnregistered, the channel[{}]", remoteAddress);
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.debug("NETTY SERVER PIPELINE: channelActive, the channel[{}]", remoteAddress);
            super.channelActive(ctx);

            if (channelEventListener != null) {
                putChannelEvent(new ChannelEvent(ChannelEventType.ACTIVE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.debug("NETTY SERVER PIPELINE: channelInactive, the channel[{}]", remoteAddress);
            super.channelInactive(ctx);

            if (channelEventListener != null) {
                putChannelEvent(new ChannelEvent(ChannelEventType.INACTIVE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                final String remoteAddress = ctx.channel().remoteAddress().toString();
                switch (event.state()) {
                    case ALL_IDLE:
                        if (channelEventListener != null) {
                            putChannelEvent(new ChannelEvent(ChannelEventType.ALL_IDLE, remoteAddress, ctx.channel()));
                        }
                        break;
                    case READER_IDLE:
                        if (channelEventListener != null) {
                            putChannelEvent(new ChannelEvent(ChannelEventType.READ_IDLE, remoteAddress, ctx.channel()));
                        }
                        ctx.channel().close().addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                                logger.warn("close channel: {} channel read idle more than: {}, isSuccess: {} ",
                                        channelFuture.channel(),
                                        config.getIdleReadSeconds(),
                                        channelFuture.isSuccess()
                                );
                            }
                        });
                        break;
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.warn("NETTY SERVER PIPELINE: exceptionCaught {}", remoteAddress);
            logger.error(cause.getMessage(), cause);
            ctx.channel().close();

            if (channelEventListener != null) {
                putChannelEvent(new ChannelEvent(ChannelEventType.EXCEPTION, remoteAddress, ctx.channel()));
            }

        }
    }

    private void putChannelEvent(ChannelEvent channelEvent) {
        this.channelEventExecutor.putChannelEvent(channelEvent);
    }

    @Override
    protected ChannelEventListener getChannelEventListener() {
        return this.channelEventListener;
    }

    @Override
    protected ExecutorService publicExecutorService() {
        return publicExecutorService;
    }

    @Override
    public void shutdownGracefully() {
        nioEventLoopGroupMain.shutdownGracefully().syncUninterruptibly();
        nioEventLoopGroupWorker.shutdownGracefully().syncUninterruptibly();

        if (publicExecutorService != null) {
            publicExecutorService.shutdownNow();
        }
        if (scanResponseTableExecutorService != null) {
            scanResponseTableExecutorService.shutdownNow();
        }
    }


}
