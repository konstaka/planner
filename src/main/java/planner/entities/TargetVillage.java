package planner.entities;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import planner.util.Converters;

public class TargetVillage extends Village {

    @Getter
    @Setter
    private boolean capital = false;

    @Getter
    @Setter
    private boolean offvillage = false;

    @Getter
    @Setter
    private boolean wwvillage = false;

    @Getter
    @Setter
    private String artefact = "";

    @Getter
    @Setter
    private List<String> arteEffects = new ArrayList<>();

    @Getter
    @Setter
    private long randomShiftSeconds = 0L;


    @Builder
    public TargetVillage(int coordId) {
        super(coordId);
    }
}
