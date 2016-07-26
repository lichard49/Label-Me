package controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
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
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.util.*;

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

    private Map<String, WaveformFile> waveformFiles;

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
        resourceTree.getRoot().addEventHandler(CheckBoxTreeItem.checkBoxSelectionChangedEvent(),
                new EventHandler<CheckBoxTreeItem.TreeModificationEvent<Object>>() {
            @Override
            public void handle(CheckBoxTreeItem.TreeModificationEvent<Object> event) {
                String selectedFilename = (String) event.getTreeItem().getParent().getValue();
                String selectedColumnHeader = (String) event.getTreeItem().getValue();
                WaveformFile selectedWaveform = waveformFiles.get(selectedFilename);
                insertWaveform(selectedWaveform.getTimeColumn(), selectedWaveform.getColumn(selectedColumnHeader));
            }
        });

        waveformFiles = new LinkedHashMap<>();
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

        for(Node node : waveformList.getChildren()) {
            MarkeredLineChart<Number, Number> waveform = (MarkeredLineChart) node;
            waveform.updateTime(mediaPlayer.getCurrentTime().toSeconds());
            waveform.layoutPlotChildren();
        }
    }

    private void insertWaveform(List<Float> x, List<Float> y) {
        if(x.size() != y.size()) {
            return;
        }
        MarkeredLineChart<Number, Number> waveform = new MarkeredLineChart<>(new NumberAxis(), new NumberAxis());
        XYChart.Series series = new XYChart.Series();

        for(int i = 0; i < x.size(); i++) {
            series.getData().add(new XYChart.Data<>(x.get(i), y.get(i)));
        }

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

        if(videoFile != null) {
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

        if(file != null) {
            try {
                WaveformFile waveformFile = new WaveformFile(file);
                String timeColumn = chooseTimeColumn(waveformFile);
                if (timeColumn != null) {
                    try {
                        waveformFile.setTimeColumn(timeColumn);
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    }
                    waveformFiles.put(waveformFile.getFilename(), waveformFile);
                    addWaveformToResourceTree(waveformFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private String chooseTimeColumn(WaveformFile waveformFile) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(waveformFile.getColumnHeaders().iterator().next(),
                waveformFile.getColumnHeaders());
        dialog.setTitle("Importing Waveform File");
        dialog.setHeaderText("Importing " + waveformFile.getFilename());
        dialog.setContentText("Choose time column: ");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            return result.get();
        }
        return null;
    }
}
