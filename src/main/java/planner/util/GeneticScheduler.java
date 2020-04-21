package planner.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class GeneticScheduler {

    private Operation operation;

    private static final int POPULATION_SIZE = 100000;
    private static final int GENERATIONS = 100;
    private static final double PROB_CROSSOVER = 0.7;
    private static final double PROB_MUTATION = 0.2;
    private static final double RATIO_SPATIAL = 0.2;

    private double bestValue;
    private double baseValueRatio;

    private Map<Integer, List<Attack>> attacksPerPlayer = new HashMap<>();

    private List<Integer> targetList = new ArrayList<>();

    private Random random = new Random();


    /**
     * Initialises a genetic attack scheduler.
     * Parameters determine the fitness function
     * and can be used to weight long send intervals to taste.
     * The higher the bestValue, the slower the value of the interval
     * drops when it gets longer.
     * baseValueRatio of 0.0 means that long intervals are highly penalised,
     * baseValueRatio of 1.0 means that long intervals are as good as the
     * optimal ones.
     * baseValueRatio of over 0.5 means that a long send interval is
     * considered better than a very small one. This is generally preferred.
     * @param operation operation to be scheduled
     * @param bestValue value for an optimal interval
     * @param baseValueRatio proportion of the best value
     */
    public GeneticScheduler(Operation operation,
                            double bestValue,
                            double baseValueRatio
    ) {
        this.operation = operation;
        this.bestValue = bestValue;
        this.baseValueRatio = baseValueRatio;
    }


    /**
     * Runs the algorithm on a given operation with the fixed parameters.
     * @return map from target coordId to landing time shift
     * or null if the attacks could not be scheduled.
     * @throws IllegalStateException if the attacks could not be read
     * (for instance, if there are no attacks to schedule).
     */
    public Map<Integer, Long> schedule() throws IllegalStateException {

        // Read attacks
        assembleAttackLists();
        // Stop if there are no attacks to schedule
        if (targetList.isEmpty()) throw new IllegalStateException("Could not read attacks");
        // Stop if the flex window is zero
        if (operation.getRandomShiftWindow() == 0) throw new IllegalStateException("No flex window set");
        System.out.println("--- Flex window: " + operation.getRandomShiftWindow());

        Map<Integer, Long> currentBest = randomChromosome();
        double currentBestFitness = 0.0;
        long smallestInterval = 0L;

        // Initial population
        @SuppressWarnings("unchecked")
        Map<Integer, Long>[] population = new HashMap[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = randomChromosome();
        }

        for (int i = 0; i < GENERATIONS; i++) {

            // Compute the fitness values of this generation
            double[] fitnessValues = new double[POPULATION_SIZE];
            double totalFitness = 0;
            for (int j = 0; j < POPULATION_SIZE; j++) {
                fitnessValues[j] = fitness(population[j]);
                totalFitness += fitnessValues[j];
            }

            // Find the best in generation
            int bestInThisIdx = findBest(fitnessValues);
            // Compare the best from this generation to the best of all chromosomes
            double difference = fitnessValues[bestInThisIdx] - currentBestFitness;
            if (smallestInterval(population[bestInThisIdx]) >= smallestInterval) {
                currentBest = copy(population[bestInThisIdx]);
                currentBestFitness = fitness(currentBest);
                smallestInterval = smallestInterval(currentBest);
            }
            // Update best values
            // Report scores
            System.out.println(
                    "*** Generation " + i +
                            ":\t" + fitnessValues[bestInThisIdx] + (difference > 0 ? "+" : "") +
                            ", average score: " + Math.round(totalFitness / POPULATION_SIZE) +
                            ",\tcurrent best: " + currentBestFitness +
                            ", smallest interval: " + smallestInterval
            );

            // Reproduce
            @SuppressWarnings("unchecked")
            Map<Integer, Long>[] newPop = new HashMap[POPULATION_SIZE];
            // If population has zero fitness, re-initialise
            if (totalFitness < 0.001) {
                for (int j = 0; j < POPULATION_SIZE; j++) {
                    newPop[j] = randomChromosome();
                }
            } else {
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
                // Spatial search: clone the best solution with slight variations
                for (int j = 0; j < (int) Math.round(POPULATION_SIZE * RATIO_SPATIAL); j++) {
                    newPop[j] = tweak(currentBest);
                }
                // Fill up new population by crossovers or old candidates
                for (int j = (int) Math.round(POPULATION_SIZE * RATIO_SPATIAL); j < POPULATION_SIZE; j++) {
                    if (random.nextDouble() < PROB_CROSSOVER) {
                        @SuppressWarnings("unchecked")
                        Map<Integer, Long>[] parents = new HashMap[2];
                        dist.sample(2, parents);
                        newPop[j] = crossover(parents[0], parents[1]);
                    } else {
                        newPop[j] = dist.sample();
                    }
                    // Mutation
                    if (random.nextDouble() < PROB_MUTATION) {
                        int randomIdx = random.nextInt(targetList.size());
                        newPop[j].put(
                                targetList.get(randomIdx),
                                (long) random.nextInt(operation.getRandomShiftWindow() * 2)
                                        - operation.getRandomShiftWindow()
                        );
                    }
                }
            }

            // Switch to the new generation
            population = newPop;
        }
        System.out.println("--- Best in all generations: " + currentBestFitness);
        if (currentBestFitness < 0.001) return null;
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
                attacksPerPlayer.get(attackerVillage.getPlayerId()).add(attack);
                targetIds.add(attack.getTarget().getCoordId());
            }
        }
        // Initialise target list for chromosome building
        targetList.clear();
        targetList.addAll(targetIds);
        System.out.println("--- Genetic scheduler started with chromosomes of length " + targetList.size());
    }


    /**
     * Constructs a random chromosome in the search space.
     * @return map from target coordId to landing time shift
     */
    private Map<Integer, Long> randomChromosome() {
        Map<Integer, Long> chromosome = new HashMap<>();
        for (Integer t_coordId : targetList) {
            chromosome.put(
                    t_coordId,
                    (long) random.nextInt(operation.getRandomShiftWindow() * 2)
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
    public double fitness(Map<Integer, Long> candidate) {

        List<SortableAttack[]> attackArrays = new ArrayList<>();

        // Insert candidate values
        for (List<Attack> attackList : attacksPerPlayer.values()) {
            SortableAttack[] attackArray = new SortableAttack[attackList.size()];
            for (int i = 0; i < attackList.size(); i++) {
                attackList.get(i).setLandingTime(
                        operation.getDefaultLandingTime()
                                .plusSeconds(candidate.get(attackList.get(i).getTarget().getCoordId()))
                );
                attackArray[i] = new SortableAttack(
                        attackList.get(i).getTarget().getCoordId(),
                        attackList.get(i).getSendingTime(),
                        attackList.get(i).getWaves()
                );
            }
            attackArrays.add(attackArray);
        }

        double candidateFitness = 0;

        // Sort by new sending times
        for (SortableAttack[] attackArray : attackArrays) {
            Arrays.sort(attackArray);
        }

        // Sum interval values
        // If any of the values is zero, the fitness of this chromosome is zero
        // (discourages solutions with impossible send windows)
        for (SortableAttack[] attackArray : attackArrays) {
            for (int i = 0; i < attackArray.length-1; i++) {
                LocalDateTime send1 = attackArray[i].sendingTime;
                LocalDateTime send2 = attackArray[i+1].sendingTime;
                long interval = ChronoUnit.SECONDS.between(send1, send2);
                double value = value(interval, attackArray[i+1].waves);
                if (value < 0.001) return 0.0;
                candidateFitness += value;
            }
        }
        return candidateFitness;
    }


    /**
     * Computes the value of the sending time interval based on deviation from the optimum.
     * TODO make all parameters changeable settings
     * @param interval interval to be evaluated
     * @param waves waves to be set for the next send
     * @return value of this interval, between 0 and 1 (both inclusive)
     */
    public double value(long interval, int waves) {
        // Cutoff
        if (interval < 30) return 0.0;
        long diff = interval - optimalInterval(waves);
        // Discounting
        if (diff < -4L) return Math.max(0, bestValue - Math.pow(diff+4, 2) / 4);
        if (diff > 54L) return baseValueRatio * bestValue + (1-baseValueRatio) * bestValue / (diff-54);
        // Flat peak; in a certain window around the optimal interval we do not care about the actual seconds
        return bestValue;
    }


    /**
     * Optimal interval for sends is defined by a base value
     * and the amount of waves the player needs to set for the next send.
     * 60s + 5s per wave is used as an optimal interval,
     * so 65s for a single attack, 80s for a 4-wave attack, and 1min30s for a 8-wave attack.
     * @param waves the amount of waves
     * @return the optimal sending interval
     */
    private static long optimalInterval(int waves) {
        return 60L + 5 * waves;
    }


    /**
     * Returns the smallest interval in a chromosome.
     * @param chromosome map from target coordId to landing time shift
     */
    public long smallestInterval(Map<Integer, Long> chromosome) {

        List<SortableAttack[]> attackArrays = new ArrayList<>();

        // Insert candidate values
        for (List<Attack> attackList : attacksPerPlayer.values()) {
            SortableAttack[] attackArray = new SortableAttack[attackList.size()];
            for (int i = 0; i < attackList.size(); i++) {
                attackList.get(i).setLandingTime(
                        operation.getDefaultLandingTime()
                                .plusSeconds(chromosome.get(attackList.get(i).getTarget().getCoordId()))
                );
                attackArray[i] = new SortableAttack(
                        attackList.get(i).getTarget().getCoordId(),
                        attackList.get(i).getSendingTime(),
                        attackList.get(i).getWaves()
                );
            }
            attackArrays.add(attackArray);
        }

        long smallest = Integer.MAX_VALUE;

        // Sort by new sending times
        for (SortableAttack[] attackArray : attackArrays) {
            Arrays.sort(attackArray);
        }

        // Find the smallest
        for (SortableAttack[] attackArray : attackArrays) {
            for (int i = 0; i < attackArray.length-1; i++) {
                LocalDateTime send1 = attackArray[i].sendingTime;
                LocalDateTime send2 = attackArray[i+1].sendingTime;
                long interval = ChronoUnit.SECONDS.between(send1, send2);
                if (interval < smallest) smallest = interval;
            }
        }
        return smallest;
    }


    /**
     * Simple argmax to find the fittest chromosome.
     */
    private int findBest(double[] fitnessValues) {
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
        int crosspoint1 = random.nextInt(targetList.size());
        int crosspoint2 = random.nextInt(targetList.size());
        for (int i = 0; i < crosspoint1; i++) {
            offspring.put(targetList.get(i), parent2.get(targetList.get(i)));
        }
        for (int i = crosspoint1; i < crosspoint2; i++) {
            offspring.put(targetList.get(i), parent1.get(targetList.get(i)));
        }
        for (int i = crosspoint2; i < targetList.size(); i++) {
            offspring.put(targetList.get(i), parent2.get(targetList.get(i)));
        }
        return offspring;
    }


    /**
     * Introduces small random tweaks to a chromosome to discover possible nearby improvements.
     * Focuses on moving badly conflicting targets.
     * @param original map from target coordId to landing time shift
     */
    private Map<Integer, Long> tweak(Map<Integer, Long> original) {
        Map<Integer, Long> tweaked = new HashMap<>();
        for (Integer t_coordId : targetList) {
            long randomShift = random.nextInt(operation.getRandomShiftWindow() / 10);
            long shift = 0;
            int die = random.nextInt(3);
            if (die == 1) {
                shift -= randomShift;
            }
            if (die == 2) {
                shift += randomShift;
            }
            long newTiming = original.get(t_coordId) + shift;
            if (Math.abs(newTiming) > operation.getRandomShiftWindow()) {
                newTiming = original.get(t_coordId);
            }
            tweaked.put(
                    t_coordId,
                    newTiming
            );
        }
        return tweaked;
    }


    /**
     * Makes a deep copy of a chromosome.
     * @param original map from target coordId to landing time shift
     */
    private Map<Integer, Long> copy(Map<Integer, Long> original) {
        Map<Integer, Long> copy = new HashMap<>();
        for (Integer t_coordId : targetList) {
            copy.put(t_coordId, original.get(t_coordId));
        }
        return copy;
    }

}
