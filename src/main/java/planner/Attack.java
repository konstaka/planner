package planner;

import java.time.LocalDateTime;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class Attack {

    @Getter
    @Setter
    private TargetVillage target;

    @Getter
    @Setter
    private AttackerVillage attacker;

    @Getter
    @Setter
    private IntegerProperty waves;

    @Getter
    @Setter
    private BooleanProperty real;

    @Getter
    @Setter
    private int unitSpeed;

    @Getter
    @Setter
    private LocalDateTime landingTime;

    @Getter
    @Setter
    private int serverSpeed;

    @Getter
    @Setter
    private int serverSize;


    public LocalDateTime getSendingTime() {
        return landingTime.minusSeconds(this.travelSeconds());
    }

    /**
     * Calculates travel time on a T4 (2019) Travian map.
     * @return travel time in seconds
     */
    public long travelSeconds() {
        // Distance on a torus surface
        double distance = Math.sqrt(
                Math.pow(
                        Math.min(
                                Math.abs(this.attacker.getXCoord() - this.target.getXCoord()),
                                this.serverSize*2+1 - Math.abs(this.attacker.getXCoord() - this.target.getXCoord())),
                        2)
                + Math.pow(
                        Math.min(
                                Math.abs(this.attacker.getYCoord() - this.target.getYCoord()),
                                this.serverSize*2+1 - Math.abs(this.attacker.getYCoord() - this.target.getYCoord())),
                        2)
        );
        // Baseline speed
        double squaresPerSecond = unitSpeed * serverSpeed * this.attacker.getSpeed() / 60 / 60;
        // Return if no TS
        if (distance <= 20 || this.attacker.getTs().getValue() == 0) return Math.round(distance / squaresPerSecond);
        // No-TS part of travel
        double travelTime = 20L / squaresPerSecond;
        // Reduce distance
        distance -= 20;
        // Calculate TS factor
        double factor = 1.0 + this.attacker.getTs().getValue() * 0.2;
        // Adjust speed
        squaresPerSecond *= factor;
        // Compute remaining time
        travelTime += distance / squaresPerSecond;
        return Math.round(travelTime);
    }
}
