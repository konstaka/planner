package planner;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import planner.util.GeneticScheduler;

public class PlanSceneController implements Initializable {

    @Getter
    private StringProperty toScene = new SimpleStringProperty("");

    @Getter
    private Operation operation;

    Thread optimiseThread = new Thread();

    @FXML
    VBox enemyTickboxes;

    @FXML
    VBox targetTickboxes;

    @FXML
    CheckBox caps;

    @FXML
    CheckBox offs;

    @FXML
    CheckBox deffs;

    @FXML
    CheckBox small_artes;

    @FXML
    CheckBox large_artes;

    @FXML
    CheckBox bps_wws;

    @FXML
    CheckBox planned_attacks;

    @FXML
    TextField landingTime;

    @FXML
    TextField randomShiftMinsField;

    int randomShiftMins = 0;

    @FXML
    Button optimiseButton;

    @FXML
    RadioButton fakes;

    @FXML
    RadioButton reals;

    @FXML
    TextField wavesField;

    int waves = 4;

    @FXML
    CheckBox conquer;

    @FXML
    CheckBox withHero;

    @FXML
    TextField unitSpeedField;

    int unitSpeed = 3;

    @FXML
    HBox attackerCols;

    @FXML
    VBox targetRows;

    @FXML
    Label savedText;


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

        // Listen to update the hero checkbox
        fakes.setOnAction(this::updateHeroCheckBox);
        reals.setOnAction(this::updateHeroCheckBox);

        // Listen to the waves field
        wavesField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && oldValue) {
                wavesField.fireEvent(new ActionEvent());
            }
        });

        // Listen to the unit speed field
        unitSpeedField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && oldValue) {
                unitSpeedField.fireEvent(new ActionEvent());
            }
        });
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
        try {
            this.operation = Operation.load();
            if (this.operation != null) {
                initOperation();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * After an operation has been initialised, sets up the view.
     * Only called internally from newOperation and loadOperation.
     */
    private void initOperation() {

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

        // Update operation and view
        updateCycle();

        // Set landing time
        landingTime.setText(operation.getDefaultLandingTime().format(App.FULL_DATE_TIME));
        // Listen to changes
        landingTime.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && oldValue) {
                landingTime.fireEvent(new ActionEvent());
            }
        });

        // Set random minutes
        randomShiftMinsField.setText(""+operation.getRandomShiftWindow() / 60);
        // Listen to changes
        randomShiftMinsField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && oldValue) {
                updateRandomMinutes();
            }
        });

        // Listen to changes in attacks
        for (Map<Integer, Attack> attackerAttacks : operation.getAttacks().values()) {
            for (Attack attack : attackerAttacks.values()) {
                attack.getUpdated().addListener((observable, oldValue, newValue) -> {
                    if (newValue && !oldValue) {
                        attack.getUpdated().set(false);
                        this.updateCycle();
                    }
                });
            }
        }
        // Listen to changes in participants
        for (AttackerVillage attackerVillage : operation.getAttackers()) {
            attackerVillage.getUpdated().addListener((observable, oldValue, newValue) -> {
                if (newValue && !oldValue) {
                    attackerVillage.getUpdated().set(false);
                    this.updateCycle();
                }
            });
        }
    }


    /**
     * Master update method; first triggers the update in the Operation object and then redraws the screen.
     */
    public void updateCycle() {

        if (operation != null) {

            savedText.setText("");
            operation.update();
            randomShiftMins = operation.getRandomShiftWindow() / 60;
            randomShiftMinsField.setText(""+randomShiftMins);
            optimiseButton.textProperty().unbind();
            optimiseButton.setText("Optimise (slow!)");
            updateTargets();
            updateAttackers();
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

        // For those alliances, check types of village to show
        List<TargetVillage> shownVillages = new ArrayList<>();
        for (TargetVillage target : operation.getTargets()) {
            if (enemyAlliances.contains(target.getAllyName())
                    && (
                    (target.isCapital() && this.caps.isSelected())
                            || (target.isOffvillage() && this.offs.isSelected())
                            || (target.isDeffvillage() && this.deffs.isSelected())
                            || (target.getArtefact().contains("Small") && this.small_artes.isSelected())
                            || (target.getArtefact().contains("Large") && this.large_artes.isSelected())
                            || (target.getArtefact().contains("Unique") && this.large_artes.isSelected())
                            || (target.isWwvillage() && this.bps_wws.isSelected())
                            || (target.getArtefact().contains("Buildplan") && this.bps_wws.isSelected())
                    )
            ) {
                shownVillages.add(target);
            }
        }

        // Checkbox for planned attacks should show all planned attacks
        if (planned_attacks.isSelected()) {
            for (AttackerVillage attackerVillage : operation.getAttackers()) {
                for (Attack attack : attackerVillage.getPlannedAttacks()) {
                    if (!shownVillages.contains(attack.getTarget())) {
                        shownVillages.add(attack.getTarget());
                    }
                }
            }
        }

        // Create rows
        for (TargetVillage targetVillage : shownVillages) {
            targetRows.getChildren().add(this.targetRow(targetVillage));
        }
    }

    public void refreshTargets(ActionEvent actionEvent) {
        if (operation != null) this.updateTargets();
    }


    /**
     * Refresh participant list based on current info.
     */
    private void updateAttackers() {

        attackerCols.getChildren().clear();
        for (AttackerVillage attackerVillage : operation.getAttackers()) {
            attackerCols.getChildren().add(attackerVillage.toDisplayBox());
        }
    }


    /**
     * Crafts a target row from the village identifier.
     * TODO could use some refactoring
     * @param target village object
     * @return hbox the target row
     */
    private HBox targetRow(TargetVillage target) {

        HBox row = new HBox();
        row.getChildren().add(new Label(target.getAllyName()));
        row.getChildren().add(new Label(target.getVillageName()));
        row.getChildren().add(new Label(target.getCoords()));
        row.getChildren().add(new Label(target.getPlayerName()));
        row.getChildren().add(new Label(Converters.toTribe(target.getTribe())));
        row.getChildren().add(new Label(""+target.getPopulation()));

        String arteData = "";
        if (target.isCapital()) arteData += "cap ";
        if (target.isOffvillage()) arteData += "off ";
        if (target.isWwvillage()) arteData += "WW";
        arteData += target.getArtefact();
        row.getChildren().add(new Label(arteData));

        StringBuilder effectData = new StringBuilder();
        for (String s : target.getArteEffects()) {
            if (!effectData.toString().equals("")) effectData.append(", ");
            effectData.append(s, 0, 3);
        }
        row.getChildren().add(new Label(effectData.toString()));

        // Landing time as an editable field
        TextField targetLandTime = new TextField();
        targetLandTime.setText(
                operation.getLandTimes()
                        .get(target.getCoordId())
                        .format(App.TIME_ONLY)
        );
        targetLandTime.setOnAction(actionEvent -> {
            LocalDateTime newLandingTime = LocalDateTime.of(
                    operation.getLandTimes().get(target.getCoordId()).toLocalDate(),
                    LocalTime.parse(targetLandTime.getText(), App.TIME_ONLY)
            );
            target.setRandomShiftSeconds(
                    ChronoUnit.SECONDS.between(operation.getDefaultLandingTime(), newLandingTime)
            );
            operation.computeLandingTimes(false);
            updateCycle();
        });
        targetLandTime.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && oldValue) targetLandTime.fireEvent(new ActionEvent());
        });
        // UP and DOWN keys can also modify the value
        targetLandTime.setOnKeyPressed(press -> {
            LocalTime oldTime = LocalTime.parse(targetLandTime.getText(), App.TIME_ONLY);
            if (press.getCode().toString().equals("UP")) {
                // Plus one second
                LocalTime newTime = oldTime.plusSeconds(1);
                targetLandTime.setText(newTime.format(App.TIME_ONLY));
                if (newTime.isBefore(oldTime)) {
                    // Different date, update screen.
                    LocalDateTime newLandingTime = LocalDateTime.of(
                            operation.getLandTimes().get(target.getCoordId()).toLocalDate().plusDays(1),
                            LocalTime.parse(targetLandTime.getText(), App.TIME_ONLY)
                    );
                    target.setRandomShiftSeconds(
                            ChronoUnit.SECONDS.between(operation.getDefaultLandingTime(), newLandingTime)
                    );
                    operation.computeLandingTimes(false);
                    updateCycle();
                }
            } else if (press.getCode().toString().equals("DOWN")) {
                // Minus one second
                LocalTime newTime = oldTime.minusSeconds(1);
                targetLandTime.setText(newTime.format(App.TIME_ONLY));
                if (newTime.isAfter(oldTime)) {
                    // Different date, update screen.
                    LocalDateTime newLandingTime = LocalDateTime.of(
                            operation.getLandTimes().get(target.getCoordId()).toLocalDate().minusDays(1),
                            LocalTime.parse(targetLandTime.getText(), App.TIME_ONLY)
                    );
                    target.setRandomShiftSeconds(
                            ChronoUnit.SECONDS.between(operation.getDefaultLandingTime(), newLandingTime)
                    );
                    operation.computeLandingTimes(false);
                    updateCycle();
                }
            }
        });
        row.getChildren().add(targetLandTime);

        // Dropdown for adding an attack
        // TODO display times for different unit speeds and hero boots without affecting the Attack object
        ComboBox<Attack> attackerPicker = new ComboBox<>();
        for (AttackerVillage attackerVillage : operation.getAttackers()) {
            Attack pickableAttack = operation
                    .getAttacks()
                    .get(attackerVillage.getCoordId())
                    .get(target.getCoordId());
            //pickableAttack.setWithHero(withHero.isSelected());
            //pickableAttack.setUnitSpeed(unitSpeed);
            attackerPicker
                    .getItems()
                    .add(pickableAttack);
        }
        attackerPicker.setPromptText("Add attacker...");
        attackerPicker.getSelectionModel().selectedItemProperty().addListener(observable -> {
            Attack attack = attackerPicker.getSelectionModel().getSelectedItem();
            if (attack != null) {
                if (!attack.getAttacker().getPlannedAttacks().contains(attack)) {
                    attack.getAttacker().getPlannedAttacks().add(attack);
                }
                attack.setWaves(waves);
                attack.setUnitSpeed(unitSpeed);
                attack.setReal(reals.isSelected());
                attack.setConq(conquer.isSelected());
                attack.setWithHero(withHero.isSelected());
                this.updateHeroCheckBox(new ActionEvent());
                attackerPicker.getSelectionModel().clearSelection();
                attackerPicker.setPromptText("Add attacker...");
                attack.getUpdated().set(true);
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
        for (Map<Integer, Attack> attackMap: operation.getAttacks().values()) {
            Attack attack = attackMap.get(target.getCoordId());
            if (attack.getWaves() > 0) rowAttacks.add(attack);
        }
        rowAttacks.sort((attack1, attack2) -> {
            if (attack1.getLandingTime().equals(attack2.getLandingTime())) {
                return attack1.getSendingTime().compareTo(attack2.getSendingTime());
            } else {
                return attack1.getLandingTime().compareTo(attack2.getLandingTime());
            }
        });
        for (Attack attack : rowAttacks) {
            row.getChildren().add(attack.toDisplayBox());
        }

        return row;
    }


    /**
     * Shifts all landing times without re-randomising.
     * @param actionEvent focus leave or enter keypress
     */
    public void updateTimes(ActionEvent actionEvent) {
        LocalDateTime newDefaultTime = LocalDateTime.parse(landingTime.getText(), App.FULL_DATE_TIME);
        operation.setDefaultLandingTime(newDefaultTime);
        operation.computeLandingTimes(false);
        updateCycle();
    }


    /**
     * Re-randomises the landing times.
     * @param actionEvent button press
     */
    public void randomiseTimes(ActionEvent actionEvent) {
        if (operation != null) {
            operation.computeLandingTimes(true);
            updateCycle();
        }
    }


    /**
     * Optimises the landing times with GeneticScheduler.
     * @param actionEvent button press
     */
    public void optimiseTimes(ActionEvent actionEvent) {

        if (!optimiseThread.isAlive()) {

            Task<Map<Integer, Long>> task = new Task<>() {
                @Override
                protected Map<Integer, Long> call() {

                    Map<Integer, TargetVillage> targetsMap = new HashMap<>();
                    for (AttackerVillage attackerVillage : operation.getAttackers()) {
                        for (Attack attack : attackerVillage.getPlannedAttacks()) {
                            targetsMap.put(attack.getTarget().getCoordId(), attack.getTarget());
                        }
                    }

                    GeneticScheduler gs = new GeneticScheduler(
                            operation,
                            240.0,
                            0.8
                    );
                    Map<Integer, Long> solution = null;
                    try {
                        for (int i = operation.getRandomShiftWindow() / 60; i <= 10; i++) {
                            updateMessage("Optimising... 0%");
                            gs.getProgress().addListener((observable, oldValue, newValue) -> {
                                long percents = Math.round((double) newValue * 100);
                                updateMessage("Optimising... " + percents + "%");
                            });
                            operation.setRandomShiftWindow(i * 60);
                            System.out.println("Scheduling for a flex window of " + i + " minute(s)");
                            solution = gs.schedule();
                            System.out.println(solution);
                            if (solution != null) break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (solution != null) {
                        GSDebugger.printSolution(
                                gs,
                                solution,
                                targetsMap,
                                operation.getAttackers(),
                                operation.getDefaultLandingTime()
                        );
                    }
                    return solution;
                }
            };

            optimiseButton.textProperty().unbind();
            optimiseButton.textProperty().bind(task.messageProperty());
            task.setOnSucceeded(event -> {
                Map<Integer, Long> solution = null;
                try {
                    solution = task.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                if (solution != null) {
                    Map<Integer, LocalDateTime> schedule = operation.getLandTimes();
                    for (Integer t_coordId : solution.keySet()) {
                        schedule.put(
                                t_coordId,
                                operation.getDefaultLandingTime().plusSeconds(solution.get(t_coordId))
                        );
                    }
                    operation.setLandTimes(schedule);
                }
                updateCycle();
            });
            task.setOnFailed(event -> updateCycle());
            optimiseThread = new Thread(task);
            optimiseThread.start();
        }
    }


    /**
     * Updates the number of waves to be added.
     * @param actionEvent focus leave or enter keypress
     */
    public void updateWaves(ActionEvent actionEvent) {
        try {
            waves = Integer.parseInt(wavesField.getText());
            if (waves < 1 || waves > 8) waves = 4;
        } catch (NumberFormatException e) {
            waves = 4;
        }
        wavesField.setText(""+waves);
    }


    /**
     * Updates the default status of the hero checkbox based on if we're adding fakes or reals.
     */
    private void updateHeroCheckBox(ActionEvent actionEvent) {
        if (reals.isSelected()) withHero.setSelected(true);
        else withHero.setSelected(false);
        updateCycle();
    }


    /**
     * Updates the number of minutes to randomise the hitting times.
     * Triggered by focus leave
     */
    public void updateRandomMinutes() {
        try {
            randomShiftMins = Integer.parseInt(randomShiftMinsField.getText());
            if (randomShiftMins < 0) randomShiftMins = 0;
        } catch (NumberFormatException e) {
            randomShiftMins = 0;
        }
        randomShiftMinsField.setText(""+randomShiftMins);
        if (operation != null) {
            operation.setRandomShiftWindow(randomShiftMins * 60);
        }
    }


    /**
     * Updates the unit speed used for added attacks.
     * @param actionEvent focus leave on enter keypress
     */
    public void updateUnitSpeed(ActionEvent actionEvent) {
        try {
            unitSpeed = Integer.parseInt(unitSpeedField.getText());
            if (unitSpeed < 3 || unitSpeed > 99) unitSpeed = 3;
        } catch (NumberFormatException e) {
            unitSpeed = 3;
        }
        unitSpeedField.setText(""+unitSpeed);
    }


    /**
     * Saves the current plan to database. Overwrites anything that was there.
     */
    public void save() {
        if (operation != null) {
            boolean success = operation.save();
            if (success) {
                savedText.setText("Saved to DB");
                savedText.setStyle("-fx-text-fill: green");
            } else {
                savedText.setText("ERROR");
                savedText.setStyle("-fx-text-fill: red");
            }
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
