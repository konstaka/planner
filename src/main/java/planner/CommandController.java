package planner;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

public class CommandController implements Initializable {

    StringProperty toScene = new SimpleStringProperty("");

    @FXML
    TextArea template1;

    @FXML
    TextArea template2;

    @FXML
    VBox commands;

    List<AttackerVillage> attackers;

    List<HBox> attackerRows;

    DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm:ss");

    DateTimeFormatter day = DateTimeFormatter.ofPattern("dd.MM");


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
        attackerRows = new ArrayList<>();
    }


    public void updateCommands() {
        attackerRows.clear();
        for (AttackerVillage a : attackers) {
            attackerRows.add(this.toAttackerRow(a));
        }
    }


    private HBox toAttackerRow(AttackerVillage a) {
        List<Attack> targets = a.getPlannedAttacks();
        targets.sort(Comparator.comparing(Attack::getSendingTime));

        HBox attackerRow = new HBox();

        VBox attackerDetails = new VBox();
        Label name = new Label(a.getPlayerName());
        Label village = new Label(a.getVillageName() + " (" + a.getCoords() + ")");
        Label sendWindow = new Label("Send window: " + a.getSendMin() + " - " + a.getSendMax());
        Label sendInfo = new Label("Sends between " +
                targets.get(0).getSendingTime().format(time) +
                " - " +
                targets.get(targets.size()-1).getSendingTime().format(time));
        attackerDetails.getChildren().addAll(name, village, sendWindow, sendInfo);
        attackerDetails.getStyleClass().add("attacker-details");
        attackerRow.getChildren().add(attackerDetails);

        TextArea attackerCommand = new TextArea();
        StringBuilder commandText = new StringBuilder();
        commandText.append(template1);
        commandText
                .append("Attacks from village [b]")
                .append(a.getVillageName())
                .append(" (")
                .append(a.getCoords())
                .append(")[/b]\n");
        commandText
                .append("TS")
                .append(a.getTs())
                .append(", Speed multiplier: ")
                .append(a.getSpeed()).append("\n\n");
        commandText.append("Targets are in form:\n");
        commandText.append("[b]Departure[/b]  // Travel time //  [b]Arrival[/b]  " +
                "Target  Other  [b]Type[/b]  Attack order\n\n");
        for (Attack attack : targets) {
            commandText.append(this.toAttackRow(attack));
        }
        commandText.append(template2);
        attackerCommand.setText(commandText.toString());
        attackerCommand.getStyleClass().add("attacker-command");
        attackerRow.getChildren().add(attackerCommand);

        return attackerRow;
    }


    /**
     * Generates a target row for the command message. Example:
     * [b]19:04:24 05.03[/b] // 14:21:18 // [b]9:25:42 06.03[/b] [x|y]-6|5[/x|y] Scout effect [b]Fake cata[/b] anger, others
     */
    private String toAttackRow(Attack attack) {
        String travelTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0))
                .plusSeconds(attack.travelSeconds()).format(time);
        StringBuilder attackRow = new StringBuilder();
        attackRow
                .append("[b]")
                .append(attack.getSendingTime().format(time))
                .append(" ")
                .append(attack.getSendingTime().format(day))
                .append("[/b] // ")
                .append(travelTime)
                .append(" // [b]")
                .append(attack.getLandingTime().format(time))
                .append(" ")
                .append(attack.getLandingTime().format(day))
                .append("[/b] [x|y]")
                .append(attack.getTarget().getCoords())
                .append("[/x|y] ");
        for (String effect : attack.getTarget().getArteEffects()) {
            attackRow.append(effect).append(" ");
        }
        attackRow
                .append("[b]")
                .append(attack.getWaves())
                .append("x ")
                .append(this.attackType(attack))
                .append("[/b] ");
        for (AttackerVillage a : attackers) {

        }


        attackRow.append("\n");
        return attackRow.toString();
    }


    private String attackType(Attack attack) {
        switch (attack.getUnitSpeed()) {
            case 5:
                return attack.getReal().get() ? "Conquer" : "Fake conquer";
            case 4:
                if (attack.getConq().get()) {
                    return attack.getReal().get() ? "Conquer" : "Fake conquer";
                } else {
                    return attack.getReal().get() ? "Ram" : "Fake ram";
                }
            case 3:
                if (attack.getConq().get()) {
                    return attack.getReal().get() ? "Conquer" : "Fake conquer";
                } else {
                    return attack.getReal().get() ? "Cata" : "Fake cata";
                }
            default:
                return attack.getReal().get() ? "Sweep" : "Fake sweep";
        }
    }


    // TODO
    public void saveTemplates(ActionEvent actionEvent) {
    }


    /**
     * Changes to the planning view.
     */
    public void toPlanning() {
        this.toScene.set("planning");
    }
}
