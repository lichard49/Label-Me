package sample;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        String workingDir = System.getProperty("user.dir");
        final File videoFile = new File("/home/richard/Videos/Webcam/flexspark-demo.mp4");

        final Media videoMedia = new Media(videoFile.toURI().toString());
        final MediaPlayer mediaPlayer = new MediaPlayer(videoMedia);
        final MediaView mediaView = new MediaView(mediaPlayer);

        final DoubleProperty width = mediaView.fitWidthProperty();
        final DoubleProperty height = mediaView.fitHeightProperty();
        width.bind(Bindings.selectDouble(mediaView.sceneProperty(), "width"));
        height.bind(Bindings.selectDouble(mediaView.sceneProperty(), "height"));
        mediaView.setPreserveRatio(true);

        StackPane rootPane = new StackPane();
        rootPane.getChildren().add(mediaView);

        final Scene scene = new Scene(rootPane, 960, 540);
        scene.setFill(Color.BLACK);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Label Me");
        primaryStage.show();

        mediaPlayer.play();
        mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
                System.out.println(oldValue + ", " + newValue);
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
