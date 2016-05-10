/* MessageQueryHandler
*  Handles queries/hitqueries seen by a listening QueryThread.
*  Determines if the requested file is available locally, and sends
* a hitquery if so; otherwise forwards the query to its neighbors.
*/
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.UUID;

/**
 * Handles queries passing through the servent
 */
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
        // remove from set of neighbors
        String message = Utility.byteArrayToString(pkt.getPayload());
        System.out.println("BYE message: " + message);
        for (InetAddress n : parent.getNeighbors()) {
            if (n.equals(from)) {
                parent.getNeighbors().remove(n);
            }
        }

    }

    /// REGULAR QUERY HANDLING
    private void onQuery(InetAddress from, GnutellaPacket pkt) {
        Debug.DEBUG_F("Handling a query from: " + from, "onQuery");

        parent.addMessageID(pkt.getMessageID(), GnutellaPacket.QUERY, from);

        String uri = Utility.byteArrayToString(pkt.getPayload());
        String file = parent.conPath(uri);
        Debug.DEBUG("File to search: " + file, "onQuery");

        if (Utility.fileExists(file)) {
            Debug.DEBUG("File exists: " + uri, "onQuery");
            File f = new File(file);
            parent.fileTable.put(uri.hashCode(), f);
            Debug.DEBUG("Putting as key: " + uri.hashCode(), "onQuery");
            GnutellaPacket respPkt = makeResponsePacket(pkt, uri);
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
        String resp = parent.getName() + ";" + parent.getIdentifier() + ";" + file;
        byte[] payload = Utility.stringToByteArray(resp);

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

                assert payloadWords.length == 3;
                String host = payloadWords[0];
                UUID identifier = UUID.fromString(payloadWords[1]);
                String file = payloadWords[2];
                InetAddress originAddr = InetAddress.getByName(host);

                Debug.DEBUG("Host: " + host + " File: " + file +
                        " originAddr: " + originAddr.toString(), "onHitQuery");

                if (parent.isFirewalled(originAddr)) {


                    Socket sk = new Socket(originAddr, parent.getHTTPPORT());
                    sendRequestPacket(sk, makePushPacket(identifier, file));
                    return;
                }

                retrieveFile(file, originAddr, messageID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { // otherwise forward it along
            forwardPacket(upstream, parent.getQUERYPORT(), pkt);
        }
    }


    // request from client to server to push stuff
    private GnutellaPacket makePushPacket(UUID identifier, String file) {
        Debug.DEBUG("Hashcode for: " + file + " is" + file.hashCode(), "makePushPacket");
        PushMessage msg = new PushMessage(identifier,
                file.hashCode(),
                parent.getAddress(),
                parent.getHTTPPORT());
        Debug.DEBUG(msg.toString(), "makePushPacket");
        return new GnutellaPacket(UUID.randomUUID(),
                GnutellaPacket.PUSH,
                GnutellaPacket.DEF_TTL,
                GnutellaPacket.DEF_HOPS,
                msg.pack());

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
            String newfilename = parent.cfg.dirRoot + file + "-download";
            System.out.println(newfilename);

            FileOutputStream out = new FileOutputStream(newfilename);

            byte[] holder = new byte[1024];
            while ((read = r.read(holder)) != -1 &&
                    !sk.isInputShutdown() && !sk.isClosed()) {
                numRead += read;
                out.write(holder, 0, read);
            }

            System.out.println("\t[" + file + "] Downloaded " + numRead + " bytes out of " + length);
            if (length != numRead){
                System.out.println(file + " not successfully downloaded.");
                Debug.DEBUG_F("Read wrong length", "retrieveFile");
            }
            else{
                System.out.println(file + " was successfully downloaded. Contents of file:");
                
                BufferedReader br = new BufferedReader(new FileReader(newfilename));
                String line = null;
                while ((line = br.readLine()) != null) {
                System.out.println(line);
                }
                 //remove the file from the list of files we want to request
                parent.removeFile(file);
            }
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
