import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;

class BroadcastThread implements Callable<Thread>  {
    private final Servent servent;
    private final InetAddress neighbor;
    private final String filename;


    public BroadcastThread(Servent servent, InetAddress neighbor, String filename, ArrayList<String> files) {
        this.servent = servent;
        this.neighbor = neighbor;
        this.filename = filename;
    }

    public Thread call() {
        Debug.DEBUG("Running broadcast thread", "call");
        if (neighbor == null || filename == null || servent == null)
            return null;

        sendQuery();

        //wait until the file no longer is being looked for (means it was downloaded successfully)
        Debug.DEBUG("Function finished", "call");
        synchronized (ClientThread.files) {
            while (!ClientThread.files.isEmpty()) {
                try {
                    ClientThread.files.wait();
                } catch (InterruptedException ex) {
                    System.out.println("Waiting for pool interrupted.");
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }

    private void sendQuery() {
        sendQuery(filename, neighbor);
    }

    private void sendQuery(String filename, InetAddress neighbor) {
        System.out.println("Sending query for " + neighbor);

        UUID descriptorID = UUID.randomUUID();
        servent.arr.add(descriptorID, GnutellaPacket.QUERY, null);
        byte[] payload = Utility.stringToByteArray(filename);
        GnutellaPacket queryPacket =
                new GnutellaPacket(descriptorID, GnutellaPacket.QUERY, GnutellaPacket.DEF_TTL, 0, payload);
        sendPacket(neighbor, servent.getQUERYPORT(), queryPacket);
    }

    private void sendPacket(InetAddress to, int port, GnutellaPacket pkt) {
        Debug.DEBUG_F("Sending packet to " + to.getCanonicalHostName()
                + ":" + pkt.toString(), "sendPacket");

        try {
            Socket s = new Socket(to, port);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            Debug.DEBUG("Packet length: " + pkt.pack().length, "sendPacket");
            out.writeInt(pkt.pack().length);
            out.write(pkt.pack());
            out.flush();
            Debug.DEBUG("Wrote the packet", "sendPacket");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
