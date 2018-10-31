package replanForCar;

import javax.xml.parsers.*;

import org.matsim.api.core.v01.population.PopulationWriter;
import org.w3c.dom.*;
import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class  Replan {
    public static void main(String[] args) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse("/Users/jo/git/matsim/contribs/UT_MATSim/resources/Numata_0829/5_2_output_plans.xml");

        Element root = document.getDocumentElement();

        System.out.println("Node: " + root.getNodeName());
        System.out.println("name: " + root.getAttribute("name"));

        System.out.println("==============================");



        //PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		//populationWriter.write("/Users/jo/Desktop/R_Plans/plans_strict_0823.xml");
        /*
        NodeList personsNodeList = root.getChildNodes();
        for(int i = 0; i < personsNodeList.getLength(); i++) {
            Node personNode = personsNodeList.item(i);
            if(personNode.getNodeType() == Node.ELEMENT_NODE) {
                Element personElement = (Element)personNode;
                if(personElement.getNodeName().equals("person")) {
                    System.out.println("[" + personElement.getAttribute("name") + "]");
                    NodeList personChildrenNodeList = personElement.getChildNodes();
                    for(int j = 0; j < personChildrenNodeList.getLength(); j++) {
                        Node node = personChildrenNodeList.item(j);
                        if(node.getNodeType() == Node.ELEMENT_NODE) {
                            System.out.println(node.getNodeName() + ": " + node.getTextContent());
                        }
                    }
                }
            }
        }
        */

        System.out.println("==============================");
    }
}
