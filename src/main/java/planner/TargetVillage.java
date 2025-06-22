package planner;

import lombok.Getter;
import lombok.Setter;

public class TargetVillage extends Village {

    @Getter
    @Setter
    private boolean capital = false;

    @Getter
    @Setter
    private boolean offvillage = false;

    public TargetVillage(int coordId) {
        super(coordId);
    }
}
