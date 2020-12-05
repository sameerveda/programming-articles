
package sam.article.reader.model;
interface ArticleMeta {
    String DATA_TABLE_NAME = "Data";

    String ID = "id";    // id 	INTEGER NOT NULL UNIQUE
    String TITLE = "title";    // title 	TEXT NOT NULL
    String SOURCE = "source";    // source 	TEXT NOT NULL
    String REDIRECT = "redirect";    // redirect 	TEXT
    String TAGS = "tags";    // tags 	TEXT
    String FAVICON = "favicon";    // favicon 	INTEGER DEFAULT 0
    String ADDEDON = "addedOn";    // addedOn 	INTEGER
    String STATUS = "status";    // status 	TEXT
    String NOTES = "notes";    // notes 	TEXT
    String UPDATEDON = "updatedOn";    // updatedOn 	INTEGER
    String VERSION = "version";    // version 	INTEGER
}