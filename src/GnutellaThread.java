import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;

public class GnutellaThread extends Thread{

	ServerSocket welcomeSocket;

	//public GnutellaThread(ServerSocket welcomeSocket){
	//	this.welcomeSocket = welcomeSocket;
	//}

	public void run(){
		System.out.println("Thread " + this + " started.");

		while(true){
			Socket s = null;
			synchronized(welcomeSocket){
				try{
					s = welcomeSocket.accept();
					serveRequest(s);
				} catch(IOException e){
					e.printStackTrace();
				}
			}
		}

	}

	public void serveRequest(Socket socket){
		//override this, call the appropriate handler
	}
}