import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.math.BigInteger;
import java.nio.ByteBuffer;

abstract class MessageHandler {
    final Servent parent;
    final Socket socket;
    private InputStream is = null;

    MessageHandler(Servent parent, ServerSocket welcomeSocket, Socket socket) {
        this.parent = parent;
        this.socket = socket;
        try {
            this.is = socket.getInputStream();
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    void forwardPacket(InetAddress to, int port, GnutellaPacket pkt) {
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

    void sendRequestPacket(Socket s, GnutellaPacket pkt){
        try {
            DataOutputStream out =
                    new DataOutputStream(s.getOutputStream());
            out.write(ByteBuffer.allocate(4).putInt(pkt.pack().length).array());
            out.write(pkt.pack());
            out.flush();
            //does NOT close socket
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendPayload(byte[] payload){
        try{
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
            out.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    byte[] readFromSocket() {
        Debug.DEBUG("Attempting to read from socket: " + socket.toString(), "readFromSocket");
        byte[] request = null;
        byte[] lenArr;
        try {
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        return request;
    }

// --Commented out by Inspection START (5/7/16, 12:36 AM):
//    public void sendPacket(int port, GnutellaPacket pkt) {
//        sendPacket(this.socket.getInetAddress(), port, pkt);
//    }
// --Commented out by Inspection STOP (5/7/16, 12:36 AM)
}
