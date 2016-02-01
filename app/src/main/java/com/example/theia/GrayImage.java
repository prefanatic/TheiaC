package com.example.theia;

public class GrayImage {
    public byte[] data;
    public int width;
    public int height;

    public GrayImage() {
    }

    public GrayImage(int dataSize) {
        data = new byte[dataSize];
    }
}
