/* GnutellaThread
*  Superclass for threads listening for queries and file requests from a welcome socket.
*/

import java.io.*;
import java.net.*;
import java.lang.Thread;

abstract class GnutellaThread extends Thread{
	public final Servent servent;
	public final ServerSocket welcomeSocket;

	GnutellaThread(Servent p, ServerSocket welcomeSocket){
		this.welcomeSocket = welcomeSocket;
		this.servent = p;
	}

	public void run(){
		System.out.println("Thread " + this + " started.");

		for (;;){
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



	protected abstract void serveRequest(Socket socket);
}
