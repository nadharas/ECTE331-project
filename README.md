# ECTE331-project
package Project1;
import java.util.Random;
import java.util.Scanner;

class FileLogger {
    final int MAX_BACKUP_FILES = 5;
}

public class Driver {
    public static final Random random = new Random();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Ask user for max temperature
        System.out.print("What is the maximum temperature you want to simulate (in °C)?");
        float maxTemp = scanner.nextFloat();

        // Ask user for max humidity
        System.out.print("What is the maximum humidity you want to simulate (value between 0 and 1)?");
        float maxHumidity = scanner.nextFloat();

        // Generate temperature and humidity values
        float temperature = generateTemperature(maxTemp);
        float humidity = generateHumidity(maxHumidity); // between 0 and 1

        System.out.printf("Generated Temperature: %.2f°C\n", temperature);
        System.out.printf("Generated Humidity: %.2f (normalized)\n", humidity);

        // Sensor 3 - three redundant replicas
        double[] threeSensors = new double[3];
//        }

        scanner.close();
    }
        
        public static double MajorityVoter(double [] three_sesnors) {
        
        final double epsilon=0.001;
		return epsilon;
        
        //compare all three inputs 
        // store boolean flags for each comparison 
        //if 2 flags are true ;
        //then return 1 of them 
        //if flag  12 is true 
        //log the faulty reading with its index else 
        //
    }

    public static float generateTemperature(float maxTemp) {
        return random.nextFloat() * maxTemp;
    }

    public static float generateHumidity(float maxHumidity) {
        // Humidity is expected to be a value between 0.0 and 1.0
        return random.nextFloat() * maxHumidity;
    }

    public static double generateThirdSensor() {
        // Simulates a sensor value between 0 and 100
        return random.nextDouble() * 100;
    }
}
