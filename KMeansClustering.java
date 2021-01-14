import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import java.io.File;
import javax.swing.JLabel;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class KMeansClustering {

    Cluster[] clusters;

    static class Cluster {
        int Greens;
        int Blues;
        int Reds;
        int green;
        int blue;
        int red;
        int pixSum;
        int id;
        public Cluster(int id, int RGB) {
            int r = RGB>>16&0x000000FF;
            int g = RGB>> 8&0x000000FF;
            int b = RGB>> 0&0x000000FF;
            red = r;
            green = g;
            blue = b;
            this.id = id;
            addPix(RGB);
        }
        int distance(int Col) {
            int r = Col>>16&0x000000FF;
            int g = Col>> 8&0x000000FF;
            int b = Col>> 0&0x000000FF;
            int rx = Math.abs(red-r);
            int gx = Math.abs(green-g);
            int bx = Math.abs(blue-b);
            int d = (rx+gx+bx) / 3;
            return d;
        }
        int getId() {
            return id;
        }
        int getRGB() {
            int r = Reds / pixSum;
            int g = Greens / pixSum;
            int b = Blues / pixSum;
            return 0xff000000|r<<16|g<<8|b;
        }
        void addPix(int color) {
            int r = color>>16&0x000000FF;
            int g = color>> 8&0x000000FF;
            int b = color>> 0&0x000000FF;
            Reds+=r;
            Greens+=g;
            Blues+=b;
            pixSum++;
            red   = Reds/pixSum;
            green = Greens/pixSum;
            blue  = Blues/pixSum;
        }
        void delPix(int color) {
            int r = color>>16&0x000000FF;
            int g = color>> 8&0x000000FF;
            int b = color>> 0&0x000000FF;
            Reds-=r;
            Greens-=g;
            Blues-=b;
            pixSum--;
            red   = Reds/pixSum;
            green = Greens/pixSum;
            blue  = Blues/pixSum;
        }
    }
    public Cluster[] newClusters(BufferedImage image, int k) {

        Cluster[] result = new Cluster[k];
        int x = 0; int y = 0;
        int dx = image.getWidth()/k;
        int dy = image.getHeight()/k;
        for (int i=0;i<k;i++) {
            result[i] = new Cluster(i,image.getRGB(x, y));
            x+=dx; y+=dy;
        }
        return result;
    }

    public Cluster MinimalCluster(int rgb) {
        Cluster cluster = null;
        int min = Integer.MAX_VALUE;
        for (int i=0;i<clusters.length;i++) {
            int distance = clusters[i].distance(rgb);
            if (distance<min) {
                min = distance;
                cluster = clusters[i];
            }
        }
        return cluster;

    }
    public static BufferedImage loadImage(String filename) {
        BufferedImage result = null;
        try {
            result = ImageIO.read(new File(filename));
        } catch (Exception e) {
            System.out.println(e.toString()+" Image '"
                    +filename+"' not found.");
        }
        return result;
    }
    public BufferedImage calculate(BufferedImage image,
                                   int k, int mode) {
        long start = System.currentTimeMillis();
        int w = image.getWidth();
        int h = image.getHeight();

        clusters = newClusters(image,k);

        int[] lut = new int[w*h];
        Arrays.fill(lut, -1);

        boolean pixelChangedCluster = true;
        int loops = 0;
        while (pixelChangedCluster) {
            pixelChangedCluster = false;
            loops++;
            for (int y=0;y<h;y++) {
                for (int x=0;x<w;x++) {
                    int pixel = image.getRGB(x, y);
                    Cluster cluster = MinimalCluster(pixel);
                    if (lut[w*y+x]!=cluster.getId()) {
                        if (lut[w*y+x]!=-1) {
                            clusters[lut[w*y+x]].delPix(
                                    pixel);
                        }
                        cluster.addPix(pixel);
                        pixelChangedCluster = true;

                        lut[w*y+x] = cluster.getId();
                    }
                }
            }

        }
        BufferedImage result = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_RGB);
        for (int y=0;y<h;y++) {
            for (int x=0;x<w;x++) {
                int clusterId = lut[w*y+x];
                result.setRGB(x, y, clusters[clusterId].getRGB());
            }
        }
        long end = System.currentTimeMillis();
        System.out.println( k + " clusters were generated in "+loops +" loops, in "+(end-start)+" milliseconds");
        try {
            ImageIO.write(result, "png", new File("output.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) throws IOException{

        String src = "rembrandt.jpg";
        int k =20;
        int mode = 1;

        Set<Integer> colors = new HashSet<Integer>();
        BufferedImage image = ImageIO.read(new File(src));
        int width = image.getWidth();
        int height = image.getHeight();
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                int pix = image.getRGB(x, y);
                colors.add(pix);
            }
        }
        System.out.println("There are "+colors.size()+" colors in the original image, after clustering we have "+ k +" colors.");
        KMeansClustering kmeans = new KMeansClustering();
        BufferedImage dstImage = kmeans.calculate(loadImage(src),
                k,mode);
        JFrame frame = new JFrame();

        JLabel labelimg = new JLabel(new ImageIcon(dstImage));
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(labelimg);

        frame.add(mainPanel);
        frame.setVisible(true);
        frame.setSize(1000, 900);

    }
}