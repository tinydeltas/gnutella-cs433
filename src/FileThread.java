import java.net.*;

public class FileThread extends GnutellaThread{
	public FileThread(Servent p, ServerSocket welcomeSocket){
		super(p, welcomeSocket);
	}

	@Override
	public void serveRequest(Socket socket){
        MessageFileRequestHandler handler = new MessageFileRequestHandler(this, socket);
        InetAddress addr = socket.getInetAddress();
		Debug.DEBUG("Received file request", "FileThread serveRequest");
        byte[] request = handler.readFromSocket();
        handler.onPacketReceive(addr, request);
	}
}
