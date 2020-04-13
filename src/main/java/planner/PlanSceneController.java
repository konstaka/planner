package planner;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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

public class PlanSceneController implements Initializable {

    StringProperty toScene = new SimpleStringProperty("");

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

    DateTimeFormatter f = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    LocalDateTime defaultLandingTime;

    IntegerProperty flexSeconds;

    int waves;

    List<TargetVillage> villages;

    List<AttackerVillage> attackers;

    Map<Integer, Map<Integer, Attack>> attacks;

    Map<Integer, LocalDateTime> landTimes;

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

        flexSeconds = new SimpleIntegerProperty();
        waves = 4;
        villages = new ArrayList<>();
        attackers = new ArrayList<>();
        attacks = new HashMap<>();
        landTimes = new HashMap<>();
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
                        attacker.getTs().set(rs.getInt("ts"));
                        attacker.setSpeed(rs.getDouble("speed"));
                        attacker.setOffString(rs.getString("offstring"));
                        attacker.setOffSize(rs.getInt("offsize"));
                        attacker.setCatas(rs.getInt("catas"));
                        attacker.setChiefs(rs.getInt("chiefs"));
                        attacker.setSendMin(rs.getString("sendmin"));
                        attacker.setSendMax(rs.getString("sendmax"));
                        attacker.setComment(rs.getString("comment"));
                        attacker.getTs().addListener((ov, oldV, newV) -> {
                            this.updateTargets();
                        });
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
            VBox attackerBox = a.toDisplayBox();
            attackerCols.getChildren().add(attackerBox);
        }

        // Set default timing to overmorrow morning
        defaultLandingTime = LocalDateTime.of(
                LocalDate.now().plusDays(2),
                LocalTime.of(8, 0));
        landingTime.setText(defaultLandingTime.format(f));
        // Make it editable
        landingTime.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    defaultLandingTime = LocalDateTime.parse(landingTime.getText(), f);
                    this.updateLandingTimes();
                    for (Map<Integer, Attack> attackMap : attacks.values()) {
                        for (Attack a : attackMap.values()) {
                            a.setLandingTime(landTimes.get(a.getTarget().getCoordId()));
                        }
                    }
                    this.updateTargets();
                } catch (Exception ex) {
                    landingTime.setText(defaultLandingTime.format(f));
                }
            }
        });
        // Set flex default to 0
        flexMinutes.setText("0");
        // Make it editable
        flexMinutes.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    flexSeconds.set(Math.abs(Integer.parseInt(flexMinutes.getText()) * 60));
                    flexMinutes.setText(""+flexSeconds.get() / 60);
                    this.updateLandingTimes();
                    for (Map<Integer, Attack> attackMap : attacks.values()) {
                        for (Attack a : attackMap.values()) {
                            a.setLandingTime(landTimes.get(a.getTarget().getCoordId()));
                        }
                    }
                    this.updateTargets();
                } catch (NumberFormatException ex) {
                    flexMinutes.setText("0");
                }
            }
        });

        // Group fake/real radio buttons
        ToggleGroup attackType = new ToggleGroup();
        fakes.setToggleGroup(attackType);
        reals.setToggleGroup(attackType);

        // Set default waves to 4
        wavesBox.setText("4");
        // Make it editable
        wavesBox.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    waves = Integer.parseInt(wavesBox.getText());
                    if (waves < 1 || waves > 8) {
                        waves = 4;
                        wavesBox.setText("4");
                    }
                } catch (NumberFormatException ex) {
                    wavesBox.setText("4");
                }
            }
        });

        // Update landing times
        this.updateLandingTimes();
        // Create the attack matrix
        for (AttackerVillage attacker : attackers) {
            Map<Integer, Attack> toAdd = new HashMap<>();
            for (TargetVillage target : villages) {
                // TODO make all magic numbers editable.
                // Currently: unit speed, server speed, server size
                // Waves needs to be 0 at this point to mark that the attack is not planned for now
                Attack a = new Attack(
                        target,
                        attacker,
                        new SimpleIntegerProperty(0),
                        new SimpleBooleanProperty(false),
                        new SimpleBooleanProperty(false),
                        3,
                        landTimes.get(target.getCoordId()),
                        new SimpleIntegerProperty(0),
                        1,
                        200);
                // Listen to changes that mark planned fake/real attacks
                a.getWaves().addListener(observable -> {
                    this.updateTargets();
                    this.updateAttackers();
                });
                a.getReal().addListener(observable -> {
                    this.updateTargets();
                    this.updateAttackers();
                });
                a.getConq().addListener(observable -> {
                    this.updateTargets();
                    this.updateAttackers();
                });
                a.getLandingTimeShift().addListener(observable -> {
                    this.updateTargets();
                    this.updateAttackers();
                });
                toAdd.put(target.getCoordId(), a);
            }
            attacks.put(attacker.getCoordId(), toAdd);
        }
    }


    private void updateAttackers() {
        // Reset alerts
        for (AttackerVillage a : attackers) {
            a.getAlert().set(false);
        }
//        // Alert if there are sending times too close to each other
//        // WARNING: Time complexity O(m * n^2) where m is #attackers and n is amount of villages on server.
//        // TODO make this a background task
//        // TODO set fastest possible send interval individually per attacker
//        for (Map<Integer, Attack> attackMap : attacks.values()) {
//            for (Attack a1 : attackMap.values()) {
//                for (Attack a2 : attackMap.values()) {
//                    if (!a1.equals(a2)
//                            && a1.getWaves().get() > 0 && a2.getWaves().get() > 0
//                            && ChronoUnit.SECONDS.between(a1.getSendingTime(), a2.getSendingTime()) < 60) {
//                        a1.getAttacker().getAlert().set(true);
//                        a2.getAttacker().getAlert().set(true);
//                    }
//                }
//            }
//        }
        for (AttackerVillage a : attackers) {
//            // Alert if two sending times are equal for the same target village (unlikely, but yeah)
//            // TODO fix this, doesn't work for now. Sorting lambda probably doesn't even check all pairs.
//            System.out.println(o1.getSendingTime() + " " + o1.getAttacker().getCoords());
//            System.out.println(o2.getSendingTime() + " " + o2.getAttacker().getCoords());
//            if (o1.getSendingTime().format(DateTimeFormatter.ofPattern("dd HH:mm:ss"))
//                    .equals(o2.getSendingTime().format(DateTimeFormatter.ofPattern("dd HH:mm:ss")))
//                    && o1.getWaves().get() > 0 && o2.getWaves().get() > 0) {
//                o1.getAttacker().getAlert().set(true);
//                o2.getAttacker().getAlert().set(true);
//            }
        }

        // Alert if too many sends
        for (AttackerVillage a : attackers) {
            a.getPlannedAttacks().removeIf(attack -> attack.getWaves().get() == 0);
            if (a.getPlannedAttacks().size() > 14) {
                a.getAlert().set(true);
                // TODO add a tooltip
            }
        }

        // Redraw
        attackerCols.getChildren().clear();
        for (AttackerVillage a : attackers) {
            VBox attackerBox = a.toDisplayBox();
            attackerCols.getChildren().add(attackerBox);
        }
    }


    /**
     * Refreshes the landing times map based on baseline hit time and flex seconds.
     */
    private void updateLandingTimes() {
        for (TargetVillage target : villages) {
            // Randomise landing times:
            long flexSec = Math.round(flexSeconds.get() * (Math.random() * 2 - 1));
            LocalDateTime thisHit = defaultLandingTime.plusSeconds(flexSec);
            // Save some lookup time by storing these in a map
            landTimes.put(target.getCoordId(), thisHit);
        }
    }


    /**
     * Checks the village artefact and possible account-wide effects.
     */
    private void assembleArteEffects() {
        for (TargetVillage t : villages) {
            if (t.getArtefact().contains("Eyes") || scoutAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("Scout effect");
            }
            if (t.getArtefact().contains("Fool") || foolAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("Fool effect");
            }
            if (t.getArtefact().contains("Confuser") || confuserAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("Confuser effect");
            }
            if (t.getArtefact().contains("Architect") || architectAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("Architect effect");
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
                arte = "Small ";
                break;
            case 1:
                arte = "Large ";
                break;
            case 2:
                arte = "Unique ";
                break;
            default:
                return "Invalid Artefact";
        }
        switch (type) {
            case 1: return "Buildplan";
            case 2: return arte + "Architect";
            case 4: return arte + "Boots";
            case 5: return arte + "Eyes";
            case 6: return arte + "Diet";
            case 8: return arte + "Trainer";
            case 9: return arte + "Storage";
            case 10: return arte + "Confuser";
            case 11: return arte + "Fool";
            default:
                return "Invalid Artefact";
        }
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
        if (enemyAlliances.size() == 0) return;

        // For those alliances, check types of village to show
        List<TargetVillage> shownVillages = new ArrayList<>();
        for (TargetVillage v : villages) {
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

        // Get landing time
        row.getChildren().add(new Label(
                landTimes
                        .get(village.getCoordId())
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        ));

        // Dropdown for adding an attack
        ComboBox<AttackerVillage> attackerPicker = new ComboBox<>();
        attackerPicker.getItems().addAll(attackers);
        attackerPicker.setPromptText("Add attacker...");
        attackerPicker.getSelectionModel().selectedItemProperty().addListener(observable -> {
            AttackerVillage a = attackerPicker.getSelectionModel().getSelectedItem();
            if (a != null) {
                Attack attack = attacks.get(a.getCoordId()).get(village.getCoordId());
                a.getPlannedAttacks().add(attack);
                attack.getWaves().set(waves);
                if (reals.isSelected()) attack.getReal().set(true);
                else attack.getReal().set(false);
                if (conquer.isSelected()) attack.getConq().set(true);
                else attack.getConq().set(false);
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


    /**
     * Saves the current plan to database. Overwrites anything that was there.
     */
    public void save() {
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            conn.prepareStatement("DELETE FROM operation_meta").execute();
            String sql = "INSERT INTO operation_meta VALUES ("
                    + flexSeconds.get() + ",'"
                    + landingTime.getText() + "')";
            conn.prepareStatement(sql).execute();
            conn.prepareStatement("DELETE FROM attacks").execute();
            for (AttackerVillage attacker : attackers) {
                for (Attack a : attacker.getPlannedAttacks()) {
                    System.out.println("saving attack " + a.getAttacker().getCoordId() + " " + a.getTarget().getCoordId());
                    conn.prepareStatement("INSERT INTO attacks VALUES ("
                            + a.getAttacker().getCoordId() + ","
                            + a.getTarget().getCoordId() + ",'"
                            + landTimes.get(a.getTarget().getCoordId()).format(f) + "',"
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
     * Loads saved operation from database. Overwrites anything that is now planned.
     */
    public void load() {
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            ResultSet rs1 = conn.prepareStatement("SELECT * FROM operation_meta").executeQuery();
            String time = rs1.getString("defaultLandingTime");
            landingTime.setText(time);
            defaultLandingTime = LocalDateTime.parse(time, f);
            int seconds = rs1.getInt("flex_seconds");
            flexMinutes.setText(""+seconds / 60);
            flexSeconds.set(seconds);
            ResultSet rs3 = conn.prepareStatement("SELECT * FROM attacks").executeQuery();
            while (rs3.next()) {
                int a_coordId = rs3.getInt("a_coordId");
                int t_coordId = rs3.getInt("t_coordId");
                Attack a = attacks.get(a_coordId).get(t_coordId);
                a.setLandingTime(LocalDateTime.parse(rs3.getString("landing_time"), f));
                a.getWaves().set(rs3.getInt("waves"));
                a.getReal().set(rs3.getInt("realTgt") == 1);
                a.getConq().set(rs3.getInt("conq") == 1);
                a.getLandingTimeShift().set(rs3.getInt("time_shift"));
                a.setUnitSpeed(rs3.getInt("unit_speed"));
                a.setServerSpeed(rs3.getInt("server_speed"));
                a.setServerSize(rs3.getInt("server_size"));
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Changes to the updating view.
     */
    public void toUpdating(ActionEvent actionEvent) {
        this.toScene.set("updating");
    }
}
