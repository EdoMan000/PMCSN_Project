package org.pmcsn;

import java.util.Scanner;
import org.pmcsn.controller.BasicModelSimulationRunner;
import org.pmcsn.controller.BatchModelSimulationRunner;
import org.pmcsn.controller.ImprovedModelSimulationRunner;

public class MenaraAirportSimulator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BasicModelSimulationRunner basicRunner = new BasicModelSimulationRunner();
        ImprovedModelSimulationRunner improvedRunner = new ImprovedModelSimulationRunner();
        BatchModelSimulationRunner batchRunner = new BatchModelSimulationRunner();

        while (true) {
            clearScreen();
            printAsciiArt();
            showMenu(scanner, basicRunner, improvedRunner, batchRunner);
        }
    }

    private static void printAsciiArt() {
        System.out.println("╔╦╗┌─┐┌┐┌┌─┐┬─┐┌─┐╔═╗┬┬─┐┌─┐┌─┐┬─┐┌┬┐  ╔═╗┬┌┬┐┬ ┬┬  ┌─┐┌┬┐┌─┐┬─┐");
        System.out.println("║║║├┤ │││├─┤├┬┘├─┤╠═╣│├┬┘├─┘│ │├┬┘ │   ╚═╗│││││ ││  ├─┤ │ │ │├┬┘");
        System.out.println("╩ ╩└─┘┘└┘┴ ┴┴└─┴ ┴╩ ╩┴┴└─┴  └─┘┴└─ ┴   ╚═╝┴┴ ┴└─┘┴─┘┴ ┴ ┴ └─┘┴└─");
    }

    private static void showMenu(Scanner scanner, BasicModelSimulationRunner basicRunner, ImprovedModelSimulationRunner improvedRunner, BatchModelSimulationRunner batchRunner) {
        System.out.println("\nWelcome to Menara Airport Simulator!");
        System.out.println("Please select an option:");
        System.out.println("1. Start Simulation");
        System.out.println("2. View Simulation Types");
        System.out.println("3. Exit");

        System.out.print("Enter your choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        switch (choice) {
            case 1:
                startSimulation(scanner, basicRunner, improvedRunner, batchRunner);
                break;
            case 2:
                viewSimulationTypes();
                pauseAndClear(scanner);
                break;
            case 3:
                System.out.println("Exiting Menara Airport Simulator. Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                pauseAndClear(scanner);
        }
    }

    private static void startSimulation(Scanner scanner, BasicModelSimulationRunner basicRunner, ImprovedModelSimulationRunner improvedRunner, BatchModelSimulationRunner batchRunner) {
        System.out.println("Starting Simulation...");
        System.out.println("Select simulation type:");
        viewSimulationTypes();

        System.out.print("Enter the simulation type number: ");
        int simulationType = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        switch (simulationType) {
            case 1:
                basicRunner.runBasicModelSimulation();
                break;
            case 2:
                improvedRunner.runImprovedModelSimulation();
                break;
            case 3:
                batchRunner.runBatchSimulation();
                break;
            default:
                System.out.println("Invalid simulation type. Returning to main menu.");
        }

        pauseAndClear(scanner);
    }

    private static void viewSimulationTypes() {
        System.out.println("Simulation Types:");
        System.out.println("1. Basic Model Simulation");
        System.out.println("2. Improved Model Simulation");
        System.out.println("3. Batch Simulation");
    }

    private static void pauseAndClear(Scanner scanner) {
        System.out.println("\nPress Enter to return to the menu...");
        scanner.nextLine();
        clearScreen();
    }

    private static void clearScreen() {
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
}
