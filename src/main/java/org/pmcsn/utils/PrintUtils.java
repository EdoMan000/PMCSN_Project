package org.pmcsn.utils;

import org.pmcsn.centers.*;
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
        // controllo di consistenza sul numero di jobs processati
        long jobServedEntrances = 0;
        int i = 1;
        for (long l : luggageChecks.getNumberOfJobsPerCenter()) {
            jobServedEntrances += l;
            if(includePartialValues) {
                System.out.println("Check-In Desks Others Center " + i + ": Jobs Served = " + l);
            }
            i++;
        }
        System.out.println("TOT Luggage Checks Jobs Served = " + jobServedEntrances);

        long jobServedCheckIns = checkInDesksTarget.getJobsServed();
        if(includePartialValues) {
            System.out.println("Check-In Desks Target: Jobs Served = " + jobServedCheckIns);
        }
        i = 1;
        for (long l : checkInDesksOthers.getNumberOfJobsPerCenter()) {
            jobServedCheckIns += l;
            if (includePartialValues) {
                System.out.println("Check-In Desks Others Center " + i + ": Jobs Served = " + l);
            }
            i++;
        }
        System.out.println("TOT Check-In Desks Jobs Served = " + jobServedCheckIns);

        long boardingPassScannersJobsServed = boardingPassScanners.getJobsServed();
        System.out.println("Boarding Pass Scanners: Jobs Served = " + boardingPassScannersJobsServed);

        long securityChecksJobsServed = securityChecks.getJobsServed();
        System.out.println("Security Checks: Jobs Served = " + securityChecksJobsServed);

        long passportChecksJobsServed = passportChecks.getJobsServed();
        System.out.println("Passport Checks: Jobs Served = " + passportChecksJobsServed);

        long stampsCheckJobsServed = stampsCheck.getJobsServed();
        System.out.println("Stamps Check: Jobs Served = " + stampsCheckJobsServed);

        long jobServedBoarding = boardingTarget.getJobsServed();
        if (includePartialValues) {
            System.out.println("BoardingTarget: Jobs Served = " + jobServedBoarding);
        }

        i = 1;
        for (long l : boardingOthers.getNumberOfJobsPerCenter()) {
            jobServedBoarding += l;
            if (includePartialValues) {
                System.out.println("Boarding Others Center " + i + ": Jobs Served = " + l);
            }
            i++;
        }

        System.out.println("TOT Boarding Jobs Served = " + jobServedBoarding);
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

    public static void printResult(Verification.Result result) {
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

    public static void printComparisonResult(Comparison.ComparisonResult comparisonResult) {
        System.out.println(BLUE + "\n\n********************************************");
        System.out.println("Comparison results for " + comparisonResult.name.toUpperCase());
        System.out.println("********************************************" + RESET);

        // Print results with color based on the value
        System.out.println("E[Ts]_Diff: " + getColor(comparisonResult.responseTimeDiff) + comparisonResult.responseTimeDiff + RESET);
        System.out.println("E[Tq]_Diff: " + getColor(comparisonResult.queueTimeDiff) + comparisonResult.queueTimeDiff + RESET);
        System.out.println("E[s]_Diff: " + getColor(comparisonResult.serviceTimeDiff) + comparisonResult.serviceTimeDiff + RESET);
        System.out.println("E[Ns]_Diff: " + getColor(comparisonResult.systemPopulationDiff) + comparisonResult.systemPopulationDiff + RESET);
        System.out.println("E[Nq]_Diff: " + getColor(comparisonResult.queuePopulationDiff) + comparisonResult.queuePopulationDiff + RESET);
        System.out.println("rho_Diff: " + getColor(comparisonResult.utilizationDiff) + comparisonResult.utilizationDiff + RESET);
        System.out.println("lambda_Diff: " + getColor(comparisonResult.lambdaDiff) + comparisonResult.lambdaDiff + RESET);

        System.out.println(BLUE + "********************************************" + RESET);
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

    public static void printError(String errorMessage){
        System.out.println(RED + errorMessage + RESET);
    }
}
