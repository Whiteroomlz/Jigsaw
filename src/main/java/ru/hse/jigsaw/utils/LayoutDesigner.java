package ru.hse.jigsaw.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.json.JSONArray;
import ru.hse.jigsaw.App;
import ru.hse.jigsaw.model.shape.Shape;
import ru.hse.jigsaw.model.shape.Tile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import java.util.Random;


public class LayoutDesigner {
    /**
     * Стили, поддерживаемые приложением. Каждому стилю соответствует собственный CSS-файл.
     */
    public enum GameStyle {
        DARK_STYLE
    }

    /**
     * Поддерживаемые цвета генерируемых фигур. Каждому цвету соответствует HEX-код.
     */
    public enum ShapeColor {
        CYAN,
        YELLOW,
        PURPLE,
        GREEN,
        RED,
        BLUE,
        ORANGE,
        GREY
    }

    /**
     * Название класса стиля пустой ячейки в игровой зоне.
     * В соответствии с этим названием производится поиск представления ячейки в CSS-файле применённого стиля.
     */
    public static final String EMPTY_CELL_STYLE_CLASS = "unfilled-game-cell";

    /**
     * Название класса стиля строительного блока фигуры в игровой зоне.
     * В соответствии с этим названием производится поиск представления блока в CSS-файле применённого стиля.
     */
    public static final String GAME_BLOCK_STYLE_CLASS = "game-block";

    /**
     * Считывает FXML-файл представления сцены, записывает в поля класса сцену и соответствующий ей объект-контроллер.
     *
     * @param style применяемый игровой стиль;
     */
    public static <T> LayoutInfo<T> loadNewScene(GameStyle style, String fxmlPath) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxmlPath));
            Scene scene = new Scene(fxmlLoader.load());
            T controller = fxmlLoader.getController();

            setStyle(scene, style);

            return new LayoutInfo<T>(scene, controller);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Меняет иконку приложения на изображение, лежащее по заранее определённому пути.
     *
     * @param stage действующее основное окно приложения;
     */
    public static void setIcon(Stage stage, String iconPath) {
        stage.getIcons().add(new Image(Objects.requireNonNull(App.class.getResourceAsStream(iconPath))));
    }

    /**
     * Применяет выбранный игровой стиль к объектам приложения.
     *
     * @param gameStyle один из стилей, доступных в перечислении {@link GameStyle};
     */
    public static void setStyle(Scene scene, GameStyle gameStyle) {
        /* Предполагается, что каждому стилю соответствует отдельный CSS-файл в директории ресурсов. */
        String cssPath = switch (gameStyle) {
            case DARK_STYLE -> "view/styles/dark_style.css";
        };

        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource(cssPath)).toExternalForm());
    }

    /**
     * Фабричный метод, генерирующий фигуры по запросу.
     *
     * @return фигура, сгенерированная по случайно полученному паттерну.
     * Цвет фигуры также выбирается рандомайзером из перечисления доступных цветов {@link ShapeColor}.
     * @throws UncheckedIOException в случае, если не удалось загрузить паттерны конструирования фигур из JSON-файла.
     */
    public static Shape generateShape() throws UncheckedIOException {
        Random generator = new Random();

        URL shapesUrl = App.class.getResource("shapes.json");
        assert shapesUrl != null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(shapesUrl.openStream()))) {
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            JSONArray shapesData = new JSONArray(builder.toString());
            String shapeData = shapesData.getString(generator.nextInt(shapesData.length()));

            /* Каждому цвету из перечисления сопоставляется HEX-код. */
            String hexColor = switch (ShapeColor.values()[generator.nextInt(ShapeColor.values().length)]) {
                case CYAN -> "#00ffff";
                case YELLOW -> "#ffff00";
                case PURPLE -> "#800080";
                case GREEN -> "#00ff00";
                case RED -> "#ff0000";
                case BLUE -> "#0000ff";
                case ORANGE -> "#ff7f00";
                case GREY -> "#7f7f7f";
            };

            return new Shape(shapeData, hexColor, GAME_BLOCK_STYLE_CLASS);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Располагает фигуру в игровой зоне. Расположение соответсвует относительным координатам блоков фигуры.
     *
     * @param shape    размещаемая фигура;
     * @param gridPane место привязки;
     */
    public static void placeShape(Shape shape, GridPane gridPane) {
        for (Tile tile : shape.getTiles()) {
            gridPane.add(tile, tile.getColumn(), tile.getRow());
        }
    }

    /**
     * Выделяет на игровой зоне силуэт переданной фигуры по координатам ячеек её блоков.
     *
     * @param shape    фигура, силуэт которой необходимо отобразить на сетке;
     * @param gridPane игровая зона, в которой предположительно находится фигура;
     */
    public static void highlightShapePosition(Shape shape, GridPane gridPane) {
        for (Tile shapeTile : shape.getTiles()) {
            for (Node mainGridTile : gridPane.getChildren()) {
                if (GridPane.getRowIndex(mainGridTile) == shapeTile.getRow()
                        && GridPane.getColumnIndex(mainGridTile) == shapeTile.getColumn()) {
                    // Цвет границы ячейки совпадает с цветом фигуры.
                    mainGridTile.setStyle(String.format("-fx-border-color: %s", shape.getHexColor()));
                    break;
                }
            }
        }
    }

    /**
     * Сбрасывает в переданной игровой зоне все выделения,
     * полученные при помощи метода {@link LayoutDesigner#highlightShapePosition(Shape, GridPane)}.
     *
     * @param gridPane игровая зона, выделения в которой необходимо сбросить;
     */
    public static void deHighlightGrid(GridPane gridPane) {
        for (Node mainGridTile : gridPane.getChildren()) {
            if (mainGridTile.getStyleClass().contains(EMPTY_CELL_STYLE_CLASS)
                    && mainGridTile.getStyleClass().size() == 1) {
                mainGridTile.setStyle("-fx-border-color: transparent");
            }
        }
    }
}
