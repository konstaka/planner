package planner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.junit.Before;
import org.junit.Test;
import planner.entities.Attack;
import planner.entities.AttackerVillage;
import planner.entities.Operation;
import planner.entities.TargetVillage;
import planner.util.GeneticScheduler;

public class GeneticSchedulerTest {

    private static final Operation operation = mock(Operation.class);

    // Participants
    private static final AttackerVillage spank_me_14 = mock(AttackerVillage.class);
    private static final AttackerVillage spank_me_03 = mock(AttackerVillage.class);
    private static final AttackerVillage spank_me_23 = mock(AttackerVillage.class);
    private static final AttackerVillage donald_trump_01 = mock(AttackerVillage.class);
    private static final List<AttackerVillage> attackers = Arrays.asList(
            spank_me_14, spank_me_03, spank_me_23, donald_trump_01
    );

    // Targets
    private transient TargetVillage iluvatar_cap = mock(TargetVillage.class);
    private transient TargetVillage gothmog_11 = mock(TargetVillage.class);
    private transient TargetVillage sauron_cap = mock(TargetVillage.class);
    private transient TargetVillage treebeard_cap = mock(TargetVillage.class);
    private transient TargetVillage gothmog_09 = mock(TargetVillage.class);
    private transient TargetVillage arPharazon_cap = mock(TargetVillage.class);
    private transient TargetVillage gothmog_05 = mock(TargetVillage.class);
    private transient TargetVillage gandalf_cap = mock(TargetVillage.class);
    private transient List<TargetVillage> targets = Arrays.asList(
            iluvatar_cap, gothmog_11, sauron_cap, treebeard_cap,
            gothmog_09, arPharazon_cap, gothmog_05, gandalf_cap
    );
    private transient Map<Integer, TargetVillage> targetsMap = new HashMap<>();

    // Attack params
    private static final int waves = 4;
    private static final boolean real = false;
    private static final boolean conq = false;
    private static final int unitSpeed = 3;
    private static final LocalDateTime landingTime = LocalDateTime.of(
            LocalDate.now().plusDays(2),
            LocalTime.of(6, 30)
    );
    private static final int landingTimeShift = 0;
    private static final int serverSpeed = 1;
    private static final int serverSize = 200;
    private static final boolean conflicting = false;
    private static final boolean withHero = false;
    private static final BooleanProperty updated = new SimpleBooleanProperty(false);

    private transient GeneticScheduler geneticScheduler;


    @Before
    public void setUp() {
        for (AttackerVillage attacker : attackers) {
            when(attacker.getUnitSpeed()).thenReturn(new SimpleIntegerProperty(3));
            when(attacker.getTs()).thenReturn(20);
            when(attacker.getHeroBoots()).thenReturn(0);
        }
        for (TargetVillage target : targets) {
            targetsMap.put(target.getCoordId(), target);
            for (AttackerVillage attacker : attackers) {
                attacker.getPlannedAttacks().add(
                        new Attack(
                                target,
                                attacker,
                                waves,
                                real,
                                conq,
                                unitSpeed,
                                landingTime,
                                landingTimeShift,
                                serverSpeed,
                                serverSize,
                                conflicting,
                                withHero,
                                updated
                        )
                );
            }
        }

        when(operation.getAttackers()).thenReturn(attackers);
        when(operation.getTargets()).thenReturn(targets);
        when(operation.getDefaultLandingTime()).thenReturn(landingTime);
        geneticScheduler = new GeneticScheduler(operation);
    }


    @Test
    public void tenMinuteWindow() {
        when(operation.getRandomShiftWindow()).thenReturn(5);

        this.printSolution(geneticScheduler.schedule());
    }


    /**
     * Prints the solution in the format "coords landingtime"
     * Example: -15|8 6:29:21
     * @param solution output of the scheduler
     */
    private void printSolution(Map<Integer, Long> solution) {
        System.out.println("*** SOLUTION:");
        for (Integer t_coordId : solution.keySet()) {
            System.out.println(
                    targetsMap.get(t_coordId).getCoords() +
                            " " +
                            landingTime.plusSeconds(solution.get(t_coordId)).format(App.TIME_ONLY)
            );
        }
    }
}
