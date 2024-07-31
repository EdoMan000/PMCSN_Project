package org.pmcsn.conf;

import org.pmcsn.centers.*;

public class CenterFactory {
    private final Config config = new Config();

    public CenterFactory() {
    }

    //LuggageChecks(6,
    // (24 * 60) / 6300.0,
    // 1,
    // approximateServiceAsExponential);
    public LuggageChecks createLuggageChecks(boolean approximateServiceAsExponential) {
        return new LuggageChecks(
                config.getString("luggageChecks", "centerName"),
                config.getInt("luggageChecks", "numberOfCenters"),
                config.getDouble("luggageChecks", "interArrivalTime"),
                config.getDouble("luggageChecks", "meanServiceTime"),
                approximateServiceAsExponential);
    }

    //CheckInDesksTarget("CHECK_IN_TARGET",
    // 10,
    // 3,
    // 19,
    // approximateServiceAsExponential);
    public CheckInDesksTarget createCheckinDeskTarget(boolean approximateServiceAsExponential) {
        return new CheckInDesksTarget(
                config.getString("checkInDeskTarget", "centerName"),
                config.getDouble("checkInDeskTarget", "meanServiceTime"),
                config.getInt("checkInDeskTarget", "serversNumber"),
                config.getInt("checkInDeskTarget", "centerIndex"),
                approximateServiceAsExponential);
    }


    //CheckInDesksOthers(19,
    // 3,
    // 10,
    // 21,
    // approximateServiceAsExponential);
    public CheckInDesksOthers createCheckinDeskOthers(boolean approximateServiceAsExponential) {
        return new CheckInDesksOthers(
                config.getString("checkInDeskOthers", "centerName"),
                config.getInt("checkInDeskOthers", "numberOfCenters"),
                config.getInt("checkInDeskOthers", "serversNumber"),
                config.getDouble("checkInDeskOthers", "meanServiceTime"),
                config.getInt("checkInDeskOthers", "centerIndex"),
                approximateServiceAsExponential);
    }

    //BoardingPassScanners("BOARDING_PASS_SCANNERS",
    // 0.3,
    // 3,
    // 59,
    // approximateServiceAsExponential);
    public BoardingPassScanners createBoardingPassScanners(boolean approximateServiceAsExponential) {
        return new BoardingPassScanners(
                config.getString("boardingPassScanners", "centerName"),
                config.getDouble("boardingPassScanners", "meanServiceTime"),
                config.getInt("boardingPassScanners", "serversNumber"),
                config.getInt("boardingPassScanners", "centerIndex"),
                approximateServiceAsExponential
        );
    }

    public SecurityChecks createSecurityChecks(boolean approximateServiceAsExponential) {
        return new SecurityChecks(
                config.getString("securityChecks", "centerName"),
                config.getDouble("securityChecks", "meanServiceTime"),
                config.getInt("securityChecks", "serversNumber"),
                config.getInt("securityChecks", "centerIndex"),
                approximateServiceAsExponential
        );
    }

    public PassportChecks createPassportChecks(boolean approximateServiceAsExponential) {
        return new PassportChecks(
                config.getString("passportChecks", "centerName"),
                config.getDouble("passportChecks", "meanServiceTime"),
                config.getInt("passportChecks", "serversNumber"),
                config.getInt("passportChecks", "centerIndex"),
                approximateServiceAsExponential
        );
    }

    public StampsCheck createStampsCheck(boolean approximateServiceAsExponential) {
        return new StampsCheck(
                config.getString("stampsCheck", "centerName"),
                config.getDouble("stampsCheck", "meanServiceTime"),
                config.getInt("stampsCheck", "centerIndex"),
                approximateServiceAsExponential
        );
    }

    public BoardingTarget createBoardingTarget(boolean approximateServiceAsExponential) {
        return new BoardingTarget(
                config.getString("boardingTarget", "centerName"),
                config.getDouble("boardingTarget", "meanServiceTime"),
                config.getInt("boardingTarget", "serversNumber"),
                config.getInt("boardingTarget", "centerIndex"),
                approximateServiceAsExponential
        );
    }

    public BoardingOthers createBoardingOthers(boolean approximateServiceAsExponential) {
        return new BoardingOthers(
                config.getString("boardingOthers", "centerName"),
                config.getInt("boardingOthers", "numberOfCenters"),
                config.getInt("boardingOthers", "serversNumber"),
                config.getDouble("boardingOthers", "meanServiceTime"),
                config.getInt("boardingOthers", "centerIndex"),
                approximateServiceAsExponential
        );
    }
}
