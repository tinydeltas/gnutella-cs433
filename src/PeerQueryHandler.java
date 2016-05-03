
public class PeerQueryHandler implements PeerHandler {
    Peer parent;

    public PeerQueryHandler(Peer parent) {
        this.parent = parent;
    }

    public void onPacketReceive(byte[] packet) {
        // call either onQuery or onHitQuery
    }

    void onQuery(GnutellaPacket pkt) {

    }

    void onHitQuery(GnutellaPacket pkt) {

    }
}
