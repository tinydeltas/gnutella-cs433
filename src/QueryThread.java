/*  QueryThread
*
*  Thread that listens for queries and hitqueries from neighboring servents.
*
*/

import java.net.*;

public class QueryThread extends GnutellaThread {
	public QueryThread(Servent p, ServerSocket welcomeSocket){
        super(p, welcomeSocket);
	}

	@Override
	public void serveRequest(Socket socket){
		Debug.DEBUG("received request from" + socket.getInetAddress().toString(),
				"QueryThread: serveRequest");
		MessageQueryHandler handler = new MessageQueryHandler(this, socket);
        InetAddress addr = socket.getInetAddress();
        byte[] request = handler.readFromSocket();
		Debug.DEBUG("Received request from socket: ",
				"QueryThread: serveRequest");
        handler.onPacketReceive(addr, request);
	}
}
