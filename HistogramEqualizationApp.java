package q2;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicIntegerArray;
import javax.imageio.ImageIO;

public class HistogramEqualizationApp {
    public static void main(String[] args) {
        String inputFile = "C:/Users/Nadha/Downloads/q2/Rain_Tree.jpg";
        String outputSingleThread = "C:/Users/Nadha/Downloads/q2/output_single.jpg";
        String outputMultiThread = "C:/Users/Nadha/Downloads/q2/output_multi.jpg";
        
        colourImage originalImage = new colourImage();
        imageReadWrite.readJpgImage(inputFile, originalImage);
        
        System.out.println("Image loaded: " + originalImage.width + "x" + originalImage.height);
        
        // Test different thread counts
        int[] threadCounts = {1, 2, 4, 8};
        
        System.out.println("\n=== Performance Comparison ===");
        System.out.println("Threads\tSingle(ms)\tMulti-Shared(ms)\tMulti-Separate(ms)");
        
        for (int numThreads : threadCounts) {
            // Test single-threaded (only for 1 thread)
            long singleTime = 0;
            if (numThreads == 1) {
                singleTime = testSingleThreaded(originalImage, outputSingleThread);
            }
            
            // Test multi-threaded implementations
            long multiSharedTime = testMultiThreadedShared(originalImage, outputMultiThread, numThreads);
            long multiSeparateTime = testMultiThreadedSeparate(originalImage, outputMultiThread, numThreads);
            
            System.out.printf("%d\t%d\t\t%d\t\t\t%d\n", 
                numThreads, singleTime, multiSharedTime, multiSeparateTime);
        }
    }
    
    private static long testSingleThreaded(colourImage image, String outputFile) {
        long totalTime = 0;
        int runs = 3;
        
        for (int run = 0; run < runs; run++) {
            colourImage resultImage = new colourImage();
            resultImage.width = image.width;
            resultImage.height = image.height;
            resultImage.pixels = new short[image.height][image.width][3];
            
            long startTime = System.currentTimeMillis();
            HistogramEqualizer.equalizeSingleThreaded(image, resultImage);
            long endTime = System.currentTimeMillis();
            
            totalTime += (endTime - startTime);
            
            if (run == 0) { // Save result from first run
                imageReadWrite.writeJpgImage(resultImage, outputFile);
            }
        }
        
        return totalTime / runs;
    }
    
    private static long testMultiThreadedShared(colourImage image, String outputFile, int numThreads) {
        long totalTime = 0;
        int runs = 3;
        
        for (int run = 0; run < runs; run++) {
            colourImage resultImage = new colourImage();
            resultImage.width = image.width;
            resultImage.height = image.height;
            resultImage.pixels = new short[image.height][image.width][3];
            
            long startTime = System.currentTimeMillis();
            HistogramEqualizer.equalizeMultiThreadedShared(image, resultImage, numThreads);
            long endTime = System.currentTimeMillis();
            
            totalTime += (endTime - startTime);
            
            if (run == 0) {
                imageReadWrite.writeJpgImage(resultImage, outputFile.replace(".jpg", "_shared.jpg"));
            }
        }
        
        return totalTime / runs;
    }
    
    private static long testMultiThreadedSeparate(colourImage image, String outputFile, int numThreads) {
        long totalTime = 0;
        int runs = 3;
        
        for (int run = 0; run < runs; run++) {
            colourImage resultImage = new colourImage();
            resultImage.width = image.width;
            resultImage.height = image.height;
            resultImage.pixels = new short[image.height][image.width][3];
            
            long startTime = System.currentTimeMillis();
            HistogramEqualizer.equalizeMultiThreadedSeparate(image, resultImage, numThreads);
            long endTime = System.currentTimeMillis();
            
            totalTime += (endTime - startTime);
            
            if (run == 0) {
                imageReadWrite.writeJpgImage(resultImage, outputFile.replace(".jpg", "_separate.jpg"));
            }
        }
        
        return totalTime / runs;
    }
}

class HistogramEqualizer {
    private static final int INTENSITY_LEVELS = 256; // For 8-bit images
    
    /**
     * Single-threaded histogram equalization
     */
    public static void equalizeSingleThreaded(colourImage input, colourImage output) {
        int size = input.width * input.height;
        
        // Process each color channel (R, G, B) separately
        for (int channel = 0; channel < 3; channel++) {
            // Step 1: Compute histogram
            int[] histogram = new int[INTENSITY_LEVELS];
            for (int y = 0; y < input.height; y++) {
                for (int x = 0; x < input.width; x++) {
                    int intensity = input.pixels[y][x][channel];
                    histogram[intensity]++;
                }
            }
            
            // Step 2: Calculate cumulative histogram with optimization
            int[] cumulativeHist = new int[INTENSITY_LEVELS];
            cumulativeHist[0] = histogram[0];
            for (int i = 1; i < INTENSITY_LEVELS; i++) {
                cumulativeHist[i] = cumulativeHist[i-1] + histogram[i];
            }
            
            // Optimize: Pre-calculate mapping values
            for (int i = 0; i < INTENSITY_LEVELS; i++) {
                cumulativeHist[i] = (cumulativeHist[i] * (INTENSITY_LEVELS - 1)) / size;
            }
            
            // Step 3: Apply histogram equalization
            for (int y = 0; y < input.height; y++) {
                for (int x = 0; x < input.width; x++) {
                    int originalIntensity = input.pixels[y][x][channel];
                    output.pixels[y][x][channel] = (short) cumulativeHist[originalIntensity];
                }
            }
        }
    }
    
    /**
     * Multi-threaded with shared histogram (horizontal strips)
     */
    public static void equalizeMultiThreadedShared(colourImage input, colourImage output, int numThreads) {
        int size = input.width * input.height;
        Thread[] threads = new Thread[numThreads];
        
        // Process each color channel separately
        for (int channel = 0; channel < 3; channel++) {
            // Shared atomic histogram
            AtomicIntegerArray sharedHistogram = new AtomicIntegerArray(INTENSITY_LEVELS);
            
            // Step 1: Compute histogram using multiple threads
            int rowsPerThread = input.height / numThreads;
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                final int startRow = t * rowsPerThread;
                final int endRow = (t == numThreads - 1) ? input.height : (t + 1) * rowsPerThread;
                final int currentChannel = channel;
                
                threads[t] = new Thread(() -> {
                    for (int y = startRow; y < endRow; y++) {
                        for (int x = 0; x < input.width; x++) {
                            int intensity = input.pixels[y][x][currentChannel];
                            sharedHistogram.incrementAndGet(intensity);
                        }
                    }
                });
                threads[t].start();
            }
            
            // Wait for histogram computation to complete
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            // Step 2: Calculate cumulative histogram (single-threaded for simplicity)
            int[] cumulativeHist = new int[INTENSITY_LEVELS];
            cumulativeHist[0] = sharedHistogram.get(0);
            for (int i = 1; i < INTENSITY_LEVELS; i++) {
                cumulativeHist[i] = cumulativeHist[i-1] + sharedHistogram.get(i);
            }
            
            // Optimize mapping
            for (int i = 0; i < INTENSITY_LEVELS; i++) {
                cumulativeHist[i] = (cumulativeHist[i] * (INTENSITY_LEVELS - 1)) / size;
            }
            
            // Step 3: Apply equalization using multiple threads
            for (int t = 0; t < numThreads; t++) {
                final int startRow = t * rowsPerThread;
                final int endRow = (t == numThreads - 1) ? input.height : (t + 1) * rowsPerThread;
                final int currentChannel = channel;
                
                threads[t] = new Thread(() -> {
                    for (int y = startRow; y < endRow; y++) {
                        for (int x = 0; x < input.width; x++) {
                            int originalIntensity = input.pixels[y][x][currentChannel];
                            output.pixels[y][x][currentChannel] = (short) cumulativeHist[originalIntensity];
                        }
                    }
                });
                threads[t].start();
            }
            
            // Wait for equalization to complete
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Multi-threaded with separate histograms per thread
     */
    public static void equalizeMultiThreadedSeparate(colourImage input, colourImage output, int numThreads) {
        int size = input.width * input.height;
        Thread[] threads = new Thread[numThreads];
        
        // Process each color channel separately
        for (int channel = 0; channel < 3; channel++) {
            // Separate histograms for each thread
            int[][] threadHistograms = new int[numThreads][INTENSITY_LEVELS];
            
            // Step 1: Compute histograms using multiple threads
            int rowsPerThread = input.height / numThreads;
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                final int startRow = t * rowsPerThread;
                final int endRow = (t == numThreads - 1) ? input.height : (t + 1) * rowsPerThread;
                final int currentChannel = channel;
                
                threads[t] = new Thread(() -> {
                    for (int y = startRow; y < endRow; y++) {
                        for (int x = 0; x < input.width; x++) {
                            int intensity = input.pixels[y][x][currentChannel];
                            threadHistograms[threadId][intensity]++;
                        }
                    }
                });
                threads[t].start();
            }
            
            // Wait for histogram computation to complete
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            // Step 2: Merge histograms and calculate cumulative histogram
            int[] finalHistogram = new int[INTENSITY_LEVELS];
            for (int i = 0; i < INTENSITY_LEVELS; i++) {
                for (int t = 0; t < numThreads; t++) {
                    finalHistogram[i] += threadHistograms[t][i];
                }
            }
            
            int[] cumulativeHist = new int[INTENSITY_LEVELS];
            cumulativeHist[0] = finalHistogram[0];
            for (int i = 1; i < INTENSITY_LEVELS; i++) {
                cumulativeHist[i] = cumulativeHist[i-1] + finalHistogram[i];
            }
            
            // Optimize mapping
            for (int i = 0; i < INTENSITY_LEVELS; i++) {
                cumulativeHist[i] = (cumulativeHist[i] * (INTENSITY_LEVELS - 1)) / size;
            }
            
            // Step 3: Apply equalization using multiple threads
            for (int t = 0; t < numThreads; t++) {
                final int startRow = t * rowsPerThread;
                final int endRow = (t == numThreads - 1) ? input.height : (t + 1) * rowsPerThread;
                final int currentChannel = channel;
                
                threads[t] = new Thread(() -> {
                    for (int y = startRow; y < endRow; y++) {
                        for (int x = 0; x < input.width; x++) {
                            int originalIntensity = input.pixels[y][x][currentChannel];
                            output.pixels[y][x][currentChannel] = (short) cumulativeHist[originalIntensity];
                        }
                    }
                });
                threads[t].start();
            }
            
            // Wait for equalization to complete
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

// Your existing classes (keeping them as they are)
class imageReadWrite {
    public static void readJpgImage(String fileName, colourImage ImgStruct) {
        try {
            File file = new File(fileName);
            BufferedImage image = ImageIO.read(file);
            
            System.out.println("file: " + file.getCanonicalPath());
            
            if (!image.getColorModel().getColorSpace().isCS_sRGB()) {
                System.out.println("Image is not in sRGB color space");
                return;
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            ImgStruct.width = width;
            ImgStruct.height = height;
            ImgStruct.pixels = new short[height][width][3];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = image.getRGB(x, y);
                    Color color = new Color(pixel, true);

                    ImgStruct.pixels[y][x][0] = (short) color.getRed();
                    ImgStruct.pixels[y][x][1] = (short) color.getGreen();
                    ImgStruct.pixels[y][x][2] = (short) color.getBlue();
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading image file: " + e.getMessage());
        }
    }

    public static void writeJpgImage(colourImage ImgStruct, String fileName) {
        try {
            int width = ImgStruct.width;
            int height = ImgStruct.height;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = new Color(ImgStruct.pixels[y][x][0], ImgStruct.pixels[y][x][1], ImgStruct.pixels[y][x][2]).getRGB();
                    image.setRGB(x, y, rgb);
                }
            }

            File outputFile = new File(fileName);
            ImageIO.write(image, "jpg", outputFile);

        } catch (IOException e) {
            System.out.println("Error writing image file: " + e.getMessage());
        }
    }
}

class matManipulation {
    public static void mat2Vect(short[][] mat, int width, int height, short[] vect) {
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++)
                vect[j + i * width] = mat[i][j];
    }
}

class colourImage {
    public int width;
    public int height;
    public short pixels[][][];
}