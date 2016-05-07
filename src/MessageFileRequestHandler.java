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
            default:
                System.out.println("Unrecognized descriptor");
                break;
        }
    }

    private void onFileQuery(GnutellaPacket pkt, InetAddress from) {
        Debug.DEBUG("Responding to file query", "onFileQuery");

        String file = Utility.byteArrayToString(pkt.getPayload());
        if (file == null) {
            Debug.DEBUG("No file requested", "onFileQuery");
        }

        File f = new File(parent.dirRoot + "/" + file);
        if (!f.exists() || f.isDirectory()) {
            Debug.DEBUG("File doesn't exist", "onFileQuery");
            return;
        }

        try {
            Path path = Paths.get(parent.dirRoot + "/" + file);
            byte[] fileBytes = Files.readAllBytes(path);
            Debug.DEBUG("writing " + fileBytes.length + "bytes to connection", "onFileQuery");
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

