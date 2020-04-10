package planner;

import java.util.Arrays;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;

public class AttackerVillage extends Village {

    @Getter
    @Setter
    private int ts;

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

    public AttackerVillage(int coordId) {
        super(coordId);
    }

    public VBox toDisplayBox() {
        Image image = null;
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
        ImageView chief = new ImageView(image);
        chief.setViewport(new Rectangle2D(152, 0, 18, 16));
        chief.setPreserveRatio(true);
        chief.setFitHeight(11);
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
        box.getChildren().add(offRow);
        box.getChildren().add(new Label(this.offSizeRounded()));
        // TS
        HBox tsRow = new HBox();
        ImageView tsImg = new ImageView("images/ts.gif");
        tsImg.setPreserveRatio(true);
        tsImg.setFitHeight(11);
        tsRow.getChildren().add(tsImg);


        for (Node n : box.getChildren()) {
            n.getStyleClass().add("attacker-box-label");
        }
        Region spacer = new Region();
        box.getChildren().add(spacer);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label name = new Label(this.getPlayerName());
        name.getStyleClass().add("attacker-name");
        box.getChildren().add(name);
        return box;
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

    private String offSizeRounded() {
        return Math.round(this.offSize / 1000.0) + "k";
    }
}
