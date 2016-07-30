package model;

import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.effect.FloatMap;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
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
    private String filename;
    private String timeColumn;

    private Duration offsetTime;
    private Duration previousOffsetTime;
    private Map<String, XYSeries> waveforms;
    private List<XYChart.Data<Float, Float>> labels;

    public WaveformFile(File file, String timeColumn) throws IOException, ParseException, NumberFormatException,
            IndexOutOfBoundsException {
        filename = file.getName();
        int numColumns = -1;
        int lineNumber = 0;
        this.timeColumn = timeColumn;

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        Map<Integer, String> indexToColumnHeader = new HashMap<>();
        int timeColumnIndex = -1;
        waveforms = new HashMap<>();

        while ((line = br.readLine()) != null) {
            String[] lineParts = line.split(",");
            if(numColumns == -1) {
                numColumns = lineParts.length;
                for(int i = 0; i < lineParts.length; i++) {
                    String columnHeader = lineParts[i];
                    waveforms.put(columnHeader, new XYSeries(columnHeader));
                    indexToColumnHeader.put(i, columnHeader);
                    if(columnHeader.equals(timeColumn)) {
                        timeColumnIndex = i;
                    }
                }
            } else if(lineParts.length != numColumns) {
                if(!(lineParts.length == 1 && lineParts[0].trim().length() == 0)) {
                    throw new ParseException("Wrong number of columns on line " + lineNumber + " expected " + numColumns
                            + " but found " + lineParts.length, lineNumber);
                }
            } else {
                if(timeColumnIndex < 0 || timeColumnIndex >= lineParts.length) {
                    throw new IndexOutOfBoundsException("Time column index " + timeColumnIndex + " is invalid");
                }
                float x = Float.parseFloat(lineParts[timeColumnIndex]);
                for(int i = 0; i < numColumns; i++) {
                    float y = Float.parseFloat(lineParts[i]);
                    waveforms.get(indexToColumnHeader.get(i)).add(x, y);
                }
            }
        }

        labels = new LinkedList<>();
        offsetTime = Duration.ZERO;
        previousOffsetTime = Duration.ZERO;
    }

    public XYSeries getWaveform(String column, Pane rootPane) throws InvalidKeyException {
        if(!waveforms.containsKey(column)) {
            throw new InvalidKeyException("No such column " + column);
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

        /*
        for(MarkeredLineChart waveform : waveforms.values()) {
            XYChart.Series series = waveform.getData().get(0); // each line chart should only have one series
            for(int i = 0; i < series.getData().size(); i++) {
                XYChart.Data point = (XYChart.Data) series.getData().get(i);
                point.setXValue((Float) point.getXValue() + deltaOffset);
            }
        }
        */
        // TODO figure out implementation for labeling
    }

    public void addLabel(XYChart.Data<Float, Float> label) {
        labels.add(label);
        /*
        for(MarkeredLineChart waveform : waveforms.values()) {
            waveform.addVerticalRangeMarker(label);
            waveform.layoutPlotChildren();
        }
        */
        // TODO figure out implementation for labeling
    }

    public void setTimeColumn(String column) throws InvalidKeyException {
        if(!getColumnHeaders().contains(column)) {
            throw new InvalidKeyException(column + " is not an option for time column.");
        }
        timeColumn = column;
        // TODO update all waveforms
    }

    public XYSeries getTimeColumn() {
        return getColumn(timeColumn);
    }

    public XYSeries getColumn(String column) {
        return waveforms.get(column);
    }

    public String getFilename() {
        return filename;
    }

    public String getFormattedOffsetTime() {
        return String.valueOf(offsetTime.getSeconds());
    }

    public int getNumColumns() {
        return waveforms.size();
    }

    public Set<String> getColumnHeaders() {
        return waveforms.keySet();
    }
}
