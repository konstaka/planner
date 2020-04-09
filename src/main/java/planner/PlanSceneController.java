package planner;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    VBox targetRows;

    List<TargetVillage> villages;

    List<AttackerVillage> attackers;

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

        // Add all alliances as checkboxes
        Set<String> enemies = new HashSet<>();
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            ResultSet rs = conn.prepareStatement("SELECT allyName FROM x_world").executeQuery();
            while (rs.next()) {
                enemies.add(rs.getString("allyName"));
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (String e : enemies) {
            enemyTickboxes.getChildren().add(new CheckBox(e));
        }

        // Load village data from DB to memory
        // Join with cap/off/artefact/etc information
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            String sql = "SELECT * FROM x_world LEFT JOIN village_data ON x_world.coordId=village_data.coordId";
            ResultSet rs = conn.prepareStatement(sql).executeQuery();
            while (rs.next()) {
                TargetVillage t = new TargetVillage(rs.getInt("coordId"));
                if (rs.getInt("capital") == 1) t.setCapital(true);
                if (rs.getInt("offvillage") == 1) t.setOffvillage(true);
                villages.add(t);
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

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
        System.out.println(attackers.size() + " attackers loaded");
    }

    /**
     * Refresh target list based on current filters
     * @param actionEvent event
     */
    public void refreshTargets(ActionEvent actionEvent) {

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

        List<TargetVillage> shownVillages = new ArrayList<>();
        for (TargetVillage v : villages) {
            if (enemyAlliances.contains(v.getAllyName())) {
                shownVillages.add(v);
            }
        }
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

        System.out.println(village.isCapital() + " " + village.isOffvillage());

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
