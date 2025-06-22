package planner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;

public class MainController implements Initializable {

    @FXML
    Button mapsqlButton;

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  {@code null} if the location is not known.
     * @param resources The resources used to localize the root object, or {@code null} if
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {



    }

    public void loadMapsql(ActionEvent actionEvent) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File f = fileChooser.showOpenDialog(mapsqlButton.getScene().getWindow());
        if (f != null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));

                Connection conn = DriverManager.getConnection(App.getDB());
                conn.prepareStatement("DELETE FROM x_world").execute();

                String line;
                while ((line = br.readLine()) != null) {
                    if (line.endsWith(";")) {
                        conn.prepareStatement(line).execute();
                    }
                }

                conn.close();
            } catch (Exception e) {
                System.out.println("Could not update map.sql");
            }
        }
    }
}
