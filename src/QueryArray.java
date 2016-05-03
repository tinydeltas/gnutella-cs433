import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class QueryArray {
    Map<Integer, InetAddress> map;
    int maxEntries;

    public QueryArray(int maxEntries) {
        this.maxEntries = maxEntries;
        this.map = new HashMap<Integer, InetAddress>();
    }

    InetAddress retrieve(int messageID) {
        return null;
    }

    void add(int messageID, InetAddress addr) {

    }

    void flush() {
        // flushes oldest (?) entry
    }
}
