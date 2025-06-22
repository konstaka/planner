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

    private int POPULATION_SIZE;
    private int GENERATIONS;
    private double PROB_CROSSOVER;
    private double PROB_MUTATION;

    private double bestValue;
    private double baseValueRatio;

    private Map<Integer, List<Attack>> attacksPerPlayer = new HashMap<>();

    private List<Integer> targetList = new ArrayList<>();

    private Random random = new Random();


    /**
     * Initialises a genetic attack scheduler.
     * bestValue and baseValueRatio determine the fitness function
     * and can be used to weight long send intervals to taste.
     * The higher the bestValue, the slower the value of the interval
     * drops when it gets longer.
     * baseValueRatio of 0.0 means that long intervals are highly penalised,
     * baseValueRatio of 1.0 means that long intervals are as good as the
     * optimal ones.
     * @param operation operation to be scheduled
     * @param POPULATION_SIZE population size
     * @param GENERATIONS #generations
     * @param PROB_CROSSOVER crossover probability
     * @param PROB_MUTATION mutation probability
     * @param bestValue value for an optimal interval
     * @param baseValueRatio proportion of the best value
     */
    public GeneticScheduler(
            Operation operation,
            int POPULATION_SIZE,
            int GENERATIONS,
            double PROB_CROSSOVER,
            double PROB_MUTATION,
            double bestValue,
            double baseValueRatio
    ) {
        this.operation = operation;
        this.POPULATION_SIZE = POPULATION_SIZE;
        this.GENERATIONS = GENERATIONS;
        this.PROB_CROSSOVER = PROB_CROSSOVER;
        this.PROB_MUTATION = PROB_MUTATION;
        this.bestValue = bestValue;
        this.baseValueRatio = baseValueRatio;
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
            System.out.println(
                    "*** Generation " + i +
                            ",\tbest score: " + Math.round(fitnessValues[bestInThisIdx]) +
                            " (" + ((fitnessValues[bestInThisIdx] - currentBestFitness) >= 0 ? "+" : "") +
                            Math.round(fitnessValues[bestInThisIdx] - currentBestFitness) + ")" +
                            ",\taverage score: " + Math.round(totalFitness / POPULATION_SIZE)
            );
            // Compare the best from this generation to the best of all chromosomes
            if (currentBest.isEmpty() || fitnessValues[bestInThisIdx] > currentBestFitness) {
                currentBest = population[bestInThisIdx];
                currentBestFitness = fitnessValues[bestInThisIdx];
            }

            // If current best is zero, start with a new random population
            if (currentBestFitness < 0.001) {
                for (int j = 0; j < POPULATION_SIZE; j++) {
                    population[j] = randomChromosome();
                }
            }

            // Reproduce
            @SuppressWarnings("unchecked")
            Map<Integer, Long>[] newPop = new HashMap[POPULATION_SIZE];

            // If total fitness is absolutely zero, crossover is not used, only mutation.
            if (totalFitness == 0) {
                newPop = population;
            } else {
                // Compute fitness ratios
                for (int j = 0; j < POPULATION_SIZE; j++) {
                    fitnessValues[j] = fitnessValues[j] / totalFitness * 100;
                }
                // Initialise the distribution
                List<Pair<Map<Integer, Long>, Double>> itemsWeights = new ArrayList<>();
                // If the fitness is zero, the chromosome is replaced with a random one.
                for (int j = 0; j < POPULATION_SIZE; j++) {
                    itemsWeights.add(new Pair<>(population[j], fitnessValues[j]));
                }
                EnumeratedDistribution<Map<Integer, Long>> dist = new EnumeratedDistribution<>(itemsWeights);
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
                    // Mutation
                    if (random.nextDouble() < PROB_MUTATION) {
                        int randomIdx = random.nextInt(targetList.size());
                        population[j].put(targetList.get(randomIdx),
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
            chromosome.put(t_coordId,
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
    private double fitness(Map<Integer, Long> candidate) {

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
     * @param interval interval to be evaluated
     * @param waves waves to be set for the next send
     * @return value of this interval, between 0 and 1 (both inclusive)
     */
    public double value(long interval, int waves) {
        if (interval <= 0) return 0.0;
        long diff = interval - optimalInterval(waves);
        if (diff < -4L) return 0.0;
        if (diff > 84L) return baseValueRatio * bestValue + (1-baseValueRatio) * bestValue / Math.pow(diff-84, 2);
        return bestValue;
    }


    /**
     * Optimal interval for sends is defined by a base value
     * and the amount of waves the player needs to set for the next send.
     * 40s + 5s per wave is used as an optimal interval,
     * so 45s for a single attack, 60s for a 4-wave attack, and 1min20s for a 8-wave attack.
     * @param waves the amount of waves
     * @return the optimal sending interval
     */
    private static long optimalInterval(int waves) {
        return 40L + 5 * waves;
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
