/* MessageFileRequestHandler
*  Handles file requests received by a listening FileThread.
*  Searches for a file in the servent's local machine and sends it
*  to the requesting servent.
*/


import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


class MessageFileRequestHandler extends MessageHandler {

    public MessageFileRequestHandler(GnutellaThread thread, Socket socket) {
        super(thread.servent, thread.welcomeSocket, socket);
    }

    public void onPacketReceive(InetAddress from, byte[] packet) {
        GnutellaPacket pkt = GnutellaPacket.unpack(packet);
        assert pkt != null;
        Debug.DEBUG_F("Received packet from: " + from.getCanonicalHostName()
                + ":\n" + pkt.toString(), "FileRequest: onPacketReceive");

        switch (pkt.getPayloadDescriptor()) {
            case GnutellaPacket.OBTAIN:
                onFileQuery(pkt, from);
                break;
            case GnutellaPacket.PUSH:
                onPush(pkt, from);
            default:
                System.out.println("Unrecognized descriptor");
                break;
        }
    }

    // todo: fix so you exchange UUID identifiers as well
    private void onPush(GnutellaPacket pkt, InetAddress from) {
        PushMessage msg = PushMessage.unpack(pkt.getPayload());
        Debug.DEBUG(msg.toString(), "onPush");
        if (!msg.getIdentifier().equals(parent.getIdentifier())) {
            Debug.DEBUG("Removing weird push message", "onPush");
            return;
        }

        int fileIndex = msg.getFileIndex();
        if (!parent.fileTable.containsKey(fileIndex) ||
                parent.fileTable.get(fileIndex) == null) {
            Debug.DEBUG("Haven't sent HITQUERY for file " + fileIndex, "onPush");
            return;
        }

        // otherwise send the file.
        sendFile(parent.fileTable.get(fileIndex));
    }

    private void onFileQuery(GnutellaPacket pkt, InetAddress from) {
        String uri = Utility.byteArrayToString(pkt.getPayload());
        String file = parent.conPath(uri);
        Debug.DEBUG("Responding to file query " + file, "onFileQuery");
        if (file == null || !Utility.fileExists(file)) {
            Debug.DEBUG("File not found", "onFileQuery");
            return;
        }

        Debug.DEBUG("Hashcode for " + uri + " is" + uri.hashCode(), "onFileQuery");
        if (!parent.fileTable.containsKey(uri.hashCode())) {
            Debug.DEBUG("File request not seen", "onFileQuery");
            return;
        }
        sendFile(new File(file));
    }

    private void sendFile(File f) {
        try {
            Path path = Paths.get(f.getAbsolutePath());
            byte[] fileBytes = Files.readAllBytes(path);
            Debug.DEBUG("Atttempting to write " + fileBytes.length + "bytes to connection", "onFileQuery");
            sendPayload(fileBytes);
            Debug.DEBUG("Successfully wrote all bytes to connection", "onFileQuery");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanUp();
        }

    }

    private void cleanUp() {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
            Debug.DEBUG_F("Cleaned up successfully", "cleanUp");
        }
        catch (IOException e) {
            System.out.println("Error cleaning up");
            e.printStackTrace();

        }
    }
}

