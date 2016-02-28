import java.util.ArrayList;

public class Cast {
    public String filmTitle;
    public ArrayList<String> castList;

    public Cast (String title, String actor) {
        filmTitle = title;
        castList.add(actor);
    }
}

