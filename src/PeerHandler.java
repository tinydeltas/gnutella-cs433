import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class PeerHandler {
    public Peer parent;
    public Socket socket;
    public ServerSocket welcomeSocket;

    public PeerHandler(Peer parent, ServerSocket welcomeSocket, Socket socket) {
        this.parent = parent;
        this.welcomeSocket = welcomeSocket;
        this.socket = socket;
    }

    public abstract void onPacketReceive(InetAddress from, byte[] packet);

    public void sendPacket(InetAddress to, int port, GnutellaPacket pkt) {
        try {
            Socket s = new Socket(to, port);
            DataOutputStream out =
                    new DataOutputStream(s.getOutputStream());
            out.write(pkt.pack());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPacket(int port, GnutellaPacket pkt) {
        sendPacket(this.socket.getInetAddress(), port, pkt);
    }
}
