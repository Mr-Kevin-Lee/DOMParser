import java.sql.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DOMParser
{
    //No generics
    List xmlContent;
    HashMap<String, ArrayList<String>> castMap = new HashMap<String, ArrayList<String>>();
    Document dom;
    String importDataType = "";
    private static Connection connection = null;

    public DOMParser(){
        //create a list to hold the employee objects
        xmlContent = new ArrayList();
    }

    public void runParser() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql:///moviedb", "root", "122b");
            parseXmlFile();
            parseDocument();
            addToDatabase();
            connection.close();
        }
        catch (SQLException e) {
            System.out.println(e);
        }
    }


    private void parseXmlFile(){
        //get the factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();

            //replace with desired XML e.g. actors63.xml
//            dom = db.parse("stanford-movies/mains243.xml");
//            dom = db.parse("stanford-movies/actors63.xml");
            dom = db.parse("stanford-movies/casts124.xml");

        }catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        }catch(SAXException se) {
            se.printStackTrace();
        }catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void parseDocument(){
        Element docEle = dom.getDocumentElement();

        // Look for parent nodes in document tree
        NodeList moviesList = docEle.getElementsByTagName("film");
        NodeList actorsList = docEle.getElementsByTagName("actor");
        NodeList castList = docEle.getElementsByTagName("m");

        if(moviesList != null && moviesList.getLength() > 0) {
            importDataType = "movies";
            addToContentList(moviesList);
        }
        else if (actorsList != null && actorsList.getLength() > 0) {
            importDataType = "actors";
            addToContentList(actorsList);
        }
        else if (castList != null && castList.getLength() > 0){
            importDataType = "cast";
            addToContentList(castList);
        }
    }

    private void addToContentList(NodeList parentList) {
        for(int i = 0 ; i < parentList.getLength();i++) {
            Element el = (Element)parentList.item(i);
            HashMap<String, String> newElement;

            if (el != null) {
                switch (importDataType) {
                    case "movies":
                        newElement = getMovie(el);
                        xmlContent.add(newElement);
                        break;
                    case "actors":
                        newElement = getActor(el);
                        xmlContent.add(newElement);
                        break;
                    case "cast":
                        getCast(el);
                        break;
                    default:
                        newElement = getMovie(el);
                        break;
                }
            }
        }
//        System.out.println(xmlContent);
    }

    private HashMap<String, String> getMovie (Element element) {
        HashMap<String, String> movieObject = new HashMap<>();
        if (element != null) {
            String filmTitle = getTextValue(element, "t");
            String releaseYear = getTextValue(element, "year");
            String directorName = getTextValue(element, "dirn");
            String genre = getTextValue(element, "cat");

            movieObject.put("title", filmTitle);
            movieObject.put("year", releaseYear);
            movieObject.put("director", directorName);
            movieObject.put("genre", genre);
        }
        return movieObject;
    }

    private HashMap<String, String> getActor(Element element) {
        HashMap<String, String> actorObject = new HashMap<>();
        if (element != null) {
            String stageName = getTextValue(element, "stagename");
            String birthyear = getTextValue(element, "dob");

            String[] names = stageName.split(" ");
            String firstName = "";
            if (names.length != 1)
                firstName = names[names.length - 2];
            String lastName = names[names.length - 1];

            String dob = null;
            if (birthyear != null && tryParseDate(birthyear + "/01/01"))
                dob = birthyear + "/01/01";

            actorObject.put("first_name", firstName);
            actorObject.put("last_name", lastName);
            actorObject.put("dob", dob);
        }
        return actorObject;
    }

    private void getCast(Element element) {
        try {
            if (element != null) {
                String movieTitle = getTextValue(element, "t");
                String stageName = getTextValue(element, "a");

                ArrayList<String> filmCast = castMap.get(movieTitle);
                if (filmCast == null)
                    filmCast = new ArrayList<String>();

                filmCast.add(stageName);
                castMap.put(movieTitle, filmCast);
            }
        }
        catch (NullPointerException ex) {
            System.out.println(ex);
        }
    }

    private String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        try {
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);
                    if (el.getFirstChild() != null) {
                        textVal = el.getFirstChild().getNodeValue();
                    }
                }
            }
        }
        catch (NullPointerException ex) {
            System.out.println(ex);
        }
        return textVal;
    }

    public void addToDatabase() {
        switch (importDataType) {
            case "movies":
                String insertFilmString = (
                        "INSERT INTO movies (title, year, director) " +
                                "VALUES (?, ?, ?)"
                );

                String selectGenreString = (
                        "SELECT id FROM genres " +
                                "WHERE name = ? "
                );

                String insertGenreString = (
                        "INSERT INTO genres (name) " +
                                "VALUES (?)"
                );

                String insertGenresMoviesString = (
                        "INSERT INTO genres_in_movies (genre_id, movie_id) " +
                                "VALUES(?, ?)"
                );

                // iterate through xmlContent
                for (int i = 0; i < xmlContent.size(); i++) {
                    try {
                        PreparedStatement insertFilmStatement = connection.prepareStatement(insertFilmString);
                        PreparedStatement selectGenreStatement = connection.prepareStatement(selectGenreString);
                        PreparedStatement insertGenresMoviesStatement = connection.prepareStatement(insertGenresMoviesString);

                        HashMap<String, String> filmItem = (HashMap<String, String>) (xmlContent.get(i));
                        String title = filmItem.get("title");
                        String year = filmItem.get("year");
                        String director = filmItem.get("director");
                        String genre = filmItem.get("genre");
                        int genreID = -1;

                        if (title != null && director != null && year != null && tryParseInt(year)) {

                            insertFilmStatement.setString(1, title);
                            insertFilmStatement.setString(2, year);
                            insertFilmStatement.setString(3, director);

                            insertFilmStatement.executeUpdate();

                            ResultSet insertFilmID = insertFilmStatement.executeQuery("select last_insert_id()");
                            insertFilmID.next();
                            int filmID = insertFilmID.getInt(1);

                            if (genre != null) {
                                // Check for existing genre and update ID
                                selectGenreStatement.setString(1, genre);
                                ResultSet result = selectGenreStatement.executeQuery();
                                while(result.next()) {
                                    genreID = result.getInt(1);
                                }
                                try { if (result != null) result.close(); }
                                catch (Exception e) { System.out.println(e); }

                                // Add new genre to DB and retrieve ID
                                if (genreID == -1) {
                                    PreparedStatement insertGenreStatement = connection.prepareStatement(insertGenreString);
                                    insertGenreStatement.setString(1, genre);
                                    insertGenreStatement.executeUpdate();

                                    ResultSet lastInsertID = insertGenreStatement.executeQuery("select last_insert_id()");
                                    lastInsertID.next();
                                    genreID = lastInsertID.getInt(1);
                                }

                                // Insert movie and genre into genres_in_movies
                                insertGenresMoviesStatement.setInt(1, genreID);
                                insertGenresMoviesStatement.setInt(2, filmID);
                                insertGenresMoviesStatement.executeUpdate();
                            }

                            //System.out.println("Added movie: " + title);
                        }

                        try { if (insertFilmStatement != null) insertFilmStatement.close(); } catch (Exception e) {}
                        try { if (selectGenreStatement != null) selectGenreStatement.close(); } catch (Exception e) {}
                        try { if (insertGenresMoviesStatement != null) insertGenresMoviesStatement.close(); } catch (Exception e) {}
                    }
                    catch (SQLException e) {
                        System.out.println(e);
                    }
                }
                break;
            case "actors":
                String insertActorString = (
                        "INSERT INTO stars(first_name, last_name, dob) " +
                                "VALUES(?, ?, ?)"
                );
                for (int i = 0; i < xmlContent.size(); i++) {
                    try {
                        HashMap<String, String> actorMap = (HashMap<String, String>) (xmlContent.get(i));
                        String first_name = actorMap.get("first_name");
                        String last_name = actorMap.get("last_name");
                        String dob = actorMap.get("dob");

                        PreparedStatement insertActorStatement = connection.prepareStatement(insertActorString);
                        insertActorStatement.setString(1, first_name);
                        insertActorStatement.setString(2, last_name);
                        insertActorStatement.setString(3, dob);
                        insertActorStatement.executeUpdate();
                        //System.out.println("Inserted: " + first_name + " " + last_name);

                        try { if (insertActorStatement != null) insertActorStatement.close(); } catch (Exception e) {}
                    } catch (SQLException e) {
                        System.out.println(e);
                    }
                }
                break;
            case "cast":
                String selectMovieString = (
                        "SELECT id FROM movies WHERE title = ?"
                );
                String selectActorString = (
                        "SELECT id FROM stars WHERE first_name = ? AND last_name = ?"
                );
                String insertCastString = (
                        "INSERT INTO stars_in_movies(star_id, movie_id) " +
                                "VALUES(?, ?)"
                );

                for (Map.Entry<String, ArrayList<String>> entry : castMap.entrySet()) {
                    try {
                        String film = entry.getKey();
                        ArrayList<String> actors = entry.getValue();

                        for (int i = 0; i < actors.size(); i++) {
                            String stageName = actors.get(i);
                            String first_name = "";
                            String last_name = "";
                            if (stageName != null) {
                                String[] names = stageName.split(" ");
                                if (names.length != 1)
                                    first_name = names[names.length - 2];
                                last_name = names[names.length - 1];
                            }

                            PreparedStatement selectMovieStatement = connection.prepareStatement(selectMovieString);
                            selectMovieStatement.setString(1, film);
                            ResultSet movieSet = selectMovieStatement.executeQuery();
                            int movieID = -1;
                            if (movieSet.next())
                                movieID = movieSet.getInt(1);

                            PreparedStatement selectActorStatement = connection.prepareStatement(selectActorString);
                            selectActorStatement.setString(1, first_name);
                            selectActorStatement.setString(2, last_name);
                            ResultSet actorSet = selectActorStatement.executeQuery();
                            int actorID = -1;
                            if (actorSet.next())
                                actorID = actorSet.getInt(1);

                            PreparedStatement insertCastStatement = connection.prepareStatement(insertCastString);
                            if (movieID != -1 && actorID != -1) {
                                insertCastStatement.setInt(1, actorID);
                                insertCastStatement.setInt(2, movieID);
                                insertCastStatement.executeUpdate();
                            }

                            try { if (selectMovieStatement != null) selectMovieStatement.close(); } catch (Exception e) {}
                            try { if (selectActorStatement != null) selectActorStatement.close(); } catch (Exception e) {}
                            try { if (insertCastStatement != null) insertCastStatement.close(); } catch (Exception e) {}
                        }

                    } catch (SQLException e) {
                        System.out.println(e);
                    }
                }
                break;
            default:
                break;
        }
    }

    boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    boolean tryParseDate(String dateString)
    {
        try
        {
            Date df = new SimpleDateFormat("yyyy/MM/dd").parse(dateString);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args){
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            DOMParser dpe = new DOMParser();
            dpe.runParser();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
