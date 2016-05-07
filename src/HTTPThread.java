import java.net.*;

public class HTTPThread extends GnutellaThread{
	public HTTPThread(Servent p, ServerSocket welcomeSocket){
		super(p, welcomeSocket);
	}

	@Override
	public void serveRequest(Socket socket){
        MessageFileRequestHandler handler = new MessageFileRequestHandler(this, socket);
        InetAddress addr = socket.getInetAddress();
		Debug.DEBUG("Received file request", "HTTPThread serveRequest");
        byte[] request = handler.readFromSocket();
        handler.onPacketReceive(addr, request);
	}
}
