import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.math.BigInteger;
import java.nio.ByteBuffer;

abstract class PeerHandler {
    final Peer parent;
    final Socket socket;
    final ServerSocket welcomeSocket;
    InputStream is = null;
    DataOutputStream os = null;

    PeerHandler(Peer parent, ServerSocket welcomeSocket, Socket socket) {
        this.parent = parent;
        this.welcomeSocket = welcomeSocket;
        this.socket = socket;
        try {
            this.is = socket.getInputStream();
            this.os = new DataOutputStream(socket.getOutputStream());
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendPacket(InetAddress to, int port, GnutellaPacket pkt) {
        Debug.DEBUG_F("Sending packet to" + to.getCanonicalHostName()
                + ":" + pkt.toString(), "sendPacket");

        try {
            Socket s = new Socket(to, port);
            DataOutputStream out =
                    new DataOutputStream(s.getOutputStream());
            out.write(ByteBuffer.allocate(4).putInt(pkt.pack().length).array()); //encode the length in a int
            out.write(pkt.pack());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendPacket(Socket s, GnutellaPacket pkt){
        try {
            DataOutputStream out =
                    new DataOutputStream(s.getOutputStream());
            out.write(ByteBuffer.allocate(4).putInt(pkt.pack().length).array());
            out.write(pkt.pack());
            //does NOT close socket
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendPayload(Socket s, byte[] payload){
        try{
            //Socket s = new Socket(to, port);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.write(payload);
            out.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    byte[] readFromSocket() {
        Debug.DEBUG("Attempting to read from socket: " + socket.toString(), "readFromSocket");
        byte[] request = null;
        byte[] lenArr = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            lenArr = new byte[4];
            request = new byte[ 128 ];
            int bytesRead = 0;
            int sum = 0;

            //not optimal because doesn't detect socket close

            while((bytesRead = is.read(lenArr, bytesRead, 4-bytesRead))!= 0 && sum+bytesRead < 4){
                sum += bytesRead;
            }
            int messageLen = new BigInteger(lenArr).intValue();
            System.out.println("messageLen: " + messageLen);

            bytesRead = 0;
            sum = 0;
            while ((bytesRead = is.read(request, bytesRead, messageLen-bytesRead)) != -1 && sum+bytesRead < messageLen) {
                sum += bytesRead;
                System.out.println(bytesRead);
            }
            //baos.write(request, 0, bytesRead);
            //return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return request;
    }

    public void sendPacket(int port, GnutellaPacket pkt) {
        sendPacket(this.socket.getInetAddress(), port, pkt);
    }
}
