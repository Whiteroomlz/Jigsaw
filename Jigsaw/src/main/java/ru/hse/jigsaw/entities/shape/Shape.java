package ru.hse.jigsaw.entities.shape;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс, описывающий фигуру в игровой зоне.
 */
public class Shape {
    /**
     * Паттерн поиска координат блока в строковом представлении структуры фигуры.
     */
    private static final Pattern shapeDataPattern = Pattern.compile("\\((?<row>\\d);(?<column>\\d)\\)");

    /**
     * Паттерн поиска зашифрованной в строку фигуры.
     */
    private static final Pattern encodedShapePattern = Pattern.compile(
            "\\[(?<shapeData>.+)\\$(?<hexColor>.+)\\$(?<styleClass>.+)]"
    );

    /**
     * Список блоков фигуры.
     */
    private final List<Tile> tiles;

    /**
     * Строка-шифр, описывающая фигуру.
     */
    private final String encodedShape;

    /**
     * HEX-код цвета всех ячеек фигуры.
     */
    private final String hexColor;

    private boolean isDraggable = false;

    public Shape(String shapeData, String hexColor, String styleClass) {
        tiles = new ArrayList<>();
        this.hexColor = hexColor;

        StringJoiner stringJoiner = new StringJoiner("$", "[", "]");
        stringJoiner.add(shapeData).add(hexColor).add(styleClass);
        this.encodedShape = stringJoiner.toString();

        Matcher matcher = shapeDataPattern.matcher(shapeData);
        while (matcher.find()) {
            int row = Integer.parseInt(matcher.group("row"));
            int column = Integer.parseInt(matcher.group("column"));

            Tile tile = new Tile(row, column, this, styleClass);
            tile.setStyle(String.format("-fx-background-color: %s", hexColor));
            tiles.add(tile);
        }
    }

    /**
     * Десериализует фигуру по её строковому представлению.
     *
     * @param encodedShape зашифрованная в строку фигура;
     * @return результат десериализации;
     * @throws ParseException в случае, если не удалось реконструировать фигуру на основе переданной строки.
     */
    public static Shape parse(String encodedShape) throws ParseException {
        Matcher matcher = encodedShapePattern.matcher(encodedShape);
        if (matcher.matches()) {
            String shapeData = matcher.group("shapeData");
            String hexColor = matcher.group("hexColor");
            String styleClass = matcher.group("styleClass");

            return new Shape(shapeData, hexColor, styleClass);
        } else {
            throw new ParseException(
                    String.format("Impossible to parse figure from the provided string: %s", encodedShape), 0);
        }
    }

    public List<Tile> getTiles() {
        return tiles;
    }

    public String getEncodedShape() {
        return encodedShape;
    }

    public String getHexColor() {
        return hexColor;
    }

    public boolean isDraggable() {
        return isDraggable;
    }

    /**
     * Определяет, может ли фигура быть целью перетаскивания в событии "Drag and drop".
     *
     * @param draggable может ли фигура перетаскиваться мышью;
     */
    public void setDraggable(boolean draggable) {
        isDraggable = draggable;

        if (draggable) {
            for (Tile tile : getTiles()) {
                tile.setOnDragDetected(tile::onDragDetected);
            }
        } else {
            for (Tile tile : getTiles()) {
                tile.setOnDragDetected(null);
            }
        }
    }

    @Override
    public String toString() {
        return encodedShape;
    }
}
