package planner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController implements Initializable {

    @FXML
    Label lastUpdated;

    @FXML
    TextArea participants;

    @FXML
    Label noParticipants;

    Scene scene;

    Stage stage;

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

        lastUpdated.setText("No map.sql found");
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            ResultSet rs = conn.prepareStatement("SELECT * FROM updated").executeQuery();
            if (rs != null && !rs.isClosed()) {
                lastUpdated.setText("Last updated at " + rs.getString("last"));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not connect to database 1");
        }
        noParticipants.setText("No participants in database");
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            ResultSet rs = conn.prepareStatement("SELECT COUNT(*) FROM participants").executeQuery();
            if (rs != null && !rs.isClosed()) {
                noParticipants.setText("Current operation: " + rs.getInt(1) + " participants");
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not connect to database 2");
        }
    }

    /**
     * Asks user for the map.sql file and inserts the contents into internal DB.
     * @param actionEvent event
     */
    public void loadMapsql(ActionEvent actionEvent) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File f = fileChooser.showOpenDialog(stage);
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
                lastUpdated.setText("Last updated at " + updateInfo);

                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not update map.sql");
            }
        }
    }

    /**
     * Loads the participant data into internal DB.
     * TODO: contains a lot of magic numbers for parsing.
     * @param actionEvent event
     */
    public void loadParticipants(ActionEvent actionEvent) {

        boolean error = false;

        String[] offs = participants.getText().split("\n");

        OUTER:
        for (String o : offs) {
            String[] off = o.split("\t");
            // Input validation: only last 3 can be empty, off size should include '+'-signs
            // We do not care about the timestamp
            if (off.length < 10) {
                noParticipants.setText("Error parsing participant data: insufficient length");
                error = true;
                break;
            }
            for (int i = 1; i < off.length; i++) {
                if (off[i] == null
                        || (i < 10 && off[i].equals(""))
                        || (i == 7 && !off[i].contains("+"))) {
                    noParticipants.setText("Error parsing participant data: syntax");
                    error = true;
                    break OUTER;
                }
            }
        }

        // Add participants to database
        if (!error) {
            try {
                Connection conn = DriverManager.getConnection(App.getDB());
                conn.prepareStatement("DELETE FROM participants").execute();
                for (String o : offs) {
                    String[] off = o.split("\t");
                    String sql = "INSERT INTO participants VALUES ("
                            + null + ","
                            + "'" + off[1] + "',"
                            + Integer.parseInt(off[2]) + ","
                            + Integer.parseInt(off[3]) + ","
                            + Integer.parseInt(off[4]) + ","
                            + Double.parseDouble(off[5].replace(',', '.')) + ","
                            + this.tribeNumber(off[6]) + ","
                            + "'" + off[7] + "',"
                            + offSize(this.tribeNumber(off[6]), off[7]) + ","
                            + Integer.parseInt(off[8]) + ","
                            + Integer.parseInt(off[9]) + ","
                            + "'" + (off.length > 10 ? off[10] : null) + "',"
                            + "'" + (off.length > 11 ? off[11] : null) + "',"
                            + "'" + off[off.length - 1] + "'"
                            + ")";
                    conn.prepareStatement(sql).execute();
                }
                noParticipants.setText("Current operation: " + offs.length + " participants");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Database error");
            }
        }
    }

    /**
     * Computes the size of this off from submitted tribe and off size string.
     * @param tribe 1 = Roman, 2 = Teuton, 3 = Gaul
     * @param offString off size in the format (example) 1000+0+500+100+100
     * @return off consumption (romans computed without drinking trough)
     */
    private int offSize(int tribe, String offString) {
        String[] offs = offString.split("\\+");
        int[] ints = new int[offs.length];
        for (int i = 0; i < offs.length; i++) {
            ints[i] = Integer.parseInt(offs[i].trim());
        }
        switch (tribe) {
            case 1: return ints[0] + 3*ints[1] + 4*ints[2];
            case 2: return ints[0] + ints[1] + 3*ints[2];
            case 3: return ints[0] + 2*ints[1] + 3*ints[2];
        }
        return 0;
    }

    /**
     * Converts tribe name to number.
     * @param tribe tribe
     * @return 1 = Roman, 2 = Teuton, 3 = Gaul
     */
    private int tribeNumber(String tribe) {
        switch (tribe) {
            case "Roman": return 1;
            case "Teuton": return 2;
            case "Gaul": return 3;
        }
        return -1;
    }

    /**
     * Changes to the planning view.
     * @throws IOException if the fxml file is not found
     */
    public void toPlanning() throws IOException {
        scene = lastUpdated.getScene();
        stage = (Stage) lastUpdated.getScene().getWindow();
        scene.setRoot(FXMLLoader.load(getClass().getResource("plan.fxml")));
        scene.getStylesheets().add(getClass().getResource("plan.css").toExternalForm());
        stage.show();
    }
}
