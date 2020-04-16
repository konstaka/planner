package planner;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import planner.entities.Attack;
import planner.entities.AttackerVillage;
import planner.entities.Operation;
import planner.entities.TargetVillage;
import planner.util.Converters;

public class PlanSceneController implements Initializable {

    @Getter
    private StringProperty toScene = new SimpleStringProperty("");

    @Getter
    Operation operation;

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
    TextField landingTime;

    @FXML
    TextField flexMinutes;

    @FXML
    RadioButton fakes;

    @FXML
    RadioButton reals;

    @FXML
    CheckBox conquer;

    @FXML
    TextField wavesBox;

    @FXML
    HBox attackerCols;

    @FXML
    VBox targetRows;


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

        // Add all alliances as checkboxes
        Set<String> enemies = new HashSet<>();
        Map<String, Integer> enemyCounts = new HashMap<>();
        // TODO move DB operation to the Database class
        try {
            Connection conn = DriverManager.getConnection(App.DB);
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
        List<String> sortedEnemies = new ArrayList<>();
        for (String e : enemies) {
            int maxCount = 0;
            String maxE = null;
            for (String ec : enemyCounts.keySet()) {
                if (enemyCounts.get(ec) > maxCount) {
                    maxCount = enemyCounts.get(ec);
                    maxE = ec;
                }
            }
            sortedEnemies.add(maxE);
            enemyCounts.remove(maxE);
        }
        for (String e : sortedEnemies) {
            CheckBox c = new CheckBox(e);
            c.setOnAction(this::refreshTargets);
            enemyTickboxes.getChildren().add(c);
        }

        // Add auto-updating listeners to cap/off/arte/etc filters
        for (Node n : targetTickboxes.getChildren()) {
            if (n instanceof CheckBox) {
                ((CheckBox) n).setOnAction(this::refreshTargets);
            }
        }

        // Group fake/real radio buttons
        ToggleGroup attackType = new ToggleGroup();
        fakes.setToggleGroup(attackType);
        reals.setToggleGroup(attackType);
    }


    /**
     * Sets up a new operation.
     */
    public void newOperation() {

        this.operation = new Operation();
        initOperation();
    }


    /**
     * Loads last saved operation from database.
     */
    public void loadOperation() {

        this.operation = Operation.load();
        initOperation();
    }


    /**
     * After an operation has been initialised, sets up the view.
     * Only called internally from newOperation and loadOperation.
     */
    private void initOperation() {

        // Set landing time
        landingTime.setText(operation.getDefaultLandingTime().format(App.FULL_DATE_TIME));

        // Display participants
        for (AttackerVillage a : operation.getAttackers()) {
            VBox attackerBox = a.toDisplayBox();
            attackerCols.getChildren().add(attackerBox);
        }

        // Draw target rows
        updateTargets();
    }


    /**
     * Saves current operation to database.
     */
    private void saveOperation() {
        boolean success = operation.save();
        // TODO do something with the success information.
    }


    /**
     * Master update method; first triggers the update in the Operation object and then redraws the screen.
     */
    private void updateCycle() {

        operation.update();
        updateTargets();
        updateAttackers();
    }


    /**
     * Refresh target list based on current filters.
     */
    public void updateTargets() {

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

        // For those alliances, check types of village to show
        List<TargetVillage> shownVillages = new ArrayList<>();
        for (TargetVillage v : operation.getTargets()) {
            if (enemyAlliances.contains(v.getAllyName())
                    && (
                    (v.isCapital() && this.caps.isSelected())
                            || (v.isOffvillage() && this.offs.isSelected())
                            || (v.getArtefact().contains("Small") && this.small_artes.isSelected())
                            || (v.getArtefact().contains("Large") && this.large_artes.isSelected())
                            || (v.getArtefact().contains("Unique") && this.large_artes.isSelected())
                            || (v.isWwvillage() && this.bps_wws.isSelected())
                            || (v.getArtefact().contains("Buildplan") && this.bps_wws.isSelected())
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

    public void refreshTargets(ActionEvent actionEvent) {
        this.updateTargets();
    }


    /**
     * Refresh participant list based on current info.
     */
    private void updateAttackers() {

        // Redraw
        attackerCols.getChildren().clear();
        for (AttackerVillage a : operation.getAttackers()) {
            VBox attackerBox = a.toDisplayBox();
            attackerCols.getChildren().add(attackerBox);
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
        row.getChildren().add(new Label(Converters.toTribe(village.getTribe())));
        row.getChildren().add(new Label(""+village.getPopulation()));

        String arteData = "";
        if (village.isCapital()) arteData += "cap ";
        if (village.isOffvillage()) arteData += "off ";
        if (village.isWwvillage()) arteData += "WW";
        arteData += village.getArtefact();
        row.getChildren().add(new Label(arteData));
        StringBuilder effectData = new StringBuilder();
        for (String s : village.getArteEffects()) {
            if (!effectData.toString().equals("")) effectData.append(", ");
            effectData.append(s, 0, 3);
        }
        row.getChildren().add(new Label(effectData.toString()));

        // Get landing time
        row.getChildren().add(new Label(
                landTimes
                        .get(village.getCoordId())
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        ));

        // Dropdown for adding an attack
        ComboBox<Attack> attackerPicker = new ComboBox<>();
        for (AttackerVillage a : attackers) {
            attackerPicker.getItems().add(attacks.get(a.getCoordId()).get(village.getCoordId()));
        }
        attackerPicker.setPromptText("Add attacker...");
        attackerPicker.getSelectionModel().selectedItemProperty().addListener(observable -> {
            Attack a = attackerPicker.getSelectionModel().getSelectedItem();
            if (a != null) {
                if (!a.getAttacker().getPlannedAttacks().contains(a)) {
                    a.getAttacker().getPlannedAttacks().add(a);
                }
                a.getWaves().set(waves);
                if (reals.isSelected()) a.getReal().set(true);
                else a.getReal().set(false);
                if (conquer.isSelected()) a.getConq().set(true);
                else a.getConq().set(false);
                attackerPicker.getSelectionModel().clearSelection();
                attackerPicker.setPromptText("Add attacker...");
            }
        });
        row.getChildren().add(attackerPicker);

        // Assign column id:s for styling
        for (int i = 0; i < row.getChildren().size(); i++) {
            Node n = row.getChildren().get(i);
            n.getStyleClass().add("target-col-"+i);
            n.getStyleClass().add("target-col");
        }

        // List attacks in landing order
        List<Attack> rowAttacks = new ArrayList<>();
        for (Map<Integer, Attack> attackMap: attacks.values()) {
            Attack a = attackMap.get(village.getCoordId());
            if (a.getWaves().getValue() > 0) rowAttacks.add(a);
        }
        rowAttacks.sort((o1, o2) -> {
            if (o1.getLandingTime().equals(o2.getLandingTime())) {
                return o1.getSendingTime().compareTo(o2.getSendingTime());
            } else {
                return o1.getLandingTime().compareTo(o2.getLandingTime());
            }
        });
        for (Attack a : rowAttacks) {
            row.getChildren().add(a.toDisplayBox());
        }

        return row;
    }


    /**
     * Saves the current plan to database. Overwrites anything that was there.
     */
    public void save() {
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            conn.prepareStatement("DELETE FROM operation_meta").execute();
            String sql = "INSERT INTO operation_meta VALUES ("
                    + flexSeconds.get() + ",'"
                    + landingTime.getText() + "')";
            conn.prepareStatement(sql).execute();
            conn.prepareStatement("DELETE FROM attacks").execute();
            for (AttackerVillage attacker : attackers) {
                for (Attack a : attacker.getPlannedAttacks()) {
                    conn.prepareStatement("INSERT INTO attacks VALUES ("
                            + a.getAttacker().getCoordId() + ","
                            + a.getTarget().getCoordId() + ",'"
                            + landTimes.get(a.getTarget().getCoordId()).format(App.FULL_DATE_TIME) + "',"
                            + a.getWaves().get() + ","
                            + (a.getReal().get() ? 1 : 0) + ","
                            + (a.getConq().get() ? 1 : 0) + ","
                            + a.getLandingTimeShift().get() + ","
                            + a.getUnitSpeed() + ","
                            + a.getServerSpeed() + ","
                            + a.getServerSize() + ")"
                    )
                            .execute();
                }
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Loads saved operation from database on top of all information that currently is in planning.
     */
    public void load() {
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            ResultSet rs1 = conn.prepareStatement("SELECT * FROM operation_meta").executeQuery();
            String time = rs1.getString("defaultLandingTime");
            landingTime.setText(time);
            defaultLandingTime = LocalDateTime.parse(time, App.FULL_DATE_TIME);
            int seconds = rs1.getInt("flex_seconds");
            flexMinutes.setText(""+seconds / 60);
            flexSeconds.set(seconds);
            ResultSet rs3 = conn.prepareStatement("SELECT * FROM attacks").executeQuery();
            while (rs3.next()) {
                int a_coordId = rs3.getInt("a_coordId");
                int t_coordId = rs3.getInt("t_coordId");
                Attack a = attacks.get(a_coordId).get(t_coordId);
                a.setLandingTime(LocalDateTime.parse(rs3.getString("landing_time"), App.FULL_DATE_TIME));
                a.getWaves().set(rs3.getInt("waves"));
                a.getReal().set(rs3.getInt("realTgt") == 1);
                a.getConq().set(rs3.getInt("conq") == 1);
                a.getLandingTimeShift().set(rs3.getInt("time_shift"));
                a.setUnitSpeed(rs3.getInt("unit_speed"));
                a.setServerSpeed(rs3.getInt("server_speed"));
                a.setServerSize(rs3.getInt("server_size"));
                if (a.getWaves().get() > 0) {
                    for (AttackerVillage attackerVillage : attackers) {
                        if (attackerVillage.getCoordId() == a_coordId) {
                            attackerVillage.getPlannedAttacks().add(a);
                        }
                    }
                }
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Changes to the updating view.
     * @param actionEvent event
     */
    public void toUpdating(ActionEvent actionEvent) {
        this.toScene.set("updating");
    }


    /**
     * Changes to the command editor view.
     * @param actionEvent event
     */
    public void toCommands(ActionEvent actionEvent) {
        this.toScene.set("commands");
    }
}
