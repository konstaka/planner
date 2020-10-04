package planner;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import planner.entities.Attack;
import planner.entities.AttackerVillage;
import planner.util.Converters;

public class CommandController implements Initializable {

    @Getter
    private StringProperty toScene = new SimpleStringProperty("");

    @FXML
    TextArea template1;

    @FXML
    TextArea template2;

    @FXML
    VBox commands;

    @Setter
    private List<AttackerVillage> attackers;

    @Setter
    private LocalDateTime defaultHittingTime;

    private Map<Integer, List<Attack>> attacksPerPlayer = new HashMap<>();


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
            Connection conn = DriverManager.getConnection(App.DB);
            ResultSet rs = conn.prepareStatement("SELECT * FROM templates").executeQuery();
            while (rs.next()) {
                template1.setText(rs.getString("template1"));
                template2.setText(rs.getString("template2"));
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void updateCommands() {
        commands.getChildren().clear();
        attacksPerPlayer.clear();
        for (AttackerVillage av : attackers) {
            if (!attacksPerPlayer.containsKey(av.getPlayerId())) {
                attacksPerPlayer.put(av.getPlayerId(), new ArrayList<>());
            }
            for (Attack a : av.getPlannedAttacks()) {
                attacksPerPlayer.get(a.getAttacker().getPlayerId()).add(a);
            }
        }
        for (AttackerVillage av : attackers) {
            commands.getChildren().add(this.toAttackerRow(av));
        }
    }


    private HBox toAttackerRow(AttackerVillage a) {
        HBox attackerRow = new HBox();
        List<Attack> targets = a.getPlannedAttacks();
        if (targets.isEmpty()) return attackerRow;
        attackerRow.setSpacing(8);
        targets.sort(Comparator.comparing(Attack::getSendingTime));

        VBox attackerDetails = new VBox();
        Label name = new Label(a.getPlayerName());
        Label village = new Label(a.getVillageName() + " (" + a.getCoords() + ")");
        Label sendWindow = new Label("Send window: " + a.getSendMin() + " - " + a.getSendMax());
        Label comment = new Label("Comment: " + a.getComment());
        Label sendInfo = new Label("Sends between " +
                targets.get(0).getSendingTime().format(App.TIME_ONLY) +
                " - " +
                targets.get(targets.size()-1).getSendingTime().format(App.TIME_ONLY));
        attackerDetails.getChildren().addAll(name, village, sendWindow, comment, sendInfo);
        attackerDetails.getStyleClass().add("attacker-details");
        attackerRow.getChildren().add(attackerDetails);

        TextArea attackerCommand = new TextArea();
        StringBuilder commandText = new StringBuilder();
        commandText.append(template1.getText());
        commandText.append("\n");
        commandText.append("------------------------------------------");
        commandText.append("\n");
        commandText.append("\n");
        commandText
                .append("Ops hits on ")
                .append(defaultHittingTime.getDayOfWeek().toString())
                .append(" ")
                .append(defaultHittingTime.format(App.DAY_AND_MONTH))
                .append(" around ")
                .append(defaultHittingTime.format(App.TIME_ONLY))
                .append("\n\n");
        commandText.append("Summary:\n");
        if (attacksPerPlayer.get(a.getPlayerId()).size() > targets.size()) {
            // More than one sweep by this player
            List<Attack> attacksForPlayer = attacksPerPlayer.get(a.getPlayerId());
            attacksForPlayer.sort(Comparator.comparing(Attack::getSendingTime));
            for (Attack attack : attacksForPlayer) {
                commandText.append(attack.isReal() ? "[b]" : "")
                        .append(attack.getSendingTime().format(App.TIME_ONLY))
                        .append(attack.isReal() ? " real, " : " fake, ")
                        .append(attack.getAttacker().getVillageName())
                        .append(" (")
                        .append(attack.getAttacker().getCoords())
                        .append(")")
                        .append(attack.isReal() ? "[/b] " : " ")
                        .append(attack.getUnitSpeed())
                        .append("sq/h TS")
                        .append(attack.getTs())
                        .append("\n");
            }
        } else {
            for (Attack attack : targets) {
                commandText.append(attack.isReal() ? "[b]" : "")
                        .append(attack.getSendingTime().format(App.TIME_ONLY))
                        .append(attack.isReal() ? " real[/b] " : " fake ")
                        .append(attack.getUnitSpeed())
                        .append("sq/h TS")
                        .append(attack.getTs())
                        .append("\n");
            }
        }
        commandText.append("\n");
        commandText.append("------------------------------------------");
        commandText.append("\n");
        commandText.append("\n");
        commandText
                .append("Attacks from village [b]")
                .append(a.getVillageName())
                .append(" (")
                .append(a.getCoords())
                .append(")[/b]\n");
        commandText
                .append("Speed multiplier: ")
                .append(a.getArteSpeed()).append("\n\n");
        commandText.append("Targets are in form:\n");
        commandText.append("[b]Departure[/b] // Travel time // [b]Arrival[/b] " +
                "Target\n[b]:: Type ::[/b] (speed TS)\n[ Other info ]\nLanding order\n");
        for (int i = 0; i < targets.size(); i++) {
            Attack attack = targets.get(i);
            if (i > 0 && targets.get(i - 1).getTs() != attack.getTs()) {
                commandText
                        .append("\n*** CHANGE TS LEVEL ")
                        .append(targets.get(i - 1).getTs())
                        .append(" -> ")
                        .append(attack.getTs())
                        .append("\n");
            }
            commandText.append(this.toAttackRow(attack));
        }
        commandText.append(template2.getText());
        attackerCommand.setText(commandText.toString());
        attackerCommand.getStyleClass().add("attacker-command");
        attackerRow.getChildren().add(attackerCommand);

        return attackerRow;
    }


    /**
     * Generates a target row for the command message.
     */
    private String toAttackRow(Attack attack) {
        String travelTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0))
                .plusSeconds(attack.travelSeconds()).format(App.TIME_ONLY);
        StringBuilder attackRow = new StringBuilder();
        attackRow
                .append("\n[b]")
                .append(attack.getSendingTime().format(App.TIME_ONLY))
                .append(" ")
                .append(attack.getSendingTime().format(App.DAY_AND_MONTH))
                .append("[/b] // ")
                .append(travelTime)
                .append(" // [b]")
                .append(attack.getLandingTime().format(App.TIME_ONLY))
                .append(" ")
                .append(attack.getLandingTime().format(App.DAY_AND_MONTH))
                .append("[/b] [x|y]")
                .append(attack.getTarget().getCoords())
                .append("[/x|y] ");
        attackRow
                .append("\n[b]:: ")
                .append(attack.getWaves())
                .append("x ")
                .append(Converters.attackType(attack))
                .append(" ::[/b] (")
                .append(attack.getUnitSpeed())
                .append("sq/h TS")
                .append(attack.getTs())
                .append(")");
        attackRow.append("\n");
        // List artefacts and their effects on target
        if (!attack.getTarget().getArtefact().isEmpty()
                || !attack.getTarget().getArteEffects().isEmpty()) {
            attackRow.append("[ ");
        }
        if (!attack.getTarget().getArtefact().isEmpty()) {
            attackRow.append(attack.getTarget().getArtefact());
            if (!attack.getTarget().getArteEffects().isEmpty()) {
                attackRow.append(", ");
            } else {
                attackRow.append(" ");
            }
        }
        for (String effect : attack.getTarget().getArteEffects()) {
            attackRow
                    .append(effect)
                    .append(" ");
        }
        if (!attack.getTarget().getArtefact().isEmpty()
                || !attack.getTarget().getArteEffects().isEmpty()) {
            attackRow.append("]\n");
        }
        // List attacks in landing order
        List<Attack> targetAttacks = new ArrayList<>();
        for (AttackerVillage a : attackers) {
            for (Attack plannedAttack : a.getPlannedAttacks()) {
                if (plannedAttack.getTarget().getCoordId() == attack.getTarget().getCoordId()) {
                    targetAttacks.add(plannedAttack);
                }
            }
        }
        targetAttacks.sort((attack1, attack2) -> {
            if (attack1.getLandingTime().equals(attack2.getLandingTime())) {
                return attack1.getSendingTime().compareTo(attack2.getSendingTime());
            } else {
                return attack1.getLandingTime().compareTo(attack2.getLandingTime());
            }
        });
        if (targetAttacks.size() > 1) {
            for (int i = 0; i < targetAttacks.size(); i++) {
                Attack a = targetAttacks.get(i);
                attackRow
                        .append(a.getAttacker().getPlayerName())
                        //.append(" (")
                        //.append(a.getAttacker().getCoords())
                        //.append(") ")
                        .append(a.getLandingTime().format(DateTimeFormatter.ofPattern(" :ss")));
                if (i < targetAttacks.size()-1) attackRow.append(", ");
            }
            attackRow.append("\n");
        }
        return attackRow.toString();
    }


    /**
     * Saves the command templates to database and updates the commands.
     * TODO move DB operations to the Database class
     * @param actionEvent button press
     */
    public void saveTemplates(ActionEvent actionEvent) {
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            conn.prepareStatement("DELETE FROM templates").execute();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO templates VALUES(?, ?)");
            ps.setString(1, template1.getText());
            ps.setString(2, template2.getText());
            ps.execute();
            conn.close();
            updateCommands();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Changes to the planning view.
     */
    public void toPlanning() {
        this.toScene.set("planning");
    }
}
