import java.net.*;
import java.util.*;
import java.lang.Thread;

public class QueryThread extends GnutellaThread{

	public QueryThread(ServerSocket welcomeSocket){
		this.welcomeSocket = welcomeSocket;
	}

	@Override
	public void serveRequest(Socket socket){
		System.out.println("[serve request]");
	}

}