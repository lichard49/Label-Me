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
    private ContextMenu waveformListContextMenu;
    private FileChooser videoFileChooser;
    private FileChooser waveformFileChooser;
    private ChoiceDialog<String> chooseTimeColumnDialog;

    private UserInterfaceElements(Controller controller) {
        this.controller = controller;

        createWaveformListContextMenu();
        createVideoFileChooser();
        createWaveformFileChooser();
        createChooseTimeColumnDialog();
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
                    controller.addLabel(result.get());
                }
            }
        });
    }

    public ContextMenu getWaveformListContextMenu() {
        return getWaveformListContextMenu(null);
    }

    public ContextMenu getWaveformListContextMenu(XYChart.Data<Float, Float> label) {
        waveformListContextMenu.getItems().get(1).setDisable(label == null);
        waveformListContextMenu.getItems().get(2).setDisable(label == null);

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
