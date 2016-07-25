package view;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
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
public class MarkeredLineChart<X, Y> extends LineChart<X, Y> {
    private ObservableList<XYChart.Data<X, Y>> verticalMarkers;
    private ObservableList<XYChart.Data<X, X>> verticalRangeMarkers;

    public MarkeredLineChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
        verticalRangeMarkers = FXCollections.observableArrayList(data -> new Observable[] {data.XValueProperty()});
        verticalRangeMarkers = FXCollections.observableArrayList(data -> new Observable[] {data.YValueProperty()}); // 2nd type of the range is X type as well
        verticalRangeMarkers.addListener((InvalidationListener) observable -> layoutPlotChildren());

        verticalMarkers = FXCollections.observableArrayList(data -> new Observable[] {data.XValueProperty()});
        verticalMarkers.addListener((InvalidationListener)observable -> layoutPlotChildren());
    }

    public void addVerticalValueMarker(Data<X, Y> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (verticalMarkers.contains(marker)) return;
        Line line = new Line();
        marker.setNode(line);
        getPlotChildren().add(line);
        verticalMarkers.add(marker);
    }

    public void addVerticalRangeMarker(XYChart.Data<X, X> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (verticalRangeMarkers.contains(marker)) return;

        Rectangle rectangle = new Rectangle(0,0,0,0);
        rectangle.setStroke(Color.TRANSPARENT);
        rectangle.setFill(Color.BLUE.deriveColor(1, 1, 1, 0.2));

        marker.setNode( rectangle);

        getPlotChildren().add(rectangle);
        verticalRangeMarkers.add(marker);
    }

    public void removeVerticalRangeMarker(XYChart.Data<X, X> marker) {
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

        for (Data<X, Y> verticalMarker : verticalMarkers) {
            Line line = (Line) verticalMarker.getNode();
            line.setStartX(getXAxis().getDisplayPosition(verticalMarker.getXValue()) + 0.5);  // 0.5 for crispness
            line.setEndX(line.getStartX());
            line.setStartY(0d);
            line.setEndY(getBoundsInLocal().getHeight());
            line.toFront();
        }

        for (XYChart.Data<X, X> verticalRangeMarker : verticalRangeMarkers) {
            Rectangle rectangle = (Rectangle) verticalRangeMarker.getNode();
            rectangle.setX( getXAxis().getDisplayPosition(verticalRangeMarker.getXValue()) + 0.5);  // 0.5 for crispness
            rectangle.setWidth( getXAxis().getDisplayPosition(verticalRangeMarker.getYValue()) - getXAxis().getDisplayPosition(verticalRangeMarker.getXValue()));
            rectangle.setY(0d);
            rectangle.setHeight(getBoundsInLocal().getHeight());
            rectangle.toBack();
        }
    }
}
