package sample;

import com.jfoenix.controls.*;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import sample.sqlite.DbHelper;
import sample.sqlite.model.Device;
import sample.sysinfo.GrabRunner;
import sample.utils.NetUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.prefs.Preferences;


public class Controller implements Initializable {

    @FXML
    public ImageView iv_logo, iv_stop, iv_settings, iv_main;

    @FXML
    public ImageView mark_main, mark_settings, mark_connections;

    @FXML
    public JFXPasswordField tv_password;

    @FXML
    public JFXToggleButton tb_password;

    @FXML
    public Label l_hostname, l_ip, l_status
            , l_s_hostname, l_s_extIP, l_s_localIP, l_s_ram, l_s_cpu, l_s_uptime;

    @FXML
    public JFXListView lv_s_disks;

    @FXML
    public JFXButton btn_start, btn_refresh;

    @FXML
    public AnchorPane ap_main, ap_settings, ap_settings_load, ap_connections;

    @FXML
    public ProgressIndicator pi_progress;

    @FXML
    public TableView<Device> tableUsers;

    @FXML
    public TableColumn<Device, String> tableCellDevice, tableCellLastConn;

    @FXML
    public ComboBox<String> cb_lang;

    private ResourceBundle resourceBundle;
    private Thread thread;
    private static boolean isPortOpen = true;

    private final static Logger log = LogManager.getRootLogger();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        resourceBundle = resources;

        onRefresh(new ActionEvent());
        btn_refresh.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/assets/refresh.png"))));
        tv_password.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #888888");

        String ip = NetUtils.getExternalIP();
        l_ip.setText(ip);

        String HostName = "<No info>";
        try {
            HostName = InetAddress.getLocalHost().getHostName();
            l_hostname.setText(HostName);
        } catch (UnknownHostException e) {
            log.error("Initialize: Get hostname: | " + e.toString());
            l_hostname.setText(HostName);
        }

        pullPassword();
        OnChangeLangListener();
        refreshConnectionLoop();

    }

    private void pullPassword() {
        Preferences preferences = Preferences.userRoot().node("jsoth");
        String userPass = preferences.get("pass", "");
        tv_password.setDisable(true);
        tb_password.setSelected(false);
        tv_password.setText(userPass.equals("") ? "" : userPass);
    }

    private void refreshConnectionLoop(){
        Timeline refreshConn = new Timeline(new KeyFrame(Duration.seconds(10), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //  TableView. Connected devices
                ObservableList<Device> userdata = DbHelper.selectAll(DbHelper.dbName);
                tableCellDevice.setCellValueFactory(new PropertyValueFactory<Device, String>("model"));
                tableCellLastConn.setCellValueFactory(new PropertyValueFactory<Device, String>("last_conn"));
                tableUsers.setItems(userdata);
            }
        }));
        refreshConn.setCycleCount(Timeline.INDEFINITE);
        refreshConn.play();
    }

    private void OnChangeLangListener() {
        ObservableList<String> langlist = FXCollections.observableArrayList("en", "ru", "ua");
        cb_lang.setItems(langlist);
        Preferences preferences = Preferences.userRoot().node("jsoth");
        cb_lang.setValue(preferences.get("lang", "en"));
        cb_lang.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                preferences.put("lang", newValue);
                resourceBundle = ResourceBundle.getBundle("Locale.locale", new Locale(newValue));

                if (thread != null) {
                    thread.interrupt();
                }

                FXMLLoader fxmlLoader = new FXMLLoader();
                fxmlLoader.setLocation(getClass().getResource("sample.fxml"));
                fxmlLoader.setResources(resourceBundle);
                Parent root = null;
                try {
                    root = fxmlLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Main.thestage.setScene(new Scene(root, 600, 360));
            }
        });
    }

    public void onTbPass(ActionEvent actionEvent) {
        if (tb_password.isSelected()) {
            tb_password.setText(resourceBundle.getString("main.toggleOn"));
            tv_password.setDisable(false);
        } else {
            tb_password.setText(resourceBundle.getString("main.toggleOff"));
            tv_password.setDisable(true);
        }
    }


    public void onStop(MouseEvent mouseEvent) {
        l_status.setText(resourceBundle.getString("main.status"));
        l_status.setStyle("-fx-text-fill: #ff544f;");

        btn_start.setDisable(!btn_start.isDisabled());
        iv_stop.setVisible(!iv_stop.isVisible());

        thread.interrupt();
    }

    public void onStartClicked(ActionEvent actionEvent) {

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SocketRunner.runServer();
                } catch (Exception e) {
                    log.error("OnStartClicked: Start listening in new thread: | " + e.toString());
                    e.printStackTrace();

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Information");
                            alert.setHeaderText(null);
                            alert.setContentText("Please open " + DbHelper.PORT + " port and try again! ");
                            alert.showAndWait();

                            isPortOpen = false;
                        }
                    });
                }
            }
        });
        thread.start();

        if (!isPortOpen) return;

        l_status.setText(resourceBundle.getString("main.status.running"));
        l_status.setStyle("-fx-text-fill: #0fffa5;");

        Preferences preferences = Preferences.userRoot().node("jsoth");

        preferences.put("pass", (tb_password.isSelected())?tv_password.getText():"");

        btn_start.setDisable(!btn_start.isDisabled());
        iv_stop.setVisible(!iv_stop.isVisible());
    }

    public void onSettings(MouseEvent mouseEvent) {
        ap_main.setVisible(false);
        ap_connections.setVisible(false);

        ap_settings.setVisible(true);
        mark_settings.setVisible(true);
    }

    public void onMain(MouseEvent mouseEvent) {
        ap_settings.setVisible(false);
        ap_connections.setVisible(false);

        ap_main.setVisible(true);
        mark_main.setVisible(true);
    }

    public void onConnections(MouseEvent mouseEvent) {
        ap_settings.setVisible(false);
        ap_main.setVisible(false);

        ap_connections.setVisible(true);
        mark_connections.setVisible(true);
    }


    public void onRefresh(ActionEvent actionEvent) {

        Task<Map> task = new Task<Map>() {
            @Override
            protected Map call() throws Exception {
                GrabRunner info = new GrabRunner();
                return info.getMap();
            }
        };

        task.setOnRunning(event -> {
            ap_settings_load.setVisible(true);
            pi_progress.setVisible(true);
        });

        task.setOnSucceeded(event -> {
            try {
                Map systemInfo = task.get();
                l_s_hostname.setText(systemInfo.get("MachineName").toString());
                Map net = (Map)systemInfo.get("Net");
                l_s_extIP.setText(net.get("ExtIP").toString() + "  (" + net.get("CountryCode").toString() + ")");
                l_s_localIP.setText(net.get("LocalIP").toString());
                Map memory = (Map)systemInfo.get("Memory");
                Map ram = (Map)memory.get("RAM");
                l_s_ram.setText(ram.get("Total").toString() + " Gb");
                l_s_uptime.setText(systemInfo.get("UpTime").toString());
                Map cpu = (Map)systemInfo.get("CPU");
                Map details = (Map)cpu.get("Details");
                l_s_cpu.setText(details.get("Vendor").toString() + " " + details.get("Model").toString());
                Map disks = (Map)systemInfo.get("Disks");
                ArrayList<Map> localDisks = (ArrayList<Map>) disks.get("LocalDisks");
                List<String> disks_list = new LinkedList<>();
                for (Map temp: localDisks) {
                    disks_list.add(temp.get("DevName").toString() + "    " + temp.get("Used").toString() + "/" +
                            temp.get("Total").toString() + " Gb");
                }
                ObservableList<String> disks_info = new ObservableListWrapper<String>(disks_list);
                //lv_s_disks.setStyle("-fx-background-color:  #2d344; -fx-text-fill: while;");
                lv_s_disks.setItems(disks_info);
                lv_s_disks.getSelectionModel().selectFirst();
            } catch (InterruptedException e) {
                log.error("OnRefresh: get new stats: | " + e.toString());
            } catch (ExecutionException e) {
                log.error("OnRefresh: get new stats: | " + e.toString());
            }
            // There update UI

            pi_progress.setVisible(false);
            ap_settings_load.setVisible(false);
        });

        new Thread(task).start();

    }



}
