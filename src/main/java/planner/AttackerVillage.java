package planner;

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
}
