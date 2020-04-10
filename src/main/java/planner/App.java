/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package planner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;


public class App extends Application {

    @Getter
    private static final String GREETING = "Welcome";

    @Getter
    private static final String DB = "jdbc:sqlite:mapsql.db";

    @Override
    public void start(Stage stage) throws Exception {

        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("main.fxml")));
        scene.getStylesheets().add(getClass().getResource("main.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("plan.css").toExternalForm());

        stage.setTitle("Planner 0.1");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}