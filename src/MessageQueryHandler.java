import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;

class MessageQueryHandler extends MessageHandler {

    public MessageQueryHandler(GnutellaThread thread, Socket socket) {
        super(thread.servent, thread.welcomeSocket, socket);
        Debug.DEBUG("Creating new handler for " + socket.toString(), "MessageQueryHandler constructor");
    }

    public void onPacketReceive(InetAddress from, byte[] packet) {
        // call either onQuery or onHitQuery
        GnutellaPacket pkt = GnutellaPacket.unpack(packet);
        if (pkt == null) {
            Debug.DEBUG("uh oh", "onPacketReceive");
            return;
        }

        Debug.DEBUG_F("Packet received: " + from.getCanonicalHostName() + ":\n"
            + pkt.toString(), "MessageQueryHandler: onPacketReceive");

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
            case GnutellaPacket.BYE:
                onBye(from, pkt);
                break;
            default:
                System.out.println("Unrecognized descriptor");
                break;
        }
    }

    // todo
    private void onBye(InetAddress from, GnutellaPacket pkt) {
        // see
//        A Bye packet MUST be sent with TTL=1 (to avoid accidental propagation
//        by an unaware servent), and hops=0 (of course).
//
//        A servent receiving a Bye message MUST close he connection
//        immediately. The servent that sent the packet MUST wait a few
//        seconds for the remote host to close the connection before closing
//        it.  Other data MUST NOT be sent after the Bye message.  Make sure
//        any send queues are cleared.
//
//                The servent that sent by Bye message MAY also call shutdown() with
//        'how' set to 1 after sending the Bye message, partially closing the
//        connection.  Doing a full close() immediately after sending the Bye
//        messages would prevent the remote host from possibly seeing the Bye
//        message.
//
//                After sending the Bye message, and during the "grace period" when
//        we don't immediately close the connection, the servent MUST read
//        all incoming messages, and drop them unless they are Query Hits
//        or Push, which MAY still be forwarded (it would be nice to the
//        network).  The connection will be closed as soon as the servent
//        gets an EOF condition when reading, or when the "grace period"
//        expires.
    }

    /// REGULAR QUERY HANDLING
    private void onQuery(InetAddress from, GnutellaPacket pkt) {
        Debug.DEBUG_F("Handling a query from: " + from, "onQuery");

        parent.addMessageID(pkt.getMessageID(), GnutellaPacket.QUERY, from);

        String file = Utility.byteArrayToString(pkt.getPayload());
        Debug.DEBUG("File to search: " + file, "onQuery");

        if (Utility.fileExists(parent.conPath(file))) {
            Debug.DEBUG("File exists", "onQuery");
            GnutellaPacket respPkt = makeResponsePacket(pkt, file);
            forwardPacket(from, super.parent.getQUERYPORT(), respPkt);   // send packet upstream
        }

        if (pkt.getTTL() - 1 != 0) {
            Debug.DEBUG("Forwarding to neighbors", "onQuery");
            GnutellaPacket forwardPkt = makeForwardPacket(pkt);
            forwardToNeighbors(from, forwardPkt); //forwarding to neighbors using TCP socket
        }
    }

    private GnutellaPacket makeForwardPacket(GnutellaPacket pkt) {

        return new GnutellaPacket(pkt.getMessageID(),
                GnutellaPacket.QUERY,
                pkt.getTTL() - 1,   // decrement ttl
                pkt.getHops() + 1, // increment hops (altho doesn't really matter)
                pkt.getPayload());
    }

    private GnutellaPacket makeResponsePacket(GnutellaPacket pkt, String file) {
        byte[] payload = Utility.stringToByteArray(parent.getName() + ";" + file);

        return new GnutellaPacket(pkt.getMessageID(),
                GnutellaPacket.HITQUERY,
                pkt.getHops() + 2,   // set ttl to hops + 2, as specified in protocol
                0,                  // reset hops to 0
                payload);
    }

    private void forwardToNeighbors(InetAddress from, GnutellaPacket pkt) {
        for (InetAddress n : parent.getNeighbors()) {
            if (from.getCanonicalHostName().equals(n.getCanonicalHostName())) {
                Debug.DEBUG("Skipping node that sent this", "forwardToNeighbors");
                continue;
            }
            Debug.DEBUG("Forwarding to neighbor: " + n.toString() + " from: " + from.toString(), "forwardToNeighbor");
            forwardPacket(n, parent.getQUERYPORT(), pkt);
        }
    }

    /// HIT QUERY HANDLING
    private void onHitQuery(InetAddress from, GnutellaPacket pkt) {
        Debug.DEBUG_F("Received hit query from " + from.getCanonicalHostName(), "onHitQuery");
        UUID messageID = pkt.getMessageID();

        if (!parent.containsID(messageID, GnutellaPacket.QUERY)) {
            Debug.DEBUG("Original query not seen, removing request from network ", "onHitQuery");
            return;
        }


        InetAddress upstream = parent.getUpstream(messageID, GnutellaPacket.QUERY);
        if (upstream == null) {
            //  this was my request!!, open connection to port & retrieve file
            assert parent.arr.retrieve(messageID, GnutellaPacket.QUERY) == null;  //null if we originated this query

            try {
                String payload = Utility.byteArrayToString(pkt.getPayload());
                assert payload != null;
                String payloadWords[] = payload.split(";");

                assert payloadWords.length == 2;
                String host = payloadWords[0];
                String file = payloadWords[1];
                InetAddress originAddr = InetAddress.getByName(host);

                Debug.DEBUG("Host: " + host + " File: " + file +
                        " originAddr: " + originAddr.toString(), "onHitQuery");

                retrieveFile(file, originAddr, messageID);
                //remove the file from the list of files we want to request
                parent.removeFile(file);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { // otherwise forward it along
            forwardPacket(upstream, parent.getQUERYPORT(), pkt);
        }
    }

    // File requesting is done through packets as well, although
    // more complex form in the actual implementation
    private GnutellaPacket makeRequestPacket(UUID messageID, String file) {
        return new GnutellaPacket(messageID,
                GnutellaPacket.OBTAIN,    // special descriptor we added for this
                GnutellaPacket.DEF_TTL,   // these don't matter
                GnutellaPacket.DEF_HOPS,
                Utility.stringToByteArray(file));
    }


    private void retrieveFile(String file, InetAddress sentAddr, UUID messageID) {
        Socket sk = null;
        try {
            sk = new Socket(sentAddr, parent.getHTTPPORT());
            DataInputStream r = new DataInputStream(sk.getInputStream());
            sendRequestPacket(sk, makeRequestPacket(messageID, file));    // sent off the request packet

            int length = r.readInt();

            int numRead, read;
            numRead = 0;
            byte[] holder = new byte[1024];
            while ((read = r.read(holder)) != -1 &&
                    !sk.isInputShutdown() && !sk.isClosed()) {
                numRead += read;
            }

            System.out.println("\t[" + file + "] Downloaded " + numRead + " bytes out of " + length);
            if (length != numRead)
                Debug.DEBUG_F("Read wrong length", "retrieveFile");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sk != null){
                try {
                    sk.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
