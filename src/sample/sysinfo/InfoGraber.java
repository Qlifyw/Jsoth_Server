package sample.sysinfo;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.hyperic.sigar.*;
import sample.LibraryLoader;
import sample.Terminal;
import sample.utils.NetUtils;

import java.net.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static sample.utils.NetUtils.getExternalIP;

public class InfoGraber {

    public static final String SERVER_STATE = "State";
    public static final String SERVER_MACHINE_NAME = "MachineName";
    public static final String SERVER_OS = "OSName";
    public static final String SERVER_UPTIME = "UpTime";
    public static final String SERVER_BLOCK_NET = "Net";
    public static final String SERVER_EXTERNAL_IP = "ExtIP";
    public static final String SERVER_LOCAL_IP = "LocalIP";
    public static final String SERVER_COUNTRY_CODE = "CountryCode";
    public static final String SERVER_BLOCK_CPU = "CPU";
    public static final String SERVER_BLOCK_CPU_DETAILS = "Details";
    public static final String SERVER_CPU_CORES = "Cores";
    public static final String SERVER_CPU_MODEL = "Model";
    public static final String SERVER_CPU_VENDOR = "Vendor";
    public static final String SERVER_CPU_MHZ = "Mhz";
    public static final String SERVER_CPU_USAGES = "Usage";
    public static final String SERVER_CPU_LOADED_USER = "User";
    public static final String SERVER_CPU_LOADED_SYSTEM = "System";
    public static final String SERVER_CPU_IDLE = "Idle";
    public static final String SERVER_BLOCK_MEMORY = "Memory";
    public static final String SERVER_BLOCK_RAM = "RAM";
    public static final String SERVER_RAM_TOTAL = "Total";
    public static final String SERVER_RAM_USED = "Used";
    public static final String SERVER_RAM_FREE = "Free";
    public static final String SERVER_RAM_USED_PERCENT = "UsedPerc";
    public static final String SERVER_BLOCK_SWAP = "SWAP";
    public static final String SERVER_SWAP_TOTAL = "Total";
    public static final String SERVER_BLOCK_DISKS = "Disks";
    public static final String SERVER_LOCAL_DISKS = "LocalDisks";
    public static final String SERVER_DISK_DEVNAME = "DevName";
    public static final String SERVER_DISK_USED = "Used";
    public static final String SERVER_DISK_FREE = "Free";
    public static final String SERVER_DISK_TOTAL = "Total";
    public static final String SERVER_DISK_USED_PERCENT = "UsedPerc";

    private final static Logger log = LogManager.getRootLogger();

    @SuppressWarnings("unchecked")
    public Map run () throws SigarException {

        Map SystemInfo = new LinkedHashMap();
        if (!(new LibraryLoader().extract())) {
            SystemInfo.put(SERVER_STATE, false);
            return SystemInfo;
        } else {
            SystemInfo.put(SERVER_STATE, true);
        }

        Sigar sigar = new Sigar();
        sigar.enableLogging(false);

        // Get machine name
        try {
            String HostName = InetAddress.getLocalHost().getHostName();
            SystemInfo.put(SERVER_MACHINE_NAME, HostName);
        } catch (UnknownHostException e) {
            log.warn("Failure getting Host Name");
            SystemInfo.put(SERVER_MACHINE_NAME, "<No info>");
        }

        SystemInfo.put(SERVER_OS, getOSname());

        Map Net = new LinkedHashMap();
        // Get Local IP
        Net.put(SERVER_LOCAL_IP, sigar.getFQDN());


        String ExtIP = "";
        try {
            ExtIP = getExternalIP();
            Net.put(SERVER_EXTERNAL_IP, ExtIP);
        } catch (Exception e) {
            log.warn("Failure getting External IP");
            Net.put(SERVER_EXTERNAL_IP, ExtIP);
        }

        // Get Country code
        String countryCode  = "";
        try {
            countryCode = NetUtils.getCountryCode(ExtIP);
            Net.put(SERVER_COUNTRY_CODE, countryCode);
        } catch (Exception e) {
            log.warn("Failure getting Country Code");
            Net.put(SERVER_COUNTRY_CODE, countryCode);
        }
        SystemInfo.put(SERVER_BLOCK_NET, Net);

        Map Memory = new LinkedHashMap();
        Map Ram = new LinkedHashMap();
        // Get RAM usage statistics
        Mem mem = sigar.getMem();
        Ram.put(SERVER_RAM_TOTAL,getGB(mem.getTotal()));
        Ram.put(SERVER_RAM_USED,getGB(mem.getUsed()));
        Ram.put(SERVER_RAM_FREE,getGB(mem.getFree()));
        Ram.put(SERVER_RAM_USED_PERCENT, getOneDecimalPlacePercent(mem.getUsedPercent()));
        Memory.put(SERVER_BLOCK_RAM, Ram);

        Map Swap = new LinkedHashMap();
        // Get SWAP stats
        Swap swap = sigar.getSwap();
        Swap.put(SERVER_SWAP_TOTAL, getGB(swap.getTotal()-mem.getTotal()));
        Memory.put(SERVER_BLOCK_SWAP, Swap);
        SystemInfo.put(SERVER_BLOCK_MEMORY, Memory);

        Map Cpu = new LinkedHashMap();
        Map Usage = new LinkedHashMap();
        // Get CPU usage statistics
        CpuPerc cpuPerc = sigar.getCpuPerc();
        Usage.put(SERVER_CPU_LOADED_USER, toPercentFormat(cpuPerc.getUser()));
        Usage.put(SERVER_CPU_LOADED_SYSTEM, toPercentFormat(cpuPerc.getSys()));
        Usage.put(SERVER_CPU_IDLE, toPercentFormat(cpuPerc.getIdle()));
        Cpu.put(SERVER_CPU_USAGES, Usage);

        Map Detail = new LinkedHashMap();
        // Get CPU detail info
        CpuInfo[] cpuInfo = sigar.getCpuInfoList();
        Detail.put(SERVER_CPU_CORES, cpuInfo[0].getTotalCores());
        Detail.put(SERVER_CPU_MODEL, cpuInfo[0].getModel());
        Detail.put(SERVER_CPU_VENDOR, cpuInfo[0].getVendor());
        Detail.put(SERVER_CPU_MHZ, cpuInfo[0].getMhz());
        Cpu.put(SERVER_BLOCK_CPU_DETAILS, Detail);
        SystemInfo.put(SERVER_BLOCK_CPU, Cpu);

        // Get System's UpTime
        String uptime = getUpTime(sigar.getUptime().getUptime());
        SystemInfo.put(SERVER_UPTIME, uptime);

        Map Disks = new LinkedHashMap();
        // Get File system usage
        ArrayList<Map> LocalDisks = new ArrayList<>();
        FileSystem[] fileSystemList = sigar.getFileSystemList();
        for (int i = 0; i < fileSystemList.length; i++) {
            Map temp = new LinkedHashMap();
            FileSystem fs = fileSystemList[i];
            if (fs.getType() == FileSystem.TYPE_LOCAL_DISK){
                FileSystemUsage usage = sigar.getFileSystemUsage(fs.getDirName());

                String devName = fs.getDevName();
                temp.put(SERVER_DISK_DEVNAME , devName);
                temp.put(SERVER_DISK_TOTAL , getDiskSpace(usage.getTotal()));
                temp.put(SERVER_DISK_USED , getDiskSpace(usage.getUsed()));
                temp.put(SERVER_DISK_FREE , getDiskSpace(usage.getFree()));
                temp.put(SERVER_DISK_USED_PERCENT , usage.getUsePercent());
                LocalDisks.add(temp);
            }
        }
        Disks.put(SERVER_LOCAL_DISKS, LocalDisks);
        SystemInfo.put(SERVER_BLOCK_DISKS, Disks);

        return SystemInfo;
    }

    String getOSname() {

        String osname = System.getProperty("os.name").toLowerCase();
        if (osname.contains("win")) {
            return System.getProperty("os.name");
        } else if (osname.contains("nux") || osname.contains("nix") || osname.contains("aix")) {
            String hostnameInfo = Terminal.executeCommand("hostnamectl");
            String pattern = "^\\s*Operating System:\\s*(.*)";

            Pattern r = Pattern.compile(pattern, Pattern.MULTILINE);
            Matcher m = r.matcher(hostnameInfo);
            if (m.find()) {
                return m.group(1);
            }
        } else if (osname.contains("mac") || osname.contains("darwin")) {
            return System.getProperty("os.name") + " " + System.getProperty("os.version");
        }
        return "-";
    }

    // getting server uptime. Format -- days:hours:minutes:seconds
    private String getUpTime(double seconds) {
        int days = (int)seconds/(60*60*24);
        seconds = seconds%(60*60*24);
        int hours = (int)seconds/(60*60);
        seconds = seconds%(60*60);
        int minutes  = (int)seconds/(60);
        seconds = seconds%60;
        return (days + ":" + hours + ":" + minutes + ":" + (int)seconds);
    }

    // Convert from bytes to Gigabytes/ Rounding to 2 decimal places
    private double getGB(double d) {
        d /= 1000000000;
        int i = (int)Math.round(d*100);
        d = (double) i/100;
        return d;
    }

    // Convert double in percent format
    // Example: 0.09 -> 9 %
    private double toPercentFormat(double percent) {
        percent *= 1000;
        percent = Math.round(percent);
        int i = (int)percent;
        return (double)i/10;
    }

    //  Convert from kb to Gb. Rounding to 2 decimal places
    private double getDiskSpace(double d) {
        d /= 1000000;
        int i = (int)Math.round(d*100);
        d = (double) i/100;
        return d;
    }

    // Rounding to 1 decimal place
    private double getOneDecimalPlacePercent(double d) {
        int i = (int) Math.round(d*10);
        d = (double) i/10;
        return d;
    }

}
