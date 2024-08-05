package org.pmcsn.utils;

import org.pmcsn.centers.*;
import org.pmcsn.conf.Config;
import org.pmcsn.model.MsqSum;

import java.util.List;
import java.util.Scanner;

public class PrintUtils {
    public static final String RESET = "\033[0m";
    public static final String YELLOW = "\033[0;33m";
    public static final String GREEN = "\033[0;32m";
    public static final String RED = "\033[0;31m";
    public static final String BLUE = "\033[0;34m";

    public static void printTitle() {
        System.out.println(BLUE + "╔╦╗┌─┐┌┐┌┌─┐┬─┐┌─┐╔═╗┬┬─┐┌─┐┌─┐┬─┐┌┬┐  ╔═╗┬┌┬┐┬ ┬┬  ┌─┐┌┬┐┌─┐┬─┐");
        System.out.println("║║║├┤ │││├─┤├┬┘├─┤╠═╣│├┬┘├─┘│ │├┬┘ │   ╚═╗│││││ ││  ├─┤ │ │ │├┬┘");
        System.out.println("╩ ╩└─┘┘└┘┴ ┴┴└─┴ ┴╩ ╩┴┴└─┴  └─┘┴└─ ┴   ╚═╝┴┴ ┴└─┘┴─┘┴ ┴ ┴ └─┘┴└─" + RESET);
    }

    public static void printJobsServedByNodes(LuggageChecks luggageChecks, CheckInDesksTarget checkInDesksTarget, CheckInDesksOthers checkInDesksOthers, BoardingPassScanners boardingPassScanners, SecurityChecks securityChecks, PassportChecks passportChecks, StampsCheck stampsCheck, BoardingTarget boardingTarget, BoardingOthers boardingOthers, boolean includePartialValues) {
        System.out.println(BLUE + "\n\n*************************************************");
        System.out.println("Jobs Served by Nodes");
        System.out.println("*************************************************" + RESET);

        // Luggage Checks
        long jobServedEntrances = 0;
        int i = 1;
        for (long l : luggageChecks.getTotalNumberOfJobsServed()) {
            jobServedEntrances += l;
            if(includePartialValues) {
                System.out.printf(BLUE + "Luggage Checks Center " + i + ": " + RESET + "%d", l);
            }
            i++;
        }
        System.out.printf(YELLOW + "TOT Luggage Checks Jobs Served: " + RESET + "%d\n", jobServedEntrances);

        // Check-In Desks
        long jobServedCheckIns = checkInDesksTarget.getTotalNumberOfJobsServed();
        if(includePartialValues) {
            System.out.printf(BLUE + "Check-In Desks Target: " + RESET + "%d", jobServedCheckIns);
        }
        i = 1;
        for (long l : checkInDesksOthers.getTotalNumberOfJobsServed()) {
            jobServedCheckIns += l;
            if (includePartialValues) {
                System.out.printf(BLUE + "Check-In Desks Others Center " + i + ": " + RESET + "%d", l);
            }
            i++;
        }
        System.out.printf(YELLOW + "TOT Check-In Desks Jobs Served: " + RESET + "%d\n", jobServedCheckIns);

        // Boarding Pass Scanners
        long boardingPassScannersJobsServed = boardingPassScanners.getTotalNumberOfJobsServed();
        System.out.printf(BLUE + "Boarding Pass Scanners: " + RESET + "%d\n", boardingPassScannersJobsServed);

        // Security Checks
        long securityChecksJobsServed = securityChecks.getTotalNumberOfJobsServed();
        System.out.printf(BLUE + "Security Checks: " + RESET + "%d\n", securityChecksJobsServed);

        // Passport Checks
        long passportChecksJobsServed = passportChecks.getTotalNumberOfJobsServed();
        System.out.printf(BLUE + "Passport Checks: " + RESET + "%d\n", passportChecksJobsServed);

        // Stamps Check
        long stampsCheckJobsServed = stampsCheck.getTotalNumberOfJobsServed();
        System.out.printf(BLUE + "Stamps Check: " + RESET + "%d\n", stampsCheckJobsServed);

        // Boarding
        long jobServedBoarding = boardingTarget.getTotalNumberOfJobsServed();
        if (includePartialValues) {
            System.out.printf(BLUE + "BoardingTarget: " + RESET + "%d", jobServedBoarding);
        }

        i = 1;
        for (long l : boardingOthers.getTotalNumberOfJobsServed()) {
            jobServedBoarding += l;
            if (includePartialValues) {
                System.out.printf(BLUE + "Boarding Others Center " + i + ": " + RESET + "%d", l);
            }
            i++;
        }

        System.out.printf(YELLOW + "TOT Boarding Jobs Served: " + RESET + "%d\n", jobServedBoarding);
        System.out.println(BLUE + "*************************************************" + RESET);
    }

    public static void printStats(String centerName, int numberOfServers, long numberOfJobsServed, double area, MsqSum[] sum, double firstArrivalTime, double lastArrivalTime, double lastCompletionTime) {
        System.out.println(YELLOW + "\n\n*************************************************");
        System.out.println("Saving stats for " + centerName.toUpperCase());
        System.out.println("*************************************************" + RESET);
        // Print the parameters
        System.out.println("Number of Servers: " + numberOfServers);
        System.out.println("Number of Jobs Served: " + numberOfJobsServed);
        System.out.println("Area: " + area);
        System.out.println("First Arrival Time: " + firstArrivalTime);
        System.out.println("Last Arrival Time: " + lastArrivalTime);
        System.out.println("Last Completion Time: " + lastCompletionTime);
        System.out.println("Sum:");
        for (MsqSum s : sum) {
            System.out.println(s);
        }
        System.out.println(YELLOW + "*************************************************" + RESET);
    }

    public static String formatList(List<Double> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static void printAnalyticalResult(AnalyticalComputation.AnalyticalResult analyticalResult) {
        System.out.println(YELLOW + "\n\n*************************************************");
        System.out.println("Analytical results for " + analyticalResult.name.toUpperCase());
        System.out.println("*************************************************" + RESET);
        // Print results
        System.out.println("Lambda: " + analyticalResult.lambda);
        System.out.println("Rho: " + analyticalResult.rho);
        System.out.println("E[Tq]: " + analyticalResult.Etq);
        System.out.println("E[Nq]: " + analyticalResult.Enq);
        System.out.println("E[Ts]: " + analyticalResult.Ets);
        System.out.println("E[Ns]: " + analyticalResult.Ens);
        System.out.println("E[s]: " + analyticalResult.Es);
        System.out.println(YELLOW + "*************************************************" + RESET);
    }

    public static void printFinalResults(List<Verification.VerificationResult> verificationResultList) {
        String alreadyDoneCenterName = "";
        int centerIndex = 1;
        for (Verification.VerificationResult verificationResult : verificationResultList) {
            String centerName = verificationResult.name.toUpperCase();
            if(alreadyDoneCenterName.equalsIgnoreCase(centerName)){
                centerIndex++;
                centerName = centerName+"_"+centerIndex;
            }else{
                centerIndex = 1;
                centerName = centerName+"_"+centerIndex;
            }
            System.out.println(BLUE + "\n\n*******************************************************************************************************");
            Config config = new Config();
            System.out.println("FINAL RESULTS FOR " + centerName +
                    " with " + (int) (100.0 * config.getDouble("general", "levelOfConfidence") + 0.5) +
                    "% confidence");
            System.out.println("*******************************************************************************************************" + RESET);
            printVerificationResult(verificationResult);
            System.out.println(BLUE + "*******************************************************************************************************" + RESET);
            alreadyDoneCenterName = verificationResult.name.toUpperCase();
        }
    }

    private static void printVerificationResult(Verification.VerificationResult result) {
        String within = GREEN + "within";
        String outside = RED + "outside";

        // Compute the colors and within/outside texts
        String responseTimeColor = getColor(result.comparisonResult.responseTimeDiff);
        String responseTimeWithinOutside = result.isWithinInterval(result.comparisonResult.responseTimeDiff, result.confidenceIntervals.getResponseTimeCI()) ? within : outside;

        String queueTimeColor = getColor(result.comparisonResult.queueTimeDiff);
        String queueTimeWithinOutside = result.isWithinInterval(result.comparisonResult.queueTimeDiff, result.confidenceIntervals.getQueueTimeCI()) ? within : outside;

        String serviceTimeColor = getColor(result.comparisonResult.serviceTimeDiff);
        String serviceTimeWithinOutside = result.isWithinInterval(result.comparisonResult.serviceTimeDiff, result.confidenceIntervals.getServiceTimeCI()) ? within : outside;

        String systemPopulationColor = getColor(result.comparisonResult.systemPopulationDiff);
        String systemPopulationWithinOutside = result.isWithinInterval(result.comparisonResult.systemPopulationDiff, result.confidenceIntervals.getSystemPopulationCI()) ? within : outside;

        String queuePopulationColor = getColor(result.comparisonResult.queuePopulationDiff);
        String queuePopulationWithinOutside = result.isWithinInterval(result.comparisonResult.queuePopulationDiff, result.confidenceIntervals.getQueuePopulationCI()) ? within : outside;

        String utilizationColor = getColor(result.comparisonResult.utilizationDiff);
        String utilizationWithinOutside = result.isWithinInterval(result.comparisonResult.utilizationDiff, result.confidenceIntervals.getUtilizationCI()) ? within : outside;

        String lambdaColor = getColor(result.comparisonResult.lambdaDiff);
        String lambdaWithinOutside = result.isWithinInterval(result.comparisonResult.lambdaDiff, result.confidenceIntervals.getLambdaCI()) ? within : outside;

        // Print the results
        System.out.println("E[Ts]: mean " + BLUE  + result.meanStatistics.meanResponseTime + RESET +  ", diff " + responseTimeColor + result.comparisonResult.responseTimeDiff + RESET + " is " + responseTimeWithinOutside + " the interval ±" + result.confidenceIntervals.getResponseTimeCI() + RESET);
        System.out.println("E[Tq]: mean " + BLUE  + result.meanStatistics.meanQueueTime + RESET + ", diff " + queueTimeColor + result.comparisonResult.queueTimeDiff + RESET + " is " + queueTimeWithinOutside + " the interval ±" + result.confidenceIntervals.getQueueTimeCI() + RESET);
        System.out.println("E[s]: mean " + BLUE  + result.meanStatistics.meanServiceTime + RESET + ", diff " + serviceTimeColor + result.comparisonResult.serviceTimeDiff + RESET + " is " + serviceTimeWithinOutside + " the interval ±" + result.confidenceIntervals.getServiceTimeCI() + RESET);
        System.out.println("E[Ns]: mean " + BLUE  + result.meanStatistics.meanSystemPopulation + RESET + ", diff " + systemPopulationColor + result.comparisonResult.systemPopulationDiff + RESET + " is " + systemPopulationWithinOutside + " the interval ±" + result.confidenceIntervals.getSystemPopulationCI() + RESET);
        System.out.println("E[Nq]: mean " + BLUE  + result.meanStatistics.meanQueuePopulation + RESET + ", diff " + queuePopulationColor + result.comparisonResult.queuePopulationDiff + RESET + " is " + queuePopulationWithinOutside + " the interval ±" + result.confidenceIntervals.getQueuePopulationCI() + RESET);
        System.out.println("ρ: mean " + BLUE  + result.meanStatistics.meanUtilization + RESET + ", diff " + utilizationColor + result.comparisonResult.utilizationDiff + RESET + " is " + utilizationWithinOutside + " the interval ±" + result.confidenceIntervals.getUtilizationCI() + RESET);
        System.out.println("λ: mean " + BLUE  + result.meanStatistics.lambda + RESET + ", diff " + lambdaColor + result.comparisonResult.lambdaDiff + RESET + " is " + lambdaWithinOutside + " the interval ±" + result.confidenceIntervals.getLambdaCI() + RESET);
    }

    private static String getColor(double value) {
        if (value < 0.5) {
            return GREEN;
        } else if (value < 1) {
            return YELLOW;
        } else {
            return RED;
        }
    }

    public static void printMainMenuOptions() {
        System.out.println("\nWelcome to Menara Airport Simulator!");
        System.out.println(BLUE + "Please select an option:" + RESET);
        System.out.println(BLUE + "1" + RESET + ". Start Simulation");
        System.out.println(BLUE + "2" + RESET + ". Exit");

        System.out.print(BLUE + "Enter your choice >>> " + RESET);
    }

    public static void printStartSimulationOptions() {
        System.out.println(BLUE + "\nSelect simulation Type:" + RESET);
        System.out.println(BLUE + "1" + RESET  + ". Basic Simulation");
        System.out.println(BLUE + "2" + RESET + ". Improved Model Simulation");
        System.out.println(BLUE + "3" + RESET + ". Batch Simulation");
        System.out.println(BLUE + "4" + RESET  + ". Basic Simulation with Exponential Distributions");
        System.out.println(BLUE + "5" + RESET + ". Batch Simulation with Exponential Distributions");

        System.out.print(BLUE + "Enter the simulation type number: " + RESET);
    }


    public static void resetMenu() {
        clearScreen();
        printTitle();
    }

    public static void pauseAndClear(Scanner scanner) {
        System.out.println(BLUE + "\nPress Enter to return to the menu..." + RESET);
        scanner.nextLine();
        clearScreen();
    }

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("Error clearing the console: " + e.getMessage());
        }
    }

    public static void printf(String string){
        Config config = new Config();
        if(config.getBoolean("general", "debugInfo")){
            System.out.println(string);
        }
    }

    public static void printError(String errorMessage){
        System.out.println(RED + errorMessage + RESET);
    }
    public static void printSuccess(String successMessage){
        System.out.println(BLUE + successMessage + RESET);
    }
    public static void printWarning(String warningMessage){
        System.out.println(YELLOW + warningMessage + RESET);
    }
}
