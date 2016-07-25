package app;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // style media view
        mediaView.fitWidthProperty().bind(mediaViewContainer.prefWidthProperty());
        mediaView.fitHeightProperty().bind(mediaViewContainer.prefHeightProperty());
        mediaView.setPreserveRatio(true);


        TreeItem<String> rootItem = new TreeItem<>("Resources");
        rootItem.setExpanded(true);
        for(int i = 0; i < 6; i++) {
            TreeItem<String> fileItem = new TreeItem<>("File " + i);
            TreeItem<String> resourceTimeItem = new TreeItem<>("00:00");
            fileItem.getChildren().add(resourceTimeItem);
            fileItem.setExpanded(true);
            for(int j = 0; j < 3; j++) {
                CheckBoxTreeItem<String> columnItem = new CheckBoxTreeItem<>("Column " + j);
                fileItem.getChildren().add(columnItem);
            }
            rootItem.getChildren().add(fileItem);
        }
        resourceTree.setRoot(rootItem);
        resourceTree.setEditable(true);
        resourceTree.setCellFactory(new Callback<TreeView<String>, MixedTreeCell>() {
            @Override
            public MixedTreeCell call(TreeView<String> param) {
                return new MixedTreeCell();
            }
        });
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
    }

    private void insertWaveform(List<XYChart.Data<Float, Float>> coordinates) {
        LineChart<Number, Number> waveform = new LineChart<>(new NumberAxis(), new NumberAxis());
        XYChart.Series series = new XYChart.Series();

        for(XYChart.Data<Float, Float> coordinate : coordinates) {
            series.getData().add(coordinate);
        }

        waveform.setLegendVisible(false);
        waveform.getData().add(series);
        waveform.prefWidthProperty().bind(rootPane.widthProperty());
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

    @FXML
    private void openWaveformFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Waveform File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File waveformFile = fileChooser.showOpenDialog(stage);

        try (BufferedReader br = new BufferedReader(new FileReader(waveformFile))) {
            String line;
            int numColumns = -1;
            int lineNumber = 0;
            List<XYChart.Data<Float, Float>> coordinates = new LinkedList<>();

            while ((line = br.readLine()) != null) {
                String[] lineParts = line.split(",");
                if(numColumns == -1) {
                    numColumns = lineParts.length;

                    // TODO handle header names
                } else if(lineParts.length != numColumns) {
                    throw new ParseException("Wrong number of columns on line " + lineNumber, lineNumber);
                } else {
                    coordinates.add(new XYChart.Data<>(Float.parseFloat(lineParts[0]), Float.parseFloat(lineParts[1])));
                }
            }
            insertWaveform(coordinates);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}
