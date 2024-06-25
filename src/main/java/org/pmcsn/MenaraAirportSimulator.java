package org.pmcsn;

import org.pmcsn.libraries.Rngs;
import java.util.Scanner;

public class MenaraAirportSimulator {

    public static void main(String[] args) {
        Rngs rngs = new Rngs();
        rngs.plantSeeds(123456789L);
        printAsciiArt();
        showMenu();
        System.out.println("prova merge");
    }

    private static void printAsciiArt() {
        System.out.println("╔╦╗┌─┐┌┐┌┌─┐┬─┐┌─┐╔═╗┬┬─┐┌─┐┌─┐┬─┐┌┬┐  ╔═╗┬┌┬┐┬ ┬┬  ┌─┐┌┬┐┌─┐┬─┐");
        System.out.println("║║║├┤ │││├─┤├┬┘├─┤╠═╣│├┬┘├─┘│ │├┬┘ │   ╚═╗│││││ ││  ├─┤ │ │ │├┬┘");
        System.out.println("╩ ╩└─┘┘└┘┴ ┴┴└─┴ ┴╩ ╩┴┴└─┴  └─┘┴└─ ┴   ╚═╝┴┴ ┴└─┘┴─┘┴ ┴ ┴ └─┘┴└─");
    }

    private static void showMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\nWelcome to Menara Airport Simulator!");
            System.out.println("Please select an option:");
            System.out.println("1. Start Simulation");
            System.out.println("2. View Simulation Types");
            System.out.println("3. Exit");

            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine();  // Consume newline

            switch (choice) {
                case 1:
                    startSimulation(scanner);
                    break;
                case 2:
                    viewSimulationTypes();
                    break;
                case 3:
                    System.out.println("Exiting MenaraAirport Simulator. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 3);
    }

    private static void startSimulation(Scanner scanner) {
        System.out.println("Starting Simulation...");
        System.out.println("Select simulation type:");
        viewSimulationTypes();

        System.out.print("Enter the simulation type number: ");
        int simulationType = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        switch (simulationType) {
            case 1:
                System.out.println("Running Basic Simulation...");
                // Add code to run basic simulation
                break;
            case 2:
                System.out.println("Running Advanced Simulation...");
                // Add code to run advanced simulation
                break;
            case 3:
                System.out.println("Running Custom Simulation...");
                // Add code to run custom simulation
                break;
            default:
                System.out.println("Invalid simulation type. Returning to main menu.");
        }
    }

    private static void viewSimulationTypes() {
        System.out.println("Simulation Types:");
        System.out.println("1. Basic Simulation");
        System.out.println("2. Advanced Simulation");
        System.out.println("3. Custom Simulation");
    }
}
