package sample;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;

public class LibraryLoader {

    private final static Logger log = LogManager.getRootLogger();

    static int DIRECTORY_ALREADY_EXISTS = 0;
    static int NO_PERMISSION = -1;
    static int SUCCESS = 1;
    static int FAILURE = 2;

    private String sigarPath = "/libs/sigar/";

    private int createDir(String path) {

        File newDir = new File(System.getProperty("user.dir") + path);
        if (newDir.exists()) {
            System.out.println("Directory already exist");
            return DIRECTORY_ALREADY_EXISTS;
        } // if directory already exist

        File currentDir = new File(System.getProperty("user.dir"));
        if (!currentDir.canWrite()) {
            System.out.println("You don't have sufficient permissions");
            return NO_PERMISSION; // check permission for dir. creating
        }

        boolean makeDir = newDir.mkdirs();
        if (makeDir) {
            System.out.println("Directory successfully created");
            return SUCCESS;
        } else  {
            System.out.println("Can't create directory");
            return FAILURE;
        }

    }


    public boolean extract() {
        // Check trouble
        boolean alreadyExtracted = (new File(System.getProperty("user.dir")+ sigarPath + "log4j.properties")).exists();
        int has_errors = createDir(sigarPath);

        if ((has_errors == NO_PERMISSION) || (has_errors == FAILURE) ) return false;

        if (alreadyExtracted) {
            PropertyConfigurator.configure(System.getProperty("user.dir") + "/libs/sigar/log4j.properties");
            log.info("Libraries already extract. Update files ... ");
        }

        try {
            // Found jar file
            CodeSource src = getClass().getProtectionDomain().getCodeSource();
            if (src != null) {
                URL jar = src.getLocation();
                ZipInputStream zip = new ZipInputStream(jar.openStream());
                while(true) {
                    // getting files from list and writing in 'libs' directory
                    ZipEntry e = zip.getNextEntry();
                    if (e == null)
                        break;
                    String name = e.getName();
                    if (name.startsWith("libs/sigar/")) {
                        File temp_file = new File(System.getProperty("user.dir") + "/" + name);
                        if (!temp_file.exists()) {
                            InputStream fileStream = getClass().getResourceAsStream("/" + name);
                            System.out.println("FILE NAME: " + name);
                            Files.copy(fileStream, temp_file.getAbsoluteFile().toPath());
                            fileStream.close();
                        }
                    }
                }
            }
            else {
                 return false;
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        // Set configuration for log4j (logger)
        if (!alreadyExtracted) {
            PropertyConfigurator.configure(System.getProperty("user.dir") + "/libs/sigar/log4j.properties");
            log.info("Files were extracted from jar");
        }
        return true;
    }


}
