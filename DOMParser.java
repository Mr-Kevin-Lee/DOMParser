import java.sql.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import java.net.*;
import java.text.*;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.sql.DataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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


    public DOMParser(){
        //create a list to hold the employee objects
        xmlContent = new ArrayList();
    }

    public void runParser() {

        parseXmlFile();
        parseDocument();

        // todo: replace this step with one that adds items to appropriate database
        //printData();

    }


    private void parseXmlFile(){
        //get the factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();

            //replace with desired XML e.g. actors63.xml
            dom = db.parse("stanford-movies/mains243.xml");
            dom = db.parse("stanford-movies/actors63.xml");
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
        NodeList moviesList = docEle.getElementsByTagName("directorfilms");
        NodeList actorsList = docEle.getElementsByTagName("actor");
        NodeList castList = docEle.getElementsByTagName("filmc");

        if(moviesList != null && moviesList.getLength() > 0) {
            importDataType = "movies";
            addToContentList(moviesList);
        } else if (actorsList != null && actorsList.getLength() > 0) {
            importDataType = "actors";
            addToContentList(actorsList);
        }
        else {
            importDataType = "cast";
            addToContentList(castList);
        }
    }

    private void addToContentList(NodeList parentList) {
        for(int i = 0 ; i < parentList.getLength();i++) {
            Element el = (Element)parentList.item(i);
            HashMap<String, String> newElement;
            // TODO: replace case contents with actual functions for retrieving tag data
            switch (importDataType) {
                case "movies":
                    newElement = getMovie(el);
                    break;
                case "actors":
                    newElement = getMovie(el);
                    break;
                case "cast":
                    newElement = getMovie(el);
                    break;
                default:
                    newElement = getMovie(el);
                    break;
            }
            xmlContent.add(newElement);
        }
    }

    // todo: instead of returning an "Employee" object, return a hashmap of the values to input
    // to the DB e.g. {"title": "Blade Runner", "year": "1982", "director" : "Ridley Scott"}
    private HashMap<String, String> getMovie (Element element) {
        HashMap<String, String> movieObject = new HashMap<>();
        String directorName = getTextValue(element, "dirname");
        return movieObject;
    }

    private String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if(nl != null && nl.getLength() > 0) {
            Element el = (Element)nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }

        return textVal;
    }

    private int getIntValue(Element ele, String tagName) {
        //in production application you would catch the exception
        return Integer.parseInt(getTextValue(ele,tagName));
    }

    public static void main(String[] args){
        try {
            //Class.forName("com.mysql.jdbc.Driver").newInstance();
            DOMParser dpe = new DOMParser();
            dpe.runParser();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
