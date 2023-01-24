package com.example.pulsarworkshop.utilities;

import org.apache.pulsar.shade.org.apache.commons.io.FileUtils;
import org.apache.pulsar.shade.org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class CsvFileLineScanner {

    private File csvFile;

    private LineIterator lineIterator;

    public CsvFileLineScanner(File file) throws IOException  {
        this.csvFile = file;
        this.lineIterator = FileUtils.lineIterator(csvFile, "UTF-8");
    }

    public boolean hasNextLine() {
        return lineIterator.hasNext();
    }

    public String getNextLine() {
        return lineIterator.nextLine();
    }

    public void close() throws IOException {
        if (lineIterator != null) {
            lineIterator.close();
        }
    }

}
