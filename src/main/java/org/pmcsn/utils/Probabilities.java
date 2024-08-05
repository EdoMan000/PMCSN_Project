package org.pmcsn.utils;

import org.pmcsn.libraries.Rngs;

public class Probabilities {
    private static double pCitizen;
    private static double pTarget;
    private static double pPriority;

    public static void init(double pCitizen, double pTarget, double pPriority) {
        Probabilities.pCitizen = pCitizen;
        Probabilities.pTarget = pTarget;
        Probabilities.pPriority = pPriority;
    }

    public static boolean isCitizen(Rngs rngs, int streamIndex){
        return generateProbability(pCitizen, rngs, streamIndex);
    }

    public static boolean isTargetFlight(Rngs rngs, int streamIndex) {
        return generateProbability(pTarget, rngs, streamIndex);
    }

    public static boolean isPriority(Rngs rngs, int streamIndex){
        return generateProbability(pPriority, rngs, streamIndex);
    }

    public static int getRandomValueUpToMax(Rngs rngs, int streamIndex, int maxValue) {
        double prob = 1.0 / maxValue;
        rngs.selectStream(streamIndex);
        double random = rngs.random();

        // Mappa il valore casuale a un numero intero tra 1 e maxValue
        for (int i = 1; i <= maxValue; i++) {
            if (random < i * prob) {
                return i;
            }
        }

        // Questo punto non dovrebbe mai essere raggiunto, ma è qui come fallback
        return maxValue;
    }

    private static boolean generateProbability(double beta, Rngs rngs, int streamIndex) {
        rngs.selectStream(streamIndex);
        return rngs.random() < beta;
    }
}
