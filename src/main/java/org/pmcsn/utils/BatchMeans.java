package org.pmcsn.utils;

import org.pmcsn.conf.Config;
import org.pmcsn.controller.BatchSimulationRunner;
import org.pmcsn.model.BatchStatistics;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BatchMeans {

    public static List<Double> convertDatFileToList(String filePath) {
        List<Double> doubleList = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNext()) {
                if (scanner.hasNextDouble()) {
                    doubleList.add(scanner.nextDouble());
                } else {
                    scanner.next(); // Skip non-double tokens
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        }
        return doubleList;
    }

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        int batchesNumber = config.getInt("general", "numBatches");
        int batchSize = config.getInt("general", "batchSize");
        int warmup = config.getInt("general", "warmup");
        while (true) {
            System.out.println("Batch size: " + batchSize);
            BatchSimulationRunner batchRunner = new BatchSimulationRunner(batchesNumber, batchSize, warmup);
            var means = batchRunner.runBatchSimulation(true);
            System.out.println("Means: " + means.size());
            var results = new ArrayList<AnalyticalComputation.AnalyticalResult>();
            if (!checkEntrance(means, 0.2)) {
                return;
            }
            batchSize += batchSize / 2;

            if (batchSize > 4096) {
                System.out.println("BATCH SIZE EXCEEDED... Exiting.");
                break;
            }
        }
        System.out.println("\n------------------------------------------------------");
        System.out.println(" FINAL NUMBER OF BATCHES: " + batchSize);
        System.out.println("------------------------------------------------------");
    }

    private static boolean check(List<List<Double>> meanList, double v, int k) {
        boolean result = true;
        for (int i = 0; i < meanList.size(); i++) {
            List<Double> means = meanList.get(i);
            assert means.size() == k;
            double acf = acf(means);
            double acs = acs(means);
            result = result && Math.abs(acf) <= v;
            System.out.println("\n------------------------------------------------------");
            System.out.printf("luggage_check_%d (E[Ts])\t:\t%f\t%f%n", i+1, acf, acs);
            System.out.println("------------------------------------------------------");
        }
        return result;
    }

    private static boolean checkEntrance(List<BatchStatistics> statisticsList, double v) {
        Config config = new Config();
        String centerName = config.getString("luggageChecks", "centerName");
        List<BatchStatistics> entrancesStats = statisticsList.stream().filter(x -> x.getCenterName().contains(centerName)).toList();
        boolean result = true;
        for (BatchStatistics entrance : entrancesStats) {
            double acf = acf(entrance.meanResponseTimeList);
            // double acs = acs(entrance.meanResponseTimeList);
            result = result && Math.abs(acf) <= v;
            System.out.println("\n------------------------------------------------------");
            System.out.printf("%s (E[Ts])\t: %f%n", entrance.getCenterName(), acf);
            System.out.println("------------------------------------------------------");
            var s = new StringBuilder()
                    .append("[");
            entrance.meanQueueTimeList.forEach(x -> s.append(x).append(", "));
            s.append("]");
            System.out.println(s);
            acf = acf(entrance.meanQueueTimeList);
            System.out.println("\n------------------------------------------------------");
            System.out.printf("%s (E[Tq])\t: %f%n", entrance.getCenterName(), acf);
            System.out.println("------------------------------------------------------");
        }
        return result;
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
