package org.pmcsn;

import org.pmcsn.controller.BasicSimulationRunner;
import org.pmcsn.controller.BatchSimulationRunner;
import org.pmcsn.controller.ImprovedModelSimulationRunner;

import java.util.Scanner;

public class MenaraAirportSimulator {

    // ANSI escape codes for colors
    public static final String RESET = "\033[0m"; // Reset color
    public static final String RED = "\033[0;31m";
    public static final String BLUE = "\033[0;34m";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        BasicSimulationRunner basicRunner = new BasicSimulationRunner();
        ImprovedModelSimulationRunner improvedRunner = new ImprovedModelSimulationRunner();
        BatchSimulationRunner batchRunner = new BatchSimulationRunner();

        while (true) {
            mainMenu(scanner, basicRunner, improvedRunner, batchRunner);
        }
    }

    private static void printTitle() {
        System.out.println(BLUE + "╔╦╗┌─┐┌┐┌┌─┐┬─┐┌─┐╔═╗┬┬─┐┌─┐┌─┐┬─┐┌┬┐  ╔═╗┬┌┬┐┬ ┬┬  ┌─┐┌┬┐┌─┐┬─┐");
        System.out.println("║║║├┤ │││├─┤├┬┘├─┤╠═╣│├┬┘├─┘│ │├┬┘ │   ╚═╗│││││ ││  ├─┤ │ │ │├┬┘");
        System.out.println("╩ ╩└─┘┘└┘┴ ┴┴└─┴ ┴╩ ╩┴┴└─┴  └─┘┴└─ ┴   ╚═╝┴┴ ┴└─┘┴─┘┴ ┴ ┴ └─┘┴└─" + RESET);
    }

    private static void mainMenu(Scanner scanner, BasicSimulationRunner basicRunner, ImprovedModelSimulationRunner improvedRunner, BatchSimulationRunner batchRunner) throws Exception {
        resetMenu();
        System.out.println("\nWelcome to Menara Airport Simulator!");
        System.out.println(BLUE + "Please select an option:" + RESET);
        System.out.println(BLUE + "1" + RESET + ". Start Simulation");
        System.out.println(BLUE + "2" + RESET + ". Exit");

        System.out.print(BLUE + "Enter your choice >>> " + RESET);
        int choice = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        switch (choice) {
            case 1:
                startSimulation(scanner, basicRunner, improvedRunner, batchRunner);
                break;
            case 2:
                System.out.println(RED + "Exiting Menara Airport Simulator. Goodbye!" + RESET);
                System.exit(0);
                break;
            default:
                System.out.println(RED + "Invalid choice '" + choice + "'. Please try again." + RESET);
                pauseAndClear(scanner);
        }
    }

    private static void startSimulation(Scanner scanner, BasicSimulationRunner basicRunner, ImprovedModelSimulationRunner improvedRunner, BatchSimulationRunner batchRunner) throws Exception {
        resetMenu();
        System.out.println(BLUE + "\nSelect simulation Type:" + RESET);
        System.out.println(BLUE + "1" + RESET  + ". Basic Simulation");
        System.out.println(BLUE + "2" + RESET + ". Improved Model Simulation");
        System.out.println(BLUE + "3" + RESET + ". Batch Simulation");

        System.out.print(BLUE + "Enter the simulation type number: " + RESET);
        int simulationType = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        switch (simulationType) {
            case 1:
                basicRunner.runBasicSimulation();
                break;
            case 2:
                improvedRunner.runImprovedModelSimulation();
                break;
            case 3:
                batchRunner.runBatchSimulation();
                break;
            default:
                System.out.println(RED + "Invalid simulation type '" + simulationType + "'. Returning to main menu." + RESET);
        }
        pauseAndClear(scanner);
    }

    private static void resetMenu() {
        clearScreen();
        printTitle();
    }

    private static void pauseAndClear(Scanner scanner) {
        System.out.println(BLUE + "\nPress Enter to return to the menu..." + RESET);
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
