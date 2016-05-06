import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


class ClientThread extends Thread {
    private final Peer peer;
    private final ArrayList<String> files;
    private final int NUMTHREADS = 15;
    private final int TIMEOUT = 5;

    public ClientThread(Peer p, ArrayList<String> files){
        this.peer = p;
        this.files = files;
    }

    public void run(){
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String nextLine;

        System.out.println("Enter names of files you want to request.");

        while(true){
            try{
                nextLine = br.readLine();
            } catch(IOException e){
                e.printStackTrace();
                continue;
            }

            String[] words = nextLine.trim().split("\\s+");

            for(int i = 0; i < words.length; i++){
                Debug.DEBUG("Trying to get file: " + words[i], "ClientThread: run");
                files.add(words[i]);
            }

            //in case we need flags or something but I don't think we're doing that here
            /*for(int i = 0; i < words.length-1;){
                if(words[i].equals("-f")){
                    i++;
                    while(i < words.length && !words[i].equals("-f")){
                        files.add(words[i]);
                        i++;
                    }
                }else{
                    System.out.println("Required:  -f <files_to_request>");
                    i++;
                }
            }*/

            if(files != null){
                System.out.print("Request files: ");
                for(String s : files)
                    System.out.print(s + ", ");
                System.out.print("\n");
            }

            //problem with this design - must wait to get files before entering more.
            //could fix by making ANOTHER thread, but I'm not doing that quite yet.
//            int i = -1;
//            while(files.size() > 0){
//                i = (i+1) % files.size();
//
//            }

            for (int i = 0; i < files.size(); i++) {
                broadcast(files.get(i));
            }
        }
    }

    private void broadcast(String filename) {
        // send request to all neighbors
        System.out.println("BROADCAST " + filename);

        ExecutorService executor = Executors.newFixedThreadPool(NUMTHREADS);

        Collection threadlist = new LinkedList<Callable<BroadcastThread>>();

        if(peer.neighbors == null)
            return;

        for (InetAddress n : peer.neighbors) {
            System.out.println("Broadcast" + filename + " to " + n);

            BroadcastThread thr = new BroadcastThread(peer, n, filename, files);
            threadlist.add(thr);
        }

        Debug.DEBUG("Finished adding all threads " + threadlist.size(), "broadcast");
        List<Future<?>> futures;
        try{
            futures = executor.invokeAll(threadlist, TIMEOUT, TimeUnit.SECONDS);
            for (Future<?> future: futures) {
                future.get();
                if (future.isDone())
                    Debug.DEBUG("Task completed", "broadcast");
            }
        } catch(InterruptedException e){
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        for (;;);

//        Debug.DEBUG("Function finished", "broadcast");

        //for now, remove it so we can test it without it infinitely looping
        //(in reality you would only remove the file from the list once you finally get the file)
        //files.remove(files.indexOf(filename));
    }

    public void removeFile(String filename){
        if(files.contains(filename))
            files.remove(filename);
    }
}

class BroadcastThread implements Callable{
    Peer peer;
    InetAddress neighbor;
    String filename;
    ArrayList<String> files;

    public BroadcastThread(Peer peer, InetAddress neighbor, String filename, ArrayList<String> files){
        this.peer = peer;
        this.neighbor = neighbor;
        this.filename = filename;
        this.files = files;
    }

    public String call(){
        Debug.DEBUG("Running broadcast thread", "call");
        if(neighbor == null || filename == null || peer == null)
            return null;

        sendQuery(filename, neighbor);

        //wait until the file no longer is being looked for (means it was downloaded successfully)
        //if timeout, assume failure and re-broadcast
        Debug.DEBUG("Function finished", "call");
        for (;;) {
            if(!files.contains(filename)) {
                Debug.DEBUG("Found file", "BroadcastThread: call");
                return null;
            }
        }

    }

    public void sendQuery(){
        sendQuery(filename, neighbor);
    }

    public void sendQuery(String filename, InetAddress neighbor){
        //need to check message id in hitQuery
        Debug.DEBUG("Sending query for " + neighbor, "sendQuery");

        UUID descriptorID = UUID.randomUUID();
        peer.arr.add(descriptorID, null);
        byte[] payload = Utility.stringToByteArray(filename);
        GnutellaPacket queryPacket =
                new GnutellaPacket(descriptorID, GnutellaPacket.QUERY, GnutellaPacket.DEF_TTL, 0, payload);

        sendPacket(neighbor, peer.getQUERYPORT(), queryPacket);
    }

    void sendPacket(InetAddress to, int port, GnutellaPacket pkt) {
        Debug.DEBUG_F("Sending packet to " + to.getCanonicalHostName()
                + ":" + pkt.toString(), "sendPacket");

        try {
            Socket s = new Socket(to, port);
            DataOutputStream out =
                    new DataOutputStream(s.getOutputStream());
            out.write(pkt.pack());
            Debug.DEBUG("Wrote the packet", "sendPacket");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
