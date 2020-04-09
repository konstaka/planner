package planner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

public class MainController implements Initializable {

    @FXML
    Button mapsqlButton;

    @FXML
    Label lastUpdated;

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

        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            ResultSet rs = conn.prepareStatement("SELECT * FROM updated").executeQuery();
            if (!rs.isClosed()) {
                lastUpdated.setText(rs.getString("last"));
            } else {
                lastUpdated.setText("never");
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not connect to database");
        }

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

                // Update last updated field
                conn.prepareStatement("DELETE FROM updated").execute();
                String updateInfo = LocalDateTime.now().toString();
                conn.prepareStatement("INSERT INTO updated VALUES ("
                        + "'" + updateInfo + "'"
                        + ")").execute();
                lastUpdated.setText(updateInfo);

                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not update map.sql");
            }
        }
    }
}
