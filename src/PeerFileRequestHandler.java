public class PeerFileRequestHandler {
    Peer parent;

    public PeerFileRequestHandler(Peer parent) {
        this.parent = parent;
    }

    public void onPacketReceive(byte[] packet) {
        // call file query handler
    }

    void onFileQuery(GnutellaPacket pkt) {
        // search through local query
        // send to TCP connection
    }
}

