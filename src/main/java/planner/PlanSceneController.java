package planner;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PlanSceneController implements Initializable {

    @FXML
    VBox enemyTickboxes;

    @FXML
    VBox targetTickboxes;

    @FXML
    CheckBox caps;

    @FXML
    CheckBox offs;

    @FXML
    CheckBox small_artes;

    @FXML
    CheckBox large_artes;

    @FXML
    CheckBox bps_wws;

    @FXML
    HBox attackerCols;

    @FXML
    VBox targetRows;

    List<TargetVillage> villages;

    List<AttackerVillage> attackers;

    Set<Integer> scoutAccounts;

    Set<Integer> foolAccounts;

    Set<Integer> confuserAccounts;

    Set<Integer> architectAccounts;

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

        villages = new ArrayList<>();
        attackers = new ArrayList<>();
        scoutAccounts = new HashSet<>();
        foolAccounts = new HashSet<>();
        confuserAccounts = new HashSet<>();
        architectAccounts = new HashSet<>();

        // Add all alliances as checkboxes
        Set<String> enemies = new HashSet<>();
        Map<String, Integer> enemyCounts = new HashMap<>();
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            ResultSet rs = conn.prepareStatement("SELECT allyName FROM x_world").executeQuery();
            while (rs.next()) {
                String enemyAlly = rs.getString("allyName");
                if (enemies.contains(enemyAlly)) {
                    enemyCounts.put(enemyAlly, enemyCounts.get(enemyAlly)+1);
                } else {
                    enemyCounts.put(enemyAlly, 1);
                }
                enemies.add(enemyAlly);
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Naïve sort, number of alliances is small
        List<String> sortedAllys = new ArrayList<>();
        for (String e : enemies) {
            int maxCount = 0;
            String maxE = null;
            for (String ec : enemyCounts.keySet()) {
                if (enemyCounts.get(ec) > maxCount) {
                    maxCount = enemyCounts.get(ec);
                    maxE = ec;
                }
            }
            sortedAllys.add(maxE);
            enemyCounts.remove(maxE);
        }
        for (String e : sortedAllys) {
            CheckBox c = new CheckBox(e);
            c.setOnAction(this::refreshTargets);
            enemyTickboxes.getChildren().add(c);
        }
        // Add auto-updating listeners to other filters as well
        for (Node n : targetTickboxes.getChildren()) {
            if (n instanceof CheckBox) {
                ((CheckBox) n).setOnAction(this::refreshTargets);
            }
        }

        // Load village data from DB to memory
        // Join with cap/off/artefact/etc information
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            String sql = "SELECT * FROM x_world " +
                    "LEFT JOIN village_data ON x_world.coordId=village_data.coordId " +
                    "LEFT JOIN artefacts on x_world.coordId = artefacts.coordId";
            ResultSet rs = conn.prepareStatement(sql).executeQuery();
            while (rs.next()) {
                TargetVillage t = new TargetVillage(rs.getInt("coordId"));
                if (rs.getInt("capital") == 1) t.setCapital(true);
                if (rs.getInt("offvillage") == 1) t.setOffvillage(true);
                if (rs.getInt("wwvillage") == 1) t.setWwvillage(true);
                int small = rs.getInt("small_arte");
                int large = rs.getInt("large_arte");
                int unique = rs.getInt("unique_arte");
                if (small != 0) {
                    t.setArtefact(this.interpretArte(0, small));
                }
                if (large != 0) {
                    t.setArtefact(this.interpretArte(1, large));
                }
                if (unique != 0) {
                    t.setArtefact(this.interpretArte(2, unique));
                }
                // Note down account-wide effects
                if (large == 5 || unique == 5) {
                    scoutAccounts.add(rs.getInt("playerId"));
                }
                if (small == 11 || unique == 11) {
                    foolAccounts.add(rs.getInt("playerId"));
                }
                if (large == 10 || unique == 10) {
                    confuserAccounts.add(rs.getInt("playerId"));
                }
                if (large == 2 || unique == 2) {
                    architectAccounts.add(rs.getInt("playerId"));
                }
                villages.add(t);
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Assemble artefact effects on target villages
        this.assembleArteEffects();

        // Assemble attacking villages
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            ResultSet rs = conn.prepareStatement("SELECT * FROM participants").executeQuery();
            while (rs.next()) {
                for (Village v : villages) {
                    if (v.getXCoord() == rs.getInt("xCoord")
                            && v.getYCoord() == rs.getInt("yCoord")) {
                        AttackerVillage attacker = new AttackerVillage(v.getCoordId());
                        attacker.setTs(rs.getInt("ts"));
                        attacker.setSpeed(rs.getDouble("speed"));
                        attacker.setOffString(rs.getString("offstring"));
                        attacker.setOffSize(rs.getInt("offsize"));
                        attacker.setCatas(rs.getInt("catas"));
                        attacker.setChiefs(rs.getInt("chiefs"));
                        attacker.setSendMin(rs.getString("sendmin"));
                        attacker.setSendMax(rs.getString("sendmax"));
                        attacker.setComment(rs.getString("comment"));
                        attackers.add(attacker);
                        break;
                    }
                }
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (AttackerVillage a : attackers) {
            attackerCols.getChildren().add(a.toDisplayBox());
        }
    }


    /**
     * Checks the village artefact and possible account-wide effects.
     */
    private void assembleArteEffects() {
        for (TargetVillage t : villages) {
            if (t.getArtefact().contains("eyes") || scoutAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("scout effect");
            }
            if (t.getArtefact().contains("fool") || foolAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("fool effect");
            }
            if (t.getArtefact().contains("confuser") || confuserAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("confuser effect");
            }
            if (t.getArtefact().contains("architect") || architectAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("architect effect");
            }
        }
    }


    /**
     * Converts artefact size and type to a string representation.
     * @param size 0, 1, 2
     * @param type 1, 2, 4, 5, 6, 8, 9, 10, 11
     * @return artefact string
     */
    private String interpretArte(int size, int type) {
        String arte = "";
        switch (size) {
            case 0:
                arte = "small ";
                break;
            case 1:
                arte = "large ";
                break;
            case 2:
                arte = "unique ";
                break;
            default:
                return "invalid artefact";
        }
        switch (type) {
            case 1: return "buildplan";
            case 2: return arte + "architect";
            case 4: return arte + "boots";
            case 5: return arte + "eyes";
            case 6: return arte + "diet";
            case 8: return arte + "trainer";
            case 9: return arte + "storage";
            case 10: return arte + "confuser";
            case 11: return arte + "fool";
            default:
                return "invalid artefact";
        }
    }

    /**
     * Refresh target list based on current filters
     * @param actionEvent event
     */
    public void refreshTargets(ActionEvent actionEvent) {

        // Check which alliances to show
        targetRows.getChildren().clear();
        Set<String> enemyAlliances = new HashSet<>();
        for (Node n : enemyTickboxes.getChildren()) {
            if (n instanceof CheckBox) {
                CheckBox c = (CheckBox) n;
                if (c.isSelected()) {
                    enemyAlliances.add(c.getText());
                }
            }
        }
        if (enemyAlliances.size() == 0) return;

        // For those alliances, check types of village to show
        List<TargetVillage> shownVillages = new ArrayList<>();
        for (TargetVillage v : villages) {
            if (enemyAlliances.contains(v.getAllyName())
                    && (
                        (v.isCapital() && this.caps.isSelected())
                        || (v.isOffvillage() && this.offs.isSelected())
                        || (v.getArtefact().contains("small") && this.small_artes.isSelected())
                        || (v.getArtefact().contains("large") && this.large_artes.isSelected())
                        || (v.getArtefact().contains("unique") && this.large_artes.isSelected())
                        || (v.isWwvillage() && this.bps_wws.isSelected())
                        || (v.getArtefact().contains("buildplan") && this.bps_wws.isSelected())
                    )
            ) {
                shownVillages.add(v);
            }
        }

        // Create rows
        for (TargetVillage t : shownVillages) {
            targetRows.getChildren().add(this.targetRow(t));
        }
    }

    /**
     * Crafts a target row from the village identifier.
     * @param village village object
     * @return hbox
     */
    private HBox targetRow(TargetVillage village) {

        HBox row = new HBox();
        row.getChildren().add(new Label(village.getAllyName()));
        row.getChildren().add(new Label(village.getVillageName()));
        row.getChildren().add(new Label(village.getCoords()));
        row.getChildren().add(new Label(village.getPlayerName()));
        row.getChildren().add(new Label(this.toTribe(village.getTribe())));
        row.getChildren().add(new Label(""+village.getPopulation()));

        String data = "";
        if (village.isCapital()) data += "cap ";
        if (village.isOffvillage()) data += "off ";
        if (village.isWwvillage()) data += "WW";
        data += village.getArtefact();
        row.getChildren().add(new Label(data));
        data = "";
        for (String s : village.getArteEffects()) {
            if (data != "") data += ", ";
            data += s.substring(0, 3);
        }
        row.getChildren().add(new Label(data));

        // Assign column id:s for styling
        for (int i = 0; i < row.getChildren().size(); i++) {
            Node n = row.getChildren().get(i);
            n.getStyleClass().add("target-col-"+i);
            n.getStyleClass().add("target-col");
        }

        return row;
    }

    /**
     * Tribe ID to tribe converter.
     * @param tribeNumber 1 = Roman, 2 = Teuton, 3 = Gaul
     * @return 1 = Roman, 2 = Teuton, 3 = Gaul
     */
    private String toTribe(int tribeNumber) {
        switch (tribeNumber) {
            case 1: return "Roman";
            case 2: return "Teuton";
            case 3: return "Gaul";
        }
        return "Unknown";
    }
}
