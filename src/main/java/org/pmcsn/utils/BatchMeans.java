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

        BatchSimulationRunner batchRunner = new BatchSimulationRunner(32, 64);

        List<Statistics> statisticsList = batchRunner.runBatchSimulation(true);

        for (Statistics statistics : statisticsList) {

            computeAutocorrelation(statistics.meanResponseTimeList, 32, 33);
            computeAutocorrelation(statistics.meanQueueTimeList, 32, 33);
            computeAutocorrelation(statistics.meanServiceTimeList, 32, 33);

        }

    }


    private static double computeAutocorrelation(List<Double> values, int K, int B) {
        int    i = 0;                   /* data point index              */
        int    j;                       /* lag index                     */
        int    p = 0;                   /* points to the head of 'hold'  */
        double x;                       /* current x[i] data point       */
        double sum = 0.0;               /* sums x[i]                     */
        long   n;                       /* number of data points         */
        double mean;
        double hold[]  = new double [B]; /* K + 1 most recent data points */
        double cosum[] = new double [B]; /* cosum[j] sums x[i] * x[i+j]   */

        for (j = 0; j < B; j++)
            cosum[j] = 0.0;

        String line;
        InputStreamReader r = new InputStreamReader(System.in);
        BufferedReader ReadThis = new BufferedReader(r);
        try {                         /* the first K + 1 data values    */
            while (i < B) {              /* initialize the hold array with */
                if ( (line = ReadThis.readLine()) != null) {
                    x        = Double.parseDouble(line);
                    sum     += x;
                    hold[i]  = x;
                    i++;
                }
            }

            while ( (line = ReadThis.readLine()) != null ) {
                for (j = 0; j < B; j++)
                    cosum[j] += hold[p] * hold[(p + j) % B];
                x       = Double.parseDouble(line);
                sum    += x;
                hold[p] = x;
                p       = (p + 1) % B;
                i++;
            }
        } catch (EOFException e) {
            System.out.println("Acs: " + e);
        } catch (NumberFormatException nfe) {
//      System.out.println("Acs: " + nfe);
        }

        n = i;
        while (i < n + B) {        /* empty the circular array       */
            for (j = 0; j < B; j++)
                cosum[j] += hold[p] * hold[(p + j) % B];
            hold[p] = 0.0;
            p       = (p + 1) % B;
            i++;
        }

        mean = sum / n;
        for (j = 0; j <= K; j++)
            cosum[j] = (cosum[j] / (n - j)) - (mean * mean);

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("for " + n + " data points");
        System.out.println("the mean is ... " + f.format(mean));
        System.out.println("the stdev is .. " + f.format(Math.sqrt(cosum[0])) +"\n");
        System.out.println("  j (lag)   r[j] (autocorrelation)");
        for (j = 1; j < B; j++)
            System.out.println("  " + j + "          " + g.format(cosum[j] / cosum[0]));
    }
    }

}
