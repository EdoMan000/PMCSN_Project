package org.pmcsn.utils;

import org.pmcsn.libraries.Rngs;

public class Probabilities {

    public static boolean isCitizen(Rngs rngs, int streamIndex){
        return generateProbability(0.2, rngs, streamIndex);
    }

    public static boolean isTargetFlight(Rngs rngs, int streamIndex){
        return generateProbability(0.0159, rngs, streamIndex);
    }

    public static boolean isPriority(Rngs rngs, int streamIndex){
        return generateProbability(0.4, rngs, streamIndex);
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
