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

    public void run() {
        int i = 0;
        for (;;) {
            broadcast(i++ % files.size());
        }
    }

    private byte[] conGetRequest(String filename) {
        String sb = "GET /" +
                filename +
                " HTTP/1.0\r\n" +
                "\r\n\r\n";

        return Utility.stringToByteArray(sb);
    }

    private void broadcast(int curFile) {
        // send request to all neighbors
        for (InetAddress n : peer.neighbors) {
            //todo
        }
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
