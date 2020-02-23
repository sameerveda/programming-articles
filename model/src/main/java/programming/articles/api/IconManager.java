package programming.articles.api;

import java.util.function.BiConsumer;

import javafx.scene.image.Image;

public interface IconManager extends AutoCloseable {
	Image getIcon(String url);
	void loadIcon(String favicon, BiConsumer<Image, Throwable> onResult);
}
