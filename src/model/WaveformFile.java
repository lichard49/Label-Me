package model;

import javafx.scene.chart.XYChart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by richard on 7/24/16.
 */
public class WaveformFile {
    private List<XYChart.Data<Float, Float>> coordinates;
    private int numColumns;
    private int numLines;
    private String filename;

    private Duration offsetTime;
    private List<String> columnHeaders;

    public WaveformFile(File file) throws IOException, ParseException, NumberFormatException {
        filename = file.getName();
        offsetTime = Duration.ZERO;

        int numColumns = -1;
        int lineNumber = 0;
        coordinates = new LinkedList<>();

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        while ((line = br.readLine()) != null) {
            String[] lineParts = line.split(",");
            if(numColumns == -1) {
                numColumns = lineParts.length;
                columnHeaders = Arrays.asList(lineParts);
            } else if(lineParts.length != numColumns) {
                if(!(lineParts.length == 1 && lineParts[0].trim().length() == 0)) {
                    throw new ParseException("Wrong number of columns on line " + lineNumber + " expected " + numColumns
                            + " but found " + lineParts.length, lineNumber);
                }
            } else {
                coordinates.add(new XYChart.Data<>(Float.parseFloat(lineParts[0]), Float.parseFloat(lineParts[1])));
            }
        }
    }

    public List<XYChart.Data<Float, Float>> getCoordinates() {
        return coordinates;
    }

    public String getFilename() {
        return filename;
    }

    public String getFormattedOffsetTime() {
        return offsetTime.toString();
    }

    public int getNumColumns() {
        return numColumns;
    }

    public List<String> getColumnHeaders() {
        return columnHeaders;
    }
}
