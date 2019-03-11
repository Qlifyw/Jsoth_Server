package sample;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import sample.sqlite.DbHelper;
import sample.sysinfo.GrabRunner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.prefs.Preferences;

public class SocketRunner {

    final static int GREETING = 100;
    final static int HANDSHAKE = 200;
    final static int TERMINAL = 300;
    final static int SCREENSHOT = 400;
    final static int STATISTICS = 500;
    final static int HWID = 600;


    public static final String JSON_CODE = "code";
    public static final String JSON_HWID = "hwid";
    public static final String JSON_IS_KNOWN = "isKnown";
    public static final String JSON_RSA_PUBLIC_KEY = "publicKey";
    public static final String JSON_MESSAGE = "msg";
    public static final String JSON_TERMINAL_COMMAND = "command";
    public static final String JSON_HARDWARE_INFO =  "hardwareInfo";
    public static final String JSON_AES_KEY =  "Aes";
    public static final String JSON_AES_KEY_HASH =  "AesHash";

    public static final String PREFS_FILE_NAME =  "jsoth";
    public static final String PREFS_KEY_PASSWORD =  "pass";

    public static final String DEVICE_MODEL =  "model";
    public static final String DEVICE_VERSION =  "version";
    public static final String DEVICE_API =  "api";


    private final static Logger log = LogManager.getRootLogger();

    public static void runServer() throws IOException {

        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(DbHelper.IP_ADDRESS, DbHelper.PORT);
        try {
            serverSocketChannel.bind(inetSocketAddress);
        } catch (IOException e){
            serverSocketChannel.close();
            selector.close();
            throw e;
        }
        serverSocketChannel.configureBlocking(false);

        int ops = serverSocketChannel.validOps();
        SelectionKey selectionKey = serverSocketChannel.register(selector, ops, null);

        while (true) {
            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();

            while (iterator.hasNext() && !Thread.currentThread().isInterrupted()) {
                SelectionKey key = iterator.next();

                if(key.isAcceptable()) {
                    SocketChannel client = serverSocketChannel.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                    System.out.println("Greet accepted ...");
                }

                if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();


                    ByteBuffer request = ByteBuffer.allocate(16);
                    ByteArrayOutputStream baos_request = new ByteArrayOutputStream();
                    int pos = client.read(request);


                    while ((pos == -1) || (pos == 0)) {
                        if(pos == -1) break;
                        pos = client.read(request);
                    }
                    if (pos == -1) {
                        client.close();
                        iterator.remove();
                        continue;
                    }

                    while ((pos != -1) && (pos != 0)) {
                        //request.rewind();
                        baos_request.write(Arrays.copyOf(request.array(), pos));
                        request.clear();
                        pos = client.read(request);
                    }

                    String json_request = new String(baos_request.toByteArray());
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    Gson gson = gsonBuilder.create();
                    Map mapRequest = gson.fromJson(json_request, Map.class);
                    int iCode = ((Double) mapRequest.get(JSON_CODE)).intValue();

                    switch (iCode) {
                        case GREETING:
                            System.out.println("GREETING");
                            greeting(client, mapRequest);
                            break;
                        case HANDSHAKE:
                            System.out.println("HANDSHAKE");
                            handshake(client, mapRequest);
                            break;
                        case TERMINAL:
                            System.out.println("TERMINAL");
                            execTerminalCommand(client, mapRequest);
                            break;
                        case SCREENSHOT:
                            System.out.println("SCREENSHOT");
                            break;
                        case STATISTICS:
                            System.out.println("STATISTICS");
                            sendSystemInfo(client, mapRequest);
                            break;
                        case HWID:
                            System.out.println("HWID");
                            receiveDeviceInfo(client, mapRequest);
                            break;

                    }
                    //client.close();

                }
                iterator.remove();
                System.out.println("Waiting for message");
            }
            if (Thread.currentThread().isInterrupted()) {
                serverSocketChannel.close();
                selector.close();
                System.out.println("interrupt");
                break;
            }


        }
    }

    public static boolean greeting(SocketChannel client, Map request) {
        try {
            String hwid_str = (String)request.get(JSON_HWID);
            int isKnown = ((Double)request.get(JSON_IS_KNOWN)).intValue();
            boolean isRowExists = DbHelper.checkRowByHWID(DbHelper.dbName, hwid_str);
            DbHelper.insertHWID(DbHelper.dbName, hwid_str);

            if (isRowExists && isKnown != 0) {
                // if 1|0; 0|1; 0|0
                ByteBuffer response = ByteBuffer.allocate(8);
                response.putInt(1);
                response.flip();
                client.write(response);
                return false;
            }

        } catch (IOException e) {
            log.error("Greeting: read bytes: " + e.toString());
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("Greeting: close connection: " + e.toString());
            }
        }

        return true;
    }

    public static boolean handshake(SocketChannel client, Map request) {
        try {
            byte[] pkKey = Base64.getDecoder().decode((String)request.get(JSON_RSA_PUBLIC_KEY));
            byte[] encMsg = Base64.getDecoder().decode((String)request.get(JSON_MESSAGE));
            String hwid_str = (String)request.get(JSON_HWID);;


            // Get checksum
            Preferences preferences = Preferences.userRoot().node(PREFS_FILE_NAME);
            String pass = preferences.get(PREFS_KEY_PASSWORD, " ");
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] hashed_pass = md.digest(pass.getBytes());

            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec encpb = new X509EncodedKeySpec(pkKey);
            PublicKey publicKey = kf.generatePublic(encpb);
            byte[] encrypted_hash_pass = Encryption.rsaEnc(hashed_pass, publicKey);

            boolean isEquals = Arrays.equals(encMsg, encrypted_hash_pass);
            if (!isEquals) {
                return false;
            }
            // Get checksum

            // Generate AES
            Encryption.aesEnc gAES = new Encryption.aesEnc();
            Key aesKey = gAES.getKey();

            boolean isRowExist = DbHelper.checkRowByHWID(DbHelper.dbName, hwid_str);
            //if (isRowExist) {
            DbHelper.updateKey(DbHelper.dbName, Base64.getEncoder().encodeToString(aesKey.getEncoded()), hwid_str);
            //}

            byte[] aesEnc = Encryption.rsaEnc(aesKey.getEncoded(), publicKey);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(aesKey.getEncoded());
            baos.write(hashed_pass);
            byte[] hashAES = md.digest(baos.toByteArray());
            byte[] hashAesEnc = Encryption.rsaEnc(hashAES, publicKey);
            // Generate AES

            // Send response
            Map<String, String> response = new LinkedHashMap<>();
            response.put(JSON_AES_KEY, Base64.getEncoder().encodeToString(aesEnc));
            response.put(JSON_AES_KEY_HASH, Base64.getEncoder().encodeToString(hashAesEnc));
            Gson gson = (new GsonBuilder().create());
            String sResponse = gson.toJson(response);

            ByteBuffer bResponse = ByteBuffer.allocate(sResponse.getBytes().length);
            bResponse.put(sResponse.getBytes());
            bResponse.flip();
            client.write(bResponse);
            // Send response


        } catch (IOException e) {
            log.error("Handshake: I/O : " + e.toString());
        } catch (NoSuchAlgorithmException e) {
            log.error("Handshake: Encryption: " + e.toString());
        } catch (InvalidKeyException e) {
            log.error("Handshake: Encryption key: " + e.toString());
        } catch (NoSuchPaddingException e) {
            log.error("Handshake: Encryption padding: " + e.toString());
        } catch (BadPaddingException e) {
            log.error("Handshake: Encryption RSA padding: " + e.toString());
        } catch (InvalidKeySpecException e) {
            log.error("Handshake: Encryption generate key: " + e.toString());
        } catch (IllegalBlockSizeException e) {
            log.error("Handshake: Encryption RSA blocks: " + e.toString());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("Handshake: close connection: " + e.toString());
            }
        }
        return true;
    }

    public static boolean receiveDeviceInfo(SocketChannel client, Map request)  {
        try {
            String hwid_str = (String)request.get(JSON_HWID);;
            byte[] enc_info = Base64.getDecoder().decode((String)request.get(JSON_HARDWARE_INFO));

            // Receive Device info
            SecretKeySpec sks = new SecretKeySpec(Base64.getDecoder().decode(DbHelper.getKey(DbHelper.dbName, hwid_str).getBytes()), "AES");
            Encryption.aesEnc gAES = new Encryption.aesEnc();
            Cipher cipher = gAES.getCipher();
            cipher.init(Cipher.DECRYPT_MODE, sks);
            byte[] dec_info = cipher.doFinal(enc_info);

            Gson gson = new Gson();
            Map info = gson.fromJson(new String(dec_info), Map.class);

            boolean isRowExists = DbHelper.checkRowByHWID(DbHelper.dbName, (String)info.get(JSON_HWID));
            if (isRowExists) DbHelper.insertDeviceInfo(DbHelper.dbName,
                    (String)info.get(DEVICE_MODEL),
                    (String)info.get(DEVICE_VERSION),
                    ((Double)info.get(DEVICE_API)).intValue(),
                    (String)info.get(JSON_HWID));

            return true;
            // Receive Device info


        } catch ( NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            log.error("ReceiveDeviceInfo: -- : " + e.toString());
            return false;
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("ReceiveDeviceInfo: close connection: " + e.toString());
            }
        }
    }


    public static boolean execTerminalCommand(SocketChannel client, Map request)  {
        try {

            String hwid_str = (String)request.get(JSON_HWID);;
            byte[] enc_command = Base64.getDecoder().decode((String)request.get(JSON_TERMINAL_COMMAND));

            SecretKeySpec sks = new SecretKeySpec(Base64.getDecoder().decode(DbHelper.getKey(DbHelper.dbName, hwid_str).getBytes()), "AES");
            Encryption.aesEnc gAES = new Encryption.aesEnc();
            Cipher cipher = gAES.getCipher();
            cipher.init(Cipher.DECRYPT_MODE, sks);
            byte[] dec_command = cipher.doFinal(enc_command);

            String command = new String(dec_command);
            Terminal terminal = new Terminal();
            Map result = terminal.execTerminalCommand(command);
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            String json_result = gson.toJson(result);


            Cipher enc_cipher = gAES.getCipher();
            enc_cipher.init(Cipher.ENCRYPT_MODE, sks);
            byte[] enc_result = cipher.doFinal(json_result.getBytes(StandardCharsets.UTF_8));

            ByteBuffer result_buffer = ByteBuffer.allocate(enc_result.length);
            result_buffer.put(enc_result);
            result_buffer.flip();
            client.write(result_buffer);
            result_buffer.clear();

            return true;
            // Receive Device info


        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            log.error("ReceiveDeviceInfo: -- : " + e.toString());
            return false;
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("ReceiveDeviceInfo: close connection: " + e.toString());
            }
        }
    }

    public static void sendSystemInfo(SocketChannel client, Map request) {
        try {
            try {
                String hwid_str = (String)request.get(JSON_HWID);

                byte[] AES_key = DbHelper.getKey(DbHelper.dbName, hwid_str).getBytes();
                SecretKeySpec sks = new SecretKeySpec(Base64.getDecoder().decode(AES_key), "AES");
                Encryption.aesEnc gaes = new Encryption.aesEnc();
                Cipher cipher = gaes.getCipher();
                cipher.init(Cipher.ENCRYPT_MODE, sks);

                boolean isRowExists = DbHelper.checkRowByHWID(DbHelper.dbName, hwid_str);
                if (isRowExists) DbHelper.updateLastConnection(DbHelper.dbName, hwid_str);

                GrabRunner grabInfo = new GrabRunner();
                Map result = grabInfo.getMap();
                Gson gson = new Gson();
                String json_sysInfo = gson.toJson(result);

                byte[] json_enc = cipher.doFinal(json_sysInfo.getBytes());
                ByteBuffer info = ByteBuffer.allocate(json_enc.length);
                info.put(json_enc);
                info.flip();
                client.write(info);
                info.clear();

            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                    BadPaddingException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            log.error("sendSystemInfo: I/O: " + e.toString());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("sendSystemInfo: close connection: " + e.toString());
            }
        }


    }



}
