package controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import model.WaveformFile;
import view.MarkeredLineChart;
import view.MixedTreeCell;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private final Duration STEP_DURATION = Duration.millis(1000);

    @FXML private BorderPane rootPane;
    @FXML private BorderPane mediaViewContainer;
    @FXML private MediaView mediaView;
    @FXML private Text videoTime;
    @FXML private VBox waveformList;
    @FXML private TreeView resourceTree;

    private Stage stage;
    private MediaPlayer mediaPlayer;

    private List<WaveformFile> waveformFiles;

    private XYChart.Data<Number, Number> timeMarker;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // style media view
        mediaView.fitWidthProperty().bind(mediaViewContainer.prefWidthProperty());
        mediaView.fitHeightProperty().bind(mediaViewContainer.prefHeightProperty());
        mediaView.setPreserveRatio(true);


        TreeItem<String> rootItem = new TreeItem<>("Resources");
        rootItem.setExpanded(true);
        resourceTree.setRoot(rootItem);
        resourceTree.setEditable(true);
        resourceTree.setCellFactory(new Callback<TreeView<String>, MixedTreeCell>() {
            @Override
            public MixedTreeCell call(TreeView<String> param) {
                return new MixedTreeCell();
            }
        });

        waveformFiles = new LinkedList<>();

        timeMarker = new XYChart.Data<>(0, 0);
    }

    protected void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void playVideo() {
        mediaPlayer.play();
    }

    @FXML
    private void pauseVideo() {
        mediaPlayer.pause();
    }

    @FXML
    private void stepForwardVideo() {
        mediaPlayer.seek(mediaPlayer.getCurrentTime().add(STEP_DURATION));
        updateVideoTime();
    }

    @FXML
    private void stepBackwardVideo() {
        mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(STEP_DURATION));
        updateVideoTime();
    }

    private void updateVideoTime() {
        videoTime.setText(String.format("%1$.3f / %2$.3f",
                mediaPlayer.getCurrentTime().toSeconds(),
                mediaPlayer.getTotalDuration().toSeconds()));

        timeMarker.setXValue(mediaPlayer.getCurrentTime().toSeconds());

        for(Node node : waveformList.getChildren()) {
            MarkeredLineChart<Number, Number> waveform = (MarkeredLineChart) node;
            waveform.layoutPlotChildren();
        }
    }

    private void insertWaveform(List<XYChart.Data<Float, Float>> coordinates) {
        MarkeredLineChart<Number, Number> waveform = new MarkeredLineChart<>(new NumberAxis(), new NumberAxis());
        XYChart.Series series = new XYChart.Series();

        for(XYChart.Data<Float, Float> coordinate : coordinates) {
            series.getData().add(coordinate);
        }

        waveform.addVerticalValueMarker(timeMarker);

        waveform.setLegendVisible(false);
        waveform.getData().add(series);
        waveform.setPrefHeight(225);
        waveform.prefWidthProperty().bind(rootPane.widthProperty().subtract(265));
        waveformList.getChildren().add(waveform);
    }

    @FXML
    private void openVideoFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Video File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP4", "*.mp4"));
        File videoFile = fileChooser.showOpenDialog(stage);

        final Media videoMedia = new Media(videoFile.toURI().toString());
        mediaPlayer = new MediaPlayer(videoMedia);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
                updateVideoTime();
            }
        });

    }

    private void addWaveformToResourceTree(WaveformFile waveformFile) {
        TreeItem<String> fileItem = new TreeItem<>(waveformFile.getFilename());
        TreeItem<String> resourceTimeItem = new TreeItem<>(waveformFile.getFormattedOffsetTime());
        fileItem.getChildren().add(resourceTimeItem);

        fileItem.setExpanded(true);
        for(String columnHeader : waveformFile.getColumnHeaders()) {
            CheckBoxTreeItem<String> columnName = new CheckBoxTreeItem<>(columnHeader);
            fileItem.getChildren().add(columnName);
        }
        resourceTree.getRoot().getChildren().add(fileItem);
    }

    @FXML
    private void openWaveformFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Waveform File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = fileChooser.showOpenDialog(stage);

        try {
            WaveformFile waveformFile = new WaveformFile(file);
            waveformFiles.add(waveformFile);
            insertWaveform(waveformFile.getCoordinates());
            addWaveformToResourceTree(waveformFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}
