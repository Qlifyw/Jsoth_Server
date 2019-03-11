package sample;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Terminal {

    public static final String TERMINAL_USERNAME = "username";
    public static final String TERMINAL_CURRENT_PATH = "path";
    public static final String TERMINAL_OUTPUT = "output";

    private static String executablePath = System.getProperty("user.dir") + File.separator;

    public static void main(String[] args) {
        System.out.println(execBash("lss"));
    }
    
    public Map execTerminalCommand(String command) {
        String username = executeCommand("hostname").replaceAll("\\r\\n|\\r|\\n", "");
        String output = execBash(command);
        Map<String, String> result = new LinkedHashMap<>();
        result.put(TERMINAL_USERNAME, username);
        result.put(TERMINAL_CURRENT_PATH, username+"@"+executablePath);
        result.put(TERMINAL_OUTPUT, output);
        return result;
    }


    private static String execBash(String command) {
        command = command.replaceAll("\\s+(?=[),])", "");
        String[] splitedArgs = command.split("\\s+");
        if ((splitedArgs[0].equalsIgnoreCase("cd") && (splitedArgs.length >= 2))) {

            // Test
            File f=new File(executablePath);
            boolean isRoot = f.toPath().getNameCount()==0;
            if(isRoot) {
                if (splitedArgs[1].contains(":")){
                    executablePath = splitedArgs[1] + File.separator;
                    return "Move to disk " + executablePath;
                }
            }

            String path = splitedArgs[1];
            Pattern pattern = Pattern.compile("\\.{2}");
            Matcher matcher = pattern.matcher(path);
            int count = 0;
            while (matcher.find()) count++;
            if (count != 0) {
                if (isRoot) {
                    return executablePath + " is root";
                }
                String[] splitPath = executablePath.split(File.separator+File.separator);
                StringBuilder trimedPath = new StringBuilder();
                for(int i=0; i<splitPath.length-count; i++) {
                    trimedPath.append(splitPath[i] + File.separator);
                }
                executablePath = trimedPath.toString();

            } else {
                String osname = System.getProperty("os.name").toLowerCase();
                String tempPath = "";
                boolean isWindows = osname.contains("win");
                if (isWindows) {
                    tempPath = executablePath + splitedArgs[1].replaceAll("/", "\\\\");
                } else {
                    tempPath = executablePath + splitedArgs[1].replaceAll("\\\\", "/");
                }
                if (new File(tempPath).exists()) {
                    if (isWindows) {
                        executablePath += splitedArgs[1].replaceAll("/", "\\\\");
                    } else {
                        executablePath += splitedArgs[1].replaceAll("\\\\", "/");
                    }
                    return "";
                } else {
                    return "No such directory";
                }

            }

        }
        return executeCommand(command);
    }

    public static String executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        Process process;
        String shell = "";
        String charset = "UTF-8";
        String shell_command_arg = "";
        try {
            String osname = System.getProperty("os.name").toLowerCase();
            if (osname.contains("win")) {
                charset = "CP866";
                shell = "cmd";
                shell_command_arg = "/c";
            } else if (osname.contains("nux") || osname.contains("nix") || osname.contains("aix") ||
                    osname.contains("sunos") || osname.contains("mac")) {
                charset = "UTF-8";
                shell = "/bin/bash";
                shell_command_arg = "-c";
            }
            process = new ProcessBuilder(shell , shell_command_arg, command).directory(new File(executablePath)).start();
            BufferedReader ber = new BufferedReader(new InputStreamReader(process.getErrorStream(), charset));
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), charset));
            String line = "";
            line = br.readLine();
            if (line != null) {
                while (line != null) {
                    output.append(line);
                    output.append("\n");
                    line = br.readLine();
                }
            } else {
                while ((line = ber.readLine()) != null) {
                    output.append(line);
                    output.append("\n");
                }
            }

        } catch (IOException /*| InterruptedException */  e) {
            // Don't log it!
            return ( command + "No such command");
        }
        return output.toString();
    }

}
