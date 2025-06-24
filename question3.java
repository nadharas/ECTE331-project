package question3;

//Shared data class to store computation results and synchronization flags
class Data {
 int A1, B1, A2, B2, A3, B3; // Shared variables for computation results

 // Boolean flags to manage synchronization and progress control
 boolean gotoB2 = false;
 boolean gotoA2 = false;
 boolean gotoB3 = false;
 boolean gotoA3 = false;
}

//Utility class that contains the summation method implemented with a loop
class Utility {
 public static int calculate(int n) {
     int sum = 0;
     for (int i = 0; i <= n; i++) {
         sum += i;
     }
     return sum;
 }
}

//ThreadA handles computations A1 → A2 → A3
class ThreadA extends Thread {
 private final Data data;

 public ThreadA(Data data) {
     this.data = data;
 }

 public void run() {
     // Compute A1 and notify ThreadB that B2 can start
     synchronized (data) {
         data.A1 = Utility.calculate(500);
         System.out.println("A1 completed: " + data.A1);
         data.gotoB2 = true;
         data.notifyAll();
     }

     // Wait until B2 is done, then compute A2 and notify ThreadB to proceed to B3
     synchronized (data) {
         try {
             while (!data.gotoA2) data.wait();
             data.A2 = data.B2 + Utility.calculate(300);
             System.out.println("A2 completed: " + data.A2);
             data.gotoB3 = true;
             data.notifyAll();
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }

     // Wait until B3 is done, then compute A3 and notify ThreadC to proceed
     synchronized (data) {
         try {
             while (!data.gotoA3) data.wait();
             data.A3 = data.B3 + Utility.calculate(400);
             System.out.println("A3 completed: " + data.A3);
             data.notifyAll(); // Final notify for ThreadC
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }
 }
}

//ThreadB handles computations B1 → B2 → B3
class ThreadB extends Thread {
 private final Data data;

 public ThreadB(Data data) {
     this.data = data;
 }

 public void run() {
     // Compute B1 (independent)
     synchronized (data) {
         data.B1 = Utility.calculate(250);
         System.out.println("B1 completed: " + data.B1);
     }

     // Wait until A1 is done, then compute B2 and notify ThreadA to proceed to A2
     synchronized (data) {
         try {
             while (!data.gotoB2) data.wait();
             data.B2 = data.A1 + Utility.calculate(200);
             System.out.println("B2 completed: " + data.B2);
             data.gotoA2 = true;
             data.notifyAll();
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }

     // Wait until A2 is done, then compute B3 and notify ThreadA to proceed to A3
     synchronized (data) {
         try {
             while (!data.gotoB3) data.wait();
             data.B3 = data.A2 + Utility.calculate(400);
             System.out.println("B3 completed: " + data.B3);
             data.gotoA3 = true;
             data.notifyAll();
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }
 }
}

//ThreadC waits until A3 is done, then safely reads A2 + B3
class ThreadC extends Thread {
 private final Data data;

 public ThreadC(Data data) {
     this.data = data;
 }

 public void run() {
     // Wait until all computations are done (A3 completion implies readiness of A2 and B3)
     synchronized (data) {
         try {
             while (!data.gotoA3) data.wait();
             int result = data.A2 + data.B3;
             System.out.println("Thread C: A2 + B3 = " + result);
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }
 }
}

//Main class to run the test multiple times for verification
public class question3 {
 public static void main(String[] args) {
     for (int i = 1; i <= 7; i++) {
         System.out.println("---- Iteration: " + i + " ----");

         // Create fresh shared data object for each run
         Data data = new Data();

         // Instantiate threads
         ThreadA threadA = new ThreadA(data);
         ThreadB threadB = new ThreadB(data);
         ThreadC threadC = new ThreadC(data);

         // Start all threads
         threadA.start();
         threadB.start();
         threadC.start();

         // Wait for all threads to finish before next iteration
         try {
             threadA.join();
             threadB.join();
             threadC.join();
             System.out.println("A3 value: " + data.A3);
             System.out.println("-----------------------------\n");
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }
 }
}
