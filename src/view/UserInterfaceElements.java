package view;

import controller.Controller;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import model.WaveformFile;

import java.io.File;
import java.util.Optional;

/**
 * Created by richard on 7/27/16.
 */
public class UserInterfaceElements {

    public static UserInterfaceElements userInterfaceElements = null;
    public static UserInterfaceElements getInstance(Controller controller) {
        if(userInterfaceElements == null) {
            userInterfaceElements = new UserInterfaceElements(controller);
        }
        return userInterfaceElements;
    }

    private Controller controller;
    private Dialog<XYChart.Data<Float, Float>> editLabelDialog;
    private XYChart.Data<Float, Float> selectedLabel;
    private ContextMenu waveformListContextMenu;
    private FileChooser videoFileChooser;
    private FileChooser waveformFileChooser;
    private FileChooser labelFileChooser;
    private ChoiceDialog<String> chooseTimeColumnDialog;

    private UserInterfaceElements(Controller controller) {
        this.controller = controller;

        createEditLabelDialog();
        createWaveformListContextMenu();
        createVideoFileChooser();
        createWaveformFileChooser();
        createLabelFileChooser();
        createChooseTimeColumnDialog();
    }

    private void createEditLabelDialog() {
        editLabelDialog = new Dialog<>();
        editLabelDialog.setTitle("Add a label");
        editLabelDialog.setHeaderText("Select start/end times for the label");

        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        editLabelDialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

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

        editLabelDialog.getDialogPane().setContent(grid);
        editLabelDialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                return new XYChart.Data<>(Float.parseFloat(startTime.getText()), Float.parseFloat(endTime.getText()));
            }
            return null;
        });

    }

    private Optional<XYChart.Data<Float, Float>> showEditLabelDialog(Float start, Float end) {
        GridPane grid = (GridPane) editLabelDialog.getDialogPane().getContent();
        TextField startTime = (TextField) grid.getChildren().get(1);
        TextField endTime = (TextField) grid.getChildren().get(3);

        if(start != null) {
            startTime.setText(start.toString());
        }
        if(end != null) {
            endTime.setText(end.toString());
        }

        return editLabelDialog.showAndWait();
    }

    private void createWaveformListContextMenu() {
        waveformListContextMenu = new ContextMenu();
        MenuItem addLabel = new MenuItem("Add label");
        waveformListContextMenu.getItems().add(addLabel);
        MenuItem editLabel = new MenuItem("Edit label");
        waveformListContextMenu.getItems().add(editLabel);
        MenuItem deleteLabel = new MenuItem("Delete label");
        waveformListContextMenu.getItems().add(deleteLabel);

        addLabel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Optional<XYChart.Data<Float, Float>> result = showEditLabelDialog(selectedLabel.getXValue(), null);
                if(result.isPresent()) {
                    controller.addLabel(result.get());
                }
            }
        });
        editLabel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                XYChart.Data<Float, Float> oldLabel = new XYChart.Data<>(selectedLabel.getXValue(),
                        selectedLabel.getYValue());
                Optional<XYChart.Data<Float, Float>> result = showEditLabelDialog(selectedLabel.getXValue(),
                        selectedLabel.getYValue());
                if(result.isPresent()) {
                    controller.editLabel(oldLabel, result.get());
                }
            }
        });
        deleteLabel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                controller.deleteLabel(selectedLabel);
            }
        });
    }

    public ContextMenu getWaveformListContextMenu() {
        return getWaveformListContextMenu(null);
    }

    public ContextMenu getWaveformListContextMenu(XYChart.Data<Float, Float> label) {
        selectedLabel = label;

        waveformListContextMenu.getItems().get(1).setDisable(label.getYValue() == null);
        waveformListContextMenu.getItems().get(2).setDisable(label.getYValue() == null);

        return waveformListContextMenu;
    }

    private void createVideoFileChooser() {
        videoFileChooser = new FileChooser();
        videoFileChooser.setTitle("Open Video File");
        videoFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP4", "*.mp4"));
    }

    public FileChooser getVideoFileChooser() {
        return videoFileChooser;
    }

    private void createWaveformFileChooser() {
        waveformFileChooser = new FileChooser();
        waveformFileChooser.setTitle("Open Waveform File");
        waveformFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
    }

    public FileChooser getWaveformFileChooser() {
        return waveformFileChooser;
    }

    private void createLabelFileChooser() {
        labelFileChooser = new FileChooser();
        labelFileChooser.setTitle("Save Label File");
        labelFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
    }

    public FileChooser getLabelFileChooser() {
        return labelFileChooser;
    }

    private void createChooseTimeColumnDialog() {
        chooseTimeColumnDialog = new ChoiceDialog<>();
        chooseTimeColumnDialog.setTitle("Importing Waveform File");
        chooseTimeColumnDialog.setContentText("Choose time column: ");
    }

    public Dialog<String> getChooseTimeColumnDialog(WaveformFile waveformFile) {
        chooseTimeColumnDialog.getItems().setAll(waveformFile.getColumnHeaders());
        chooseTimeColumnDialog.setSelectedItem(waveformFile.getColumnHeaders().iterator().next());
        chooseTimeColumnDialog.setHeaderText("Importing " + waveformFile.getFilename());
        return chooseTimeColumnDialog;
    }
}
