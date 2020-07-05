package planner.entities;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class TargetVillage extends Village {

    @Getter @Setter
    private boolean capital = false;

    @Getter @Setter
    private boolean offvillage = false;

    @Getter @Setter
    private boolean deffvillage = false;

    @Getter @Setter
    private boolean wwvillage = false;

    @Getter @Setter
    private String artefact = "";

    @Getter @Setter
    private List<String> arteEffects = new ArrayList<>();

    @Getter @Setter
    private long randomShiftSeconds = 0L;


    @Builder
    public TargetVillage(int coordId) {
        super(coordId);
    }
}
