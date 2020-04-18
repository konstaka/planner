package planner.entities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import planner.App;

public class AttackerVillage extends Village {

    @Getter @Setter
    private int ts;

    @Getter @Setter
    private double speed;

    @Getter @Setter
    private String offString;

    @Getter @Setter
    private int offSize;

    @Getter @Setter
    private int catas;

    @Getter @Setter
    private int chiefs;

    @Getter @Setter
    private String sendMin;

    @Getter @Setter
    private String sendMax;

    @Getter @Setter
    private String comment;

    @Getter @Setter
    private boolean alert = false;

    @Getter
    private BooleanProperty updated = new SimpleBooleanProperty(false);

    @Getter
    private List<Attack> plannedAttacks = new ArrayList<>();

    private Image tribeTroops = null;


    @Builder
    public AttackerVillage(int coordId) {

        super(coordId);

        switch(this.getTribe()) {
            case 1:
                tribeTroops = new Image(String.valueOf(getClass().getResource("images/romans.gif")));
                break;
            case 2:
                tribeTroops = new Image(String.valueOf(getClass().getResource("images/teutons.gif")));
                break;
            case 3:
                tribeTroops = new Image(String.valueOf(getClass().getResource("images/gauls.gif")));
                break;
        }
    }


    /**
     * Crafts a displayable column for the planning view.
     * @return VBox representing this participant
     */
    public VBox toDisplayBox() {

        VBox box = new VBox();
        box.getStyleClass().add("attacker-box");
        assert tribeTroops != null;

        // Spacer
        Region r1 = new Region();
        r1.setMaxHeight(4);
        r1.setMinHeight(4);
        box.getChildren().add(r1);

        // Chiefs
        ImageView chiefImage = this.getChiefImg();
        chiefImage.setTranslateX(17);
        box.getChildren().add(chiefImage);
        box.getChildren().add(new Label(""+this.chiefs));

        // Spacer
        Region r2 = new Region();
        r2.setMaxHeight(2);
        r2.setMinHeight(2);
        box.getChildren().add(r2);

        // Catas
        ImageView cataImage = new ImageView(tribeTroops);
        cataImage.setViewport(new Rectangle2D(132, 0, 18, 16));
        cataImage.setPreserveRatio(true);
        cataImage.setFitHeight(11);
        cataImage.setTranslateX(17);
        box.getChildren().add(cataImage);
        box.getChildren().add(new Label(""+this.catas));

        // Spacer
        Region r3 = new Region();
        r3.setMaxHeight(4);
        r3.setMinHeight(4);
        box.getChildren().add(r3);

        // Offs
        HBox offIconRow = this.offIconRow();
        Tooltip offStringTooltip = new Tooltip(
                offString + "\n" +
                        "Earliest send: " + getSendMin() + "\n" +
                        "Latest send: " + getSendMax()+ "\n" +
                        "Comment: " + getComment());
        offStringTooltip.setShowDelay(Duration.millis(0));
        offStringTooltip.setHideDelay(Duration.millis(500));
        offStringTooltip.setShowDuration(Duration.INDEFINITE);
        offStringTooltip.setMaxWidth(200);
        offStringTooltip.setWrapText(true);
        Tooltip.install(offIconRow, offStringTooltip);
        box.getChildren().add(offIconRow);
        Label shortOffSizeString = new Label(this.offSizeRounded());
        shortOffSizeString.setTooltip(offStringTooltip);
        box.getChildren().add(shortOffSizeString);

        // Spacer
        Region r4 = new Region();
        r4.setMaxHeight(4);
        r4.setMinHeight(4);
        box.getChildren().add(r4);

        // TS img
        HBox tsRow = new HBox();
        ImageView tsImg = new ImageView(
                String.valueOf(getClass().getResource("images/ts.gif"))
        );
        tsImg.setPreserveRatio(true);
        tsImg.setFitWidth(11);
        tsRow.getChildren().add(tsImg);
        // Spacer
        Region r5 = new Region();
        r5.setMaxWidth(2);
        r5.setMinWidth(2);
        tsRow.getChildren().add(r5);
        // TS level selector
        TextField tsLvl = new TextField();
        tsLvl.setText(""+this.getTs());
        tsLvl.setPrefWidth(22);
        tsLvl.setAlignment(Pos.BASELINE_CENTER);
        tsLvl.setPadding(new Insets(0, 1, 0, 1));
        // Update TS level
        tsLvl.setOnAction(actionEvent -> {
            try {
                int lvl = Integer.parseInt(tsLvl.getText());
                if (lvl < 0) {
                    tsLvl.setText("0");
                    this.setTs(0);
                } else if (lvl > 20) {
                    tsLvl.setText("20");
                    this.setTs(20);
                }
                this.setTs(lvl);
                this.getUpdated().set(true);
            } catch (NumberFormatException ex) {
                tsLvl.setText("0");
            }
        });
        // Update also on focus change
        tsLvl.focusedProperty().addListener((ov, oldV, newV) -> {
            if (!newV && oldV) tsLvl.fireEvent(new ActionEvent());
        });
        tsRow.getChildren().add(tsLvl);
        box.getChildren().add(tsRow);

        // Spacer
        Region r7 = new Region();
        r7.setMaxHeight(2);
        r7.setMinHeight(2);
        box.getChildren().add(r7);


        // Artefact speed img
        HBox arteSpdRow = new HBox();
        ImageView arteSpdImg = new ImageView(
                String.valueOf(getClass().getResource("images/artefacts@2x.png"))
        );
        arteSpdImg.setViewport(new Rectangle2D(79, 0, 32, 32));
        arteSpdImg.setPreserveRatio(true);
        arteSpdImg.setFitWidth(11);
        arteSpdRow.getChildren().add(arteSpdImg);
        // Spacer
        Region r6 = new Region();
        r6.setMaxWidth(2);
        r6.setMinWidth(2);
        arteSpdRow.getChildren().add(r6);
        // Artefact selector
        TextField arteSpd = new TextField();
        arteSpd.setText(""+this.getSpeed());
        arteSpd.setPrefWidth(22);
        arteSpd.setAlignment(Pos.BASELINE_CENTER);
        arteSpd.setPadding(new Insets(0, 1, 0, 1));
        // Listen to changes
        arteSpd.setOnAction(actionEvent -> {
            try {
                double newArteSpeed = Double.parseDouble(arteSpd.getText());
                double eps = 0.01;
                if (Math.abs(newArteSpeed - 2.0) < eps) this.setSpeed(2.0);
                else if (Math.abs(newArteSpeed - 1.5) < eps) this.setSpeed(1.5);
                else if (Math.abs(newArteSpeed - 1.0) < eps) this.setSpeed(1.0);
                else if (Math.abs(newArteSpeed - 0.67) < eps) this.setSpeed(0.67);
                else if (Math.abs(newArteSpeed - 0.5) < eps) this.setSpeed(0.5);
                else if (Math.abs(newArteSpeed - 0.33) < eps) this.setSpeed(0.33);
            } catch (NumberFormatException e) {
            }
            arteSpd.setText(""+this.getSpeed());
        });
        arteSpd.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && oldValue) arteSpd.fireEvent(new ActionEvent());
        });
        arteSpdRow.getChildren().add(arteSpd);
        box.getChildren().add(arteSpdRow);















        // Style class for elements this far
        for (Node n : box.getChildren()) {
            n.getStyleClass().add("attacker-box-label");
        }

        // Automatic spacer
        Region spacer = new Region();
        box.getChildren().add(spacer);
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Player name and amount of sends from this village
        Label playerNameVillageSends = new Label(this.getPlayerName() + " (" + plannedAttacks.size() + ")");
        // Tooltip contains relevant information of the attacker
        VBox attackerInfo = new VBox();
        attackerInfo.getChildren().add(new Label("Earliest send: " + getSendMin()));
        attackerInfo.getChildren().add(new Label("Latest send: " + getSendMax()));
        if (!plannedAttacks.isEmpty()) {
            plannedAttacks.sort(Comparator.comparing(Attack::getSendingTime));
            attackerInfo.getChildren().add(new Label("Current sends:"));
        }
        for (Attack attack : plannedAttacks) {
            Label attackLabel = new Label(
                    attack.getSendingTime().format(App.TIME_ONLY) +
                            " " +
                            attack.getTarget().getCoords());
            if (attack.isConflicting()) {
                attackLabel.getStyleClass().add("alert");
            }
            attackerInfo.getChildren().add(attackLabel);
        }
        Tooltip attackerInfoTooltip = new Tooltip();
        attackerInfoTooltip.setGraphic(attackerInfo);
        attackerInfoTooltip.setShowDelay(Duration.millis(0));
        attackerInfoTooltip.setHideDelay(Duration.millis(500));
        attackerInfoTooltip.setShowDuration(Duration.INDEFINITE);
        attackerInfoTooltip.setMaxWidth(200);
        attackerInfoTooltip.setWrapText(true);
        playerNameVillageSends.setTooltip(attackerInfoTooltip);
        playerNameVillageSends.getStyleClass().add("attacker-name");
        if (alert) playerNameVillageSends.getStyleClass().add("alert");
        box.getChildren().add(playerNameVillageSends);
        return box;
    }


    public HBox offIconRow() {
        assert tribeTroops != null;
        HBox offRow = new HBox();
        int[] offUnitCoords = this.getOffUnitCoords();
        ImageView off1 = new ImageView(tribeTroops);
        off1.setViewport(new Rectangle2D(offUnitCoords[0], 0, 18, 16));
        off1.setPreserveRatio(true);
        off1.setFitHeight(11);
        offRow.getChildren().add(off1);
        ImageView off2 = new ImageView(tribeTroops);
        off2.setViewport(new Rectangle2D(offUnitCoords[1], 0, 18, 16));
        off2.setPreserveRatio(true);
        off2.setFitHeight(11);
        offRow.getChildren().add(off2);
        return offRow;
    }


    public ImageView getChiefImg() {
        ImageView chief = new ImageView(tribeTroops);
        chief.setViewport(new Rectangle2D(152, 0, 18, 16));
        chief.setPreserveRatio(true);
        chief.setFitHeight(11);
        return chief;
    }


    @Override
    public java.lang.String toString() {
        return getPlayerName() + " " + getCoords() + ", " + offSizeRounded();
    }


    /**
     * Looks into the off units and determines which two types are the largest.
     * @return in-image coordinates to show units in the planning view
     */
    private int[] getOffUnitCoords() {
        int[] ret = new int[2];
        // Defaults per tribe:
        switch (this.getTribe()) {
            case 1:
                ret[0] = 37; // imperian
                ret[1] = 73; // ei
                break;
            case 2:
                ret[0] = 0; // club
                ret[1] = 95; // tk
                break;
            case 3:
                ret[0] = 19; // sword
                ret[1] = 95; // haed
                break;
        }
        String[] offs = this.offString.split("\\+");
        int[] offCounts = new int[offs.length];
        for (int i = 0; i < offs.length; i++) {
            offCounts[i] = Integer.parseInt(offs[i].trim());
        }
        switch (this.getTribe()) {
            case 1:
                if (offCounts[2] > offCounts[1]) {
                    ret[1] = 95; // ec
                }
                break;
            case 2:
                if (offCounts[1] > offCounts[0]) {
                    ret[0] = 37; // axe
                }
                break;
            case 3:
                if (offCounts[1] > offCounts[2]) {
                    ret[1] = 55; // tt
                }
                break;
        }
        return ret;
    }


    public String offSizeRounded() {
        return Math.round(this.offSize / 1000.0) + "k";
    }
}
