import java.net.*;

public class HTTPThread extends GnutellaThread{
	public HTTPThread(Peer p, ServerSocket welcomeSocket){
		super(p, welcomeSocket);
	}

	@Override
	public void serveRequest(Socket socket){
        PeerFileRequestHandler handler = new PeerFileRequestHandler(this, socket);
        InetAddress addr = socket.getInetAddress();
        byte[] request = readFromSocket(socket);
        handler.onPacketReceive(addr, request);

        Debug.DEBUG("received request from" + socket.getInetAddress().toString(),
                "HTTPThread: serveRequest");
	}
}