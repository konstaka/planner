package planner.entities;

import java.time.LocalDateTime;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import planner.App;

@AllArgsConstructor
public class Attack implements Cloneable {

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
        // Baseline speed
        double squaresPerSecond = unitSpeed * serverSpeed * this.attacker.getArteSpeed() / 60 / 60;
        // Return if no TS
        if (distance <= 20 || this.attacker.getTs() == 0) return Math.round(distance / squaresPerSecond);
        // No-TS part of travel
        double travelTime = 20L / squaresPerSecond;
        // Reduce distance
        distance -= 20;
        // Calculate TS factor
        double factor = 1.0 + this.attacker.getTs() * 0.2;
        // Adjust speed
        if (this.isWithHero()) squaresPerSecond *= 1 + this.attacker.getHeroBoots() / 100.0;
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

        if (this.isWithHero()) {
            VBox heroBox = new VBox();
            Region r0 = new Region();
            r0.setMinHeight(2);
            r0.setMaxHeight(2);
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
        r4.setMaxHeight(2);
        r4.setMinHeight(2);
        offs.getChildren().add(r4);
        offs.getChildren().add(attacker.offIconRow());
        offs.getChildren().add(new Label(attacker.offSizeRounded()));
        for (Node n : offs.getChildren()) n.getStyleClass().add("attack-box-off-column");
        box.getChildren().add(offs);

        if (this.isConq()) {
            VBox chiefs = new VBox();
            Region r5 = new Region();
            r5.setMaxHeight(2);
            r5.setMinHeight(2);
            chiefs.getChildren().add(r5);
            chiefs.getChildren().add(attacker.getChiefImg());
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

        return box;
    }


    @Override
    public String toString() {
        return attacker.toString() + " " + this.getSendingTime().format(App.TIME_ONLY);
    }

    /**
     * Creates and returns a copy of this object.  The precise meaning
     * of "copy" may depend on the class of the object. The general
     * intent is that, for any object {@code x}, the expression:
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * will be true, and that the expression:
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * will be {@code true}, but these are not absolute requirements.
     * While it is typically the case that:
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * will be {@code true}, this is not an absolute requirement.
     * <p>
     * By convention, the returned object should be obtained by calling
     * {@code super.clone}.  If a class and all of its superclasses (except
     * {@code Object}) obey this convention, it will be the case that
     * {@code x.clone().getClass() == x.getClass()}.
     * <p>
     * By convention, the object returned by this method should be independent
     * of this object (which is being cloned).  To achieve this independence,
     * it may be necessary to modify one or more fields of the object returned
     * by {@code super.clone} before returning it.  Typically, this means
     * copying any mutable objects that comprise the internal "deep structure"
     * of the object being cloned and replacing the references to these
     * objects with references to the copies.  If a class contains only
     * primitive fields or references to immutable objects, then it is usually
     * the case that no fields in the object returned by {@code super.clone}
     * need to be modified.
     * <p>
     * The method {@code clone} for class {@code Object} performs a
     * specific cloning operation. First, if the class of this object does
     * not implement the interface {@code Cloneable}, then a
     * {@code CloneNotSupportedException} is thrown. Note that all arrays
     * are considered to implement the interface {@code Cloneable} and that
     * the return type of the {@code clone} method of an array type {@code T[]}
     * is {@code T[]} where T is any reference or primitive type.
     * Otherwise, this method creates a new instance of the class of this
     * object and initializes all its fields with exactly the contents of
     * the corresponding fields of this object, as if by assignment; the
     * contents of the fields are not themselves cloned. Thus, this method
     * performs a "shallow copy" of this object, not a "deep copy" operation.
     * <p>
     * The class {@code Object} does not itself implement the interface
     * {@code Cloneable}, so calling the {@code clone} method on an object
     * whose class is {@code Object} will result in throwing an
     * exception at run time.
     *
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not
     *                                    support the {@code Cloneable} interface. Subclasses
     *                                    that override the {@code clone} method can also
     *                                    throw this exception to indicate that an instance cannot
     *                                    be cloned.
     * @see Cloneable
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        Attack clone = (Attack) super.clone();
        clone.setTarget((TargetVillage) target.clone());
        clone.setAttacker((AttackerVillage) attacker.clone());
        clone.setUpdated(new SimpleBooleanProperty(updated.get()));
        return clone;
    }
}
