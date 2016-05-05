import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


class ClientThread extends Thread {
    private final byte[] newline = Utility.stringToByteArray("\r\n");
    private final Peer peer;
    private final ArrayList<String> files;
    private final int NUMTHREADS = 15;
    private final int TIMEOUT = 2;

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
                for(String s : files){
                    System.out.print(s + ", ");
                }
                System.out.print("\n");
            }


            //problem with this design - must wait to get files before entering more.
            //could fix by making ANOTHER thread, but I'm not doing that quite yet.
            int i = -1;
            while(files.size() > 0){
                i = (i+1)%files.size();
                broadcast(files.get(i));
            }

        }
    }

    //original run
    /*public void run() {
        int i = 0;
        for (;;) {
            broadcast(i++ % files.size());
        }
    }*/

    

    private void broadcast(String filename) {
        // send request to all neighbors
        System.out.println("BROADCAST " + filename);

        ExecutorService executor = Executors.newFixedThreadPool(NUMTHREADS);

        List<Callable<BroadcastThread>> threadlist = new LinkedList<Callable<BroadcastThread>>();

        if(peer.neighbors == null)
            return;

        for (InetAddress n : peer.neighbors) {
            //todo
            System.out.println("Broadcast" + filename + "to " + n);

            BroadcastThread thr = new BroadcastThread(peer, n, filename, files);
            threadlist.add(thr);
        }

        try{
            executor.invokeAll(threadlist, TIMEOUT, TimeUnit.SECONDS);
        } catch(InterruptedException e){
            e.printStackTrace();
        }

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
        if(neighbor == null || filename == null || peer == null)
            return null;
        sendQuery(filename, neighbor);

        //wait until the file no longer is being looked for (means it was downloaded successfully)
        //if timeout, assume failure and re-broadcast
        while(true){
            if(!files.contains(filename))
                return null;
        }

    }


    public void sendQuery(){
        sendQuery(filename, neighbor);
    }

    public void sendQuery(String filename, InetAddress neighbor){

        //need to check message id in hitQuery

        Random random = new Random();
        int messageID = random.nextInt(100000);
        peer.arr.add(messageID, null);

        byte[] payload = Utility.stringToByteArray(filename);

        GnutellaPacket queryPacket = new GnutellaPacket(messageID, GnutellaPacket.QUERY, GnutellaPacket.DEF_TTL, 0, payload);
        sendPacket(neighbor, peer.getQUERYPORT(), queryPacket);
    }

    void sendPacket(InetAddress to, int port, GnutellaPacket pkt) {
        Debug.DEBUG_F("Sending packet to" + to.getCanonicalHostName()
                + ":" + pkt.toString(), "sendPacket");

        try {
            Socket s = new Socket(to, port);
            DataOutputStream out =
                    new DataOutputStream(s.getOutputStream());
            out.write(pkt.pack());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*public void sendHTTPRequest(){
        sendRequest(filename, neighbor);
    }

    public void sendHTTPRequest(String filename, InetAddress neighbor) {
        Socket clientSocket = null;
        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(neighbor, peer.getQUERYPORT()));

            InputStream is = clientSocket.getInputStream();
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII));

            outToServer.write(conGetRequest(filename);
            outToServer.flush();

            String line;
            int contentLength = 0;
            boolean first = true;
            while ((line = serverReader.readLine()) != null && !line.equals("")) {
                if (first) {
                    first = false;
                }
                String[] lines = line.split(":\\s");
                if (lines[0].equals("Content-Length"))
                    contentLength = Integer.parseInt(lines[1]);
                else if (lines[0].equals("HTTP/1.1")) {
                    if (!lines[1].equals("200")) {
                        System.out.println("Status code: " + lines[1]);
                    }
                }
            }

            int fileBytes = 0;
            while ((line = serverReader.readLine()) != null && fileBytes < contentLength) {
                fileBytes += line.getBytes(StandardCharsets.US_ASCII).length;
                assert newline != null;
                fileBytes += newline.length;
            }
        }

        catch (IOException e1) {
            System.out.println("Exception while making request");
            e1.printStackTrace();
        } finally {
            if (clientSocket != null){
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private byte[] conGetRequest(String filename) {
        String sb = "GET /" +
                filename +
                " HTTP/1.0\r\n" +
                "\r\n\r\n";

        return Utility.stringToByteArray(sb);
    }*/

}