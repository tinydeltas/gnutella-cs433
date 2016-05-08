/* QueryArray
*
*  Maps from a message ID to an InetAddress.  Used by a servent to 
*  return a hitquery response to the appropriate neighboring servent.
*  
*/

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


class QueryArray {
    private final Map<IDDescriptorPair, InetAddress> map;
    private final int maxEntries;

    public QueryArray(int maxEntries) {
        this.maxEntries = maxEntries;
        this.map = new LinkedHashMap<IDDescriptorPair, InetAddress>();
    }

    boolean contains(UUID messageID, int descriptor){
        return map.containsKey(new IDDescriptorPair(messageID, descriptor));
    }

    InetAddress retrieve(UUID messageID, int descriptor) {
        return map.get(new IDDescriptorPair(messageID, descriptor));
    }

    void add(UUID messageID, int descriptor, InetAddress addr) {
        if (addr != null)
            Debug.DEBUG("Adding [" + messageID + ", " + addr.getCanonicalHostName(),
                "QueryArray:add");
        map.put(new IDDescriptorPair(messageID, descriptor), addr);
        flush();
    }

    private void flush() {
        // flushes oldest (?) entry
        if (map.size() > maxEntries && map.size() != 0) {
            Debug.DEBUG("Removing oldest entry", "QueryArray:flush");
            map.remove(0);
        }
    }
}

class IDDescriptorPair {
    private UUID id = null;
    private int desc = -1;

    public IDDescriptorPair(UUID id, int descriptor) {
        this.id = id;
        this.desc = descriptor;
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof IDDescriptorPair)) {
            return false;
        }
        IDDescriptorPair pairOther = (IDDescriptorPair) other;

        return !(pairOther.id == null || pairOther.desc == -1) &&
                id.equals(pairOther.id) && desc == pairOther.desc;

    }

    @Override public int hashCode() {
        if (id == null || desc == -1)
            return -1;

        int hash = 23;
        hash = hash * 31 + id.hashCode();
        hash = hash * 31 + desc;
        return hash;
    }

}

