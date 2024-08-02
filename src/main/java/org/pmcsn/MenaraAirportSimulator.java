package org.pmcsn;

import org.pmcsn.conf.Config;
import org.pmcsn.controller.BasicSimulationRunner;
import org.pmcsn.controller.BatchSimulationRunner;
import org.pmcsn.controller.ImprovedModelSimulationRunner;

import java.util.Scanner;

import static org.pmcsn.utils.PrintUtils.*;

public class MenaraAirportSimulator {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        BasicSimulationRunner basicRunner = new BasicSimulationRunner();
        ImprovedModelSimulationRunner improvedRunner = new ImprovedModelSimulationRunner();
        BatchSimulationRunner batchRunner = new BatchSimulationRunner();

        while (true) {
            mainMenu(scanner, basicRunner, improvedRunner, batchRunner);
        }
    }

    private static void mainMenu(Scanner scanner, BasicSimulationRunner basicRunner, ImprovedModelSimulationRunner improvedRunner, BatchSimulationRunner batchRunner) throws Exception {
        resetMenu();
        printMainMenuOptions();
        int choice = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        switch (choice) {
            case 1:
                startSimulation(scanner, basicRunner, improvedRunner, batchRunner);
                break;
            case 2:
                printError("Exiting Menara Airport Simulator. Goodbye!");
                System.exit(0);
                break;
            default:
                printError("Invalid choice '" + choice + "'. Please try again.");
                pauseAndClear(scanner);
        }
    }

    private static void startSimulation(Scanner scanner, BasicSimulationRunner basicRunner, ImprovedModelSimulationRunner improvedRunner, BatchSimulationRunner batchRunner) throws Exception {
        resetMenu();
        printStartSimulationOptions();
        int simulationType = scanner.nextInt();
        scanner.nextLine();  // Consume newline
        Config config = new Config();
        boolean shouldTrackObservations = config.getBoolean("general", "shouldTrackObservations");
        switch (simulationType) {
            case 1:
                basicRunner.runBasicSimulation(false, shouldTrackObservations);
                break;
            case 2:
                improvedRunner.runImprovedModelSimulation();
                break;
            case 3:
                batchRunner.runBatchSimulation(false);
                break;
            case 4:
                basicRunner.runBasicSimulation(true, shouldTrackObservations);
                break;
            case 5:
                batchRunner.runBatchSimulation(true);
                break;
            default:
                printError("Invalid simulation type '" + simulationType + "'.");
        }
        pauseAndClear(scanner);
    }
}
