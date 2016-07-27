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
    private ContextMenu waveformListContextMenu;
    private List<XYChart.Data<Float, Float>> labelList;

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
        waveformListContextMenu = new ContextMenu();
        MenuItem addLabel = new MenuItem("Add label");
        addLabel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // Create the custom dialog.
                Dialog<XYChart.Data<Float, Float>> dialog = new Dialog<>();
                dialog.setTitle("Add a label");
                dialog.setHeaderText("Select start/end times for the label");

                ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(20, 150, 10, 10));

                TextField startTime = new TextField();
                startTime.setPromptText("Start time (seconds)");
                TextField endTime = new TextField();
                endTime.setPromptText("End time (seconds)");

                grid.add(new Label("Start time (seconds):"), 0, 0);
                grid.add(startTime, 1, 0);
                grid.add(new Label("End time (seconds):"), 0, 1);
                grid.add(endTime, 1, 1);

                // TODO validation on start/end times

                Platform.runLater(() -> startTime.requestFocus());
                dialog.getDialogPane().setContent(grid);
                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == addButton) {
                        return new XYChart.Data<>(Float.parseFloat(startTime.getText()), Float.parseFloat(endTime.getText()));
                    }
                    return null;
                });

                Optional<XYChart.Data<Float, Float>> result = dialog.showAndWait();
                if(result.isPresent()) {
                    labelList.add(result.get());
                    for(WaveformFile waveformFile : waveformFiles.values()) {
                        waveformFile.addLabel(result.get());
                    }
                }
            }
        });
        waveformListContextMenu.getItems().add(addLabel);
        waveformList.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(event.getButton() == MouseButton.PRIMARY) {
                    if(waveformListContextMenu.isShowing()) {
                        waveformListContextMenu.hide();
                    } else {
                        // TODO move waveform time ticker
                    }
                } else if(event.getButton() == MouseButton.SECONDARY) {
                    waveformListContextMenu.show(waveformList, event.getScreenX(), event.getScreenY());
                }
            }
        });

        // TODO offset time is a WaveformFile level property, so maintain a list here with global time and make each WaveformFile map a global label to a post offset label
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
