import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
            out.write(pkt.pack());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    byte[] readFromSocket() {
        Debug.DEBUG("Attempting to read from socket: " + socket.toString(), "readFromSocket");
        byte[] request = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            request = new byte[ 128 ];
            int bytesRead;
            while ((bytesRead = is.read(request)) != -1) {
                baos.write(request, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return request;
    }

    public void sendPacket(int port, GnutellaPacket pkt) {
        sendPacket(this.socket.getInetAddress(), port, pkt);
    }
}
