package createDrtVehicles2Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 * This is an example script to create (robo)taxi vehicle files. The vehicles are distributed randomly in the network.
 *
 */
public class RunCreateDrtVehicles2Type {
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		int numberofVehicles1 = 0;//ride-sharing
		int numberofVehicles2 = 100;//car-sharing
		double operationStartTime = 0.; //t0
		double operationEndTime = 27*3600.;	//t1
		int seats1 = 8;
		int seats2 = 1;
		String networkfile = "/Users/jo/git/matsim/contribs/UT_MATSim/resources/Numata_0130/Network.xml";
		String drtsFile = "/Users/jo/git/matsim/contribs/UT_MATSim/resources/Numata_0130/drts_L"+numberofVehicles1+"_S"+numberofVehicles2+".xml";
		List<Vehicle> vehicles = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		List<Id<Link>> allLinks = new ArrayList<>();
		allLinks.addAll(scenario.getNetwork().getLinks().keySet());
		for (int i = 0; i< numberofVehicles1;i++){
			Link startLink;
			do {
			Id<Link> linkId = allLinks.get(random.nextInt(allLinks.size()));
			startLink =  scenario.getNetwork().getLinks().get(linkId);
			}
			while (!startLink.getAllowedModes().contains(TransportMode.car));
			//for multi-modal networks: Only links where cars can ride should be used.

			Vehicle v = new VehicleImpl(Id.create("drtL"+i, Vehicle.class), startLink, seats1, operationStartTime, operationEndTime);
		    vehicles.add(v);

		}
		for (int i = 0; i< numberofVehicles2;i++){
			Link startLink;
			do {
			Id<Link> linkId = allLinks.get(random.nextInt(allLinks.size()));
			startLink =  scenario.getNetwork().getLinks().get(linkId);
			}
			while (!startLink.getAllowedModes().contains(TransportMode.car));
			//for multi-modal networks: Only links where cars can ride should be used.

			Vehicle v = new VehicleImpl(Id.create("drtS"+i, Vehicle.class), startLink, seats2, operationStartTime, operationEndTime);
		    vehicles.add(v);

		}
		new VehicleWriter(vehicles).write(drtsFile);
	}

}