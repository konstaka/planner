package planner;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
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
    private int tribe;

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
        VBox box = new VBox();
        box.getStyleClass().add("attacker-box");
        box.getChildren().add(new Label(""+this.chiefs));
        box.getChildren().add(new Label(""+this.catas));
        box.getChildren().add(new Label(this.offSizeRounded()));
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

    private String offSizeRounded() {
        return Math.round(this.offSize / 1000.0) + "k";
    }
}
