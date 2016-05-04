import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;

public abstract class GnutellaThread extends Thread{
	Peer peer;
	ServerSocket welcomeSocket;

	public GnutellaThread(Peer p, ServerSocket welcomeSocket){
		this.welcomeSocket = welcomeSocket;
		this.peer = p;
	}

	public void run(){
		System.out.println("Thread " + this + " started.");

		while(true){
			Socket s;
			synchronized(welcomeSocket){
				try{
					s = welcomeSocket.accept();
					serveRequest(s);
					s.close();
				} catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}

	public byte[] readFromSocket(Socket socket) {
		byte[] request = null;
		try {
			InputStream inputStream = socket.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			request = new byte[ 128 ];
			int bytesRead = -1;
			while ((bytesRead = inputStream.read(request)) != -1) {
				baos.write(request, 0, bytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			return request;
		}
	}

	public abstract void serveRequest(Socket socket);
}