package model;

import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import view.MarkeredLineChart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.time.Duration;
import java.util.*;

/**
 * Created by richard on 7/24/16.
 */
public class WaveformFile {
    private Map<String, List<Float>> coordinates;
    private int numLines;
    private String filename;
    private String timeColumn;

    private Duration offsetTime;
    private Map<String, MarkeredLineChart> waveforms;

    public WaveformFile(File file) throws IOException, ParseException, NumberFormatException {
        filename = file.getName();
        offsetTime = Duration.ZERO;

        int numColumns = -1;
        int lineNumber = 0;
        coordinates = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        while ((line = br.readLine()) != null) {
            String[] lineParts = line.split(",");
            if(numColumns == -1) {
                numColumns = lineParts.length;
                for(String columnHeader : lineParts) {
                    coordinates.put(columnHeader, new LinkedList<>());
                }
            } else if(lineParts.length != numColumns) {
                if(!(lineParts.length == 1 && lineParts[0].trim().length() == 0)) {
                    throw new ParseException("Wrong number of columns on line " + lineNumber + " expected " + numColumns
                            + " but found " + lineParts.length, lineNumber);
                }
            } else {
                Iterator<String> headers = getColumnHeaders().iterator();
                int index = 0;
                while(headers.hasNext()) {
                    coordinates.get(headers.next()).add(Float.parseFloat(lineParts[index++]));
                }
            }
        }

        waveforms = new HashMap<>();
    }

    public MarkeredLineChart getWaveform(String column, Pane rootPane) {
        if(!waveforms.containsKey(column)) {
            MarkeredLineChart<Number, Number> waveform = new MarkeredLineChart<>(new NumberAxis(), new NumberAxis());
            XYChart.Series series = new XYChart.Series();

            for(int i = 0; i < getColumn(timeColumn).size(); i++) {
                series.getData().add(new XYChart.Data<>(getColumn(timeColumn).get(i), getColumn(column).get(i)));
            }

            waveform.setLegendVisible(false);
            waveform.getData().add(series);
            waveform.setPrefHeight(225);
            waveform.prefWidthProperty().bind(rootPane.widthProperty().subtract(265));
            waveforms.put(column, waveform);
        }
        return waveforms.get(column);
    }

    public void setTimeColumn(String column) throws InvalidKeyException {
        if(!getColumnHeaders().contains(column)) {
            throw new InvalidKeyException(column + " is not an option for time column.");
        }
        timeColumn = column;
    }

    public List<Float> getTimeColumn() {
        return getColumn(timeColumn);
    }

    public List<Float> getColumn(String column) {
        return coordinates.get(column);
    }

    public String getFilename() {
        return filename;
    }

    public String getFormattedOffsetTime() {
        return offsetTime.toString();
    }

    public int getNumColumns() {
        return coordinates.size();
    }

    public Set<String> getColumnHeaders() {
        return coordinates.keySet();
    }
}
