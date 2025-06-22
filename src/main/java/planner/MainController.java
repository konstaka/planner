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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController implements Initializable {

    StringProperty toScene = new SimpleStringProperty("");

    BooleanProperty newOp = new SimpleBooleanProperty(false);

    BooleanProperty loadOp = new SimpleBooleanProperty(false);

    @FXML
    Label lastUpdated;

    @FXML
    TextArea participants;

    @FXML
    Label noParticipants;

    @FXML
    TextArea villageInfo;

    @FXML
    TextArea artefactInfo;

    @FXML
    Button planButton;

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
                DateTimeFormatter frm = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                String updateInfo = LocalDateTime.now().format(frm);
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
                            + this.offSize(this.tribeNumber(off[6]), off[7]) + ","
                            + Integer.parseInt(off[8]) + ","
                            + Integer.parseInt(off[9]) + ","
                            + "'" + (off.length > 10 ? off[10] : null) + "',"
                            + "'" + (off.length > 11 ? off[11] : null) + "',"
                            + "'" + off[off.length - 1] + "'"
                            + ")";
                    conn.prepareStatement(sql).execute();
                }
                noParticipants.setText("Current operation: " + offs.length + " participants");
                newOp.set(true);
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
     * Updates the cap/off/etc data from the user input.
     * TODO remove old capital if player has more than one after this.
     * TODO clear/remove button(s)
     * @param column column to be updated
     */
    private void updateVillageData(String column) {
        String[] villages = this.villageInfo.getText().split("\n");
        for (String v : villages) {
            String[] c = v.split("\\|");
            int[] co = new int[c.length];
            for (int i = 0; i < c.length; i++) {
                if (c[i].contains("−")) {
                    co[i] = -1;
                    c[i] = c[i].substring(c[i].indexOf('−')+1);
                } else {
                    co[i] = 1;
                }
                String coordi = "";
                Pattern p = Pattern.compile("\\d");
                Matcher m = p.matcher(c[i]);
                while (m.find()) coordi += m.group();
                co[i] *= Integer.parseInt(coordi);
            }
            try {
                Connection conn = DriverManager.getConnection(App.getDB());
                String sql = "INSERT INTO village_data (coordId, " + column + ") VALUES (" +
                        "(SELECT coordId " +
                        "FROM x_world " +
                        "WHERE xCoord=" + co[0] + " AND yCoord=" + co[1] + "), 1) " +
                        "ON CONFLICT(coordId) DO UPDATE SET " + column + "=1";
                conn.prepareStatement(sql).execute();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
    }


    public void markCapitals(ActionEvent actionEvent) {
        this.updateVillageData("capital");
    }


    public void markOffs(ActionEvent actionEvent) {
        this.updateVillageData("offvillage");
    }


    public void markWWs(ActionEvent actionEvent) { this.updateVillageData("wwvillage");}


    /**
     * Updates artefacts in database.
     * @param size which artefact page is being updated, small_arte/large_arte
     * @param artefacts coordId -> artefact type map
     * @param uniques coordId:s that have unique artefacts
     */
    private void updateArtefacts(String size, Map<Integer, Integer> artefacts, Set<Integer> uniques) {
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            conn.prepareStatement("UPDATE artefacts SET " + size + "=0").execute();
            if (size.equals("large_arte")) {
                conn.prepareStatement("UPDATE artefacts SET unique_arte=0").execute();
            }
            for (int coordId : artefacts.keySet()) {
                String col = size;
                if (uniques.contains(coordId)) col = "unique_arte";
                String sql = "INSERT INTO artefacts (coordId, " + col + ") " +
                        "VALUES (" + coordId + ", " + artefacts.get(coordId) + ") " +
                        "ON CONFLICT(coordId) DO UPDATE SET " + col + "=" + artefacts.get(coordId);
                conn.prepareStatement(sql).execute();
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    private void parseArtefacts(String size) {
        Map<Integer, Integer> artefacts = new HashMap<>();
        Set<Integer> uniques = new HashSet<>();
        String html = this.artefactInfo.getText();
        int trimmer = html.indexOf("show_artefacts");
        html = html.substring(trimmer);
        // Read bp's if they are in the game
        if (size.equals("small_arte") && html.contains("type1\"")) {
            for (int i = 1; i <= 13; i++) {
                html = html.substring(html.indexOf("karte.php")+1);
                int coordId = Integer.parseInt(html.substring(html.indexOf('=')+1, html.indexOf('"')));
                artefacts.put(coordId, 1);
            }
        }
        // Read architects
        for (int i = 1; i <= 6; i++) {
            html = html.substring(html.indexOf("karte.php")+1);
            int coordId = Integer.parseInt(html.substring(html.indexOf('=')+1, html.indexOf('"')));
            artefacts.put(coordId, 2);
            if (size.equals("large_arte") && i == 5) {
                uniques.add(coordId);
                break;
            }
        }
        // Read boots
        for (int i = 1; i <= 6; i++) {
            html = html.substring(html.indexOf("karte.php")+1);
            int coordId = Integer.parseInt(html.substring(html.indexOf('=')+1, html.indexOf('"')));
            artefacts.put(coordId, 4);
            if (size.equals("large_arte") && i == 5) {
                uniques.add(coordId);
                break;
            }
        }
        // Read eyes
        for (int i = 1; i <= 6; i++) {
            html = html.substring(html.indexOf("karte.php")+1);
            int coordId = Integer.parseInt(html.substring(html.indexOf('=')+1, html.indexOf('"')));
            artefacts.put(coordId, 5);
            if (size.equals("large_arte") && i == 5) {
                uniques.add(coordId);
                break;
            }
        }
        // Read diets
        for (int i = 1; i <= 6; i++) {
            html = html.substring(html.indexOf("karte.php")+1);
            int coordId = Integer.parseInt(html.substring(html.indexOf('=')+1, html.indexOf('"')));
            artefacts.put(coordId, 6);
            if (size.equals("large_arte") && i == 5) {
                uniques.add(coordId);
                break;
            }
        }
        // Read trainers
        for (int i = 1; i <= 6; i++) {
            html = html.substring(html.indexOf("karte.php")+1);
            int coordId = Integer.parseInt(html.substring(html.indexOf('=')+1, html.indexOf('"')));
            artefacts.put(coordId, 8);
            if (size.equals("large_arte") && i == 5) {
                uniques.add(coordId);
                break;
            }
        }
        // Read storages
        for (int i = 1; i <= 6; i++) {
            html = html.substring(html.indexOf("karte.php")+1);
            int coordId = Integer.parseInt(html.substring(html.indexOf('=')+1, html.indexOf('"')));
            artefacts.put(coordId, 9);
            if (size.equals("large_arte") && i == 4) break;
        }
        // Read confusers
        for (int i = 1; i <= 6; i++) {
            html = html.substring(html.indexOf("karte.php")+1);
            int coordId = Integer.parseInt(html.substring(html.indexOf('=')+1, html.indexOf('"')));
            artefacts.put(coordId, 10);
            if (size.equals("large_arte") && i == 5) {
                uniques.add(coordId);
                break;
            }
        }
        // Read fools
        for (int i = 1; i <= 10; i++) {
            html = html.substring(html.indexOf("karte.php")+1);
            int coordId = Integer.parseInt(html.substring(html.indexOf('=')+1, html.indexOf('"')));
            artefacts.put(coordId, 11);
            if (size.equals("large_arte")) {
                uniques.add(coordId);
                break;
            }
        }
        // Update database
        this.updateArtefacts(size, artefacts, uniques);
    }


    public void parseSmall(ActionEvent actionEvent) {
        this.parseArtefacts("small_arte");
    }


    public void parseLarge(ActionEvent actionEvent) {
        this.parseArtefacts("large_arte");
    }


    /**
     * Changes to the planning view.
     */
    public void toPlanning() {
        this.toScene.set("planning");
    }


    public void load(ActionEvent actionEvent) {
        loadOp.set(true);
    }
}
