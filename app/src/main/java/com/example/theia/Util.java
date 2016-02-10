package com.example.theia;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Size;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class Util {
    public static GrayImage[] storedImages = new GrayImage[5];

    public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new AreaComparator());
        } else {
            Timber.e("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static void imageToGrayImage(Image image, GrayImage grayImage) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data;

        if (grayImage.data != null && grayImage.data.length == buffer.remaining()) {
            data = grayImage.data;
        } else {
            data = new byte[buffer.remaining()];
        }

        buffer.get(data);
        grayImage.data = data;
        grayImage.width = image.getWidth();
        grayImage.height = image.getHeight();
    }

    private static void shiftDownPool() {
        for (int i = 0; i < storedImages.length - 1; i++) {
            System.arraycopy(storedImages[i + 1].data, 0, storedImages[i].data, 0, storedImages[i + 1].data.length);
        }
    }

    private static void initPool(int dataSize) {
        for (int i = 0; i < storedImages.length; i++) {
            storedImages[i] = new GrayImage(dataSize);
        }
    }

    public static void addToPool(GrayImage image) {
        if (storedImages[storedImages.length - 1] == null) {
            initPool(image.data.length);
        } else {
            shiftDownPool();
        }

        GrayImage stored = storedImages[storedImages.length - 1];
        System.arraycopy(image.data, 0, stored.data, 0, image.data.length);
    }

    public static void subtractFromAveragedPool(GrayImage image) {
        int[] storage = new int[image.data.length];
        byte[] byteStorage = new byte[image.data.length];

        for (int i = 0; i < storedImages.length; i++) {
            for (int p = 0; p < storedImages[i].data.length; p++) {
                storage[p] += storedImages[i].data[p];
            }
        }

        for (int i = 0; i < storage.length; i++) {
            byteStorage[i] = clampToGrayscale(storage[i] / storedImages.length);
        }

        GrayImage sub = new GrayImage();
        sub.data = byteStorage;

        subtract(image, sub);
    }

    public static void thresholdDifferenceFromPool(GrayImage image, int poolIndex) {
        if (poolIndex > storedImages.length - 1) return;
    }

    public static void thresholdDifference(GrayImage currentImage, GrayImage previousImage) {
        int index;
        for (int h = 0; h < currentImage.height; h++) {
            index = h * currentImage.width;
            for (int w = 0; w < currentImage.width; w++) {

            }
        }
    }

    public static void subtractFromPool(GrayImage image, int poolIndex) {
        if (poolIndex > storedImages.length - 1) return;

        subtract(image, storedImages[poolIndex]);
    }

    private static int BELT_SIZE = 16;
    private static int maximumPixelCountThreshold = 128000;
    private static byte constant0xFF = (byte) 0xFF;
    private static float[][] LOWPASS_KERNEL = new float[][]{
            {1 / 9, 1 / 9, 1 / 9},
            {1 / 9, 1 / 9, 1 / 9},
            {1 / 9, 1 / 9, 1 / 9}
    };

    public static void detectionPainter(int threshold, GrayImage current, GrayImage background, Bitmap output, byte[] storage, byte[] heightCollapseMap, boolean[] beltMap) {
        int i, h, w, m, n;
        float sum;
        byte val;
        int bitmapIndex = 0;
        int pixelCountAboveThreshold = 0;

        for (h = 0; h < current.height; h++) {
            i = h * current.width;
            for (w = 0; w < current.width; w++) {
                if (Math.abs((0xFF & current.data[i + w]) - (0xFF & background.data[i + w])) >= threshold) {
                    val = constant0xFF;
                    pixelCountAboveThreshold++;
                } else {
                    val = 0;
                }

                current.data[i + w] = val;
            }
        }

        for (h = 0; h < current.height; h++) {
            i = h * current.width;
            for (w = 0; w < current.width; w++) {
                sum = 0f;

                if (w != 0 && h != 0 && w != current.width - 1 && h != current.height - 1) {
                    sum = 1 / 9f * (
                            current.data[i + w - 1 - current.width] + current.data[i + w - current.width] + current.data[i + w + 1 - current.width] +
                                    current.data[i + w - 1] + current.data[i + w] + current.data[i + w + 1] +
                                    current.data[i + w - 1 + current.width] + current.data[i + w + current.width] + current.data[i + w + 1 + current.width]
                    );
                } else {
                    sum = current.data[i + w];
                }

                val = (byte) sum;
                storage[bitmapIndex++] = val;
                storage[bitmapIndex++] = val;
                storage[bitmapIndex++] = val;
                storage[bitmapIndex++] = constant0xFF;

            }
        }

        // Loop through each belt chunk
        int a = current.width / BELT_SIZE;
        int chunkBrightness;
        int chunkCollector = 0;
        for (int chunk = 0; chunk < BELT_SIZE; chunk++) {
            chunkBrightness = 0;

            // Loop through the width of the belt chunk
            for (w = (chunk * a); w < (chunk * a) + a; w++) {
                chunkCollector = 0;

                // Collapse the height of this chunk into one value.
                for (h = 0; h < current.height; h++) {
                    i = (h * current.width) + w;
                    if ((current.data[i] & 0xFF) == 0xFF) {
                        chunkCollector++;
                    }
                }
            }

            beltMap[chunk] = chunkCollector > 80;
            //Timber.d("CHUNK %d == %d", chunk, chunkBrightness);
        }

        output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));

        // Reset our background image if we've detected too much motion.
        if (pixelCountAboveThreshold > maximumPixelCountThreshold)
            background.data = null;
    }

    public static void subtract(GrayImage currentImage, GrayImage previousImage) {
        int index, h, w;

        for (h = 0; h < currentImage.height; h++) {
            index = h * currentImage.width;
            for (w = 0; w < currentImage.width; w++) {

                currentImage.data[index + w] = clampToGrayscale(Math.abs((0xFF & currentImage.data[index + w]) - (0xFF & previousImage.data[index + w])));
            }
        }
    }

    private static byte clampToGrayscale(int val) {
        if (val < 0) return (byte) 0;
        return val > 255 ? (byte) 255 : (byte) val;
    }

    public static void binarize(GrayImage input, int threshold) {
        int index;

        for (int y = 1; y < input.height - 2; y++) {
            index = y * input.width;
            for (int x = 1; x < input.width - 2; x++) {
                input.data[index + x] = clampToGrayscale(input.data[index + x] > threshold ? 255 : 0);
            }
        }
    }

    private static void collapseIntoBelt(GrayImage image) {
        int i, x, y;
        for (y = 0; y < image.height; y++) {
            i = y * image.width;
            for (x = 0; x < image.width; x++) {

            }
        }
    }

    private static float[][] GAUSSIAN_KERNEL;
    private static int radiusLooper = -1;
    private static int radius = 7;
    private static int sigma = 1;

    public static void gaussianBlur(GrayImage input) {
        if (GAUSSIAN_KERNEL == null) {
            generateGaussianKernel();
        }

        int i, y, x, kernX, kernY;
        float sum;
        for (y = radiusLooper; y < input.height - (radiusLooper * 2); y++) {
            i = y * input.width;
            for (x = radiusLooper; x < input.width - (radiusLooper * 2); x++) {
                sum = 0;
                for (kernY = radiusLooper * -1; kernY <= radiusLooper; kernY++) {
                    for (kernX = radiusLooper * -1; kernX <= radiusLooper; kernX++) {
                        sum += input.data[i + x + kernX + (kernY * input.width)] * GAUSSIAN_KERNEL[kernY + radiusLooper][kernX + radiusLooper];
                    }
                }

                input.data[i + x] = clampToGrayscale((int) sum);

            }
        }
    }

    private static void generateGaussianKernel() {
        GAUSSIAN_KERNEL = new float[radius][radius];

        int xx, yy;
        float sum = 0;
        for (int y = 0; y < radius; y++) {
            for (int x = 0; x < radius; x++) {
                xx = x - radius / 2;
                yy = y - radius / 2;
                GAUSSIAN_KERNEL[y][x] = (float) Math.pow(Math.E, -(xx * xx + yy * yy)
                        / (2 * (sigma * sigma)));
                sum += GAUSSIAN_KERNEL[y][x];
            }
        }

        for (int i = 0; i < radius; i++) {
            for (int b = 0; b < radius; b++) {
                GAUSSIAN_KERNEL[i][b] /= sum;
            }
        }

        radiusLooper = radius / 2;
    }

    private static int[][] MEDIAN_KERNEL = new int[][]{
            {1, 1, 1},
            {1, 1, 1},
            {1, 1, 1}
    };

    public static void median(GrayImage input, int passes) {
        int index;
        int kernelIndex = 0;
        int sum = 0;
        byte[] data = new byte[input.data.length];
        byte[] kernelStorage = new byte[9];

        for (int i = 0; i < passes; i++) {
            index = 0;

            for (int y = 1; y < input.height - 2; y++) {
                index = y * input.width;
                for (int x = 1; x < input.width - 2; x++) {
                    kernelIndex = 0;
                    for (int m = -1; m <= 1; m++) {
                        for (int n = -1; n <= 1; n++) {
                            kernelStorage[kernelIndex++] = input.data[index + x + n + (m * input.width)];
                        }
                    }

                    data[index + x] = clampToGrayscale(kernelStorage[4]);
                }
            }

            input.data = data;
        }
    }

    public static void lowPass(GrayImage input) {
        int indexSrc, value, sum, x, y, m, n;

        for (y = 1; y < input.height - 2; y++) {
            indexSrc = 1 + y * input.width;
            for (x = 1; x < input.width - 2; x++) {
                sum = 0;

                for (m = -1; m < 2; m++) {
                    for (n = -1; n < 2; n++) {
                        value = input.data[indexSrc + m + (n * input.width)];

                        sum += value;
                    }
                }

                indexSrc++;
                input.data[indexSrc - 1] = clampToGrayscale((int) (1f / (9f * sum)));
            }
        }
    }

    private static int[][] SOBEL_KERNEL_X = new int[][]{
            {1, 0, -1},
            {2, 0, -2},
            {1, 0, -1}
    };
    private static int[][] SOBEL_KERNEL_Y = new int[][]{
            {1, 2, 1},
            {0, 0, 0},
            {-1, -2, -1}
    };

    //@DebugLog
    public static void sobel(GrayImage input) {
        int indexSrc, value, sumX, sumY;

        for (int y = 1; y < input.height - 2; y++) {
            indexSrc = 1 + y * input.width;
            for (int x = 1; x < input.width - 2; x++) {
                sumX = 0;
                sumY = 0;

                for (int m = -1; m < 2; m++) {
                    for (int n = -1; n < 2; n++) {
                        value = input.data[indexSrc + m + (n * input.width)];

                        sumX += value * SOBEL_KERNEL_X[m + 1][n + 1];
                        //sumY += value * SOBEL_KERNEL_Y[m + 1][n + 1];
                    }
                }

                indexSrc++;

                input.data[indexSrc - 1] = clampToGrayscale(Math.abs(sumX) + Math.abs(sumY));
                //input.data[indexSrc - 1] = (byte) 255;
            }
        }

        //return storage;
    }

    public static void convertToBitmap(GrayImage image, Bitmap output, byte[] storage) {
        convertToBitmap(image.data, output, storage);
    }

    //@DebugLog
    public static void convertToBitmap(byte[] input, Bitmap output, byte[] storage) {
        int indexDst = 0;
        for (int y = 0; y < output.getHeight() - 40; y++) {
            int indexSrc = y * output.getWidth();
            for (int x = 0; x < output.getWidth(); x++) {
                int value = input[indexSrc++];

                storage[indexDst++] = (byte) value;
                storage[indexDst++] = (byte) value;
                storage[indexDst++] = (byte) value;
                storage[indexDst++] = (byte) 0xFF;
            }
        }

        output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
    }

}
