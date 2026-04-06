package com.smartinventory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                App.class.getResource("main.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(
                App.class.getResource("css/styles.css").toExternalForm()
        );

        stage.setTitle("Smart Inventory AI — Turn Messages Into Insights");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.show();
        stage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
