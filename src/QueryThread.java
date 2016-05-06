import java.net.*;

public class QueryThread extends GnutellaThread {
	public QueryThread(Peer p,ServerSocket welcomeSocket){
        super(p, welcomeSocket);
	}

	@Override
	public void serveRequest(Socket socket){
		Debug.DEBUG("received request from" + socket.getInetAddress().toString(),
				"QueryThread: serveRequest");
		PeerQueryHandler handler = new PeerQueryHandler(this, socket);
        InetAddress addr = socket.getInetAddress();
        byte[] request = readFromSocket(socket);
		Debug.DEBUG("Received request from socket: ",
				"QueryThread: serveRequest");
        handler.onPacketReceive(addr, request);
	}
}