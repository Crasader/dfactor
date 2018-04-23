package fun.lib.actor.po;

import fun.lib.actor.api.DFUdpDecoder;

public final class DFUdpServerCfg {
	public final int port;
	public final int ioThreadNum;
	public final boolean isBroadcast;
	
	private int soRecvBuf = 1024*20;
	private int soSendBuf = 1024*20;
	
	private volatile DFUdpDecoder decoder = null;
	
	public DFUdpServerCfg(int port, int ioThreadNum, boolean isBroadcast) {
		this.port = port;
		if(ioThreadNum < 1){
			ioThreadNum = 1;
		}
		this.ioThreadNum = ioThreadNum;
		this.isBroadcast = isBroadcast;
	}
	
	public int getSoSendBuf(){
		return soSendBuf;
	}
	public DFUdpServerCfg setSoSendBuf(int soSendBuf){
		this.soSendBuf = soSendBuf;
		return this;
	}
	
	public int getSoRecvBuf(){
		return soRecvBuf;
	}
	public DFUdpServerCfg setSoRecvBuf(int soRecvBuf){
		this.soRecvBuf = soRecvBuf;
		return this;
	}

	public DFUdpDecoder getDecoder() {
		return decoder;
	}

	public DFUdpServerCfg setDecoder(DFUdpDecoder decoder) {
		this.decoder = decoder;
		return this;
	}
	
}




