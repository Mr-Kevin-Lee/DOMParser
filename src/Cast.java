import java.util.ArrayList;

public class Cast {
    public String filmTitle;
    public ArrayList<String> castList;

    public Cast (String title) {
        filmTitle = title;
        castList = new ArrayList<String>();
    }

    public void addActor(String actor) {
        castList.add(actor);
    }
}

