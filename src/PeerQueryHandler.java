import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

class PeerQueryHandler extends PeerHandler {

    public PeerQueryHandler(GnutellaThread thread, Socket socket) {
        super(thread.peer, thread.welcomeSocket, socket);
        Debug.DEBUG("Creating new handler for " + socket.toString(), "PeerQueryHandler constructor");
    }

    public void onPacketReceive(InetAddress from, byte[] packet) {
        // call either onQuery or onHitQuery
        GnutellaPacket pkt = GnutellaPacket.unpack(packet);
        if (pkt == null)
            Debug.DEBUG("uh oh", "onPacketReceive");
        assert pkt != null;
        assert from != null;
        Debug.DEBUG_F("Packet received: " + from.getCanonicalHostName() + ":\n"
            + pkt.toString(), "PeerQueryHandler: onPacketReceive");

        if (parent.containsID(pkt.getMessageID(),
                pkt.getPayloadDescriptor())) // checking that the message hasn't already been seen
            return; // avoids unnecessary forwards

        switch (pkt.getPayloadDescriptor()) {
            case GnutellaPacket.HITQUERY:
                onHitQuery(from, pkt);
                break;
            case GnutellaPacket.QUERY:
                onQuery(from, pkt);
                break;
            default:
                System.out.println("Unrecognized descriptor");
                break;
        }
    }

    /// REGULAR QUERY HANDLING
    private void onQuery(InetAddress from, GnutellaPacket pkt) {
        Debug.DEBUG_F("Handling a query from: " + from, "onQuery");
        UUID messageID = pkt.getMessageID();
        int TTL = pkt.getTTL();

        parent.addMessageID(messageID, GnutellaPacket.QUERY, from);

        String file = Utility.byteArrayToString(pkt.getPayload());
        Debug.DEBUG("File to search: " + file, "onQuery");

        if (fileExists(file)) {
            Debug.DEBUG("File exists", "onQuery");
            byte[] payload =
                    Utility.stringToByteArray(parent.addr.getCanonicalHostName() + ";" + file);
            GnutellaPacket newPkt = new GnutellaPacket(messageID,
                    GnutellaPacket.HITQUERY, TTL - 1, pkt.getHops() + 1, payload);
            sendPacket(from, super.parent.getQUERYPORT(), newPkt);   // send packet upstream
        }

        if (--TTL != 0) {
            Debug.DEBUG("Forwarding to neighbors", "onQuery");
            GnutellaPacket newPkt = new GnutellaPacket(messageID,
                    GnutellaPacket.QUERY, TTL, pkt.getHops() + 1, pkt.getPayload());
            forwardToNeighbors(from, newPkt); //forwarding to neighbors using TCP socket
        }
    }

    private boolean fileExists(String file) {
        File f = new File(parent.dirRoot + "/" + file);
        return f.exists() && !f.isDirectory();
    }

    private void forwardToNeighbors(InetAddress from, GnutellaPacket pkt) {
        for (InetAddress n : parent.neighbors) {
            if (from.getCanonicalHostName().equals(n.getCanonicalHostName())) {
                Debug.DEBUG("Skipping node that sent this", "forwardToNeighbors");
                continue;
            }
            Debug.DEBUG("Forwarding to neighbor: " + n.toString() + " from: " + from.toString(), "forwardToNeighbor");
            sendPacket(n, parent.getQUERYPORT(), pkt);
        }
    }

    /// HIT QUERY HANDLING
    private void onHitQuery(InetAddress from, GnutellaPacket pkt) {
        Debug.DEBUG_F("Received hit query from " + from.getCanonicalHostName(), "onHitQuery");
        UUID messageID = pkt.getMessageID();

        if (!parent.containsID(messageID, GnutellaPacket.QUERY)) {
            // remove request from network, original query not seen
            Debug.DEBUG("Original query not seen ", "onHitQuery");
            return;
        }

        InetAddress originAddr = null;
        String payload, host, file;

        try {
            payload = Utility.byteArrayToString(pkt.getPayload());
            String payloadWords[] = payload.split(";");

            assert payloadWords.length == 2;
            host = payloadWords[0];
            file = payloadWords[1];
            originAddr = InetAddress.getByName(host);

            Debug.DEBUG("Host: " + host + " File: " + file +
                    " originAddr: " + originAddr.toString(), "onHitQuery");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        InetAddress upstream = parent.getUpstream(messageID, GnutellaPacket.QUERY);
        if (upstream == null) {
            //  this was my request!!, open connection to port & retrieve file
            assert parent.arr.contains(messageID, GnutellaPacket.QUERY);
            assert parent.arr.retrieve(messageID, GnutellaPacket.QUERY) == null;  //null if we originated this query

            String res = retrieveFile(file, originAddr, messageID);
            Debug.DEBUG("Results is " + res.length() + " bytes", "onHitQuery");

            //remove the file from the list of files we want to request
            parent.removeFile(file);

        } else {
            sendPacket(upstream, parent.getQUERYPORT(), pkt);
            // otherwise forward it along
        }
    }

    private String retrieveFile(String file, InetAddress sentAddr, UUID messageID) {
        StringBuilder res = null;
        Socket newConn = null;
        try {
            newConn = new Socket(sentAddr, parent.getHTTPPORT());
            GnutellaPacket pkt = new GnutellaPacket(messageID, GnutellaPacket.OBTAIN,
                    GnutellaPacket.DEF_TTL, GnutellaPacket.DEF_HOPS, Utility.stringToByteArray(file));

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(),
                            StandardCharsets.US_ASCII));
            out.write(pkt.pack());
//            out.flush();
            Debug.DEBUG("Successfully flushed", "retrieveFile");

            res = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                res.append(s);
            }
            Debug.DEBUG("Successfully read file info", "retrieveFile");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (newConn != null){
                try {
                    newConn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        assert res != null;
        return res.toString();
    }
}
