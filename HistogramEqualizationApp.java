package q2;

public class HistogramEqualization {
    public static void main(String[] args) {
        try {
            String in = "C:/Users/Nadha/Downloads/q2/Rain_Tree.jpg";
            String outST = "C:/Users/Nadha/Downloads/q2/Rain_Tree_ST.jpg";

            ColourImage input = new ColourImage();
            ImageReadWrite.readJpgImage(in, input);

            double[] st = new double[3];
            for (int i=0;i<3;i++) {
                ColourImage o = new ColourImage();
                o.width = input.width; o.height = input.height;
                o.pixels = new short[o.height][o.width][3];
                Timer t = new Timer();
                for (int ch=0;ch<3;ch++) {
                    short[][] inC = extract(input.pixels, ch);
                    short[][] outC = new short[o.height][o.width];
                    HistogramProcessor.equalizeChannelST(inC, outC, input.width, input.height);
                    apply(o.pixels, outC, ch);
                }
                st[i] = t.elapsedTime();
                if (i==0) ImageReadWrite.writeJpgImage(o, outST);
            }
            double avgST = (st[0]+st[1]+st[2])/3;

            int[] threads = {2,4,8};
            double[] shared = new double[3], sub = new double[3];

            for (int ti=0;ti<threads.length;ti++) {
                int T = threads[ti];

                double[] m1 = new double[3];
                for (int i=0;i<3;i++) {
                    ColourImage o = new ColourImage();
                    o.width=input.width; o.height=input.height;
                    o.pixels=new short[o.height][o.width][3];
                    Timer t = new Timer();
                    for (int ch=0;ch<3;ch++) {
                        short[][] inC = extract(input.pixels, ch);
                        short[][] outC = new short[o.height][o.width];
                        HistogramProcessor.equalizeChannelMTShared(inC, outC, input.width, input.height, T);
                        apply(o.pixels, outC, ch);
                    }
                    m1[i] = t.elapsedTime();
                }
                shared[ti] = (m1[0]+m1[1]+m1[2])/3;

                double[] m2 = new double[3];
                for (int i=0;i<3;i++) {
                    ColourImage o = new ColourImage();
                    o.width=input.width; o.height=input.height;
                    o.pixels=new short[o.height][o.width][3];
                    Timer t = new Timer();
                    for (int ch=0;ch<3;ch++) {
                        short[][] inC = extract(input.pixels, ch);
                        short[][] outC = new short[o.height][o.width];
                        HistogramProcessor.equalizeChannelMTSubHist(inC, outC, input.width, input.height, T);
                        apply(o.pixels, outC, ch);
                    }
                    m2[i] = t.elapsedTime();
                }
                sub[ti] = (m2[0]+m2[1]+m2[2])/3;
            }

            //  output
            System.out.println("\n------------------------------------------");
            System.out.println("   HISTOGRAM EQUALIZATION TIMINGS (ms)");
            System.out.println("------------------------------------------");
            System.out.printf("\nSingle-threaded: %.2f ms\n", avgST);

            System.out.println("\nMulti-threaded (Shared Histogram):");
            for (int i=0;i<threads.length;i++)
                System.out.printf("   T=%d : %.2f ms\n", threads[i], shared[i]);

            System.out.println("\nMulti-threaded (Sub‑Histogram):");
            for (int i=0;i<threads.length;i++)
                System.out.printf("   T=%d : %.2f ms\n", threads[i], sub[i]);

            System.out.println("------------------------------------------");

            // 4x4 matrix demo
            int w = 4, h = 4;
            short[][] mat = new short[h][w];
            for (int i=0;i<h;i++)
                for (int j=0;j<w;j++)
                    mat[i][j] = (short)(i*w+j);

            System.out.println("\nOriginal Matrix (4x4):");
            for (short[] row : mat) {
                for (short v : row)
                    System.out.printf("%4d", v);
                System.out.println();
            }

            System.out.println("\nReshaped Vector:");
            for (int i=0;i<w*h;i++)
                System.out.print(i + " ");
            System.out.println("\n------------------------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static short[][] extract(short[][][] px, int ch) {
        int H = px.length, W = px[0].length;
        short[][] a = new short[H][W];
        for (int y=0;y<H;y++)
            for (int x=0;x<W;x++)
                a[y][x] = px[y][x][ch];
        return a;
    }

    private static void apply(short[][][] px, short[][] a, int ch) {
        for (int y=0;y<px.length;y++)
            for (int x=0;x<px[0].length;x++)
                px[y][x][ch] = a[y][x];
    }
}
