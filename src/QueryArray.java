import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

class QueryArray {
    private final Map<Integer, InetAddress> map;
    private final int maxEntries;

    public QueryArray(int maxEntries) {
        this.maxEntries = maxEntries;
        this.map = new LinkedHashMap<Integer, InetAddress>();
    }

    boolean contains(int messageID){
        return map.containsKey(new Integer(messageID));
    }

    InetAddress retrieve(int messageID) {
        return map.get(messageID);
    }

    void add(int messageID, InetAddress addr) {
        Debug.DEBUG("Adding [" + messageID + ", " + addr.getCanonicalHostName(),
                "QueryArray:add");
        map.put(messageID, addr);
        flush();
    }

    boolean containsKey(int messageID) {
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
