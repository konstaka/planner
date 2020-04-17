package planner.entities;

import java.time.LocalDateTime;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import planner.App;

@AllArgsConstructor
public class Attack {

    @Getter @Setter
    private TargetVillage target;

    @Getter @Setter
    private AttackerVillage attacker;

    @Getter @Setter
    private int waves;

    @Getter @Setter
    private boolean real;

    @Getter @Setter
    private boolean conq;

    @Getter @Setter
    private int unitSpeed;

    @Setter
    private LocalDateTime landingTime;

    @Getter @Setter
    private int landingTimeShift;

    @Getter @Setter
    private int serverSpeed;

    @Getter @Setter
    private int serverSize;

    @Getter @Setter
    private boolean conflicting;

    @Getter
    private BooleanProperty updated;


    public LocalDateTime getLandingTime() {
        return landingTime.plusSeconds(landingTimeShift);
    }


    public LocalDateTime getSendingTime() {
        return this.getLandingTime().minusSeconds(this.travelSeconds());
    }


    /**
     * Calculates travel time on a T4 (2019) Travian map.
     * @return travel time in seconds
     */
    public long travelSeconds() {
        // Distance on a torus surface
        double distance = Math.sqrt(
                Math.pow(
                        Math.min(
                                Math.abs(this.attacker.getXCoord() - this.target.getXCoord()),
                                this.serverSize*2+1 - Math.abs(this.attacker.getXCoord() - this.target.getXCoord())),
                        2)
                + Math.pow(
                        Math.min(
                                Math.abs(this.attacker.getYCoord() - this.target.getYCoord()),
                                this.serverSize*2+1 - Math.abs(this.attacker.getYCoord() - this.target.getYCoord())),
                        2)
        );
        // Baseline speed
        double squaresPerSecond = unitSpeed * serverSpeed * this.attacker.getSpeed() / 60 / 60;
        // Return if no TS
        if (distance <= 20 || this.attacker.getTs() == 0) return Math.round(distance / squaresPerSecond);
        // No-TS part of travel
        double travelTime = 20L / squaresPerSecond;
        // Reduce distance
        distance -= 20;
        // Calculate TS factor
        double factor = 1.0 + this.attacker.getTs() * 0.2;
        // Adjust speed
        squaresPerSecond *= factor;
        // Compute remaining time
        travelTime += distance / squaresPerSecond;
        return Math.round(travelTime);
    }


    /**
     * Creates a visual representation of the attack for planning.
     */
    public HBox toDisplayBox() {
        HBox box = new HBox();

        VBox offs = new VBox();
        Region r4 = new Region();
        r4.setMaxHeight(2);
        r4.setMinHeight(2);
        offs.getChildren().add(r4);
        offs.getChildren().add(attacker.offIconRow());
        offs.getChildren().add(new Label(attacker.offSizeRounded()));
        for (Node n : offs.getChildren()) n.getStyleClass().add("attack-box-off-column");
        box.getChildren().add(offs);

        if (conq) {
            VBox chiefs = new VBox();
            Region r5 = new Region();
            r5.setMaxHeight(2);
            r5.setMinHeight(2);
            chiefs.getChildren().add(r5);
            chiefs.getChildren().add(attacker.getChief());
            Label chiefAmt = new Label(""+attacker.getChiefs());
            chiefAmt.setTranslateX(-4);
            chiefs.getChildren().add(chiefAmt);
            for (Node n : chiefs.getChildren()) n.getStyleClass().add("attack-box-chief-column");
            box.getChildren().add(chiefs);
        }

        VBox wavesBox = new VBox();
        String wavesString = this.waves+"x ";
        if (this.getLandingTimeShift() != 0) {
            if (this.getLandingTimeShift() > 0) wavesString += "+";
            wavesString += this.getLandingTimeShift() + "s";
        }
        Label wavesLabel = new Label(wavesString);
        if (real) wavesLabel.getStyleClass().add("real-target");
        wavesBox.getChildren().add(wavesLabel);
        Button minus = new Button("-");
        Button plus = new Button("+");
        minus.setOnAction(a -> {
            this.setLandingTimeShift(landingTimeShift - 1);
            this.getUpdated().set(true);
        });
        plus.setOnAction(a -> {
            this.setLandingTimeShift(landingTimeShift + 1);
            this.getUpdated().set(true);
        });
        minus.getStyleClass().add("attack-box-button");
        plus.getStyleClass().add("attack-box-button");
        HBox plusminus = new HBox();
        plusminus.getChildren().addAll(minus, plus);
        plusminus.setAlignment(Pos.BASELINE_RIGHT);
        wavesBox.getChildren().add(plusminus);
        box.getChildren().add(wavesBox);

        ImageView del = new ImageView(new Image(String.valueOf(getClass().getResource("images/delete.gif"))));
        del.setViewport(new Rectangle2D(0, 0, 7, 7));
        del.setOnMouseClicked(actionEvent -> {
            this.setWaves(0);
            this.setReal(false);
            this.setConq(false);
            this.setLandingTimeShift(0);
            this.getUpdated().set(true);
        });
        box.getChildren().add(del);

        return box;
    }


    @Override
    public String toString() {
        return attacker.toString() + " " + getSendingTime().format(App.TIME_ONLY);
    }
}