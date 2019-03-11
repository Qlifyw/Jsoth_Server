package sample;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class Main extends Application {

    public static Stage thestage;

    @Override
    public void start(Stage primaryStage) throws Exception{

        // Set custom classpath to libraries
        System.setProperty("java.library.path", System.getProperty("user.dir")+File.separator+"libs"+File.separator+"sigar"+File.separator);;
        System.out.println(System.getProperty("java.library.path"));
        (new LibraryLoader()).extract();

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("sample.fxml"));

        // Get current languages, else set default languages 'en'
        Preferences preferences = Preferences.userRoot().node("jsoth");
        fxmlLoader.setResources(ResourceBundle.getBundle("Locale.locale", new Locale(preferences.get("lang", "en"))));

        Parent root = fxmlLoader.load();

        // Set window parameters
        thestage = primaryStage;
        primaryStage.getIcons().add(new Image(getClass().getResource("/assets/icon_fox.png").toString()));
        primaryStage.setTitle("Jsoth");
        primaryStage.setScene(new Scene(root, 600-10, 360-10));

        primaryStage.setResizable(false);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
