import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class QueryArray {
    Map<Integer, InetAddress> map;
    int maxEntries;

    public QueryArray(int maxEntries) {
        this.maxEntries = maxEntries;
        this.map = new LinkedHashMap<Integer, InetAddress>();
    }

    InetAddress retrieve(int messageID) {
        return map.get((Object) messageID);
    }

    void add(int messageID, InetAddress addr) {
        map.put(messageID, addr);
        flush();
    }

    boolean containsKey(int messageID) {
        return map.containsKey((Object) messageID);
    }

    void flush() {
        // flushes oldest (?) entry
        if (map.size() > maxEntries && map.size() != 0) {
            map.remove(0);
        }
    }
}
