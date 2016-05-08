/* Debug
*  Functions for debugging.
*/

class Debug {
    private static boolean debug = false;

    public static void setDebug(boolean deb) {
        debug = deb;
    }

    public static boolean getDebug() {
        return debug;
    }

    public static void DEBUG(String s, String function) {
        if (debug)
            System.out.println("\t[" + function + "]:\t"  + s);
    }

    public static void DEBUG_F(String s, String function) {
        if (debug) {
            System.out.println("======================================");
            DEBUG(s, function);
            System.out.println("======================================");
        }
    }
}
