import java.net.InetAddress;

public class Peer {
    private int id;
    private QueryArray arr;
    private InetAddress[] neighbors; // list of neighbors

    public static void main(String args) {

    }

    // this design is not the most concise? not sure what else to do
    public void run() {
        // run the server listening at queries port
        // accepts queries
        // spins off PeerQueryHandler thread

    }

    public void accept() {
        // accepts file request connections on http port
        // spins off PeerFileRequestHandler thread
    }


    private void getUpstream(int messageID) {

    }

    private void addMessageID(int messageID, String upstreamIP) {

    }

    public void forwardToNeighbors() {

    }
}
