package org.pmcsn;

import org.pmcsn.model.Observations;
import org.pmcsn.model.Statistics;
import org.pmcsn.utils.Verification;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.pmcsn.utils.StatisticsUtils.computeConfidenceInterval;

public class WelchPlot {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Please provide the number of replications (R) and the max number of observations (n)");
        }
        final int R = Integer.parseInt(args[0]);
        final int n = Integer.parseInt(args[1]);
        List<List<Double>> observations = new ArrayList<>();
        IntStream.range(0, R).forEach(_ -> observations.add(new ArrayList<>()));
    }

    private static List<Double> averages(List<List<Double>> observations) {
        long m = observations.stream().mapToLong(List::size).min().orElseThrow();
        List<Double> averages = new ArrayList<>();
        for (List<Double> replication : observations) {
            averages.add(replication.stream().mapToDouble(d -> d).average().orElseThrow());
        }
        return averages;
    }


    public static void writeObservations(String simulationType, List<Observations> observationsList) {
        File file = new File("csvFiles/"+simulationType+"/observations/" );
        if (!file.exists()) {
            file.mkdirs();
        }
        String DELIMITER = "\n";
        String COMMA = ",";
        for (Observations o : observationsList) {
            file = new File("csvFiles/"+simulationType+"/observations/" + o.getCenterName()+ ".csv");
            try(FileWriter fileWriter = new FileWriter(file)) {

                // fileWriter.append("E[Ts], E[Tq], E[s], E[Ns], E[Nq], ρ, λ").append(DELIMITER);
                fileWriter.append("E[Ts]").append(DELIMITER);
                List<Double> points = o.welchPlot("E[Ts]");
                int x = 0;
                for (Double y : points) {
                    fileWriter.append(COMMA);
                    fileWriter
                            .append(String.valueOf(x))
                            .append(COMMA)
                            .append(String.valueOf(y)).append(DELIMITER);
                }
                    //fileWriter.append(observation.centerName).append(COMMA)
                    //        .append(String.valueOf(observation.Ets)).append(COMMA)
                    //        .append(String.valueOf(observation.Etq)).append(COMMA)
                    //        .append(String.valueOf(observation.Es)).append(COMMA)
                    //        .append(String.valueOf(observation.Ens)).append(COMMA)
                    //        .append(String.valueOf(observation.Enq)).append(COMMA)
                    //        .append(String.valueOf(observation.rho)).append(COMMA)
                    //        .append(String.valueOf(observation.lambda)).append(DELIMITER);
                fileWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
