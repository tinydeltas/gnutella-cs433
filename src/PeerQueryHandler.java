
public class PeerQueryHandler implements PeerHandler {
    Peer parent;

    public PeerQueryHandler(Peer parent) {
        this.parent = parent;
    }

    public void onPacketReceive(byte[] packet) {
        // call either onQuery or onHitQuery
    }

    void onQuery(GnutellaPacket pkt) {
//        checking that the message hasn't already been seen

//        updating the associative array otherwise

//        searching local storage
//        if file found, sends hitquery message upstream (using hitquery handling function)

//        decrement TTL, (increment hops field)

//        if TTL is not 0 & not seen already,
//          forwarding to neighbors using TCP socket
    }

    void onHitQuery(GnutellaPacket pkt) {
//        searching the associative array

//        if not something the peer sent,
//                sends hitquery message upstream

//        otherwise, open connection to the appropriate port and download the file
    }
}
