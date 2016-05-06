import java.net.*;

public class HTTPThread extends GnutellaThread{
	public HTTPThread(Peer p, ServerSocket welcomeSocket){
		super(p, welcomeSocket);
	}

	@Override
	public void serveRequest(Socket socket){
        PeerFileRequestHandler handler = new PeerFileRequestHandler(this, socket);
        InetAddress addr = socket.getInetAddress();
		Debug.DEBUG("Received file request", "HTTPThread serveRequest");
        byte[] request = handler.readFromSocket();
        handler.onPacketReceive(addr, request);
	}
}
