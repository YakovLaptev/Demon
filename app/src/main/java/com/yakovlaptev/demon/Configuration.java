package com.yakovlaptev.demon;

public class Configuration {

    public static final boolean DEBUG_VERSION = true;

    public static final int GROUPOWNER_PORT = 4545;
    public static final int CLIENT_PORT = 5000;
    public static final int THREAD_COUNT = 20;
    public static final int THREAD_POOL_EXECUTOR_KEEP_ALIVE_TIME = 10;

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "p2pChat";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int FIRSTMESSAGEXCHANGE = 0x400 + 2;

    public static final String MESSAGE_READ_MSG = "MESSAGE_READ";
    public static final String FIRSTMESSAGEXCHANGE_MSG = "FIRSTMESSAGEXCHANGE";

    public static final String MAGICADDRESSKEYWORD = "4<D<D<R<3<5<5";
    public static final String PLUSSYMBOLS = "++++++++++++++++++++++++++";
}
