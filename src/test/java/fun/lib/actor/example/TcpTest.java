package fun.lib.actor.example;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.DFActorManagerConfig;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * tcp服务端和客户端通信示例代码
 * @author lostsky
 *
 */
public class TcpTest {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setClientIoThreadNum(1);     //设置作为客户端向外连接时，通信层io使用的线程数
		//启动入口actor，开始事件循环		
		mgr.start(cfg, Server.class);
	}
	//server
	private static class Server extends DFActor{
		public Server(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		private final int serverPort = 10001;
		@Override
		public void onStart(Object param) {
			DFTcpServerCfg cfg = new DFTcpServerCfg(serverPort, 2, 1)
					.setTcpProtocol(TCP_PROTOCOL_LENGTH); //设置tcp server编码类型为二进制流，前两个字节标识消息长度
			
			log.info("onStart, ready to listen on port "+serverPort);
			//启动端口监听
			net.tcpSvr(cfg);
		}
		@Override
		public void onTcpConnOpen(int requestId, DFTcpChannel channel) {
			//获取新建连接id
			final int channelId = channel.getChannelId();
			log.info("onTcpConnOpen, remote="+channel.getRemoteHost()+":"+channel.getRemotePort()
				+", channelId="+channelId);
		}
		@Override
		public void onTcpConnClose(int requestId, DFTcpChannel channel) {
			//获取连接对象id
			final int channelId = channel.getChannelId();
			log.info("onTcpConnClose, remote="+channel.getRemoteHost()+":"+channel.getRemotePort()
				+", channelId="+channelId);
		}
		@Override
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object m) {
			ByteBuf msg = (ByteBuf) m;
			//获取消息体长度
			final int msgLen = msg.readableBytes();
			log.info("recv cli msg, len="+msgLen);
			//构造返回的消息
			final ByteBuf bufOut = PooledByteBufAllocator.DEFAULT.ioBuffer(msgLen);
			//拷贝收到的消息数据
			bufOut.writeBytes(msg);
			//向客户端返回
			channel.write(bufOut);
			//消息对象交由框架释放
			return MSG_AUTO_RELEASE;  //DFActorDefine.MSG_MANUAL_RELEASE
		}
		@Override
		public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg) {
			log.info("onTcpServerListenResult, port="+requestId+", succ="+isSucc+", err="+errMsg);
			//创建一个actor模拟客户端发送
			sys.createActor("actorTcpCliTest", Client.class, new Integer(serverPort));
		}
	}	
	//client
	private static class Client extends DFActor{
		public Client(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}		
		//服务端端口
		private int serverPort = 0;
		@Override
		public void onStart(Object param) {
			serverPort = (Integer) param;
			//开始连接服务端
			DFTcpClientCfg cfg = new DFTcpClientCfg("127.0.0.1", serverPort) 
				.setConnTimeout(5000) //设置连接超时，毫秒
				.setTcpProtocol(TCP_PROTOCOL_LENGTH); //设置解码器为length，头两字节为包长度
			net.tcpCli(cfg, serverPort);
			
			//启动定时器定时发送  一秒发送一次
			timer.timeout(1000, 10000);
		}
		
		private DFTcpChannel svrChannel = null;
		@Override
		public void onTcpConnOpen(int requestId, DFTcpChannel channel) {
			svrChannel = channel;
			log.debug("onTcpConnOpen, conn svr succ");
		}
		@Override
		public void onTcpConnClose(int requestId, DFTcpChannel channel) {
			svrChannel = null;
			log.debug("onTcpConnOpen, disconnect with svr");
		}
		@Override
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object m) {
			ByteBuf msg = (ByteBuf) m;
			final int msgLen = msg.readableBytes();
			final String str = (String) msg.readCharSequence(msgLen, Charset.forName("utf-8"));
			log.debug("recv msg from svr: "+str);
			
			return MSG_AUTO_RELEASE;
		}
		@Override
		public void onTcpClientConnResult(int requestId, boolean isSucc, String errMsg) {
			log.debug("onTcpClientConnResult, requestId="+requestId+", succ="+isSucc+", errMsg="+errMsg);
		}
		
		@Override
		public void onTimeout(int requestId) {
			if(svrChannel != null){ //已连接到服务器
				//发送数据
				svrChannel.write("hello server, "+System.currentTimeMillis());
			}
			//启动下一个定时器
			timer.timeout(1000, requestId);
		}
	}
}
