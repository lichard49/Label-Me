package controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.Pair;
import model.WaveformFile;
import view.MarkeredLineChart;
import view.MixedTreeCell;
import view.UserInterfaceElements;

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
    private UserInterfaceElements ui;

    private Map<String, WaveformFile> waveformFiles;
    private List<XYChart.Data<Float, Float>> labelList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // style media view
        mediaView.fitWidthProperty().bind(mediaViewContainer.prefWidthProperty());
        mediaView.fitHeightProperty().bind(mediaViewContainer.prefHeightProperty());
        mediaView.setPreserveRatio(true);

        ui = UserInterfaceElements.getInstance(this);

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
                if(event.getTreeItem().isSelected()) {
                    insertWaveform(selectedColumnHeader, selectedWaveform);
                } else {
                    removeWaveform(selectedColumnHeader, selectedWaveform);
                }
            }
        });
        resourceTree.getRoot().addEventHandler(TreeItem.valueChangedEvent(), new EventHandler<TreeItem.TreeModificationEvent<Object>>() {
            @Override
            public void handle(TreeItem.TreeModificationEvent<Object> event) {
                String selectedFilename = (String) event.getTreeItem().getParent().getValue();
                waveformFiles.get(selectedFilename).setOffsetTime(Float.parseFloat(
                        event.getTreeItem().getValue().toString()));
            }
        });

        waveformFiles = new LinkedHashMap<>();
        labelList = new LinkedList<>();

        waveformList.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(event.getButton() == MouseButton.PRIMARY) {
                    if(ui.getWaveformListContextMenu().isShowing()) {
                        ui.getWaveformListContextMenu().hide();
                    } else {
                        // TODO move waveform time ticker
                    }
                } else if(event.getButton() == MouseButton.SECONDARY) {
                    double localX = WaveformFile.getXAxis().sceneToLocal(event.getSceneX(), event.getY()).getX();
                    double time = WaveformFile.getXAxis().getValueForDisplay(localX).doubleValue();
                    XYChart.Data<Float, Float> selectedLabel = null;
                    for(XYChart.Data<Float, Float> label : labelList) {
                        if(time >= label.getXValue() && time <= label.getYValue()) {
                            selectedLabel = label;
                            break;
                        }
                    }
                    ui.getWaveformListContextMenu(selectedLabel).show(waveformList, event.getScreenX(),
                            event.getScreenY());
                }
            }
        });
    }

    public void addLabel(XYChart.Data<Float, Float> label) {
        labelList.add(label);
        for(WaveformFile waveformFile : waveformFiles.values()) {
            waveformFile.addLabel(label);
        }
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
            MarkeredLineChart waveform = (MarkeredLineChart) node;
            waveform.updateTime((float) mediaPlayer.getCurrentTime().toSeconds());
            waveform.layoutPlotChildren();
        }
    }

    private void insertWaveform(String column, WaveformFile waveformFile) {
        waveformList.getChildren().add(waveformFile.getWaveform(column, rootPane));
    }

    private void removeWaveform(String column, WaveformFile waveformFile) {
        waveformList.getChildren().remove(waveformFile.getWaveform(column, rootPane));
    }

    @FXML
    private void openVideoFile() {
        File videoFile = ui.getVideoFileChooser().showOpenDialog(stage);

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
        File file = ui.getWaveformFileChooser().showOpenDialog(stage);

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
                    for(XYChart.Data<Float, Float> label : labelList) {
                        waveformFile.addLabel(label);
                    }
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
        Optional<String> result = ui.getChooseTimeColumnDialog(waveformFile).showAndWait();
        if (result.isPresent()){
            return result.get();
        }
        return null;
    }
}
