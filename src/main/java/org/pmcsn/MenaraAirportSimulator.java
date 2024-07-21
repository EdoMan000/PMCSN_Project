package org.pmcsn;

import org.pmcsn.controller.BasicModelSimulationRunner;
import org.pmcsn.controller.BatchModelSimulationRunner;
import org.pmcsn.controller.ImprovedModelSimulationRunner;

import java.util.Scanner;

public class MenaraAirportSimulator {

    // ANSI escape codes for colors
    public static final String RESET = "\033[0m"; // Reset color
    public static final String BLACK = "\033[0;30m";
    public static final String RED = "\033[0;31m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String BLUE = "\033[0;34m";
    public static final String PURPLE = "\033[0;35m";
    public static final String CYAN = "\033[0;36m";
    public static final String WHITE = "\033[0;37m";
    public static final String BRIGHT_BLACK = "\033[0;90m";
    public static final String BRIGHT_RED = "\033[0;91m";
    public static final String BRIGHT_GREEN = "\033[0;92m";
    public static final String BRIGHT_YELLOW = "\033[0;93m";
    public static final String BRIGHT_BLUE = "\033[0;94m";
    public static final String BRIGHT_PURPLE = "\033[0;95m";
    public static final String BRIGHT_CYAN = "\033[0;96m";
    public static final String BRIGHT_WHITE = "\033[0;97m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BasicModelSimulationRunner basicRunner = new BasicModelSimulationRunner();
        ImprovedModelSimulationRunner improvedRunner = new ImprovedModelSimulationRunner();
        BatchModelSimulationRunner batchRunner = new BatchModelSimulationRunner();

        while (true) {
            clearScreen();
            showMenu(scanner, basicRunner, improvedRunner, batchRunner);
        }
    }

    private static void printAsciiArt() {
        System.out.println(YELLOW + "╔╦╗┌─┐┌┐┌┌─┐┬─┐┌─┐╔═╗┬┬─┐┌─┐┌─┐┬─┐┌┬┐  ╔═╗┬┌┬┐┬ ┬┬  ┌─┐┌┬┐┌─┐┬─┐");
        System.out.println("║║║├┤ │││├─┤├┬┘├─┤╠═╣│├┬┘├─┘│ │├┬┘ │   ╚═╗│││││ ││  ├─┤ │ │ │├┬┘");
        System.out.println("╩ ╩└─┘┘└┘┴ ┴┴└─┴ ┴╩ ╩┴┴└─┴  └─┘┴└─ ┴   ╚═╝┴┴ ┴└─┘┴─┘┴ ┴ ┴ └─┘┴└─" + RESET);
    }

    private static void showMenu(Scanner scanner, BasicModelSimulationRunner basicRunner, ImprovedModelSimulationRunner improvedRunner, BatchModelSimulationRunner batchRunner) {
        System.out.println("\nWelcome to Menara Airport Simulator!");
        System.out.println(YELLOW + "Please select an option:" + RESET);
        System.out.println(YELLOW + "1" + RESET + ". Start Simulation");
        System.out.println(YELLOW + "2" + RESET + ". View Simulation Types");
        System.out.println(YELLOW + "3" + RESET + ". Exit");

        System.out.print(YELLOW + "Enter your choice >>> " + RESET);
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
                System.out.println(RED + "Exiting Menara Airport Simulator. Goodbye!" + RESET);
                System.exit(0);
                break;
            default:
                System.out.println(RED + "Invalid choice '" + choice + "'. Please try again." + RESET);
                pauseAndClear(scanner);
        }
    }

    private static void startSimulation(Scanner scanner, BasicModelSimulationRunner basicRunner, ImprovedModelSimulationRunner improvedRunner, BatchModelSimulationRunner batchRunner) {
        clearScreen();
        System.out.println("\nStarting Simulation...");
        System.out.println("Select simulation type:");
        viewSimulationTypes();

        System.out.print(YELLOW + "Enter the simulation type number: " + RESET);
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
                System.out.println(RED + "Invalid simulation type '" + simulationType + "'. Returning to main menu." + RESET);
        }

        pauseAndClear(scanner);
    }

    private static void viewSimulationTypes() {
        clearScreen();
        System.out.println(YELLOW + "\nSimulation Types:" + RESET);
        System.out.println(YELLOW + "1" + RESET  + ". Basic Model Simulation");
        System.out.println(YELLOW + "2" + RESET + ". Improved Model Simulation");
        System.out.println(YELLOW + "3" + RESET + ". Batch Simulation");
    }

    private static void pauseAndClear(Scanner scanner) {
        System.out.println(YELLOW + "\nPress Enter to return to the menu..." + RESET);
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
        printAsciiArt();
    }
}
