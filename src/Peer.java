import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.*;
import java.net.InetAddress;

public class Peer {
    private int id;
    public String dirRoot;
    private QueryArray arr;
    public InetAddress[] neighbors; // list of neighbors
    private ArrayList<String> files;

    private final int THREADPOOLSIZE = 5;
    private final int QUERYPORT = 7777;
    private final int HTTPPORT = 5760;

    private ServerSocket queryWelcomeSocket;
    private ServerSocket httpWelcomeSocket;

    //threads share access to the welcome socket. Requests for queries and
    //requests for HTTP GET have different welcome sockets
    private QueryThread[] queryThreads = new QueryThread[THREADPOOLSIZE];
    private HTTPThread[] httpThreads = new HTTPThread[THREADPOOLSIZE];

    public Peer(String args[]){
        String filename = "";

        for(int i = 0; i < args.length; i++){
            if(args[i].equals("-id") && i < args.length-1)
                id = Integer.parseInt(args[i+1]);
            else if(args[i].equals("-config") && i < args.length-1)
                filename = args[i+1];
            else if (args[i].equals("-f") && i < args.length - 1) {
                if ((files = setUpFiles(args[i+1])) == null) {
                    System.out.println("Failed to set up request path");
                    return;
                }
            }
        }

        if(!setUpConfiguration(filename)){
            System.out.println("Configuration failed, exiting...");
            return;
        }

        System.out.println(id);
        System.out.println(dirRoot);
        for(InetAddress ia : neighbors){
            System.out.println(ia);
        }

        arr = new QueryArray(100);
        run();
    }

    //args as follows:
    //java Peer -id <peer_id> -config <configFile>
    public static void main(String args[]) {
        if(args.length < 4){
            System.out.println("required args: -id <peer_id> -config <configFile");
            return;
        }
        new Peer(args);
    }

    private ArrayList<String> setUpFiles(String path) {
        File f = new File(path);
        ArrayList<String> filesToRequest = new ArrayList<String>();
        if (!f.exists() || f.isDirectory())
            return null;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String file;
            while ((file = reader.readLine()) != null) {
                filesToRequest.add(file);
            }
            reader.close();
        } catch (Exception e) {
            System.err.format("Exception occurred while reading: %s", path);
            e.printStackTrace();
            System.exit(1);
        }
        return filesToRequest;
    }

    public int getHTTPPORT() {
        return HTTPPORT;
    }

    public int getQUERYPORT() {
        return QUERYPORT;
    }

    public boolean containsID(int messageID) {
        return arr.containsKey(messageID);
    }

    public InetAddress getUpstream(int messageID) {
        return arr.retrieve(messageID);
    }

    public void addMessageID(int messageID, InetAddress upstreamIP) {
        arr.add(messageID, upstreamIP);
    }


    public void run() {
        // run the server listening at queries port
        // accepts queries
        // spins off PeerQueryHandler thread

         try{
            queryWelcomeSocket = new ServerSocket(QUERYPORT);
            httpWelcomeSocket = new ServerSocket(HTTPPORT);

            for(int i = 0; i < THREADPOOLSIZE; i++) {
                queryThreads[i] = new QueryThread(this, queryWelcomeSocket); //TO-DO need the parameters
                queryThreads[i].start();

                httpThreads[i] = new HTTPThread(this, httpWelcomeSocket);
                httpThreads[i].start();
            }

             ClientThread client = new ClientThread(this, files);
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
            System.out.println("All threads finished. Exit");
        } catch (Exception e) {
            System.out.println("Join errors");
        }
    }

    public boolean setUpConfiguration(String configFile){
        try {
            BufferedReader br = new BufferedReader(new FileReader(configFile));
            ArrayList<InetAddress> arrlist = new ArrayList<InetAddress>();
            String line;
            boolean current = false;
            while((line = br.readLine()) != null){
                String[] words = line.trim().split("\\s+");
                if(words[0].equals("<Peer") && words.length > 1 &&
                    Integer.parseInt(words[1].substring(0, words[1].length()-1)) == id){
                    current = true;
                    continue;
                }

                if(!current)
                    continue;

                if(words[0].equals("</Peer>")){
                    current = false;
                    continue;
                }

                if(words[0].equals("ROOT:")){
                    dirRoot = words[1];
                    continue;
                }

                //could do more stuff here if config file held more info, but for now just ignore it
                if(words[0].equals("<Neighbors>") || words[0].equals("</Neighbors>"))
                    continue;

                System.out.println("try to getByName: " + words[0]);
                arrlist.add(InetAddress.getByName(words[0]));
            }

            neighbors = arrlist.toArray(new InetAddress[arrlist.size()]);
        } catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
