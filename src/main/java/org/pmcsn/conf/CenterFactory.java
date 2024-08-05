package org.pmcsn.conf;

import org.pmcsn.centers.*;

public class CenterFactory {
    private final Config config = new Config();

    public CenterFactory() {
    }

    public LuggageChecks createLuggageChecks(boolean approximateServiceAsExponential) {
        double passengersNumber = config.getDouble("general", "numberOfPassengers");
        double flightsNumber = config.getDouble("general", "numberOfFlights");
        double observationTime = config.getDouble("general", "observationTime");
        double interArrivalTime = observationTime / (passengersNumber * flightsNumber);

        return new LuggageChecks(
                config.getString("luggageChecks", "centerName"),
                config.getInt("luggageChecks", "numberOfCenters"),
                interArrivalTime,
                config.getDouble("luggageChecks", "meanServiceTime"),
                config.getInt("luggageChecks", "streamIndex"),
                approximateServiceAsExponential);
    }

    public CheckInDesksTarget createCheckinDeskTarget(boolean approximateServiceAsExponential) {
        return new CheckInDesksTarget(
                config.getString("checkInDeskTarget", "centerName"),
                config.getDouble("checkInDeskTarget", "meanServiceTime"),
                config.getInt("checkInDeskTarget", "serversNumber"),
                config.getInt("checkInDeskTarget", "streamIndex"),
                approximateServiceAsExponential);
    }


    public CheckInDesksOthers createCheckinDeskOthers(boolean approximateServiceAsExponential) {
        return new CheckInDesksOthers(
                config.getString("checkInDeskOthers", "centerName"),
                config.getInt("checkInDeskOthers", "numberOfCenters"),
                config.getInt("checkInDeskOthers", "serversNumber"),
                config.getDouble("checkInDeskOthers", "meanServiceTime"),
                config.getInt("checkInDeskOthers", "streamIndex"),
                approximateServiceAsExponential);
    }

    public BoardingPassScanners createBoardingPassScanners(boolean approximateServiceAsExponential) {
        return new BoardingPassScanners(
                config.getString("boardingPassScanners", "centerName"),
                config.getDouble("boardingPassScanners", "meanServiceTime"),
                config.getInt("boardingPassScanners", "serversNumber"),
                config.getInt("boardingPassScanners", "streamIndex"),
                approximateServiceAsExponential
        );
    }

    public SecurityChecks createSecurityChecks(boolean approximateServiceAsExponential) {
        return new SecurityChecks(
                config.getString("securityChecks", "centerName"),
                config.getDouble("securityChecks", "meanServiceTime"),
                config.getInt("securityChecks", "serversNumber"),
                config.getInt("securityChecks", "streamIndex"),
                approximateServiceAsExponential
        );
    }

    public PassportChecks createPassportChecks(boolean approximateServiceAsExponential) {
        return new PassportChecks(
                config.getString("passportChecks", "centerName"),
                config.getDouble("passportChecks", "meanServiceTime"),
                config.getInt("passportChecks", "serversNumber"),
                config.getInt("passportChecks", "streamIndex"),
                approximateServiceAsExponential
        );
    }

    public StampsCheck createStampsCheck(boolean approximateServiceAsExponential) {
        return new StampsCheck(
                config.getString("stampsCheck", "centerName"),
                config.getDouble("stampsCheck", "meanServiceTime"),
                config.getInt("stampsCheck", "streamIndex"),
                approximateServiceAsExponential
        );
    }

    public BoardingTarget createBoardingTarget(boolean approximateServiceAsExponential) {
        return new BoardingTarget(
                config.getString("boardingTarget", "centerName"),
                config.getDouble("boardingTarget", "meanServiceTime"),
                config.getInt("boardingTarget", "serversNumber"),
                config.getInt("boardingTarget", "streamIndex"),
                approximateServiceAsExponential
        );
    }

    public BoardingOthers createBoardingOthers(boolean approximateServiceAsExponential) {
        return new BoardingOthers(
                config.getString("boardingOthers", "centerName"),
                config.getInt("boardingOthers", "numberOfCenters"),
                config.getInt("boardingOthers", "serversNumber"),
                config.getDouble("boardingOthers", "meanServiceTime"),
                config.getInt("boardingOthers", "streamIndex"),
                approximateServiceAsExponential
        );
    }
}
