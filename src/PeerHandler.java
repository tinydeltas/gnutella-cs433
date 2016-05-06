import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

abstract class PeerHandler {
    final Peer parent;
    final Socket socket;
    final ServerSocket welcomeSocket;

    PeerHandler(Peer parent, ServerSocket welcomeSocket, Socket socket) {
        this.parent = parent;
        this.welcomeSocket = welcomeSocket;
        this.socket = socket;
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

    public void sendPacket(int port, GnutellaPacket pkt) {
        sendPacket(this.socket.getInetAddress(), port, pkt);
    }
}
