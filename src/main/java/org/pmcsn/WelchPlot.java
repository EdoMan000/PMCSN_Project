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
    public static void writeObservations(List<List<Observations>> checkinDeskOthersObservations, String simulationType) {
        for (List<Observations> observations : checkinDeskOthersObservations) {
            writeObservations(simulationType, observations);
        }
    }

    public static void writeObservations(String simulationType, List<Observations> observationsList) {
        File file = new File("csvFiles/"+simulationType+"/observations/" );
        if (!file.exists()) {
            file.mkdirs();
        }
        String DELIMITER = "\n";
        String COMMA = ",";
        for (Observations o : observationsList) {
            String path = "csvFiles/"+simulationType+"/observations/" + o.getCenterName()+ ".csv";
            file = new File(path);
            try(FileWriter fileWriter = new FileWriter(file)) {
                // fileWriter.append("E[Ts], E[Tq], E[s], E[Ns], E[Nq], ρ, λ").append(DELIMITER);
                fileWriter.append("#Observation,E[Ts]").append(DELIMITER);
                List<Double> points = o.welchPlot(Observations.INDEX.RESPONSE_TIME);
                if (!points.isEmpty()) {
                    int x = 0;
                    for (Double y : points) {
                        fileWriter
                                .append(String.valueOf(x))
                                .append(COMMA)
                                .append(String.valueOf(y)).append(DELIMITER);
                        x += 1;
                    }
                } else {
                    System.out.println(path + " è vuoto!");
                }

                fileWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
