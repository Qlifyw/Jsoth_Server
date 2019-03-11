package sample.sysinfo;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hyperic.sigar.SigarException;
import sample.sysinfo.InfoGraber;

import java.util.Map;


public class GrabRunner implements Runnable {
    Thread thread;
    Map info;

    private final static Logger log = LogManager.getRootLogger();

    public GrabRunner() {
        thread = new Thread(this, "GrabInfo");
        thread.start();
    }

    @Override
    public void run() {
        try {
            info = new InfoGraber().run();
        } catch (SigarException e) {
            log.error("GrabInfo: run thread: | " + e.toString());
        }
    }

    public Map getMap(){
        System.out.println("Join thread");
        if (!thread.isAlive()) {
            return info;
        } else {
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.error("GrabInfo: getMap() join thread: | " + e.toString());
            }
        }
        return info;
    }
}
