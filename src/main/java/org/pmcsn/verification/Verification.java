package org.pmcsn.verification;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Verification {

    private static class Result {
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

    public static Result singleServer(String centerName, double lambda, double Es) {
        System.out.println("*****************" + centerName.toUpperCase() + "****************");
        double rho = lambda * Es;
        double Etq, Enq, Ets, Ens;

        if (rho >= 1) {
            Etq = Double.POSITIVE_INFINITY;
            Enq = Double.POSITIVE_INFINITY;
            Ets = Double.POSITIVE_INFINITY;
            Ens = Double.POSITIVE_INFINITY;
        } else {
            Etq = rho * Es / (1 - rho);
            Enq = Etq * lambda;
            Ets = Etq + Es;
            Ens = Ets * lambda;
        }

        return new Result(lambda, rho, Etq, Enq, Ets, Ens, centerName, Es);
    }

    public static Result multiServer(String centerName, double lambda, double Esi, int numServers) {
        System.out.println("*****************" + centerName.toUpperCase() + "****************");
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
            Etq = Pq * Es / (1 - rho);
            Enq = Etq * lambda;
            Ets = Etq + Esi;
            Ens = Ets * lambda;
        }

        return new Result(lambda, rho, Etq, Enq, Ets, Ens, centerName, Es);
    }

    public static void main(String[] args) {
        List<Result> results = new ArrayList<>();
        double lambda = 6300/(24*60); // mean of 63 flights with 100 passengers each during the whole day
        results.add(singleServer("LUGGAGE CHECK 1", lambda / 6, 1.4));
        results.add(singleServer("LUGGAGE CHECK 2", lambda / 6, 1.4));
        results.add(singleServer("LUGGAGE CHECK 3", lambda / 6, 1.4));

        double pTarget = 0.0159;
        double lambda_checkin_others = lambda * (1 - pTarget);
        results.add(multiServer("CHECK-IN TARGET", lambda * pTarget, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 1", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 2", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 3", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 4", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 5", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 6", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 7", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 8", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 9", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 10", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 11", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 12", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 13", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 14", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 15", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 16", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 17", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 18", lambda_checkin_others / 19, 10, 3));
        results.add(multiServer("CHECK-IN OTHERS 19", lambda_checkin_others / 19, 10, 3));

        results.add(multiServer("SCAN BOARDING PASS", lambda, 0.30, 3));

        results.add(multiServer("SECURITY CHECKS", lambda, 0.9, 4));

        double pCitizen = 0.2;
        results.add(multiServer("PASSPORT CHECKS", lambda * (1 - pCitizen), 5, 24));

        results.add(singleServer("STAMP CHECK", lambda * (1 - pCitizen), 0.10));

        results.add(multiServer("BOARDING", (lambda * (1 - pCitizen) * pTarget) + (lambda * pCitizen * pTarget), 2, 2));

        writeResultsVerification(results);
    }

    public static void writeResultsVerification(List<Result> results){
        File file = new File("csvFiles/verification/" );
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File("csvFiles/verification/results.csv");
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
