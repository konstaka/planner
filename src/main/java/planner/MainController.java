package planner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
import lombok.Getter;
import planner.util.Converters;

/**
 * Handles data updates.
 * TODO move DB operations to the Database class
 */
public class MainController implements Initializable {

    @Getter
    private StringProperty toScene = new SimpleStringProperty("");

    @Getter
    private BooleanProperty newOp = new SimpleBooleanProperty(false);

    @Getter
    private BooleanProperty loadOp = new SimpleBooleanProperty(false);

    @Getter
    private BooleanProperty attackersAdded = new SimpleBooleanProperty(false);

    private String action;

    @FXML
    Label serverUrl;

    @FXML
    Label serverSize;

    @FXML
    Label serverSpeed;

    @FXML
    Label lastUpdated;

    @FXML
    Label noParticipants;

    @FXML
    Label infoLabel1;

    @FXML
    Label infoLabel2;

    @FXML
    Label infoLabel3;

    @FXML
    Label infoLabel4;

    @FXML
    TextArea pastedText;

    @FXML
    Button okButton;

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

        this.action = "";

        serverUrl.setText("Server: N/A");
        serverSize.setText("Size: N/A");
        serverSpeed.setText("Speed: N/A");
        lastUpdated.setText("No map.sql found");
        noParticipants.setText("No participants in database");

        try {
            Connection conn = DriverManager.getConnection(App.DB);
            ResultSet rs1 = conn.prepareStatement("SELECT * FROM updated").executeQuery();
            if (rs1 != null && !rs1.isClosed()) {
                lastUpdated.setText("Map.sql updated at " + rs1.getString("last"));
            }
            ResultSet rs2 = conn.prepareStatement("SELECT COUNT(*) FROM participants").executeQuery();
            if (rs2 != null && !rs2.isClosed()) {
                noParticipants.setText("Current participants: " + rs2.getInt(1));
            }
            ResultSet rs3 = conn.prepareStatement("SELECT * FROM world_meta").executeQuery();
            if (rs3 != null && !rs3.isClosed()) {
                serverUrl.setText("Server: " + rs3.getString("serverurl"));
                serverSize.setText("Size: " + rs3.getString("serversize"));
                serverSpeed.setText("Speed: " + rs3.getString("serverspeed"));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not connect to database");
        }
    }


    /**
     * Asks user for the map.sql file and inserts the contents into internal DB.
     * @param actionEvent event
     */
    public void loadMapsql(ActionEvent actionEvent) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open map.sql File");
        File f = fileChooser.showOpenDialog(stage);
        if (f != null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));

                Connection conn = DriverManager.getConnection(App.DB);
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
                lastUpdated.setText("Map.sql updated at " + updateInfo);
                App.displayInfoAlert("Map.sql updated", "Reload operation to see the changes.");
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not update map.sql");
            }
        }
    }


    /**
     * Updates map.sql from server.
     */
    public void downloadMapSql() {
        String updateInfo = App.downloadMapSql();
        lastUpdated.setText("Map.sql updated at " + updateInfo);
        if (!updateInfo.equals("[error]")) {
            App.displayInfoAlert("Map.sql updated", "Reload operation to see the changes.");
        }
    }


    /**
     * Clears the db for a new operation.
     */
    public void clearOperation() {
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            conn.prepareStatement("DELETE FROM participants").execute();
            conn.prepareStatement("DELETE FROM attacker_info").execute();
            conn.prepareStatement("DELETE FROM target_info").execute();
            conn.prepareStatement("DELETE FROM attacks").execute();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Loads the participant data into internal DB.
     * TODO: contains a lot of magic numbers for parsing.
     */
    public void loadParticipants() {

        // Do nothing on empty input
        if (pastedText.getText().isEmpty()) {
            return;
        }

        String[] offs = pastedText.getText().split("\n");

        for (String o : offs) {

            String[] off = o.split("\t");
            // Input validation: only last 3 can be empty, off size should include '+'-signs
            // We do not care about the timestamp
            if (off.length < 10) {
                noParticipants.setText("Error parsing participant data: insufficient length");
                return;
            }
            for (int i = 1; i < off.length; i++) {
                if (off[i] == null
                        || (i < 10 && off[i].equals(""))
                        || (i == 7 && !off[i].contains("+"))) {
                    noParticipants.setText("Error parsing participant data: syntax");
                    return;
                }
            }
        }

        // Add participants to database
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            for (String o : offs) {
                String[] off = o.split("\t");
                String sql = "INSERT INTO participants VALUES (null,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, off[1]);
                ps.setInt(2, Integer.parseInt(off[2]));
                ps.setInt(3, Integer.parseInt(off[3]));
                ps.setInt(4, Integer.parseInt(off[4]));
                ps.setDouble(5, Double.parseDouble(off[5].replace(',', '.')));
                ps.setInt(6, Converters.tribeNumber(off[6]));
                ps.setString(7, off[7]);
                ps.setInt(8, Converters.offSize(Converters.tribeNumber(off[6]), off[7]));
                ps.setInt(9, Integer.parseInt(off[8]));
                ps.setInt(10, Integer.parseInt(off[9]));
                ps.setString(11, (off.length > 10 ? off[10] : null));
                ps.setString(12, (off.length > 11 ? off[11] : null));
                ps.setString(13, (off.length > 12 ? off[12] : null));
                ps.execute();
            }
            ResultSet rs = conn.prepareStatement("SELECT COUNT(*) FROM participants").executeQuery();
            if (rs != null && !rs.isClosed()) {
                noParticipants.setText("Current participants: " + rs.getInt(1));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database error");
        }
    }


    /**
     * Updates the cap/off/etc data from the user input.
     * Format example:
     * https://ts4.nordics.travian.com/position_details.php?x=-74&y=99
     * TODO remove old capital if player has more than one after this.
     * TODO clear/remove button(s)
     * @param column column to be updated
     */
    private void updateVillageData(String column) {
        String[] villages = this.pastedText.getText().split("\n");
        for (String v : villages) {
            String[] c = v.split("\\?")[1].split("&");
            int[] co = new int[c.length];
            for (int i = 0; i < c.length; i++) {
                if (c[i].contains("-")) {
                    co[i] = -1;
                    c[i] = c[i].substring(c[i].indexOf("-")+1);
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
                Connection conn = DriverManager.getConnection(App.DB);
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


    public void markCapitals() {
        this.updateVillageData("capital");
    }


    public void markOffs() {
        this.updateVillageData("offvillage");
    }


    public void markDeffs() { this.updateVillageData("deffvillage"); }


    public void markWWs() { this.updateVillageData("wwvillage");}


    /**
     * Updates artefacts in database.
     * @param size which artefact page is being updated, small_arte/large_arte
     * @param artefacts coordId -> artefact type map
     * @param uniques coordId:s that have unique artefacts
     */
    private void updateArtefacts(String size, Map<Integer, Integer> artefacts, Set<Integer> uniques) {
        try {
            Connection conn = DriverManager.getConnection(App.DB);
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
        String html = this.pastedText.getText();
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


    public void parseSmall() {
        this.parseArtefacts("small_arte");
    }


    public void parseLarge() {
        this.parseArtefacts("large_arte");
    }


    /**
     * Reads server size, speed, and url from the text field.
     * Saves these to DB.
     * TODO move DB operations to a new class
     */
    private void configureServer() {
        try {
            String[] settings = pastedText.getText().split(" ");
            if (settings.length == 3) {
                int size = Integer.parseInt(settings[0]);
                int speed = Integer.parseInt(settings[1]);
                String url = settings[2].strip();
                if (size < 0 || speed < 0 || url.isEmpty()) {
                    throw new Exception("Syntax error");
                }
                Connection conn = DriverManager.getConnection(App.DB);
                conn.prepareStatement("DELETE FROM world_meta").execute();
                PreparedStatement ins = conn.prepareStatement("INSERT INTO world_meta VALUES(?,?,?)");
                ins.setInt(1, size);
                ins.setInt(2, speed);
                ins.setString(3, url);
                ins.execute();
                ResultSet rs = conn.prepareStatement("SELECT * FROM world_meta").executeQuery();
                if (rs != null && !rs.isClosed()) {
                    serverUrl.setText("Server: " + rs.getString("serverurl"));
                    serverSize.setText("Size: " + rs.getString("serversize"));
                    serverSpeed.setText("Speed: " + rs.getString("serverspeed"));
                }
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Changes to the planning view.
     */
    public void toPlanning() {
        this.toScene.set("planning");
    }


    /**
     * Action confirmation: commit the chosen action based on the input in pastedText.
     */
    public void doAction(ActionEvent actionEvent) {
        switch (action) {
            case "serverDetails":
                configureServer();
                break;
            case "capitals":
                markCapitals();
                break;
            case "offs":
                markOffs();
                break;
            case "deffs":
                markDeffs();
                break;
            case "wws":
                markWWs();
                break;
            case "smallArtes":
                parseSmall();
                break;
            case "largeArtes":
                parseLarge();
                break;
            case "new":
                if (!pastedText.getText().isEmpty()) {
                    clearOperation();
                    loadParticipants();
                }
                newOp.set(true);
                break;
            case "load":
                loadOp.set(true);
                break;
            case "addParticipants":
                loadParticipants();
                attackersAdded.set(true);
                break;
            default:
                System.out.println("Action <" + action + "> not implemented yet.");
                break;
        }
    }


    /**
     * Left side navigation; updates the action variable and the labels
     */
    public void serverDetails() {
        action = "serverDetails";
        okButton.setVisible(true);
        infoLabel1.setText("Input server size, speed, and url, separated by spaces.");
        infoLabel2.setText("200 for a 401x401 map, 400 for a 801x801 map.");
        infoLabel3.setText("Example: 200 1 ts4.nordics.travian.com");
        infoLabel4.setText("");
        pastedText.setText("");
        pastedText.setVisible(true);
    }
    public void capitals() {
        action = "capitals";
        okButton.setVisible(true);
        infoLabel1.setText("Paste here links to villages you want to mark as CAPITALS.");
        infoLabel2.setText("One per line, format example: https://ts4.nordics.travian.com/position_details.php?x=-74&y=99");
        infoLabel3.setText("To see the changes, save and load the operation or create a new one.");
        infoLabel4.setText("");
        pastedText.setText("");
        pastedText.setVisible(true);
    }
    public void offs() {
        action = "offs";
        okButton.setVisible(true);
        infoLabel1.setText("Paste here links to villages you want to mark as OFF VILLAGES.");
        infoLabel2.setText("One per line, format example: https://ts4.nordics.travian.com/position_details.php?x=-74&y=99");
        infoLabel3.setText("To see the changes, save and load the operation or create a new one.");
        infoLabel4.setText("");
        pastedText.setText("");
        pastedText.setVisible(true);
    }
    public void deffs() {
        action = "deffs";
        okButton.setVisible(true);
        infoLabel1.setText("Paste here links to villages you want to mark as DEF VILLAGES.");
        infoLabel2.setText("One per line, format example: https://ts4.nordics.travian.com/position_details.php?x=-74&y=99");
        infoLabel3.setText("To see the changes, save and load the operation or create a new one.");
        infoLabel4.setText("");
        pastedText.setText("");
        pastedText.setVisible(true);
    }
    public void wws() {
        action = "wws";
        okButton.setVisible(true);
        infoLabel1.setText("Paste here links to villages you want to mark as WORLD WONDERS.");
        infoLabel2.setText("One per line, format example: https://ts4.nordics.travian.com/position_details.php?x=-74&y=99");
        infoLabel3.setText("To see the changes, save and load the operation or create a new one.");
        infoLabel4.setText("");
        pastedText.setText("");
        pastedText.setVisible(true);
    }
    public void smallArtes() {
        action = "smallArtes";
        okButton.setVisible(true);
        infoLabel1.setText("Paste here the source code of your treasury page for SMALL (lvl10) ARTEFACTS.");
        infoLabel2.setText("To see the changes, save and load the operation or create a new one.");
        infoLabel3.setText("");
        infoLabel4.setText("");
        pastedText.setText("");
        pastedText.setVisible(true);
    }
    public void largeArtes() {
        action = "largeArtes";
        okButton.setVisible(true);
        infoLabel1.setText("Paste here the source code of your treasury page for LARGE (lvl20) ARTEFACTS.");
        infoLabel2.setText("To see the changes, save and load the operation or create a new one.");
        infoLabel3.setText("");
        infoLabel4.setText("");
        pastedText.setText("");
        pastedText.setVisible(true);
    }
    public void newOperation() {
        action = "new";
        okButton.setVisible(true);
        infoLabel1.setText("Paste participant rows from sheets for a NEW OPERATION, exactly in the following format (tsv):");
        infoLabel2.setText("timestamp account x y ts speed tribe offsize catas chiefs sendmin sendmax comment");
        infoLabel3.setText("Example: 3/26/2020 22:46:59	Haamu	-73	138	18	1	Gaul	100+0+0+109+106	106	3	13:00:00	00:00:00	No night sends																			");
        infoLabel4.setText("WARNING! This will remove all existing participants and their planned attacks. Leave the field clear to use existing participants.");
        pastedText.setText("");
        pastedText.setVisible(true);
    }
    public void loadOperation() {
        action = "load";
        okButton.setVisible(true);
        infoLabel1.setText("LOAD the last saved OPERATION from database");
        infoLabel2.setText("");
        infoLabel3.setText("");
        infoLabel4.setText("");
        pastedText.setText("");
        pastedText.setVisible(false);
    }
    public void participants() {
        action = "addParticipants";
        okButton.setVisible(true);
        infoLabel1.setText("Paste participant rows from sheets to ADD PARTICIPANTS, exactly in the following format (tsv):");
        infoLabel2.setText("timestamp account x y ts speed tribe offsize catas chiefs sendmin sendmax comment");
        infoLabel3.setText("Example: 3/26/2020 22:46:59	Haamu	-73	138	18	1	Gaul	100+0+0+109+106	106	3	13:00:00	00:00:00	No night sends																			");
        infoLabel4.setText("");
        pastedText.setText("");
        pastedText.setVisible(true);
    }
}
