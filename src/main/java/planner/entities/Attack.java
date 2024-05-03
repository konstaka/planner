package planner.entities;

import java.time.LocalDateTime;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
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

    @Getter @Setter
    private int ts;

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

    @Getter @Setter
    private boolean withHero;

    @Getter @Setter
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
        // Round to 5 decimal places as per Travian implementation
        distance = Math.round(distance * 100000) / 100000.0;
        // Baseline speed
        double squaresPerSecond = unitSpeed * serverSpeed * this.attacker.getArteSpeed() / 60 / 60;
        // Return if distance is under 20
        if (distance <= 20) return Math.round(distance / squaresPerSecond);
        // No-TS part of travel
        double travelTime = 20L / squaresPerSecond;
        // Reduce distance
        distance -= 20;
        // Calculate TS factor
        double factor = 1.0 + this.getTs() * 0.2;
        // Calculate boots factor
        if (this.isWithHero()) factor += this.attacker.getHeroBoots() / 100.0;
        // Adjust speed
        squaresPerSecond *= factor;
        // Compute remaining time
        travelTime += distance / squaresPerSecond;
        return Math.round(travelTime);
    }


    /**
     * Creates a visual representation of the attack for planning.
     */
    public Pane toDisplayBox() {
        HBox box = new HBox();

        if (this.isWithHero()) {
            VBox heroBox = new VBox();
            Region r0 = new Region();
            r0.setMinHeight(4);
            r0.setMaxHeight(4);
            heroBox.getChildren().add(r0);
            ImageView heroImg = new ImageView(
                    String.valueOf(getClass().getResource("images/specials.gif"))
            );
            heroImg.setViewport(new Rectangle2D(37, 0, 18, 16));
            heroImg.setPreserveRatio(true);
            heroImg.setFitHeight(11);
            heroImg.setTranslateX(4);
            heroBox.getChildren().add(heroImg);
            box.getChildren().add(heroBox);
        }

        VBox offs = new VBox();
        Region r4 = new Region();
        r4.setMaxHeight(4);
        r4.setMinHeight(4);
        offs.getChildren().add(r4);
        HBox offIconRow = attacker.offIconRow();
        Tooltip offStringTooltip = new Tooltip(
                attacker.getPlayerName() + " " + attacker.getCoords() + "\n" +
                        attacker.getOffString() + "\n" +
                        "This send: " + this.getSendingTime().format(App.TIME_ONLY) + "\n" +
                        "with TS " + this.getTs() + ", speed " + this.getUnitSpeed() + "\n" +
                        "Earliest send: " + attacker.getSendMin() + "\n" +
                        "Latest send: " + attacker.getSendMax() + "\n" +
                        "Comment: " + attacker.getComment());
        offStringTooltip.setShowDelay(Duration.millis(0));
        offStringTooltip.setHideDelay(Duration.millis(500));
        offStringTooltip.setShowDuration(Duration.INDEFINITE);
        offStringTooltip.setMaxWidth(200);
        offStringTooltip.setWrapText(true);
        Tooltip.install(offIconRow, offStringTooltip);
        offs.getChildren().add(offIconRow);
        Label shortOffSizeString = new Label(attacker.offSizeRounded());
        shortOffSizeString.setTooltip(offStringTooltip);
        offs.getChildren().add(shortOffSizeString);

        for (Node n : offs.getChildren()) n.getStyleClass().add("attack-box-off-column");
        box.getChildren().add(offs);

        if (this.isConq()) {
            VBox chiefs = new VBox();
            Region r5 = new Region();
            r5.setMaxHeight(4);
            r5.setMinHeight(4);
            chiefs.getChildren().add(r5);
            chiefs.getChildren().add(attacker.getChiefImg());
            Label chiefAmt = new Label(""+attacker.getChiefs());
            chiefAmt.setTranslateX(-4);
            chiefs.getChildren().add(chiefAmt);
            for (Node n : chiefs.getChildren()) n.getStyleClass().add("attack-box-chief-column");
            box.getChildren().add(chiefs);
        }

        VBox wavesBox = new VBox();
        Label attackerNameLabel = new Label(this.attacker.getPlayerName());
        attackerNameLabel.getStyleClass().add("attack-attacker-name");
        wavesBox.getChildren().add(attackerNameLabel);
        String wavesString = this.waves+"x ";
        if (this.getLandingTimeShift() != 0) {
            if (this.getLandingTimeShift() > 0) wavesString += "+";
            wavesString += this.getLandingTimeShift() + "s";
        }
        Label wavesLabel = new Label(wavesString);
        wavesLabel.getStyleClass().add("attack-box-waves-label");
        if (this.isReal()) wavesLabel.getStyleClass().add("real-target");
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

        ImageView del = new ImageView(
                String.valueOf(getClass().getResource("images/delete.gif"))
        );
        del.setViewport(new Rectangle2D(0, 0, 7, 7));
        del.setOnMouseClicked(actionEvent -> {
            this.setWaves(0);
            this.setReal(false);
            this.setConq(false);
            this.setWithHero(false);
            this.setLandingTimeShift(0);
            this.getUpdated().set(true);
        });
        box.getChildren().add(del);

        VBox boxWithPadding = new VBox();
        Region r6 = new Region();
        r6.setMaxHeight(1);
        r6.setMinHeight(1);
        boxWithPadding.getChildren().add(r6);
        boxWithPadding.getChildren().add(box);

        return boxWithPadding;
    }


    @Override
    public String toString() {
        String ret = "";
        if (this.waves > 0) ret = "- ";
        return ret + attacker.toString() + " " + this.getSendingTime().format(App.TIME_ONLY);
    }
}
