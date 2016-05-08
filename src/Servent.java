import java.io.*;
import java.net.*;
import java.util.*;

public class Servent {
    private int id = -1;            // ID of this servent
    public QueryArray arr;          // queries this servent has seen
    public HashMap<Integer, File> fileTable;   // files this servent has responded with
    private final int THREADPOOLSIZE = 5;
    public ServentConfig cfg;      // config containing other info

    //threads share access to the welcome socket. Requests for queries and
    //requests for HTTP GET have different welcome sockets

    private final QueryThread[] queryThreads = new QueryThread[THREADPOOLSIZE];
    private final FileThread[] httpThreads = new FileThread[THREADPOOLSIZE];
    public ClientThread client;

    private Servent(String args[]){
        String configPath = "";
        String filesPath = "";

        for(int i = 0; i < args.length; i++){
            if(args[i].equals("-id") && i < args.length-1)
                id = Integer.parseInt(args[i+1]);
            else if (args[i].equals("-d"))
                Debug.setDebug(true);
            else if(args[i].equals("-config") && i < args.length-1)
                configPath = args[i+1];
            else if (args[i].equals("-f") && i < args.length - 1) {
                filesPath = args[i+1];
            }
        }

        if(id == -1){
            System.out.println("Must specify servent's id number with -id <number>, exiting...");
            return;
        }
        try {
            cfg = new ServentConfig(this, configPath, filesPath);
        } catch (Exception e){
            System.out.println("Configuration failed, exiting...");
            return;
        }

        arr = new QueryArray(1000);
        fileTable = new HashMap<Integer, File>();
        run();
    }

    //args as follows:
    //java Servent -id <peer_id> -config <configFile>
    public static void main(String args[]) {
        if(args.length < 4){
            System.out.println("required args: -id <peer_id> -config <configFile");
            return;
        }
        new Servent(args);
    }

    public int getHTTPPORT() {
        return cfg.HTTPPORT;
    }
    public int getQUERYPORT() {
        return cfg.QUERYPORT;
    }

    //-------------------------------------------------------
    // Interfacing with the array
    //-------------------------------------------------------
    public boolean containsID(UUID messageID, int desc) {
        return arr.contains(messageID, desc);
    }

    public InetAddress getUpstream(UUID messageID, int desc) {
        if(arr.contains(messageID, desc))
            return arr.retrieve(messageID, desc);
        return null;
    }

    public void addMessageID(UUID messageID, int desc,InetAddress upstreamIP) {
        arr.add(messageID, desc, upstreamIP);
    }

    //-------------------------------------------------------
    // Misc helper methods
    //-------------------------------------------------------

    public void removeFile(String filename){
        Debug.DEBUG("Successfully removed file: " + filename, "servent:removeFile");
        client.removeFile(filename);
    }

    public ArrayList<InetAddress> getNeighbors() {
        return cfg.neighbors;
    }

    public int getID() {
        return id;
    }

    public UUID getIdentifier() {
        return cfg.identifier;
    }

    public String getName() {
        return cfg.addr.getCanonicalHostName();
    }

    public String conPath(String file) {
        return cfg.dirRoot + file;
    }

    private void run() {
        // run the server listening at queries port
        // accepts queries
        // spins off MessageQueryHandler thread

         try{
             ServerSocket queryWelcomeSocket = new ServerSocket(cfg.QUERYPORT);
             ServerSocket httpWelcomeSocket = new ServerSocket(cfg.HTTPPORT);

            for(int i = 0; i < THREADPOOLSIZE; i++) {
                queryThreads[i] = new QueryThread(this, queryWelcomeSocket); //TO-DO need the parameters
                queryThreads[i].start();

                httpThreads[i] = new FileThread(this, httpWelcomeSocket);
                httpThreads[i].start();
            }

            client = new ClientThread(this);
            client.start();


        } catch(Exception e){
            e.printStackTrace();
            System.out.println("Server construction failed.");
        }

        try {
            for (int i = 0; i < THREADPOOLSIZE; i++) {
                queryThreads[i].join();
                httpThreads[i].join();
            }
            client.join();
            System.out.println("All threads finished. Exit");
        } catch (Exception e) {
            System.out.println("Join errors");
            e.printStackTrace();
        }
    }
}
