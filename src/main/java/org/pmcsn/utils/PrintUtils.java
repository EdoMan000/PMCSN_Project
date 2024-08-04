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
        for (long l : luggageChecks.getNumberOfJobsPerCenter()) {
            jobServedEntrances += l;
            if(includePartialValues) {
                System.out.printf(BLUE + "Luggage Checks Center " + i + ": " + RESET + "%d", l);
            }
            i++;
        }
        System.out.printf(YELLOW + "TOT Luggage Checks Jobs Served: " + RESET + "%d\n", jobServedEntrances);

        // Check-In Desks
        long jobServedCheckIns = checkInDesksTarget.getJobsServed();
        if(includePartialValues) {
            System.out.printf(BLUE + "Check-In Desks Target: " + RESET + "%d", jobServedCheckIns);
        }
        i = 1;
        for (long l : checkInDesksOthers.getNumberOfJobsPerCenter()) {
            jobServedCheckIns += l;
            if (includePartialValues) {
                System.out.printf(BLUE + "Check-In Desks Others Center " + i + ": " + RESET + "%d", l);
            }
            i++;
        }
        System.out.printf(YELLOW + "TOT Check-In Desks Jobs Served: " + RESET + "%d\n", jobServedCheckIns);

        // Boarding Pass Scanners
        long boardingPassScannersJobsServed = boardingPassScanners.getJobsServed();
        System.out.printf(BLUE + "Boarding Pass Scanners: " + RESET + "%d\n", boardingPassScannersJobsServed);

        // Security Checks
        long securityChecksJobsServed = securityChecks.getJobsServed();
        System.out.printf(BLUE + "Security Checks: " + RESET + "%d\n", securityChecksJobsServed);

        // Passport Checks
        long passportChecksJobsServed = passportChecks.getJobsServed();
        System.out.printf(BLUE + "Passport Checks: " + RESET + "%d\n", passportChecksJobsServed);

        // Stamps Check
        long stampsCheckJobsServed = stampsCheck.getJobsServed();
        System.out.printf(BLUE + "Stamps Check: " + RESET + "%d\n", stampsCheckJobsServed);

        // Boarding
        long jobServedBoarding = boardingTarget.getJobsServed();
        if (includePartialValues) {
            System.out.printf(BLUE + "BoardingTarget: " + RESET + "%d", jobServedBoarding);
        }

        i = 1;
        for (long l : boardingOthers.getNumberOfJobsPerCenter()) {
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
        System.out.println("Verification results for " + analyticalResult.name.toUpperCase());
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
            System.out.println(BLUE + "\n\n********************************************");
            System.out.println("FINAL RESULTS FOR " + centerName);
            System.out.println("********************************************" + RESET);
            printVerificationResult(verificationResult);
            System.out.println(BLUE + "********************************************" + RESET);
            alreadyDoneCenterName = verificationResult.name.toUpperCase();
        }
    }

    private static void printVerificationResult(Verification.VerificationResult result) {
        String within = GREEN + "within";
        String outside = RED + "outside";

        System.out.println("E[Ts]: " + getColor(result.responseTimeDiff) + result.responseTimeDiff + RESET + " is " + (result.responseTimeWithinInterval ? within : outside) + " the interval ±" + result.responseTimeCI + RESET);
        System.out.println("E[Tq]: " + getColor(result.queueTimeDiff) + result.queueTimeDiff + RESET + " is " + (result.queueTimeWithinInterval ? within : outside) + " the interval ±" + result.queueTimeCI + RESET);
        System.out.println("E[s]: " + getColor(result.serviceTimeDiff) + result.serviceTimeDiff + RESET + " is " + (result.serviceTimeWithinInterval ? within : outside) + " the interval ±" + result.serviceTimeCI + RESET);
        System.out.println("E[Ns]: " + getColor(result.systemPopulationDiff) + result.systemPopulationDiff + RESET + " is " + (result.systemPopulationWithinInterval ? within : outside) + " the interval ±" + result.systemPopulationCI + RESET);
        System.out.println("E[Nq]: " + getColor(result.queuePopulationDiff) + result.queuePopulationDiff + RESET + " is " + (result.queuePopulationWithinInterval ? within : outside) + " the interval ±" + result.queuePopulationCI + RESET);
        System.out.println("ρ: " + getColor(result.utilizationDiff) + result.utilizationDiff + RESET + " is " + (result.utilizationWithinInterval ? within : outside) + " the interval ±" + result.utilizationCI + RESET);
        System.out.println("λ: " + getColor(result.lambdaDiff) + result.lambdaDiff + RESET + " is " + (result.lambdaWithinInterval ? within : outside) + " the interval ±" + result.lambdaCI + RESET);
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
