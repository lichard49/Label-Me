package model;

import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.effect.FloatMap;
import javafx.scene.layout.FlowPane;
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
    private static final NumberAxis X_AXIS = new NumberAxis();

    private Map<String, List<Float>> coordinates;
    private int numLines;
    private String filename;
    private String timeColumn;

    private Duration offsetTime;
    private Duration previousOffsetTime;
    private Map<String, MarkeredLineChart> waveforms;
    private List<XYChart.Data<Float, Float>> labels;

    public WaveformFile(File file) throws IOException, ParseException, NumberFormatException {
        filename = file.getName();
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
        labels = new LinkedList<>();
        offsetTime = Duration.ZERO;
        previousOffsetTime = Duration.ZERO;
    }

    public static NumberAxis getXAxis() {
        return X_AXIS;
    }

    public MarkeredLineChart getWaveform(String column, Pane rootPane) {
        if(!waveforms.containsKey(column)) {
            MarkeredLineChart waveform = new MarkeredLineChart(X_AXIS, new NumberAxis());
            XYChart.Series series = new XYChart.Series();

            for(int i = 0; i < getColumn(timeColumn).size(); i++) {
                series.getData().add(new XYChart.Data<>(getColumn(timeColumn).get(i)+offsetTime.getSeconds(),
                        getColumn(column).get(i)));
            }

            waveform.setLegendVisible(false);
            waveform.getData().add(series);
            waveform.setPrefHeight(225);
            waveform.prefWidthProperty().bind(rootPane.widthProperty().subtract(265));
            for(XYChart.Data<Float, Float> label : labels) {
                waveform.addVerticalRangeMarker(label);
            }
            waveforms.put(column, waveform);
        }
        return waveforms.get(column);
    }

    public void setOffsetTime(float seconds) {
        if(offsetTime != null) {
            previousOffsetTime = offsetTime;
        } else {
            previousOffsetTime = Duration.ZERO;
        }
        offsetTime = Duration.ZERO.plusMillis((int)(seconds*1000));
        float deltaOffset = offsetTime.getSeconds() - previousOffsetTime.getSeconds();

        for(MarkeredLineChart waveform : waveforms.values()) {
            XYChart.Series series = waveform.getData().get(0); // each line chart should only have one series
            for(int i = 0; i < series.getData().size(); i++) {
                XYChart.Data point = (XYChart.Data) series.getData().get(i);
                point.setXValue((Float) point.getXValue() + deltaOffset);
            }
        }
    }

    public void addLabel(XYChart.Data<Float, Float> label) {
        labels.add(label);
        for(MarkeredLineChart waveform : waveforms.values()) {
            waveform.addVerticalRangeMarker(label);
            waveform.layoutPlotChildren();
        }
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
        return String.valueOf(offsetTime.getSeconds());
    }

    public int getNumColumns() {
        return coordinates.size();
    }

    public Set<String> getColumnHeaders() {
        return coordinates.keySet();
    }
}
