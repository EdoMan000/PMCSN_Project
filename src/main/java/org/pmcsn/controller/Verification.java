package org.pmcsn.controller;

import org.pmcsn.libraries.Rvms;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Verification {

    public static class Result {
        public String name;
        public double lambda;
        public double rho;
        public double Etq;
        public double Enq;
        public double Ets;
        public double Ens;
        public double Es;

        public Result(double lambda, double rho, double Etq, double Enq, double Ets, double Ens, String name, double Es) {
            this.lambda = lambda;
            this.rho = rho;
            this.Etq = Etq;
            this.Enq = Enq;
            this.Ets = Ets;
            this.Ens = Ens;
            this.name = name;
            this.Es = Es;
        }
    }

    public static double factorial(int n) {
        double fact = 1;
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }

    // Method to calculate p(0)
    public static double calculateP0(int m, double rho) {
        double sum = 0.0;
        for (int i = 0; i < m; i++) {
            sum += Math.pow(m * rho, i) / factorial(i);
        }
        sum += (Math.pow(m * rho, m) / (factorial(m) * (1 - rho)));
        return 1 / sum;
    }

    // Method to calculate Pq
    public static double calculatePq(int m, double rho, double p0) {
        double numerator = Math.pow(m * rho, m);
        double denominator = factorial(m) * (1 - rho);
        return (numerator / denominator) * p0;
    }

    public static final String RESET = "\033[0m";
    public static final String YELLOW = "\033[0;33m";

    public static void printResult(Result result) {
        System.out.println(YELLOW + "\n\n*************************************************");
        System.out.println("Verification results for " + result.name.toUpperCase());
        System.out.println("*************************************************" + RESET);
        // Print results
        System.out.println("Lambda: " + result.lambda);
        System.out.println("Rho: " + result.rho);
        System.out.println("E[Tq]: " + result.Etq);
        System.out.println("E[Nq]: " + result.Enq);
        System.out.println("E[Ts]: " + result.Ets);
        System.out.println("E[Ns]: " + result.Ens);
        System.out.println("E[s]: " + result.Es);
        System.out.println(YELLOW + "*************************************************" + RESET);
    }

    public static Result singleServer(String centerName, double lambda, double Es) {
        double rho = lambda * Es;
        double Etq, Enq, Ets, Ens;

        if (rho >= 1) {
            Etq = Double.POSITIVE_INFINITY;
            Enq = Double.POSITIVE_INFINITY;
            Ets = Double.POSITIVE_INFINITY;
            Ens = Double.POSITIVE_INFINITY;
        } else {
            Etq = (rho * Es) / (1 - rho);
            Enq = Etq * lambda;
            Ets = Etq + Es;
            Ens = Ets * lambda;
        }

        Result result = new Result(lambda, rho, Etq, Enq, Ets, Ens, centerName, Es);
        //printResult(result);
        return result;
    }

    public static Result multiServer(String centerName, double lambda, double Esi, int numServers) {
        double Es = Esi / numServers;
        double rho = lambda * Es;
        double Etq, Enq, Ets, Ens;

        if (rho >= 1) {
            Etq = Double.POSITIVE_INFINITY;
            Enq = Double.POSITIVE_INFINITY;
            Ets = Double.POSITIVE_INFINITY;
            Ens = Double.POSITIVE_INFINITY;
        } else {
            double p0 = calculateP0(numServers, rho);
            double Pq = calculatePq(numServers, rho, p0);
            Etq = (Pq * Es) / (1 - rho);
            Enq = Etq * lambda;
            Ets = Etq + Esi;
            Ens = Ets * lambda;
        }

        Result result = new Result(lambda, rho, Etq, Enq, Ets, Ens, centerName, Es);
        //printResult(result);
        return result;
    }

    public static List<Result> modelVerification(String simulationType) {
        List<Result> results = new ArrayList<>();
        double lambda = 6300/(24*60); // mean of 63 flights with 100 passengers each during the whole day


        results.add(singleServer("LUGGAGE CHECK", lambda / 6, 1.4));

        double pTarget = 0.0159;
        double lambda_checkin_others = lambda * (1 - pTarget);
        results.add(multiServer("CHECK_IN_TARGET", lambda * pTarget, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS", lambda_checkin_others / 19, 10, 3));

        results.add(multiServer("SCAN_BOARDING_PASS", lambda, 0.30, 3));

        results.add(multiServer("SECURITY_CHECKS", lambda, 1.8, 8));

        double pCitizen = 0.2;
        results.add(multiServer("PASSPORT_CHECK", lambda * (1 - pCitizen), 5, 24));

        results.add(singleServer("STAMP_CHECK", lambda * (1 - pCitizen), 0.10));

        results.add(multiServer("BOARDING", lambda * pTarget, 2, 2));

        writeResultsVerification(simulationType, results);

        return(results);
    }

    public static void computeAndPrintConfidenceInterval(double alpha, double[] data) {
        long n = 0;                     /* counts data points */
        double sum = 0.0;
        double mean = 0.0;
        double stdev;
        double u, t, w;
        double diff;

        Rvms rvms = new Rvms();

        for (double value : data) {         /* use Welford's one-pass method */
            n++;                 /* and standard deviation */
            diff = value - mean;
            sum += diff * diff * (n - 1.0) / n;
            mean += diff / n;
        }

        stdev = Math.sqrt(sum / n);

        DecimalFormat df = new DecimalFormat("###0.00");

        if (n > 1) {
            u = 1.0 - 0.5 * (1.0 - alpha);              /* interval parameter  */
            t = rvms.idfStudent(n - 1, u);            /* critical value of t */
            w = t * stdev / Math.sqrt(n - 1);         /* interval half width */

            System.out.print("\nbased upon " + n + " data points");
            System.out.print(" and with " + (int) (100.0 * alpha + 0.5) +
                    "% confidence\n");
            System.out.print("the expected value is in the interval ");
            System.out.print(df.format(mean) + " +/- " + df.format(w) + "\n");
        } else {
            System.out.print("ERROR - insufficient data\n");
        }
    }

    public static void writeResultsVerification(String simulationType, List<Result> results){
        File file = new File("csvFiles/"+simulationType+"/verification/" );
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File("csvFiles/"+simulationType+"/verification/verification.csv");
        try(FileWriter fileWriter = new FileWriter(file)) {

            String DELIMITER = "\n";
            String COMMA = ",";


            fileWriter.append("Center, E[Ts], E[Tq], E[s], E[Ns], E[Nq], ρ, λ").append(DELIMITER);
            for (Result result : results){

                fileWriter.append(result.name).append(COMMA)
                        .append(String.valueOf(result.Ets)).append(COMMA)
                        .append(String.valueOf(result.Etq)).append(COMMA)
                        .append(String.valueOf(result.Es)).append(COMMA)
                        .append(String.valueOf(result.Ens)).append(COMMA)
                        .append(String.valueOf(result.Enq)).append(COMMA)
                        .append(String.valueOf(result.rho)).append(COMMA)
                        .append(String.valueOf(result.lambda)).append(DELIMITER);
            }

            fileWriter.flush();
        } catch (IOException e) {
            //ignore
        }
    }

}
