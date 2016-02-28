import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.io.*;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DOMParser
{
    //No generics
    List xmlContent;
    private static Connection connection = null;
    private static FileOutputStream out = null;

    public DOMParser(){
        //create a list to hold the employee objects
        xmlContent = new ArrayList();
    }

    public void runParser(String filePath) {
        try {
            connection = DriverManager.getConnection("jdbc:mysql:///moviedb", "root", "122b");
            Document dom = parseXmlFile(filePath);
            parseDocument(dom);
            connection.close();
        }
        catch (SQLException e) {
            System.out.println(e);
        }
    }


    private Document parseXmlFile(String filePath){
        //get the factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(filePath);
        }catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        }catch(SAXException se) {
            se.printStackTrace();
        }catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    private void parseDocument(Document dom){
        String importDataType = "";
        Element docEle = dom.getDocumentElement();

        // Look for parent nodes in document tree
        NodeList moviesList = docEle.getElementsByTagName("film");
        NodeList actorsList = docEle.getElementsByTagName("actor");
        NodeList castList = docEle.getElementsByTagName("filmc");

        if(moviesList != null && moviesList.getLength() > 0) {
            importDataType = "movies";
            addToContentList(moviesList, importDataType);
        }
        else if (actorsList != null && actorsList.getLength() > 0) {
            importDataType = "actors";
            addToContentList(actorsList, importDataType);
        }
        else if (castList != null && castList.getLength() > 0){
            importDataType = "cast";
            addToContentList(castList, importDataType);
        }
        addToDatabase(importDataType);
    }

    private void addToContentList(NodeList parentList, String importDataType) {
        for(int i = 0 ; i < parentList.getLength();i++) {
            Element el = (Element)parentList.item(i);

            if (el != null) {
                switch (importDataType) {
                    case "movies":
                        Film film = getMovie(el);
                        xmlContent.add(film);
                        break;
                    case "actors":
                        Actor actor = getActor(el);
                        xmlContent.add(actor);
                        break;
                    case "cast":
                        Cast cast = getCast(el);
                        xmlContent.add(cast);
                        break;
                    default:
                        break;
                }
            }
        }
        System.out.println(xmlContent);
    }

    private Film getMovie (Element element) {
        if (element != null) {
            String filmTitle = getTextValue(element, "t");
            String releaseYear = getTextValue(element, "year");
            String directorName = getTextValue(element, "dirn");
            String genre = getTextValue(element, "cat");

            Film film = new Film(filmTitle, releaseYear, directorName, genre);
            return film;
        }
        return null;
    }

    private Actor getActor(Element element) {
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

            Actor actor = new Actor(firstName, lastName, dob);
            return actor;
        }
        return null;
    }

    private Cast getCast(Element element) {
        if (element != null) {
            String movieTitle = getTextValue(element, "t");
            NodeList actors = element.getElementsByTagName("m");
            Cast cast = new Cast(movieTitle);
            for (int i = 0; i < actors.getLength(); i++) {
                try {
                    Element actorItem = (Element) actors.item(i);
                    String actorName = getTextValue(actorItem, "a");
                    cast.addActor(actorName);
                }
                catch (NullPointerException ex) {
                    System.out.println(ex);
                }
            }
            return cast;
        }
        return null;
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

    public void addToDatabase(String importDataType) {
        switch (importDataType) {
            case "movies":
                // iterate through xmlContent
                for (int i = 0; i < xmlContent.size(); i++) {
                    try {
                        Film filmItem = (Film) (xmlContent.get(i));
                        String title = filmItem.title;
                        String director = filmItem.director;
                        String year = filmItem.year;
                        String genre = filmItem.genre;

                        if (title != null && director != null && year != null && tryParseInt(year)) {
                            String procCall = (
                                    "CALL `XML_parse_movies` (" +
                                            "'" + title.replace("\\", "").replace("'","''") + "'," +
                                            "'" + director.replace("\\", "").replace("'","''") + "'," +
                                            "'" + year + "'," +
                                            "'" + genre + "'," +
                                            ");\n"
                            );

                            out.write(
                                    procCall.getBytes()
                            );
                        }
                    }
                    catch (Exception e) {
                        System.out.println(e);
                    }
                }
                break;
            case "actors":
                for (int i = 0; i < xmlContent.size(); i++) {
                    try {
                        Actor actorItem = (Actor) (xmlContent.get(i));
                        String first_name = actorItem.firstName;
                        String last_name = actorItem.lastName;
                        String dob = actorItem.dateOfBirth;

                        String procCall = (
                                "CALL `XML_parse_stars` (" +
                                        "'" + first_name.replace("\\", "").replace("'","''") + "'," +
                                        "'" + last_name.replace("\\", "").replace("'","''") + "'," +
                                        "'" + dob + "'," +
                                        ");\n"
                                );

                        out.write(
                          procCall.getBytes()
                        );
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                break;
            case "cast":
                for (int i = 0; i < xmlContent.size(); i++) {
                    try {
                        Cast cast = (Cast) xmlContent.get(i);
                        String filmTitle = cast.filmTitle;
                        ArrayList<String> actors = cast.castList;

                        for (int j = 0; i < actors.size(); i++) {
                            String stageName = actors.get(j);
                            String first_name = "";
                            String last_name = "";
                            if (stageName != null) {
                                String[] names = stageName.split(" ");
                                if (names.length != 1) {
                                    first_name = names[names.length - 2];
                                }
                                last_name = names[names.length - 1];
                                String procCall = (
                                    "CALL `XML_parse_cast`(" +
                                    "   '" + filmTitle.replace("\\", "").replace("'", "''") + "'," +
                                    "   '" + first_name.replace("\\", "").replace("'", "''") + "'," +
                                    "   '" + last_name.replace("\\", "").replace("'", "''") +
                                    ");";
                                );

                                out.write(procCall.getBytes());

                            }
                        }

                    } catch (Exception e) {
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
            System.out.println(args.length);
            for (int i = 0; i < args.length; i++) {
                String filePath = args[i];
                out = new FileOutputStream(filePath + ".sql");
                System.out.println(filePath);
                DOMParser dpe = new DOMParser();
                dpe.runParser(filePath);
                out.close();
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
