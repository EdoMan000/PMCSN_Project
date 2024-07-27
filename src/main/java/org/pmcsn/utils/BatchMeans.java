package org.pmcsn.utils;

import org.pmcsn.controller.BatchSimulationRunner;
import org.pmcsn.model.Statistics;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.List;

public class BatchMeans {

    public static void main(String[] args) throws Exception {

        int numBatch_k = 32;

        BatchSimulationRunner batchRunner = new BatchSimulationRunner(numBatch_k, 64);

        List<Statistics> statisticsList = batchRunner.runBatchSimulation(true);

        for (Statistics statistics : statisticsList) {

            computeAutocorrelation(statistics.meanResponseTimeListBatch, numBatch_k, statistics.centerName, "E[Ts]");
            computeAutocorrelation(statistics.meanQueueTimeListBatch, numBatch_k, statistics.centerName, "E[Tq]");
            computeAutocorrelation(statistics.meanServiceTimeListBatch, numBatch_k, statistics.centerName, "E[s]");

        }

    }


    private static void computeAutocorrelation(List<Double> values, int K, String centerName, String metrics) {
        int    i = 0;                   /* data point index              */
        int    j;                       /* lag index                     */
        int    p = 0;                   /* points to the head of 'hold'  */
        double sum = 0.0;               /* sums x[i]                     */
        long   n;                       /* number of data points         */
        double mean;
        int SIZE = K+1;
        double hold[]  = new double [SIZE]; /* K + 1 most recent data points */
        double cosum[] = new double [SIZE]; /* cosum[j] sums x[i] * x[i+j]   */

        for (j = 0; j < SIZE; j++)
            cosum[j] = 0.0;


        while(i < SIZE-1) {
            for (double x : values) {   /* the first K + 1 data values initialize the hold array with */
                sum += x;
                hold[i] = x;
                i++;
            }
        }

        /* x is the current x[i] data point */
        for (double x : values) {
            for (j = 0; j < SIZE; j++)
                cosum[j] += hold[p] * hold[(p + j) % SIZE];
            sum += x;
            hold[p] = x;
            p = (p + 1) % SIZE;
            i++;
        }


        n = i;
        while (i < n + SIZE) {        /* empty the circular array */
            for (j = 0; j < SIZE; j++)
                cosum[j] += hold[p] * hold[(p + j) % SIZE];
            hold[p] = 0.0;
            p       = (p + 1) % SIZE;
            i++;
        }

        mean = sum / n;
        for (j = 0; j <= K; j++)
            cosum[j] = (cosum[j] / (n - j)) - (mean * mean);

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("for " + n + " data points");
        System.out.println("for " + n + " data points");
        System.out.println("the mean is ... " + f.format(mean));
        System.out.println("the stdev is .. " + f.format(Math.sqrt(cosum[0])) +"\n");
        System.out.println("  j (lag)   r[j] (autocorrelation)");
        for (j = 1; j < SIZE; j++)
            System.out.println("  " + j + "          " + g.format(cosum[j] / cosum[0]));
    }

}
