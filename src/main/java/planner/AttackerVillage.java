package planner;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.tools.Tool;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import lombok.Getter;
import lombok.Setter;

public class AttackerVillage extends Village {

    @Getter
    @Setter
    private IntegerProperty ts = new SimpleIntegerProperty();

    @Getter
    @Setter
    private double speed;

    @Getter
    @Setter
    private String offString;

    @Getter
    @Setter
    private int offSize;

    @Getter
    @Setter
    private int catas;

    @Getter
    @Setter
    private int chiefs;

    @Getter
    @Setter
    private String sendMin;

    @Getter
    @Setter
    private String sendMax;

    @Getter
    @Setter
    private String comment;

    @Getter
    @Setter
    private BooleanProperty alert = new SimpleBooleanProperty(false);

    @Getter
    @Setter
    private List<Attack> plannedAttacks = new ArrayList<>();

    private Image image = null;


    public AttackerVillage(int coordId) {
        super(coordId);
    }


    public VBox toDisplayBox() {
        switch(this.getTribe()) {
            case 1:
                image = new Image(String.valueOf(getClass().getResource("images/romans.gif")));
                break;
            case 2:
                image = new Image(String.valueOf(getClass().getResource("images/teutons.gif")));
                break;
            case 3:
                image = new Image(String.valueOf(getClass().getResource("images/gauls.gif")));
                break;
        }

        VBox box = new VBox();
        box.getStyleClass().add("attacker-box");
        assert image != null;

        // Spacer
        Region r1 = new Region();
        r1.setMaxHeight(4);
        r1.setMinHeight(4);
        box.getChildren().add(r1);
        // Chiefs
        ImageView chief = this.getChief();
        chief.setTranslateX(17);
        box.getChildren().add(chief);
        box.getChildren().add(new Label(""+this.chiefs));
        // Spacer
        Region r2 = new Region();
        r2.setMaxHeight(2);
        r2.setMinHeight(2);
        box.getChildren().add(r2);
        // Catas
        ImageView cata = new ImageView(image);
        cata.setViewport(new Rectangle2D(132, 0, 18, 16));
        cata.setPreserveRatio(true);
        cata.setFitHeight(11);
        cata.setTranslateX(17);
        box.getChildren().add(cata);
        box.getChildren().add(new Label(""+this.catas));
        // Spacer
        Region r3 = new Region();
        r3.setMaxHeight(4);
        r3.setMinHeight(4);
        box.getChildren().add(r3);
        // Offs
        Tooltip t1 = new Tooltip(offString);
        t1.setShowDelay(Duration.millis(0));
        t1.setHideDelay(Duration.millis(500));
        t1.setShowDuration(Duration.INDEFINITE);
        HBox offRow = this.toOffRow();
        Tooltip.install(offRow, t1);
        box.getChildren().add(offRow);
        Label l = new Label(this.offSizeRounded());
        l.setTooltip(t1);
        box.getChildren().add(l);
        // Spacer
        Region r4 = new Region();
        r4.setMaxHeight(4);
        r4.setMinHeight(4);
        box.getChildren().add(r4);
        // TS img
        HBox tsRow = new HBox();
        ImageView tsImg = new ImageView(String.valueOf(getClass().getResource("images/ts.gif")));
        tsImg.setPreserveRatio(true);
        tsImg.setFitHeight(11);
        tsRow.getChildren().add(tsImg);
        // Spacer
        Region r5 = new Region();
        r5.setMaxWidth(2);
        r5.setMinWidth(2);
        tsRow.getChildren().add(r5);
        // TS level selector
        TextField tsLvl = new TextField();
        tsLvl.setText(""+this.getTs().getValue());
        tsLvl.setPrefWidth(22);
        tsLvl.setAlignment(Pos.BASELINE_CENTER);
        tsLvl.setPadding(new Insets(0, 1, 0, 1));
        tsLvl.setOnAction(this::updateTs);
        tsLvl.focusedProperty().addListener((ov, oldV, newV) -> {
            if (!newV) tsLvl.fireEvent(new ActionEvent()); // Update on focus change
        });
        tsRow.getChildren().add(tsLvl);
        box.getChildren().add(tsRow);

        for (Node n : box.getChildren()) {
            n.getStyleClass().add("attacker-box-label");
        }
        Region spacer = new Region();
        box.getChildren().add(spacer);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label name = new Label(this.getPlayerName() + " (" + plannedAttacks.size() + ")");
        name.getStyleClass().add("attacker-name");
        StringBuilder tool2 = new StringBuilder("Earliest send: " + getSendMin() + "\n" +
                "Latest send: " + getSendMax() + "\n" +
                "Comment: " + getComment());
        if (!plannedAttacks.isEmpty()) {
            plannedAttacks.sort(Comparator.comparing(Attack::getSendingTime));
            tool2.append("\nCurrent sends:");
        }
        for (Attack attack : plannedAttacks) {
            tool2
                    .append("\n")
                    .append(attack.getSendingTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        Tooltip t2 = new Tooltip(tool2.toString());
        t2.setShowDelay(Duration.millis(0));
        t2.setHideDelay(Duration.millis(500));
        t2.setShowDuration(Duration.INDEFINITE);
        name.setTooltip(t2);
        if (alert.get()) name.getStyleClass().add("alert");
        box.getChildren().add(name);
        return box;
    }


    public HBox toOffRow() {
        HBox offRow = new HBox();
        int[] offUnitCoords = this.getOffUnitCoords();
        ImageView off1 = new ImageView(image);
        off1.setViewport(new Rectangle2D(offUnitCoords[0], 0, 18, 16));
        off1.setPreserveRatio(true);
        off1.setFitHeight(11);
        offRow.getChildren().add(off1);
        ImageView off2 = new ImageView(image);
        off2.setViewport(new Rectangle2D(offUnitCoords[1], 0, 18, 16));
        off2.setPreserveRatio(true);
        off2.setFitHeight(11);
        offRow.getChildren().add(off2);
        return offRow;
    }


    public ImageView getChief() {
        ImageView chief = new ImageView(image);
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
     * Updates the attacks associated with this attacker.
     */
    private void updateTs(ActionEvent e) {
        if (e.getSource() instanceof TextField) {
            TextField f = (TextField) e.getSource();
            try {
                int lvl = Integer.parseInt(f.getText());
                if (lvl < 0) {
                    f.setText("0");
                    this.getTs().set(0);
                } else if (lvl > 20) {
                    f.setText("20");
                    this.getTs().set(20);
                }
                this.getTs().set(lvl);
            } catch (NumberFormatException ex) {
                f.setText("0");
            }
        }
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
