import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

class QueryArray {
    private final Map<UUID, InetAddress> map;
    private final int maxEntries;

    public QueryArray(int maxEntries) {
        this.maxEntries = maxEntries;
        this.map = new LinkedHashMap<UUID, InetAddress>();
    }

    boolean contains(UUID messageID){
        return map.containsKey(messageID);
    }

    InetAddress retrieve(UUID messageID) {
        return map.get(messageID);
    }

    void add(UUID messageID, InetAddress addr) {
        if (addr != null)
            Debug.DEBUG("Adding [" + messageID + ", " + addr.getCanonicalHostName(),
                "QueryArray:add");
        map.put(messageID, addr);
        flush();
    }

    boolean containsKey(UUID messageID) {
        return map.containsKey(messageID);
    }

    private void flush() {
        // flushes oldest (?) entry
        if (map.size() > maxEntries && map.size() != 0) {
            Debug.DEBUG("Removing oldest entry", "QueryArray:flush");
            map.remove(0);
        }
    }
}
