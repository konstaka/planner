package planner;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Attack {

    private TargetVillage target;
    private AttackerVillage attacker;
    private String type;
    private int unitSpeed;
    private LocalDateTime landingTime;

    public LocalDateTime getSendTime() {
        return landingTime.minusSeconds(travelSeconds(target, attacker, unitSpeed));
    }

    /**
     * Calculates travel time on a travian map.
     * @param a village a
     * @param b village b
     * @param unitSpeed 3 for cata etc.
     * @return travel time in seconds
     */
    private static long travelSeconds(Village a, Village b, int unitSpeed) {
        // TODO
        return 0;
    }
}
