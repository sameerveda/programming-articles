package sam.book.search.model;

public class Title {
	public final String title;
	public final int id;
	
	public Title(int id, String title) {
		this.title = title;
		this.id = id;
	}
	
	@Override
	public String toString() {
		return title;
	}
}
