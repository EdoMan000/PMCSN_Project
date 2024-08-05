package org.pmcsn.utils;

import org.pmcsn.conf.Config;
import org.pmcsn.libraries.Rvms;

import java.util.List;
import java.util.logging.Logger;

public class StatisticsUtils {
    private static final Logger logger = Logger.getLogger(StatisticsUtils.class.getName());

    public static double computeMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private static double getStandardDeviation(List<Double> values) {
        double mean = computeMean(values);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / values.size();
        return Math.sqrt(variance);
    }

    public static double computeConfidenceInterval(List<Double> values) {
        int K = values.size();
        // System.out.println("K :"+K);
        // Compute confidence intervals
        Rvms rvms = new Rvms();

        Config config = new Config();
        double alpha = config.getDouble("general", "alpha");
        // t* in the formula
        double criticalValue = rvms.idfStudent(K - 1, 1 - alpha / 2);

        return criticalValue * getStandardDeviation(values) / Math.sqrt(K - 1);
    }



/*
    public static double computeConfidenceInterval(List<Double> value)
    {
        long   n    = 0;
        double sum  = 0.0;
        double mean = 0.0;
        double data;
        double stdev;
        double u, t, w;
        double diff;

        String line = "";

        Rvms rvms = new Rvms();

        StringBuilder s = new StringBuilder();
        value.forEach(v -> s.append(v).append("\n"));
        BufferedReader br = new BufferedReader(new StringReader(s.toString()));
        try {
            line = br.readLine();
            while (line!=null) {
                StringTokenizer tokenizer = new StringTokenizer(line);
                if(tokenizer.hasMoreTokens()){
                    data = Double.parseDouble(tokenizer.nextToken());
                    n++;
                    diff  = data - mean;
                    sum  += diff * diff * (n - 1.0) / n;
                    mean += diff / n;
                }

                line = br.readLine();

            }
        } catch (IOException e){
            logger.log(Level.INFO, e.getMessage(), e);
            System.exit(1);
        }

        stdev  = Math.sqrt(sum / n);

        Config config = new Config();
        double LOC = config.getDouble("general", "levelOfConfidence");
        if (n > 1) {
            u = 1.0 - 0.5 * (1.0 - LOC);
            t = rvms.idfStudent(n - 1, u);
            w = t * stdev / Math.sqrt(n - 1);
            return w;
        } else {
            System.out.print("ERROR - insufficient data\n");
        }
        return 0.0;
    }

 */

}
