package sample.sqlite.model;

public class Device {
    private String hwid;
    private String model;
    private String last_conn;

    public Device(String hwid, String model, String os_version, int api, String last_conn) {
        this.hwid = hwid;
        this.model = model + " V" + os_version + " (" + api + ")";
        this.last_conn = last_conn;
    }

    public String getLast_conn() {
        return last_conn;
    }

    public String getModel() {
        return model;
    }

    public String getHwid() {
        return hwid;
    }

}
