package planner;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import planner.entities.Attack;
import planner.entities.AttackerVillage;
import planner.entities.Operation;
import planner.entities.TargetVillage;
import planner.util.GeneticScheduler;

public class GSDebugger {


    public static void main(String[] args) {

        Operation operation = Operation.load();
        if (operation == null) return;

        Map<Integer, TargetVillage> targetsMap = new HashMap<>();
        for (AttackerVillage attackerVillage : operation.getAttackers()) {
            for (Attack attack : attackerVillage.getPlannedAttacks()) {
                targetsMap.put(attack.getTarget().getCoordId(), attack.getTarget());
            }
        }



        GeneticScheduler geneticScheduler = new GeneticScheduler(
                operation,
                240.0,
                0.8
        );
        Map<Integer, Long> solution = null;

        try {
            for (int i = operation.getRandomShiftWindow() / 60; i < 10; i++) {
                operation.setRandomShiftWindow(i*60);
                System.out.println("Scheduling for a flex window of " + i + " minute(s)");
                solution = geneticScheduler.schedule();
                if (solution != null) break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (solution != null) {
            printSolution(
                    geneticScheduler,
                    solution,
                    targetsMap,
                    operation.getAttackers(),
                    operation.getDefaultLandingTime()
            );
        }
    }


    /**
     * Prints the solution in the format "coords landingtime"
     * and all send intervals and their values per player.
     * @param geneticScheduler scheduler
     * @param solution solution
     * @param targetsMap target id to target map
     * @param attackers attacker list
     * @param landingTime default landing time
     */
    public static void printSolution(GeneticScheduler geneticScheduler, Map<Integer, Long> solution,
                                      Map<Integer, TargetVillage> targetsMap, List<AttackerVillage> attackers,
                                      LocalDateTime landingTime) {
        System.out.println();
        System.out.println("SOLUTION:");
        for (Integer t_coordId : solution.keySet()) {
            System.out.println(
                    targetsMap.get(t_coordId).getXCoord() +
                            "|" +
                            targetsMap.get(t_coordId).getYCoord() +
                            " " +
                            landingTime.plusSeconds(solution.get(t_coordId)).format(App.TIME_ONLY)
            );
        }
        System.out.println();
        System.out.println(
                "Fitness: " + geneticScheduler.fitness(solution) +
                        ", smallest interval: " + geneticScheduler.smallestInterval(solution)
        );
        System.out.println();
        System.out.println("Sending intervals:");
        Map<Integer, List<Attack>> attacksPerPlayer = new HashMap<>();
        for (AttackerVillage attacker : attackers) {
            if (!attacksPerPlayer.containsKey(attacker.getPlayerId())) {
                attacksPerPlayer.put(attacker.getPlayerId(), new ArrayList<>());
            }
            List<Attack> villageAttacks = new ArrayList<>();
            for (Attack attack : attacker.getPlannedAttacks()) {
                attack.setLandingTime(landingTime.plusSeconds(solution.get(attack.getTarget().getCoordId())));
                villageAttacks.add(attack);
            }
            attacksPerPlayer.get(attacker.getPlayerId()).addAll(villageAttacks);
        }
        for (List<Attack> attacksForPlayer : attacksPerPlayer.values()) {
            if (!attacksForPlayer.isEmpty()) {
                System.out.println(attacksForPlayer.get(0).getAttacker().getPlayerName());
                attacksForPlayer.sort(Comparator.comparing(Attack::getSendingTime));
                for (int i = 0; i < attacksForPlayer.size()-1; i++) {
                    long interval = ChronoUnit.SECONDS.between(
                            attacksForPlayer.get(i).getSendingTime(),
                            attacksForPlayer.get(i+1).getSendingTime()
                    );
                    System.out.println(
                            interval +
                                    " " +
                                    geneticScheduler.value(interval, attacksForPlayer.get(i+1).getWaves())
                    );
                }
            }
        }
    }
}
