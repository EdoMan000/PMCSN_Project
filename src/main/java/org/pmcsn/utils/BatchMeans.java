package org.pmcsn.utils;

import org.pmcsn.controller.BatchSimulationRunner;
import org.pmcsn.model.Statistics;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;

public class BatchMeans {

    public static void main(String[] args) throws Exception {
        int numBatch_k = 200;
        int batchSize_B = 330;
        int warmup = 6000;
        // 6000(warmup=discard) + 60000(restOfTheSimulation=keep) --> TOT RECCOMMENDED JOBS >= 66000 (K*B with 100<=K<=400)

        BatchSimulationRunner batchRunner = new BatchSimulationRunner(numBatch_k, batchSize_B, warmup);

        List<Statistics> statisticsList = batchRunner.runBatchSimulation(true);

        for (Statistics statistics : statisticsList) {
            computeAutocorrelation(statistics.meanResponseTimeList, statistics.centerName, "E[Ts]");
            computeAutocorrelation(statistics.meanQueueTimeList, statistics.centerName, "E[Tq]");
            computeAutocorrelation(statistics.meanServiceTimeList, statistics.centerName, "E[S]");
        }
    }

    static int K    = 50;               /* K is the maximum lag          */
    static int SIZE = K + 1;

    public static void computeAutocorrelation(List<Double> data, String centerName, String metric) {
        int i = 0;                   /* data point index              */
        int j;                       /* lag index                     */
        int p = 0;                   /* points to the head of 'hold'  */
        double x;                    /* current x[i] data point       */
        double sum = 0.0;            /* sums x[i]                     */
        long n;                      /* number of data points         */
        double mean;
        double hold[] = new double[SIZE]; /* K + 1 most recent data points */
        double cosum[] = new double[SIZE]; /* cosum[j] sums x[i] * x[i + j]   */

        for (j = 0; j < SIZE; j++)
            cosum[j] = 0.0;

        try {                         /* the first K + 1 data values    */
            while (i < SIZE && i < data.size()) {              /* initialize the hold array with */
                x = data.get(i);
                sum += x;
                hold[i] = x;
                i++;
            }

            while (i < data.size()) {
                for (j = 0; j < SIZE; j++)
                    cosum[j] += hold[p] * hold[(p + j) % SIZE];
                x = data.get(i);
                sum += x;
                hold[p] = x;
                p = (p + 1) % SIZE;
                i++;
            }
        } catch (NumberFormatException nfe) {
            System.out.println("Error: " + nfe);
        }

        n = i;
        while (i < n + SIZE) {        /* empty the circular array       */
            for (j = 0; j < SIZE; j++)
                cosum[j] += hold[p] * hold[(p + j) % SIZE];
            hold[p] = 0.0;
            p = (p + 1) % SIZE;
            i++;
        }

        mean = sum / n;
        for (j = 0; j <= K; j++)
            cosum[j] = (cosum[j] / (n - j)) - (mean * mean);

        System.out.println("------------------------------------------------------");
        System.out.println("            "+centerName+"     "+metric+"            ");
        System.out.println("------------------------------------------------------");

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("for " + n + " data points");
        System.out.println("the mean is ... " + f.format(mean));
        System.out.println("the stdev is .. " + f.format(Math.sqrt(cosum[0])) + "\n");
        System.out.println("  j (lag)   r[j] (autocorrelation)");
        for (j = 1; j < SIZE; j++)
            System.out.println("  " + j + "          " + g.format(cosum[j] / cosum[0]));

        System.out.println("------------------------------------------------------\n\n");
    }
}
