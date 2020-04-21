package planner.util;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;

/**
 * A lightweight class for evaluating intervals in the GeneticScheduler.
 */
@AllArgsConstructor
public class SortableAttack implements Comparable<SortableAttack> {

    int t_coordId;
    LocalDateTime sendingTime;
    int waves;

    @Override
    public int compareTo(SortableAttack other) {
        return this.sendingTime.compareTo(other.sendingTime);
    }
}
