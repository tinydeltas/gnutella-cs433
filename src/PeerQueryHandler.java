import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class PeerQueryHandler extends PeerHandler {

    public PeerQueryHandler(GnutellaThread thread, Socket socket) {
        super(thread.peer, thread.welcomeSocket, socket);
    }

    public void onPacketReceive(InetAddress from, byte[] packet) {
        // call either onQuery or onHitQuery
        GnutellaPacket pkt = GnutellaPacket.unpack(packet);
        assert pkt != null;
        Debug.DEBUG_F("Packet received: " + from.getCanonicalHostName() + ":\n"
            + pkt.toString(), "PeerQueryHandler: onPacketReceive");

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
        int messageID = pkt.getMessageID();
        int TTL = pkt.getTTL();
        if (parent.containsID(messageID)) // checking that the message hasn't already been seen
            return;
        else
            parent.addMessageID(messageID, from);

        String file =
                Utility.byteArrayToString(pkt.getPayload());

        if (fileExists(file)) {
            byte[] payload =
                    Utility.stringToByteArray(welcomeSocket.getInetAddress().getCanonicalHostName() + ";" + file);
            GnutellaPacket newPkt = new GnutellaPacket(messageID,
                    GnutellaPacket.HITQUERY, TTL - 1, pkt.getHops(), payload);
            sendPacket(from, super.parent.getQUERYPORT(), newPkt);   // send packet upstream
        }

        if (--TTL != 0) {
            forwardToNeighbors(pkt); //forwarding to neighbors using TCP socket
        }
    }

    private boolean fileExists(String file) {
        File f = new File(parent.dirRoot + "/" + file);
        return f.exists() && !f.isDirectory();
    }

    private void forwardToNeighbors(GnutellaPacket pkt) {
        for (InetAddress n : parent.neighbors) {
            sendPacket(n, parent.getQUERYPORT(), pkt);
        }
    }

    /// HIT QUERY HANDLING
    private void onHitQuery(InetAddress from, GnutellaPacket pkt) {
        int messageID = pkt.getMessageID();
        InetAddress originAddr = null;

        String payload, host, file;

        try {
            payload = Utility.byteArrayToString(pkt.getPayload());
            String payloadWords[] = payload.split(";");
            
            assert payloadWords.length == 2;
            host = payloadWords[0];
            file = payloadWords[1];
            originAddr = InetAddress.getByName(host);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (originAddr == welcomeSocket.getInetAddress()) {
            //  this was my request!!, open connection to port & retrieve file
          
            assert parent.arr.contains(new Integer(messageID));
            assert parent.arr.retrieve(messageID) == null;  //null if we originated this query
            
            String res = retrieveFile(file, originAddr, messageID);
            Debug.DEBUG("Results is " + res.length() + " bytes", "onHitQuery");

            //remove the file from the list of files we want to request
            parent.removeFile(file);

        } else {

            //check in case the entry was flushed 
            originAddr = parent.getUpstream(messageID);
            if(originAddr == null) //some problem occurred, remove the request from network
                return;

            sendPacket(originAddr, parent.getQUERYPORT(), pkt);
            // otherwise forward it along
        }
    }

    private String retrieveFile(String file, InetAddress sentAddr, int messageID) {
        StringBuilder res = null;
        try {
            Socket socket = new Socket(sentAddr, parent.getHTTPPORT());
            GnutellaPacket pkt = new GnutellaPacket(messageID, GnutellaPacket.OBTAIN,
                    GnutellaPacket.DEF_TTL, GnutellaPacket.DEF_HOPS, Utility.stringToByteArray(file));
            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());
            out.write(pkt.pack());

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(),
                            StandardCharsets.US_ASCII));

            res = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null && socket.isConnected() &&
                    !socket.isInputShutdown()) {
                res.append(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        assert res != null;
        return res.toString();
    }
}
