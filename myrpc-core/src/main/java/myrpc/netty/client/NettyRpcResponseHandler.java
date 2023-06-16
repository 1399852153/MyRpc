package myrpc.netty.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import myrpc.exchange.model.MessageProtocol;
import myrpc.exchange.model.RpcResponse;
import myrpc.exchange.DefaultFutureManager;
import myrpc.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端 rpc响应处理器
 * @author shanreng
 */
public class NettyRpcResponseHandler extends SimpleChannelInboundHandler<MessageProtocol<RpcResponse>> {

    private static final Logger logger = LoggerFactory.getLogger(NettyRpcResponseHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageProtocol<RpcResponse> rpcResponseMessageProtocol) throws Exception {
        logger.debug("NettyRpcResponseHandler channelRead0={}",JsonUtil.obj2Str(rpcResponseMessageProtocol));

        // 触发客户端的future，令其同步阻塞的线程得到结果
        DefaultFutureManager.received(rpcResponseMessageProtocol.getBizDataBody());
    }
}
