package ru.hse.jigsaw.model.shape;

import javafx.scene.Node;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.GridPane;
import ru.hse.jigsaw.utils.LayoutDesigner;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс, описывающий ячейку в игровой области.
 */
public class Tile extends Pane {
    /* Координаты в игровой области */
    private int row;
    private int column;

    /**
     * Фигура, в составе которой находится ячейка.
     */
    private final Shape parentShape;

    /**
     * Паттерн поиска данных в {@link ClipboardContent}.
     */
    private static final Pattern draggableDataPattern = Pattern.compile(
            "(?<encodedShape>.+)\\|(?<originRow>\\d+)\\|(?<originColumn>\\d+)"
    );

    public Tile(int row, int column, Shape parent, String styleClass) {
        this.row = row;
        this.column = column;
        this.parentShape = parent;
        getStyleClass().add(styleClass);
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public Shape getParentShape() {
        return parentShape;
    }

    /**
     * Обработчик события инициализации перетаскивания фигуры.
     * Ячейка выступает в роли источника.
     *
     * @param event событие {@link MouseEvent#DRAG_DETECTED};
     */
    public void onDragDetected(MouseEvent event) {
        Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();
        content.putString(String.format("%s|%d|%d", parentShape.getEncodedShape(), row, column));
        dragboard.setContent(content);
        event.consume();
    }

    /**
     * Обработчик события наведения курсора мыши на ячейку при перетаскивании фигуры.
     * Ячейка выступает в роли приёмника.
     *
     * @param event событие {@link DragEvent#DRAG_EXITED};
     */
    public void onDragOver(DragEvent event) {
        if (getStyleClass().contains(LayoutDesigner.EMPTY_CELL_STYLE_CLASS)
                && getStyleClass().size() == 1
                && event.getGestureSource().getClass() == Tile.class
                && event.getDragboard().hasString()) {

            Matcher matcher = draggableDataPattern.matcher(event.getDragboard().getString());
            if (matcher.matches()) {
                Shape shape;
                try {
                    shape = Shape.parse(matcher.group("encodedShape"));
                } catch (ParseException exception) {
                    throw new RuntimeException(exception);
                }

                int originRow = Integer.parseInt(matcher.group("originRow"));
                int originColumn = Integer.parseInt(matcher.group("originColumn"));

                GridPane parentGrid = (GridPane) this.getParent();
                for (Tile shapeTile : shape.getTiles()) {
                    int mainGridRow = this.row + (shapeTile.row - originRow);
                    int mainGridColumn = this.column + (shapeTile.column - originColumn);

                    if (mainGridRow < 0 || mainGridRow >= parentGrid.getRowCount()
                            || mainGridColumn < 0 || mainGridColumn >= parentGrid.getColumnCount()) {
                        return;
                    }

                    for (Node node : parentGrid.getChildren()) {
                        if (node instanceof Tile mainGridTile) {
                            if (GridPane.getRowIndex(mainGridTile) == mainGridRow
                                    && GridPane.getColumnIndex(mainGridTile) == mainGridColumn) {
                                if (mainGridTile.getStyleClass().contains(LayoutDesigner.EMPTY_CELL_STYLE_CLASS)
                                        && mainGridTile.getStyleClass().size() == 1) {
                                    shapeTile.row = mainGridRow;
                                    shapeTile.column = mainGridColumn;
                                } else {
                                    return;
                                }
                            }
                        }
                    }
                }
                LayoutDesigner.highlightShapePosition(shape, parentGrid);

                event.acceptTransferModes(TransferMode.MOVE);
            } else {
                throw new RuntimeException(new ParseException("Impossible to parse data from the clipboard", 0));
            }
        }
    }

    /**
     * Обработчик события отвода курсора мыши с ячейки-приёмника при перетаскивании фигуры в случае,
     * если не произошло событие {@link DragEvent#DRAG_DROPPED}.
     *
     * @param event событие {@link DragEvent#DRAG_EXITED};
     */
    public void onDragExited(DragEvent event) {
        GridPane parentGrid = (GridPane) this.getParent();
        LayoutDesigner.deHighlightGrid(parentGrid);
        event.consume();
    }

    /**
     * Обработчик события начала передачи данных из ячейки-источника в ячейку-приёмник.
     *
     * @param event событие {@link DragEvent#DRAG_DROPPED};
     */
   public void onDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();

        boolean success = false;
        if (dragboard.hasString()) {
            Matcher matcher = draggableDataPattern.matcher(event.getDragboard().getString());
            if (matcher.matches()) {
                Shape shape;
                try {
                    shape = Shape.parse(matcher.group("encodedShape"));
                } catch (ParseException exception) {
                    throw new RuntimeException(exception);
                }

                shape.setDraggable(false);
                int originRow = Integer.parseInt(matcher.group("originRow"));
                int originColumn = Integer.parseInt(matcher.group("originColumn"));

                GridPane parentGrid = (GridPane) this.getParent();

                for (Tile shapeTile : shape.getTiles()) {
                    shapeTile.row = this.row + (shapeTile.row - originRow);
                    shapeTile.column = this.column + (shapeTile.column - originColumn);
                }
                LayoutDesigner.placeShape(shape, parentGrid);

                success = true;

                getOnDragDone().handle(event);
            } else {
                throw new RuntimeException(new ParseException("Impossible to parse data from the clipboard", 0));
            }
        }
        event.setDropCompleted(success);
    }
}
