# ECTE331-project

package q1;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

public class Main {

    // Create a Random object to generate random values throughout the program
    public static final Random random = new Random();

    // Store the last valid Sensor 3 value (used when no majority is found)
    public static double lastValidSensor3Value = -1;

    // Create a FileLogger object to handle logging with failover
    public static final FileLogger logger = new FileLogger("log.txt");

    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); // Create scanner to get user input

        // Ask the user for the maximum temperature value
        System.out.print("Enter maximum temperature to simulate (°C): ");
        float maxTemp = scanner.nextFloat(); // Store the user input

        // Run 7 cycles of sensor readings
        for (int attempt = 1; attempt <= 7; attempt++) {
            System.out.println("\n--- Reading Cycle " + attempt + " ---");

            // Generate a random temperature (Sensor 1)
            float temperature = generateTemperature(maxTemp);

            // Generate a random humidity value (Sensor 2)
            float humidity = generateHumidity(); // Value between 0.0 and 1.0

            // Generate three readings for Sensor 3 (replica sensors)
            double[] sensor3Readings = {
                generateThirdSensor(),
                generateThirdSensor(),
                generateThirdSensor()
            };

            // Print Sensor 1 reading
            System.out.printf("Sensor 1 (Temperature): %.2f°C\n", temperature);

            // Print Sensor 2 reading
            System.out.printf("Sensor 2 (Humidity): %.2f\n", humidity);

            // Print all three Sensor 3 replica readings
            System.out.println("Sensor 3 Replicas: " +
                String.format("Sensor3.1=%.2f, Sensor3.2=%.2f, Sensor3.3=%.2f",
                    sensor3Readings[0], sensor3Readings[1], sensor3Readings[2]));

            // Determine final value using majority voting
            double finalValue = majorityVoter(sensor3Readings);

            // Print the final Sensor 3 value selected
            System.out.printf("Final Sensor 3 Value Used: %.2f\n", finalValue);

            // Create a message string to log the current cycle's data
            String logMessage = getTimestamp() + " - Readings: Temp=" + temperature +
                "°C, Humidity=" + humidity +
                ", Sensor3=" + Arrays.toString(sensor3Readings) +
                ", FinalValue=" + finalValue;

            // Attempt to log the message using FileLogger
            logger.log(logMessage);
        }

        // Close the scanner after all cycles are done
        scanner.close();
    }

    /**
     * Generate a random temperature reading.
     * maxTemp maximum temperature value
     * @return random temperature between 0 and maxTemp
     */
    
    public static float generateTemperature(float maxTemp) {
        return random.nextFloat() * maxTemp;
    }

    /**
     * Generate a random humidity reading.
     * @return random humidity value between 0.0 and 1.0
     */
    public static float generateHumidity() {
        return random.nextFloat();
    }

    /**
     * Generate a Sensor 3 reading with fault simulation.
     * 80% chance: return a value between 45 and 55 (normal)
     * 20% chance: return a completely random value (fault simulation)
     * @return sensor reading value
     */
    public static double generateThirdSensor() {
        if (random.nextInt(10) < 2) { // 20% probability
            return random.nextDouble() * 100; // Faulty range
        }
        return 45 + random.nextDouble() * 10; // Normal expected range
    }

    /**
     * Determine the majority reading from the three Sensor 3 values using majority voting.
     * Logs discrepancy events when sensors produce different outputs.
     * @param s array of three sensor readings
     * @return the majority value or fallback value if no majority exists
     */
    public static double majorityVoter(double[] s) {
        // Define tolerance for considering values as "same" (0.5 units)
        final double TOLERANCE = 0.5;
        
        // Check for majority between sensors
        boolean match12 = Math.abs(s[0] - s[1]) <= TOLERANCE;
        boolean match13 = Math.abs(s[0] - s[2]) <= TOLERANCE;
        boolean match23 = Math.abs(s[1] - s[2]) <= TOLERANCE;
        
        // Check if any discrepancy exists (at least one pair differs)
        boolean hasDiscrepancy = !match12 || !match13 || !match23;
        
        if (hasDiscrepancy) {
            // Log discrepancy with outlier identification
            String discrepancyMsg = getTimestamp() + " - Discrepancy Detected in Sensor 3 values: " +
                String.format("Sensor3.1=%.2f, Sensor3.2=%.2f, Sensor3.3=%.2f", s[0], s[1], s[2]);
            
            // Identify outliers
            if (match12 && !match13 && !match23) {
                // Sensors 1&2 match, 3 is outlier
                discrepancyMsg += ". Outlier: Sensor3.3";
                System.out.printf("Majority detected (Sensors 3.1 & 3.2): %.2f. Outlier: Sensor3.3=%.2f\n", s[0], s[2]);
                lastValidSensor3Value = s[0];
                logger.log(discrepancyMsg);
                return s[0];
            } else if (match13 && !match12 && !match23) {
                // Sensors 1&3 match, 2 is outlier
                discrepancyMsg += ". Outlier: Sensor3.2";
                System.out.printf("Majority detected (Sensors 3.1 & 3.3): %.2f. Outlier: Sensor3.2=%.2f\n", s[0], s[1]);
                lastValidSensor3Value = s[0];
                logger.log(discrepancyMsg);
                return s[0];
            } else if (match23 && !match12 && !match13) {
                // Sensors 2&3 match, 1 is outlier
                discrepancyMsg += ". Outlier: Sensor3.1";
                System.out.printf("Majority detected (Sensors 3.2 & 3.3): %.2f. Outlier: Sensor3.1=%.2f\n", s[1], s[0]);
                lastValidSensor3Value = s[1];
                logger.log(discrepancyMsg);
                return s[1];
            } else {
                // All three values are different - no majority
                discrepancyMsg += ". Outliers: All sensors (no majority found)";
                System.out.println("No majority found - all sensors differ.");
                System.out.printf("All Outliers: Sensor3.1=%.2f, Sensor3.2=%.2f, Sensor3.3=%.2f\n", s[0], s[1], s[2]);
                
                // Handle case where no previous valid value exists
                if (lastValidSensor3Value == -1) {
                    System.out.println("No previous valid value available. Using Sensor3.1 as fallback.");
                    lastValidSensor3Value = s[0];
                    discrepancyMsg += ". No previous valid value - using Sensor3.1 as fallback: " + s[0];
                    logger.log(discrepancyMsg);
                    return s[0];
                } else {
                    System.out.printf("Using previous valid value: %.2f\n", lastValidSensor3Value);
                    discrepancyMsg += ". Using previous valid value: " + lastValidSensor3Value;
                    logger.log(discrepancyMsg);
                    return lastValidSensor3Value;
                }
            }
        } else {
            // All sensors agree (within tolerance) - no discrepancy
            System.out.printf("All sensors agree: %.2f\n", s[0]);
            lastValidSensor3Value = s[0];
            return s[0];
        }
    }

    /**
     * Get current timestamp in yyyy-MM-dd HH:mm:ss format.
     * @return formatted timestamp string
     */
    public static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}

