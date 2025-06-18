package Project1;

import java.util.Random;
import java.util.Scanner;

class FileLogger {
    final int MAX_BACKUP_FILES = 5;

    public void logFaultySensor(int sensorIndex, double value) {
        System.out.printf("Faulty Sensor Detected: Sensor[%d] with reading %.2f\n", sensorIndex, value);
        // Here, you could write this info to a file with backup rotation logic.
    }
}

public class question1 {
    public static final Random random = new Random();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        FileLogger logger = new FileLogger();

        // Ask user for max temperature
        System.out.print("What is the maximum temperature you want to simulate (in °C)? ");
        float maxTemp = scanner.nextFloat();

        // Ask user for max humidity
        System.out.print("What is the maximum humidity you want to simulate (value between 0 and 1)? ");
        float maxHumidity = scanner.nextFloat();

        // Generate temperature and humidity values
        float temperature = generateTemperature(maxTemp);
        float humidity = generateHumidity(maxHumidity);

        System.out.printf("Generated Temperature: %.2f°C\n", temperature);
        System.out.printf("Generated Humidity: %.2f (normalized)\n", humidity);

        // Simulate three redundant sensors
        double[] threeSensors = new double[3];
        for (int i = 0; i < 3; i++) {
            threeSensors[i] = generateThirdSensor();
            System.out.printf("Sensor[%d] Reading: %.2f\n", i, threeSensors[i]);
        }

        // Use Majority Voter
        double majorityValue = MajorityVoter(threeSensors, logger);
        System.out.printf("Final Sensor Output after Majority Voter: %.2f\n", majorityValue);

        scanner.close();
    }

    public static double MajorityVoter(double[] threeSensors, FileLogger logger) {
        final double epsilon = 0.001;

        boolean match01 = Math.abs(threeSensors[0] - threeSensors[1]) < epsilon;
        boolean match12 = Math.abs(threeSensors[1] - threeSensors[2]) < epsilon;
        boolean match02 = Math.abs(threeSensors[0] - threeSensors[2]) < epsilon;

        if (match01 && match12) {
            return threeSensors[0]; // all match
        } else if (match01) {
            logger.logFaultySensor(2, threeSensors[2]);
            return threeSensors[0];
        } else if (match12) {
            logger.logFaultySensor(0, threeSensors[0]);
            return threeSensors[1];
        } else if (match02) {
            logger.logFaultySensor(1, threeSensors[1]);
            return threeSensors[0];
        } else {
            System.out.println("No majority agreement. All sensors differ. Returning average.");
            return (threeSensors[0] + threeSensors[1] + threeSensors[2]) / 3.0;
        }
    }

    public static float generateTemperature(float maxTemp) {
        return random.nextFloat() * maxTemp;
    }

    public static float generateHumidity(float maxHumidity) {
        return random.nextFloat() * maxHumidity;
    }

    public static double generateThirdSensor() {
        return random.nextDouble() * 100;
    }
}

