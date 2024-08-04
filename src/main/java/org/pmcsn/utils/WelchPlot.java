package org.pmcsn.utils;

import org.pmcsn.model.Observations;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WelchPlot {
    private final String DELIMITER = "\n";
    private final String COMMA = ",";

    public static void writeObservations(List<List<Observations>> checkinDeskOthersObservations, String simulationType) {
        for (List<Observations> observations : checkinDeskOthersObservations) {
            writeObservations(simulationType, observations);
        }
    }

    public static void clearObservationsDirectory(String simulationType) {
        File root = new File("csvFiles/%s/observations/".formatted(simulationType));
        if (root.exists() && root.isDirectory() && root.listFiles() != null) {
            for (File file : root.listFiles()) {
                file.delete();
            }
        }
    }

    public static void writeObservations(String simulationType, List<Observations> observationsList) {
        String parent = "csvFiles/%s/observations/".formatted(simulationType);
        File directory = new File(parent);
        if (!directory.exists()) {
            directory.mkdirs();
        } else {
            clearObservationsDirectory(simulationType);
        }
        for (Observations o : observationsList) {
            File file = new File(parent, "%s.data".formatted(o.getCenterName()));
            try (FileWriter fileWriter = new FileWriter(file, true)) {
                StringBuilder row = new StringBuilder();
                o.getPoints().forEach(p -> row.append(p.toString()).append(","));
                fileWriter.write(row.append("\n").toString());
                fileWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void welchPlot(String parent) throws IOException {
        List<Path> files = listAllFiles(Path.of(parent));
        for (Path file : files) {
            List<List<Double>> matrix = new ArrayList<>();
            for (String line : Files.readAllLines(file)) {
                matrix.add(Arrays.stream(line.trim().split(","))
                        .mapToDouble(Double::parseDouble)
                        .boxed()
                        .collect(Collectors.toList()));
            }
            List<Double> plot = welchPlot(matrix);
            String plotPath = file.toString().replace(".data", "_plot.csv");
            savePlot(plotPath, plot);
        }
    }

    private static void savePlot(String plotPath, List<Double> plot) {
        try (FileWriter w = new FileWriter(plotPath)) {
            StringBuilder s = new StringBuilder();
            s.append("E[Ts]\n");
            plot.forEach(x -> s.append(x).append("\n"));
            w.write(s.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Path> listAllFiles(Path parent) throws IOException {
        List<Path> paths = new ArrayList<>();
        paths.add(parent);
        List<Path> files = new ArrayList<>();
        while (!paths.isEmpty()) {
            var path = paths.removeLast();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path))
            {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        if (!Files.isHidden(entry)) {
                            paths.add(entry);
                        }
                    } else if (entry.toString().endsWith(".data")) {
                        files.add(entry);
                    }
                }
            }
        }
        return files;
    }

    public static List<Double> welchPlot(List<List<Double>> matrix) throws IOException {
        int m = matrix.stream().filter(r -> !r.isEmpty()).mapToInt(List::size).min().orElseThrow();
        List<Double> ensembleAverage = new ArrayList<>(m);

        // Calculate ensemble averages
        for (int i = 0; i < m; i++) {
            double sum = 0.0;
            int count = 0;
            for (List<Double> row : matrix) {
                if (!row.isEmpty()) {
                    sum += row.get(i);
                    count += 1;
                }
            }
            ensembleAverage.add(sum / count);
        }
        // Define window size (w)
        int w = Math.min(m / 4, 5); // Choose w as min(m/4, 5)
        // Calculate moving averages
        List<Double> movingAverage = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            double sum = 0.0;
            int count = 0;

            if (i < w) {
                for (int s = -i; s <= i; s++) {
                    sum += ensembleAverage.get(i + s);
                    count++;
                }
                movingAverage.add(sum / count);
            } else if (i >= m - w) {
                for (int s = -(m - i - 1); s <= (m - i - 1); s++) {
                    sum += ensembleAverage.get(i + s);
                    count++;
                }
                movingAverage.add(sum / count);
            } else {
                for (int s = -w; s <= w; s++) {
                    sum += ensembleAverage.get(i + s);
                    count++;
                }
                movingAverage.add(sum / (2 * w + 1));
            }
        }
        return movingAverage;
    }
}
