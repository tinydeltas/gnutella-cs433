import java.io.*;
import java.net.*;
import java.util.*;
import java.net.InetAddress;

public class Peer {
    private int id;
    private QueryArray arr;
    private InetAddress[] neighbors; // list of neighbors


    public Peer(String args[]){
        String filename = "";

        for(int i = 0; i < args.length; i++){
            if(args[i].equals("-id") && i < args.length-1)
                id = Integer.parseInt(args[i+1]);
            if(args[i].equals("-config") && i < args.length-1)
                filename = args[i+1];
        }
        
        if(!setUpConfiguration(filename)){
            System.out.println("Configuration failed, exiting...");
            return;
        }
        

        System.out.println(id);
        for(InetAddress ia : neighbors){
            System.out.println(ia);
        }
    }

    //args as follows:
    //java Peer -id <peer_id> -config <configFile>
    public static void main(String args[]) {
        if(args.length < 2){
            System.out.println("required args: -id <peer_id> -config <configFile");
            return;
        }
        new Peer(args);
    }

    // this design is not the most concise? not sure what else to do
    public void run() {
        // run the server listening at queries port
        // accepts queries
        // spins off PeerQueryHandler thread

    }

    public void accept() {
        // accepts file request connections on http port
        // spins off PeerFileRequestHandler thread
    }


    private void getUpstream(int messageID) {

    }

    private void addMessageID(int messageID, String upstreamIP) {

    }

    public void forwardToNeighbors() {

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
