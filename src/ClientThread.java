import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class ClientThread extends Thread {
    private final Servent servent;
    public static ArrayList<String> files;
    private final int NUMTHREADS = 8;
    private final int TIMEOUT = 2;

    public ClientThread(Servent p) {
        this.servent = p;
        ClientThread.files = servent.cfg.getFiles();
    }

    public void run(){
        // first it tries to go through all the files in
        // in the given request file
        broadcast();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String nextLine;

        System.out.println("Enter names of files you want to request, or BYE to close the connection");

        for (;;) {
            try{
                nextLine = br.readLine();
            } catch(IOException e){
                e.printStackTrace();
                continue;
            }

            String[] words = nextLine.trim().split("\\s+");
            if (words.length == 0 && words[0].toUpperCase().equals("BYE")) {
                // close the connection
                closeConnection();
            }
            for (String word : words) {
                if (word.equals("\n"))
                    continue;
                Debug.DEBUG("Trying to get file: " + word, "ClientThread: run");
                files.add(word);
            }

            printFiles();
            broadcast();
        }
    }

    // todo
    private void closeConnection() {

    }

    private void printFiles() {
        if(files != null){
            System.out.print("Request files: ");
            for(String s : files)
                System.out.print(s + ", ");
            System.out.print("\n");
        }
    }

    private void broadcast() {
        ArrayList<InetAddress> neighbors = servent.getNeighbors();
        if(neighbors == null || files.size() == 0)
            return;

        ExecutorService executor = Executors.newFixedThreadPool(NUMTHREADS);
        Collection threadlist = new LinkedList<Callable<BroadcastThread>>();

        for (int i = 0; i < files.size(); i++) {
            String filename = files.get(i);
            for (InetAddress n : neighbors) {
                System.out.println("\t[ " + filename + "] ---> " + n.getCanonicalHostName());
                BroadcastThread thr = new BroadcastThread(servent, n, filename, files);
                threadlist.add(thr);
            }
        }

        Debug.DEBUG("Finished adding all threads " + threadlist.size(), "broadcast");
        List<Future<?>> futures;
        try{
            futures = executor.invokeAll(threadlist, TIMEOUT, TimeUnit.SECONDS);
            for (Future<?> future: futures) {
                future.get();
                if (future.isDone()) Debug.DEBUG("Task completed", "broadcast");
            }
            System.out.println("\tAll tasks successfully completed");
        } catch(InterruptedException e){
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeFile(String filename){
        synchronized(files) {
            Debug.DEBUG("ClientThread removeFile", "removeFile");
            Debug.DEBUG("Removing file: " + filename, "client:removeFile");
            files.remove(filename);
            files.notifyAll();
        }
    }
}

class BroadcastThread implements Callable {
    private final Servent servent;
    private final InetAddress neighbor;
    private final String filename;


    public BroadcastThread(Servent servent, InetAddress neighbor, String filename, ArrayList<String> files) {
        this.servent = servent;
        this.neighbor = neighbor;
        this.filename = filename;
    }

    public String call() {
        Debug.DEBUG("Running broadcast thread", "call");
        if (neighbor == null || filename == null || servent == null)
            return null;

        sendQuery();

        //wait until the file no longer is being looked for (means it was downloaded successfully)
        Debug.DEBUG("Function finished", "call");
        synchronized (ClientThread.files) {
            while (!ClientThread.files.isEmpty()) {
                try {
                    ClientThread.files.wait();
                } catch (InterruptedException ex) {
                    System.out.println("Waiting for pool interrupted.");
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }

    private void sendQuery() {
        sendQuery(filename, neighbor);
    }

    private void sendQuery(String filename, InetAddress neighbor) {
        Debug.DEBUG("Sending query for " + neighbor, "sendQuery");

        UUID descriptorID = UUID.randomUUID();
        servent.arr.add(descriptorID, GnutellaPacket.QUERY, null);
        byte[] payload = Utility.stringToByteArray(filename);
        GnutellaPacket queryPacket =
                new GnutellaPacket(descriptorID, GnutellaPacket.QUERY, GnutellaPacket.DEF_TTL, 0, payload);
        sendPacket(neighbor, servent.getQUERYPORT(), queryPacket);
    }

    private void sendPacket(InetAddress to, int port, GnutellaPacket pkt) {
        Debug.DEBUG_F("Sending packet to " + to.getCanonicalHostName()
                + ":" + pkt.toString(), "sendPacket");

        try {
            Socket s = new Socket(to, port);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            Debug.DEBUG("Packet length: " + pkt.pack().length, "sendPacket");
            out.writeInt(pkt.pack().length);
            out.write(pkt.pack());
            out.flush();
            Debug.DEBUG("Wrote the packet", "sendPacket");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



