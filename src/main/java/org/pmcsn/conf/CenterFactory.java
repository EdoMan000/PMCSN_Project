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
        //double interArrivalTime = 1;

        return new LuggageChecks(
                config.getString("luggageChecks", "centerName"),
                config.getInt("luggageChecks", "numberOfCenters"),
                interArrivalTime,
                config.getDouble("luggageChecks", "meanServiceTime"),
                config.getInt("luggageChecks", "streamIndex"),
                approximateServiceAsExponential);
    }


    public CheckInDesks createCheckinDeskOthers(boolean approximateServiceAsExponential) {
        return new CheckInDesks(
                config.getString("checkInDesk", "centerName"),
                config.getInt("checkInDesk", "numberOfCenters"),
                config.getInt("checkInDesk", "serversNumber"),
                config.getDouble("checkInDesk", "meanServiceTime"),
                config.getInt("checkInDesk", "streamIndex"),
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
                config.getInt("stampsCheck", "serversNumber"),
                config.getInt("stampsCheck", "streamIndex"),
                approximateServiceAsExponential
        );
    }

    public Boarding createBoardingOthers(boolean approximateServiceAsExponential) {
        return new Boarding(
                config.getString("boarding", "centerName"),
                config.getInt("boarding", "numberOfCenters"),
                config.getInt("boarding", "serversNumber"),
                config.getDouble("boarding", "meanServiceTime"),
                config.getInt("boarding", "streamIndex"),
                approximateServiceAsExponential
        );
    }
}
