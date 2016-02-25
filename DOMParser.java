import java.sql.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DOMParser
{
    //No generics
    List xmlContent;
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
            //dom = db.parse("stanford-movies/mains243.xml");
            dom = db.parse("stanford-movies/actors63.xml");
            //dom = db.parse("stanford-movies/casts124.xml");

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
        NodeList castList = docEle.getElementsByTagName("filmc");

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
            // TODO: replace case contents with actual functions for retrieving tag data
            if (el != null) {
                switch (importDataType) {
                    case "movies":
                        newElement = getMovie(el);
                        break;
                    case "actors":
                        newElement = getActor(el);
                        break;
/*                    case "cast":
                        newElement = getActor(el);
                        break;*/
                    default:
                        newElement = getMovie(el);
                        break;
                }
                xmlContent.add(newElement);
            }
        }
        //System.out.println(xmlContent.size());
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
            String firstName = names[0];
            String lastName = names[1];

            String dob = "January 01, " + birthyear;
/*            DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
            Date date = format.parse(string);
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010*/

            actorObject.put("first_name", firstName);
            actorObject.put("last_name", lastName);
            actorObject.put("dob", dob);
        }
        return actorObject;
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
                                insertFilmStatement.executeUpdate();
                            }

                            //System.out.println("Added movie: " + title);
                        }

                        try { if (insertFilmStatement != null) insertFilmStatement.close(); } catch (Exception e) {}
                        try { if (selectGenreStatement != null) selectGenreStatement.close(); } catch (Exception e) {}
                    }
                    catch (SQLException e) {
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
