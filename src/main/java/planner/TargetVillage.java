package planner;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

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

    public TargetVillage(int coordId) {
        super(coordId);
    }
}
