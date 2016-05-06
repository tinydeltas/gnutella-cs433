import com.sun.management.UnixOperatingSystemMXBean;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class PeerFileRequestHandler extends PeerHandler {

    public PeerFileRequestHandler(GnutellaThread thread, Socket socket) {
        super(thread.peer, thread.welcomeSocket, socket);
    }

    public void onPacketReceive(InetAddress from, int port, byte[] packet) {
        GnutellaPacket pkt = GnutellaPacket.unpack(packet);
        assert pkt != null;
        Debug.DEBUG_F("Received packet from: " + from.getCanonicalHostName()
                + ":\n" + pkt.toString(), "FileRequest: onPacketReceive");

        switch (pkt.getPayloadDescriptor()) {
            case GnutellaPacket.OBTAIN:
                onFileQuery(pkt, port, from);
                break;
            default:
                System.out.println("Unrecognized descriptor");
                break;
        }
    }

    private void onFileQuery(GnutellaPacket pkt, int port, InetAddress from) {
        Debug.DEBUG("Responding to file query", "onFileQuery");
        String file = Utility.byteArrayToString(pkt.getPayload());
        assert file != null;
        File f = new File(parent.dirRoot + "/" + file);
        if (!f.exists() || f.isDirectory()) {
            Debug.DEBUG("File doesn't exist", "onFileQuery");
            return;
        }
        DataOutputStream out = null;
        try {
            Debug.DEBUG("writing bytes to connection", "onFileQuery");
            //out = new DataOutputStream(socket.getOutputStream());
            Path path = Paths.get(parent.dirRoot + "/" + file);
            System.out.println(Utility.byteArrayToString(Files.readAllBytes(path)));
            
            sendPayload(from, port, Files.readAllBytes(path));

            //out.write(Files.readAllBytes(path));
            //out.flush();
            Debug.DEBUG("Successfully wrote all bytes to connection", "onFileQuery");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanUp(out);
        }
    }

    private void cleanUp(DataOutputStream outToClient) {
        try {
            outToClient.close();
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

