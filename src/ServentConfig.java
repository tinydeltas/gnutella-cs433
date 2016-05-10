/*  ServentConfig
*  Defines the configuration for the servent and its network, i.e.
*  its root directory, id, and neighbors, and the functions to set up
*  the configuation when the servent is started.
*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ServentConfig {
    Servent parent;
    public UUID identifier;
    public String dirRoot;          // directory root where this servent stores its files
    public boolean firewall;        // if it's behind a firewall, so push needed
    public ArrayList<InetAddress> neighbors; // list of neighbors
    public ArrayList<String> filesToRequest;
    public HashMap<InetAddress, Boolean> isFirewalled;

    public InetAddress addr = null;             // InetAddress of this servent
    private boolean isUltrapeer = false;

    public final int QUERYPORT = 7777;
    public final int HTTPPORT = 5760;

    public ServentConfig(Servent servent, String configFile, String filePath) throws Exception{
        parent = servent;
        addr = InetAddress.getLocalHost();
        identifier = UUID.randomUUID();
        neighbors = new ArrayList<InetAddress>();
        isFirewalled = new HashMap<InetAddress, Boolean>();

        BufferedReader br = new BufferedReader(new FileReader(configFile));
        String line;

        boolean current = false;
        boolean found = false;
        while((line = br.readLine()) != null) {
            String[] words = line.trim().split("\\s+");

            if (words[0].equals("<Servent") && words.length > 1 &&
                    Integer.parseInt(words[1].substring(0, words[1].length() - 1)) == parent.getID()) {
                current = found = true;
            } else if (!current ||
                    words[0].equals("<Neighbors>") ||
                    words[0].equals("</Neighbors>") ||
                    words.length == 0) {
                continue;
            } else if(words[0].equals("</Servent>")){
                current = false;
            } else if(words[0].equals("ROOT:")){
                dirRoot = words[1];
                if (!dirRoot.endsWith("/"))
                    dirRoot += "/";
            } else {
                System.out.println("Attempting to get neighbor: " + words[0]);
                InetAddress addr = InetAddress.getByName(words[0]);
                neighbors.add(addr);
                if (words.length > 0) {
                    isFirewalled.put(addr, words[1].toUpperCase().equals("YES"));
                }
            }
        }

        if (!found)
            throw new IllegalArgumentException("Configuration for servent not found");

        setUpFiles(filePath);
        System.out.println(toString());
    }

    private void setUpFiles(String path) {
        filesToRequest = new ArrayList<String>();
        if (path == null)
            return;

        File f = new File(path);
        if (!f.exists() || f.isDirectory())
            return;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String file;
            while ((file = reader.readLine()) != null) {
                filesToRequest.add(file);
            }
            reader.close();
        } catch (Exception e) {
            System.err.format("Exception occurred while reading: %s", path);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public ArrayList<String> getFiles() {
        return filesToRequest;
    }

    public String toString() {
        ArrayList<String> debug =  new ArrayList<String>();

        debug.add("[Debug] " + Debug.getDebug());
        debug.add("[ROOT] " + dirRoot);
        debug.add("[FIREWALL] " + firewall);
        debug.add("[Neighbors] ");
        debug.add("[ADDR[ " + addr.toString());

        for(InetAddress ia : neighbors){
            debug.add("\t" + ia);
        }
        //return String.join("\n", debug); //couldn't get this line to compile so expanded it below
        String retval = "";
        for(String s : debug){
            retval += s + "\n";
        }
        return retval;
    }
}
