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

    public static int getEntrance(Rngs rngs, int streamIndex) {
        double prob = 1.0 / 6.0;
        rngs.selectStream(streamIndex);
        double random = rngs.random();

        // Mappa il valore casuale a un numero intero tra 1 e 6
        for (int i = 1; i <= 6; i++) {
            if (random < i * prob) {
                return i;
            }
        }

        // Questo punto non dovrebbe mai essere raggiunto, ma è qui come fallback
        return 6;
    }

    public static int getCheckInDesks(Rngs rngs, int streamIndex) {
        double prob = 1.0 / 19.0;
        rngs.selectStream(streamIndex);
        double random = rngs.random();

        // Mappa il valore casuale a un numero intero tra 1 e 19
        for (int i = 1; i <= 19; i++) {
            if (random < i * prob) {
                return i;
            }
        }

        // Questo punto non dovrebbe mai essere raggiunto, ma è qui come fallback
        return 19;
    }

    private static boolean generateProbability(double beta, Rngs rngs, int streamIndex) {
        rngs.selectStream(streamIndex);
        return rngs.random() < beta;
    }
}
