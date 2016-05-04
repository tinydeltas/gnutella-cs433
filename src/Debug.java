
class Debug {

    public final boolean debug = true;

    public static void DEBUG(String s, String function) {
        System.out.println("\t[" + function + "]:\t"  + s);
    }

    public static void DEBUG_F(String s, String function) {
        System.out.println("======================================");
        DEBUG(s, function);
        System.out.println("======================================");
    }

}
