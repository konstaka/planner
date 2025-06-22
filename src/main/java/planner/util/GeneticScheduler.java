package planner.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.distribution.EnumeratedDistribution;

import planner.entities.Attack;
import planner.entities.AttackerVillage;
import planner.entities.Operation;

/**
 * Employs a genetic algorithm to optimise the landing times such that
 * 1) sending times do not conflict but
 * 2) they are nicely clustered.
 * This is achieved by selecting the fitness function so that it gives
 * the best score for optimal attack intervals.
 */
public class GeneticScheduler {

    private static final int POPULATION_SIZE = 200;
    private static final int GENERATIONS = 15;
    private static final double PROB_CROSSOVER = 0.6;
    private static final double PROB_MUTATION = 0.04;

    private Operation operation;

    private Map<Integer, List<Attack>> attacksPerPlayer;

    private List<Integer> targetList;

    private Random random;


    /**
     * Instantiates a scheduler for a given operation.
     * @param operation the operation to be scheduled
     */
    public GeneticScheduler(Operation operation) {
        this.operation = operation;
        attacksPerPlayer = new HashMap<>();
        random = new Random();
    }


    /**
     * Runs the algorithm on a given operation with the fixed parameters.
     * @return map from target coordId to landing time shift.
     * @throws IllegalStateException if the attacks could not be read
     * (for instance, if there are no attacks to schedule).
     */
    public Map<Integer, Long> schedule() throws IllegalStateException {

        @SuppressWarnings("unchecked")
        Map<Integer, Long>[] population = new HashMap[POPULATION_SIZE];
        Map<Integer, Long> currentBest = new HashMap<>();
        double currentBestFitness = 0.0;

        // Read attacks
        assembleAttackLists();
        // Stop if there are no attacks to schedule
        if (targetList.isEmpty()) throw new IllegalStateException("Could not read attacks");
        // Stop if the flex window is zero
        if (operation.getRandomShiftWindow() == 0) throw new IllegalStateException("No flex window set");

        // Initial population
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = randomChromosome();
        }

        for (int i = 0; i < GENERATIONS; i++) {

            System.out.println("Compute fitnesses...");
            // Compute the fitness values of this generation
            double[] fitnessValues = new double[POPULATION_SIZE];
            double totalFitness = 0;
            for (int j = 0; j < POPULATION_SIZE; j++) {
                fitnessValues[j] = fitness(population[j]);
                totalFitness += fitnessValues[j];
            }

            // Find the best in generation
            int bestInThisIdx = findBest(fitnessValues);
            // Report scores
            System.out.println("*** Generation " + i +
                    ",\tbest score: " + fitnessValues[bestInThisIdx] +
                    " (" + ((fitnessValues[bestInThisIdx] - currentBestFitness) >= 0 ? "+" : "") +
                    Math.round((fitnessValues[bestInThisIdx] - currentBestFitness)*1000)/1000.0 + ")" +
                    ",\taverage score: " + (totalFitness / POPULATION_SIZE));
            // Compare the best from this generation to the best of all chromosomes
            if (currentBest.isEmpty() || fitnessValues[bestInThisIdx] > currentBestFitness) {
                currentBest = population[bestInThisIdx];
                currentBestFitness = fitnessValues[bestInThisIdx];
            }



            // Reproduce
            @SuppressWarnings("unchecked")
            Map<Integer, Long>[] newPop = new HashMap[POPULATION_SIZE];

            // If total fitness is absolutely zero, crossover is not used, only mutation.
            if (totalFitness == 0) {
                newPop = population;
            } else {
                System.out.println("Compute fitness ratios...");
                // Compute fitness ratios
                for (int j = 0; j < POPULATION_SIZE; j++) {
                    fitnessValues[j] = fitnessValues[j] / totalFitness * 100;
                }
                // Initialise the distribution
                List<Pair<Map<Integer, Long>, Double>> itemsWeights = new ArrayList<>();
                for (int j = 0; j < POPULATION_SIZE; j++) {
                    itemsWeights.add(new Pair<>(population[j], fitnessValues[j]));
                }
                EnumeratedDistribution<Map<Integer, Long>> dist = new EnumeratedDistribution<>(itemsWeights);
                System.out.println("Pick parents...");
                // Fill up new population by crossovers or old candidates
                for (int j = 0; j < POPULATION_SIZE; j++) {
                    if (random.nextDouble() < PROB_CROSSOVER) {
                        @SuppressWarnings("unchecked")
                        Map<Integer, Long>[] parents = new HashMap[2];
                        dist.sample(2, parents);
                        newPop[j] = crossover(parents[0], parents[1]);
                    } else {
                        newPop[j] = dist.sample();
                    }
                }
            }

            System.out.println("Mutate...");
            // Mutate
            if (random.nextDouble() < PROB_MUTATION) {
                int randomIdx = random.nextInt(POPULATION_SIZE);
                Map<Integer, Long> chromosome = population[randomIdx];
                randomIdx = random.nextInt(targetList.size());
                chromosome.put(targetList.get(randomIdx),
                        (long) random.nextInt(operation.getRandomShiftWindow() * 2)
                                - operation.getRandomShiftWindow()
                );
            }

            // Switch to the new generation
            population = newPop;
        }
        System.out.println("*** Best in all generations: " + currentBestFitness);
        return currentBest;
    }


    /**
     * Clones planned attacks from the operation to player-wise attack lists for evaluation.
     */
    private void assembleAttackLists() {

        Set<Integer> targetIds = new HashSet<>();
        // Assemble attack lists player-wise
        for (AttackerVillage attackerVillage : operation.getAttackers()) {
            if (!attacksPerPlayer.containsKey(attackerVillage.getPlayerId())) {
                attacksPerPlayer.put(attackerVillage.getPlayerId(), new ArrayList<>());
            }
            for (Attack attack : attackerVillage.getPlannedAttacks()) {
                try {
                    attacksPerPlayer.get(attackerVillage.getPlayerId()).add(
                            (Attack) attack.clone()
                    );
                    targetIds.add(attack.getTarget().getCoordId());
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        // Initialise target list for chromosome building
        targetList = new ArrayList<>(targetIds);
        System.out.println("--- Genetic scheduler started with chromosomes of length " + targetList.size());
    }


    /**
     * Constructs a random chromosome in the search space.
     * @return map from target coordId to landing time shift
     */
    private Map<Integer, Long> randomChromosome() {
        Map<Integer, Long> chromosome = new HashMap<>();
        for (Integer t_coordId : targetList) {
            chromosome.put(t_coordId,
                    (long) random.nextInt(operation.getRandomShiftWindow() * 60 * 2)
                            - operation.getRandomShiftWindow()
            );
        }
        return chromosome;
    }


    /**
     * Computes the fitness for a candidate solution based
     * on how much it deviates from the optimal schedule.
     * @param candidate map from target coordId to landing time shift
     * @return fitness value
     */
    private double fitness(Map<Integer, Long> candidate) {
        // Insert candidate values
        for (List<Attack> attackList : attacksPerPlayer.values()) {
            for (Attack attack : attackList) {
                attack.setLandingTime(
                        operation.getDefaultLandingTime()
                                .plusSeconds(candidate.get(attack.getTarget().getCoordId()))
                );
            }
        }
        double candidateFitness = 0;
        // Sort by new sending times and sum interval values
        // TODO make sorting more efficient, this eats up all memory.
        for (List<Attack> attacksForPlayer : attacksPerPlayer.values()) {
            System.out.println("Sorting list of size: " + attacksForPlayer.size());
            attacksForPlayer.sort(Comparator.comparing(Attack::getSendingTime));
            System.out.println("Maybe.");
            for (int i = 0; i < attacksForPlayer.size()-1; i++) {
                LocalDateTime send1 = attacksForPlayer.get(i).getSendingTime();
                LocalDateTime send2 = attacksForPlayer.get(i+1).getSendingTime();
                long interval = ChronoUnit.SECONDS.between(send1, send2);
                candidateFitness += value(interval, attacksForPlayer.get(i+1).getWaves());
            }
        }
        return candidateFitness;
    }


    /**
     * Computes the value of the sending time interval based on deviation from the optimum.
     * The drop in value should be sharper on the shorter side; inverse quadratic is used here.
     * On the longer side, inverse drop in value is used.
     * Around the optimal interval threshold, the max value is returned for 30 consecutive seconds.
     * If the interval is not positive, zero is returned (two sends on the exactly same second).
     * @param interval interval to be evaluated
     * @param waves waves to be set for the next send
     * @return value of this interval, between 0 and 1 (both inclusive)
     */
    private static double value(long interval, int waves) {
        if (interval <= 0) return 0.0;
        long diff = interval - optimalInterval(waves);
        if (diff < -4L) return 1.0 / Math.pow(diff+4, 2);
        if (diff > 24L) return 1.0 / (diff-24);
        return 1.0;
    }


    /**
     * 40s + 5s per wave is used as an optimal interval,
     * so 45s for a single attack, 60s for a 4-wave attack, and 1min20s for a 8-wave attack.
     * @param waves the amount of waves
     * @return the optimal sending interval
     */
    private static long optimalInterval(int waves) {
        return 40L + 5*waves;
    }


    /**
     * Simple argmax to find the fittest chromosome.
     */
    private static int findBest(double[] fitnessValues) {
        int bestIdx = 0;
        for (int i = 1; i < POPULATION_SIZE; i++) {
            if (fitnessValues[i] > fitnessValues[bestIdx]) {
                bestIdx = i;
            }
        }
        return bestIdx;
    }


    /**
     * Performs a crossover with the given parents.
     * @param parent1 first parent
     * @param parent2 second parent
     * @return the offspring
     */
    private Map<Integer, Long> crossover(Map<Integer, Long> parent1, Map<Integer, Long> parent2) {
        Map<Integer, Long> offspring = new HashMap<>();
        int crosspoint = random.nextInt(targetList.size());
        for (int i = 0; i < crosspoint; i++) {
            offspring.put(targetList.get(i), parent1.get(targetList.get(i)));
        }
        for (int i = crosspoint; i < targetList.size(); i++) {
            offspring.put(targetList.get(i), parent2.get(targetList.get(i)));
        }
        return offspring;
    }
}
