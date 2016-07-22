package sample;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML private MediaView mediaView;
    @FXML private Text videoTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // style media view
        mediaView.fitWidthProperty().bind(Bindings.select(mediaView.sceneProperty(), "width"));
        mediaView.fitHeightProperty().bind(Bindings.select(mediaView.sceneProperty(), "height"));
        mediaView.setPreserveRatio(true);

        final File videoFile = new File("/home/richard/Videos/Webcam/flexspark-demo.mp4");
        final Media videoMedia = new Media(videoFile.toURI().toString());
        final MediaPlayer mediaPlayer = new MediaPlayer(videoMedia);
        mediaView.setMediaPlayer(mediaPlayer);

        mediaPlayer.play();
        mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
                System.out.println(oldValue + ", " + newValue);
                videoTime.setText(String.format("%1$.3f / %2$.3f", newValue.toSeconds(),
                        mediaPlayer.getTotalDuration().toSeconds()));
            }
        });
    }
}
