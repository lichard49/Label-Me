package app;

import javafx.event.EventHandler;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Created by richard on 7/24/16.
 */
public class MixedTreeCell extends CheckBoxTreeCell<String> {
    private TextField textField;

    public MixedTreeCell() {
    }

    @Override
    public void startEdit() {
        super.startEdit();

        if (textField == null) {
            createTextField();
        }
        setText(null);
        setGraphic(textField);
        textField.selectAll();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(getTreeItem().getGraphic());
        updateItem(getItem(), false);
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if(empty) {
            // if the item is a checkbox
            setGraphic(null);
            setText(null);
        } else if (!(getTreeItem() instanceof CheckBoxTreeItem)) {
            setGraphic(null);
        }
    }

    private void createTextField() {
        textField = new TextField(getString());
        textField.setOnKeyReleased(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent t) {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(textField.getText());
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            }
        });
    }

    private String getString() {
        return getItem() == null ? "" : getItem().toString();
    }
}
