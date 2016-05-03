
public class Debug {

    public final boolean debug = true;

    public void DEBUG(String s, String function) {
        System.out.println("[" + function +"]: "  + s);
    }

    public void debugPacket(GnutellaPacket packet) {

    }

    public void DEBUG_F(String s, String function) {
        System.out.println("======================================");
        DEBUG(s, function);
        System.out.println("======================================");
    }

}
