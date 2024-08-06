package org.pmcsn.utils;

import org.pmcsn.conf.Config;
import org.pmcsn.controller.BatchSimulationRunner;

import java.io.*;
import java.util.List;

public class BatchMeans {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        final int MAX_BATCH_SIZE = 4096;
        int batchesNumber = config.getInt("general", "numBatches");
        int batchSize = config.getInt("general", "batchSize");
        int warmup = config.getInt("general", "warmup");
        while (batchSize < MAX_BATCH_SIZE) {
            System.out.println("Batch size: " + batchSize);
            BatchSimulationRunner batchRunner = new BatchSimulationRunner(batchesNumber, batchSize, warmup);
            var means = batchRunner.runBatchSimulation(true);
            var values = means.stream().map(x -> acf(x.meanSystemPopulationList)).toList();
            values.forEach(System.out::println);
            if (values.stream().allMatch(x -> x <= 0.2)) {
                values.forEach(System.out::println);
                break;
            }
            batchSize *= 2;
        }
        if (batchSize == MAX_BATCH_SIZE) {
            System.out.println("Batch size exceeded MAX_BATCH_SIZE.");
        }
    }

    public static double acf(List<Double> data) {
        int k = data.size();
        double mean = 0.0;

        // Calculate the mean of the batch means
        for (double value : data) {
            mean += value;
        }
        mean /= k;

        double numerator = 0.0;
        double denominator = 0.0;

        // Compute the numerator and denominator for the lag-1 autocorrelation
        for (int j = 0; j < k - 1; j++) {
            numerator += (data.get(j) - mean) * (data.get(j + 1) - mean);
        }
        for (int j = 0; j < k; j++) {
            denominator += Math.pow(data.get(j) - mean, 2);
        }
        return numerator / denominator;
    }

    public static double acs(List<Double> values) {
        return acs(values, 50);
    }

    public static double acs(List<Double> values, int lag) {
        // data point index
        int i = 0;
        // lag index
        int j;
        // points to the head of hold
        int p = 0;
        // current x[i] data point
        double x;
        // sums x[i]
        double sum = 0.0;
        // number of data points
        long n;
        double mean;
        int SIZE = lag + 1;
        // K + 1 most recent data points
        double[] hold = new double [SIZE];
        // cosum[j] sums x[i] * x[i+j]
        double[] cosum = new double [SIZE];

        for (j = 0; j < SIZE; j++)
            cosum[j] = 0.0;

        String line;
        StringBuilder s = new StringBuilder();
        values.forEach(v -> s.append(v).append("\n"));
        StringReader r = new StringReader(s.toString());
        BufferedReader ReadThis = new BufferedReader(r);
        try {                         /* the first K + 1 data values    */
            while (i < SIZE) {              /* initialize the hold array with */
                if ( (line = ReadThis.readLine()) != null) {
                    x        = Double.parseDouble(line);
                    sum     += x;
                    hold[i]  = x;
                    i++;
                }
            }
            while ((line = ReadThis.readLine()) != null) {
                for (j = 0; j < SIZE; j++)
                    cosum[j] += hold[p] * hold[(p + j) % SIZE];
                x       = Double.parseDouble(line);
                sum    += x;
                hold[p] = x;
                p       = (p + 1) % SIZE;
                i++;
            }
        } catch (NumberFormatException | IOException e) {
            System.out.println("Acs: " + e);
        }

        n = i;
        while (i < n + SIZE) {        /* empty the circular array       */
            for (j = 0; j < SIZE; j++)
                cosum[j] += hold[p] * hold[(p + j) % SIZE];
            hold[p] = 0.0;
            p       = (p + 1) % SIZE;
            i++;
        }

        mean = sum / n;
        for (j = 0; j <= lag; j++)
            cosum[j] = (cosum[j] / (n - j)) - (mean * mean);

//        DecimalFormat f = new DecimalFormat("###0.00");
//        DecimalFormat g = new DecimalFormat("###0.000");
//
//        System.out.println("for " + n + " data points");
//        System.out.println("the mean is ... " + f.format(mean));
//        System.out.println("the stdev is .. " + f.format(Math.sqrt(cosum[0])) +"\n");
//        System.out.println("  j (lag)   r[j] (autocorrelation)");
//        for (j = 1; j < SIZE; j++)
//            System.out.println("  " + j + "          " + g.format(cosum[j] / cosum[0]));
        return cosum[1] / cosum[0];
    }
}
