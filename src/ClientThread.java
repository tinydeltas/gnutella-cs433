import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

class ClientThread extends Thread {
    private final byte[] newline = Utility.stringToByteArray("\r\n");
    private final Peer peer;
    private final ArrayList<String> files;

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

    private byte[] conGetRequest(String filename) {
        String sb = "GET /" +
                filename +
                " HTTP/1.0\r\n" +
                "\r\n\r\n";

        return Utility.stringToByteArray(sb);
    }

    private void broadcast(String filename) {
        // send request to all neighbors
        System.out.println("BROADCAST " + filename);
        for (InetAddress n : peer.neighbors) {
            //todo
            System.out.println("Broadcast" + filename + "to " + n);

            


        }
        //for now, remove it so we can test it without it infinitely looping
        //(in reality you would only remove the file from the list once you finally get the file)
        files.remove(files.indexOf(filename));
    }

    public void sendRequest(int curFile, InetAddress neighbor) {
        Socket clientSocket = null;
        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(neighbor, peer.getQUERYPORT()));

            InputStream is = clientSocket.getInputStream();
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII));

            outToServer.write(conGetRequest(files.get(curFile)));
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

}
