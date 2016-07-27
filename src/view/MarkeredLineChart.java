package view;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.Objects;

/**
 * Nearly entirely comes from: http://stackoverflow.com/a/28955561
 *
 * Created by richard on 7/25/16.
 */
public class MarkeredLineChart extends LineChart<Number, Number> {
    private XYChart.Data<Float, Float> timeMarker;
    private ObservableList<XYChart.Data<Float, Float>> verticalRangeMarkers;

    public MarkeredLineChart(NumberAxis xAxis, NumberAxis yAxis) {
        super(xAxis, yAxis);
        verticalRangeMarkers = FXCollections.observableArrayList(data -> new Observable[] {data.XValueProperty()});
        verticalRangeMarkers = FXCollections.observableArrayList(data -> new Observable[] {data.YValueProperty()}); // 2nd type of the range is X type as well
        verticalRangeMarkers.addListener((InvalidationListener) observable -> layoutPlotChildren());

        timeMarker = new XYChart.Data(0, 0);
        Line line = new Line();
        timeMarker.setNode(line);
        getPlotChildren().add(line);
    }

    public void updateTime(Float time) {
        timeMarker.setXValue(time);
    }

    public void setVerticalRangeMarkersOffset(float deltaSeconds) {
        for(XYChart.Data marker : verticalRangeMarkers) {
            marker.setXValue((Float) marker.getXValue() + deltaSeconds);
            marker.setYValue((Float) marker.getYValue() + deltaSeconds);
        }
    }

    public void addVerticalRangeMarker(XYChart.Data<Float, Float> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (verticalRangeMarkers.contains(marker)) return;

        Rectangle rectangle = new Rectangle(0,0,0,0);
        rectangle.setStroke(Color.TRANSPARENT);
        rectangle.setFill(Color.BLUE.deriveColor(1, 1, 1, 0.2));

        marker.setNode( rectangle);

        getPlotChildren().add(rectangle);
        verticalRangeMarkers.add(marker);
    }

    public void removeVerticalRangeMarker(XYChart.Data<Float, Float> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (marker.getNode() != null) {
            getPlotChildren().remove(marker.getNode());
            marker.setNode(null);
        }
        verticalRangeMarkers.remove(marker);
    }

    @Override
    public void layoutPlotChildren() {
        super.layoutPlotChildren();

        Line line = (Line) timeMarker.getNode();
        line.setStartX(getXAxis().getDisplayPosition(timeMarker.getXValue()) + 0.5);  // 0.5 for crispness
        line.setEndX(line.getStartX());
        line.setStartY(0d);
        line.setEndY(getBoundsInLocal().getHeight());
        line.toFront();

        for (XYChart.Data<Float, Float> verticalRangeMarker : verticalRangeMarkers) {
            Rectangle rectangle = (Rectangle) verticalRangeMarker.getNode();
            rectangle.setX(getXAxis().getDisplayPosition(verticalRangeMarker.getXValue()) + 0.5);  // 0.5 for crispness
            rectangle.setWidth(getXAxis().getDisplayPosition(verticalRangeMarker.getYValue()) - getXAxis().getDisplayPosition(verticalRangeMarker.getXValue()));
            rectangle.setY(0d);
            rectangle.setHeight(getBoundsInLocal().getHeight());
            rectangle.toBack();
        }
    }
}
