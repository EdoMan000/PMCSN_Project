package org.pmcsn.utils;

import org.pmcsn.conf.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalyticalComputation {
    private static final Config config = new Config();

    public static class AnalyticalResult {
        public String name;
        public double lambda;
        public double rho;
        public double Etq;
        public double Enq;
        public double Ets;
        public double Ens;
        public double Es;

        public AnalyticalResult(double lambda, double rho, double Etq, double Enq, double Ets, double Ens, String name, double Es) {
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

    public static AnalyticalResult infiniteServer(String centerName, double lambda, double Es) {
        double rho = lambda * Es;
        double Etq, Enq, Ets, Ens;

        if (rho >= 1) {
            Etq = Double.POSITIVE_INFINITY;
            Enq = Double.POSITIVE_INFINITY;
            Ets = Double.POSITIVE_INFINITY;
            Ens = Double.POSITIVE_INFINITY;
        } else {
            Etq = 0;
            Enq = 0;
            Ets = Es;
            Ens = Ets * lambda;
        }

        AnalyticalResult analyticalResult = new AnalyticalResult(lambda, rho, Etq, Enq, Ets, Ens, centerName, Es);
        //printResult(analyticalResult);
        return analyticalResult;
    }

    public static AnalyticalResult singleServer(String centerName, double lambda, double Es) {
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

        AnalyticalResult analyticalResult = new AnalyticalResult(lambda, rho, Etq, Enq, Ets, Ens, centerName, Es);
        //printResult(analyticalResult);
        return analyticalResult;
    }

    public static AnalyticalResult multiServer(String centerName, double lambda, double Esi, int numServers) {
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

        AnalyticalResult analyticalResult = new AnalyticalResult(lambda, rho, Etq, Enq, Ets, Ens, centerName, Esi);
        //printResult(result);
        return analyticalResult;
    }

    public static List<AnalyticalResult> computeAnalyticalResults(String simulationType) {
        System.out.println("Computing analytical results for simulation...");
        List<AnalyticalResult> analyticalResults = new ArrayList<>();

        double numberOfPassengers = config.getDouble("general", "numberOfPassengers");
        double observationTime = config.getDouble("general", "observationTime");
        double numberOfFlights = config.getDouble("general", "numberOfFlights");

        double lambda =  (numberOfPassengers * numberOfFlights) / observationTime;

        analyticalResults.add(singleServer(
                config.getString("luggageChecks", "centerName"),
                lambda / config.getInt("luggageChecks", "numberOfCenters"),
                config.getDouble("luggageChecks", "meanServiceTime")));

        analyticalResults.add(multiServer(
                config.getString("checkInDesk", "centerName"),
                lambda  / config.getInt("checkInDesk", "numberOfCenters"),
                config.getDouble("checkInDesk", "meanServiceTime"),
                config.getInt("checkInDesk", "serversNumber")));

        analyticalResults.add(multiServer(
                config.getString("boardingPassScanners", "centerName"),
                lambda,
                config.getDouble("boardingPassScanners", "meanServiceTime"),
                config.getInt("boardingPassScanners", "serversNumber")));

        analyticalResults.add(multiServer(
                config.getString("securityChecks", "centerName"),
                lambda,
                config.getDouble("securityChecks", "meanServiceTime"),
                config.getInt("securityChecks", "serversNumber")));

        double pCitizen = config.getDouble("general", "pCitizen");
        analyticalResults.add(multiServer(
                config.getString("passportChecks", "centerName"),
                lambda * (1 - pCitizen),
                config.getDouble("passportChecks", "meanServiceTime"),
                config.getInt("passportChecks", "serversNumber")));

        analyticalResults.add(multiServer(
                config.getString("stampsCheck", "centerName"),
                lambda,
                config.getDouble("stampsCheck", "meanServiceTime"),
                config.getInt("stampsCheck", "serversNumber")));

        analyticalResults.add(multiServer(
                config.getString("boarding", "centerName"),
                lambda / config.getInt("boarding", "numberOfCenters"),
                config.getDouble("boarding", "meanServiceTime"),
                config.getInt("boarding", "serversNumber")));

        writeAnalyticalResults(simulationType, analyticalResults);

        return(analyticalResults);
    }

    public static void writeAnalyticalResults(String simulationType, List<AnalyticalResult> results){
        File file = new File("csvFiles/"+simulationType+"/analyticalResults/" );
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File("csvFiles/"+simulationType+"/analyticalResults/analyticalResults.csv");
        try(FileWriter fileWriter = new FileWriter(file)) {

            String DELIMITER = "\n";
            String COMMA = ",";


            fileWriter.append("Center, E[Ts], E[Tq], E[s], E[Ns], E[Nq], ρ, λ").append(DELIMITER);
            for (AnalyticalResult result : results){

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

    public static void main(String[] args) {
        FileUtils.deleteDirectory("csvFiles");
        String simulationType = "ANALYTICAL";
        writeAnalyticalResults(simulationType, computeAnalyticalResults(simulationType));
    }

}
