/*ClientThread
* The thread that runs on a servent that listens to user input to find files to request,
* and then starts threads to request them.
*/

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
    private final long TIMEOUT = 5;

    public ClientThread(Servent p) {
       this.servent = p;
       this.files = servent.cfg.getFiles();
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
            for (String word : words) {
                if (word.equals("\n"))
                    continue;
                Debug.DEBUG("Trying to get file: " + word, "ClientThread: run");
                files.add(word);
            }

            printFiles();
            broadcast();

            if (words.length == 0 && words[0].toUpperCase().equals("BYE")) {
                return;
            }
        }
    }

    private void printFiles() {
        if (files.get(0).toUpperCase().equals("BYE")) {
            System.out.println("BYE message: " + files.subList(1, files.size()).toString());
            return;
        }
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
        List futures;
        try{
            futures = executor.invokeAll(threadlist, TIMEOUT, TimeUnit.SECONDS);
            for (Object future: futures) {
                ((Future) future).get();
                if (((Future) future).isDone())
                    Debug.DEBUG("Task completed", "broadcast");
            }
            System.out.println("\tAll tasks successfully completed");
        } catch(InterruptedException e){
            System.out.println("Some problems downloading files...");
            for(String name : files){
                System.out.println(name + " was not downloaded.");
            }

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
