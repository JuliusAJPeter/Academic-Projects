package fi.aalto.cs.e4100.g09.project2;

public final class Urls {

    private static final int    PORT = 443;
    private static final String PATH_LOGIN = "/login";
    private static final String PATH_IMAGE_OCR = "/image";
    private static final String PATH_STORE = "/store";
    private static final String PATH_HISTORY = "/store";
    private static final String PATH_SOURCE = "/source";

    private static Urls instance;
    private String ipHost;

    private Urls(String ipHost) {
        this.ipHost = ipHost;
    }

    public static Urls getInstance(String ipHost) {
        if (instance == null) {
            instance = new Urls(ipHost);
        }
        return instance;
    }

    private String base() {
        return "https://"+ipHost+":"+PORT;
    }

    public String login() {
        return base()+PATH_LOGIN;
    }

    public String imageOcr() {
        return base()+PATH_IMAGE_OCR;
    }

    public String store() {
        return base()+PATH_STORE;
    }

    public String history() {
        return base()+PATH_HISTORY;
    }

    public String source() {
        return base()+PATH_SOURCE;
    }

}
