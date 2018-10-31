package maebashiTest_drt;

/**
 * @author KAMIJO
 *
 */

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessModeConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.drt.analysis.DrtAnalysisModule;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.DrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigConsistencyChecker;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouterModule;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunAccessibilityTest_drt_0430 {

	private static final String ROOTDIR = "/Users/jo/git/matsim/contribs/UT_MATSim/resources/Numata_0510/";
	private static final String OUTPUTDIR = ROOTDIR + "output";
	private static final String CONFIGFILE = ROOTDIR + "config_strict.xml";
	private static final String NETWORKFILE = ROOTDIR + "Network.xml";
	private static final String PLANSFILE = ROOTDIR + "plans_strict.xml";
	private static final String FACILITYFILE = ROOTDIR + "Facility.xml";

	private static final int ITERATIONTIMES = 5;

	private static final String TRANSITSCHEDULEFILE = ROOTDIR + "transitSchedule.xml";
	private static final String TRANSITVEHICLEFILE = ROOTDIR + "transitVehicles.xml";

	private static final String SUBPOP_ATTRIB_NAME = "subpopulation";
	private static final String SUBPOP1_NAME = "CARGROUP";
	private static final String SUBPOP2_NAME = "PAVGROUP";

	public static final int CELLSIZE = 500;

	private static final boolean ACTIVATE_TAXI_OR_NOT = false;
	private static final boolean ACTIVATE_OTFVIS_OR_NOT = false;
	private static final boolean ACTIVATE_PT_OR_NOT = true;
	private static final boolean ACTIVATE_PAV_OR_NOT = false;//使わない
	private static final boolean ACTIVATE_DRT_OR_NOT = false;
	private static final boolean ACTIVATE_ALLDRT_OR_NOT = false;
	private static final boolean ACTIVATE_AVAILABILITY_OR_NOT = true;//注意、自動運転ならfalse//今ではどっちもtrue
	private static final boolean ACTIVATE_ALLPAV_OR_NOT = false;//使わない
	private static final boolean ACTIVATE_ALLPAV_OR_NOT2 = false;//carをpavと読み換える、drtにも効果あり
	private static final boolean ACTIVATE_ACCESSIBILITY_OR_NOT = false;

	static String[] modesChanged2 = { "pav", "walk", "bike" };
	static String[] modesChanged = { "car", "walk", "bike" };

	public static void main(String[] args) {

		RunAccessibilityTest_drt_0430.createControler(CONFIGFILE, ACTIVATE_TAXI_OR_NOT, ACTIVATE_OTFVIS_OR_NOT,
				ACTIVATE_PT_OR_NOT, ACTIVATE_PAV_OR_NOT, ACTIVATE_DRT_OR_NOT, ACTIVATE_ALLDRT_OR_NOT, ACTIVATE_AVAILABILITY_OR_NOT,
				ACTIVATE_ALLPAV_OR_NOT, ACTIVATE_ALLPAV_OR_NOT2, ACTIVATE_ACCESSIBILITY_OR_NOT).run();
	}

	public static Controler createControler(String CONFIGFILE, boolean taxi_on, boolean otfvis_on, boolean pt_on,
			boolean pav_on, boolean drt_on, boolean alldrt_on, boolean availability_on, boolean allpav_on, boolean allpav_on2, boolean accessibility_on) {

		Config config = ConfigUtils.loadConfig(CONFIGFILE, new DrtConfigGroup(), new DvrpConfigGroup(),
				new TaxiConfigGroup(), new OTFVisConfigGroup(), new TaxiFareConfigGroup());

		if (accessibility_on) {
			AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
			accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
			accConfig.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
			accConfig.setCellSizeCellBasedAccessibility(CELLSIZE);
		}

		config.network().setInputFile(NETWORKFILE);
		config.plans().setInputFile(PLANSFILE);
		//config.plans().setNetworkRouteType(CompressedNetworkRoute);//処理が早くなるかもしれない
		config.facilities().setInputFile(FACILITYFILE);
		config.controler().setOutputDirectory(OUTPUTDIR);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setFirstIteration(1);
		config.controler().setLastIteration(ITERATIONTIMES);
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		config.controler().setMobsim("qsim");
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(108000);
		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.travelTimeCalculator().setSeparateModes(true);
		if(allpav_on2) {
			config.qsim().setFlowCapFactor(0.0269);//下の割る０.８
		}
		else {
		config.qsim().setFlowCapFactor(0.0215);
		}

		// config.qsim().setRemoveStuckVehicles(true);

		// a subtour is any sequence of trips which starts and ends at the same location
		//config.subtourModeChoice().setConsiderCarAvailability(true);
		// config.subtourModeChoice().setChainBasedModes("car, bike, pav");
		// config.subtourModeChoice().setModes("car, bike, pav, walk, taxi");

		// ----------------for drt-------------------------------------

		if (drt_on) {

			DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
			config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker());
			config.checkConsistency();
			/*
			 * if (drtCfg.getOperationalScheme().equals(DrtConfigGroup.OperationalScheme.
			 * stationbased)) {//stationbasedにしないからここは本当はいらない ActivityParams params =
			 * config.planCalcScore().getActivityParams(DrtStageActivityType.
			 * DRT_STAGE_ACTIVITY); if (params == null) { params = new
			 * ActivityParams(DrtStageActivityType.DRT_STAGE_ACTIVITY);
			 * params.setTypicalDuration(1); params.setScoringThisActivityAtAll(false);
			 * config.planCalcScore().addActivityParams(params);
			 * Logger.getLogger(DrtControlerCreator.class).info(
			 * "drt interaction scoring parameters not set. Adding default values (activity will not be scored)."
			 * ); } }
			 */

		}

		// ----------------for public transit-------------------------------------

		if (pt_on) {



			VariableAccessConfigGroup vacfg = new VariableAccessConfigGroup();

			if (drt_on==false&&pav_on==false) {

				{
					VariableAccessModeConfigGroup car = new VariableAccessModeConfigGroup();
					car.setDistance(60000);//ここは範囲内でもっとも短いものが使われるため、優先順位は一応最下位
					car.setTeleported(false);
					car.setMode("car");//自転車9km/hと同じ感じで車を表現するなら35km/h、だいたい
					vacfg.setAccessModeGroup(car);//otfvisでは肌色
				}
			}

			if (drt_on==false&&allpav_on==true) {

				{
					VariableAccessModeConfigGroup pav = new VariableAccessModeConfigGroup();
					pav.setDistance(60000);//ここは範囲内でもっとも短いものが使われるため、優先順位は一応最下位
					pav.setTeleported(false);
					pav.setMode("pav");
					vacfg.setAccessModeGroup(pav);//otfvisでは肌色
				}
			}

			if (taxi_on) {
				{
					VariableAccessModeConfigGroup taxi = new VariableAccessModeConfigGroup();
					taxi.setDistance(60000);
					taxi.setTeleported(false);
					taxi.setMode("taxi");
					vacfg.setAccessModeGroup(taxi);
				}
			}
			if (drt_on) {

				{
					VariableAccessModeConfigGroup drt = new VariableAccessModeConfigGroup();
					drt.setDistance(60000);//ここは範囲内でもっとも短いものが使われるため、優先順位は一応最下位
					drt.setTeleported(false);
					drt.setMode("drt");
					vacfg.setAccessModeGroup(drt);//otfvisでは肌色
				}
			}
			/*
			if (drt_on==false) {//KAMIJO
				{
					VariableAccessModeConfigGroup undefined = new VariableAccessModeConfigGroup();
					undefined.setDistance(60000);//ここは範囲内でもっとも短いものが使われるため、優先順位は一応最下位
					undefined.setTeleported(false);
					undefined.setMode("undefined");
					vacfg.setAccessModeGroup(undefined);

				}
			}
			*/
			{
				VariableAccessModeConfigGroup bike = new VariableAccessModeConfigGroup();
				bike.setDistance(4000);//どっちかでカバーしていないと駄目
				bike.setTeleported(true);
				bike.setMode("bike");//3kmまではバイクで行くでしょう
				vacfg.setAccessModeGroup(bike);//otfvisでは肌色

				if(drt_on==false&&pav_on==true&&allpav_on==false) {
					bike.setDistance(60000);
				}


				VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
				walk.setDistance(500);//どっちかでカバーしていないと駄目
				walk.setTeleported(true);
				walk.setMode("walk");
				vacfg.setAccessModeGroup(walk);//otfvisでは赤色
			}
			config.addModule(vacfg);

			config.transit().setUseTransit(true);
			config.transit().setTransitScheduleFile(TRANSITSCHEDULEFILE);
			config.transit().setVehiclesFile(TRANSITVEHICLEFILE);
			config.transit().setTransitModes("pt");

			config.transitRouter().setSearchRadius(15000);
			config.transitRouter().setExtensionRadius(0);
		}

		// ----------------for mixed traffic(in order to simulate two different modes,
		// and notice that taxi is an exception)--------------
		if (pav_on) {

			Collection<String> mainModes = new ArrayList<>();
			mainModes.add("car");
			mainModes.add("pav");
			// do not and no need adding taxi, pt, walk, access_walk and egress_walk, since
			// they have been injected from related module. LLC

			config.plansCalcRoute().setNetworkModes(mainModes);

			config.qsim().setMainModes(mainModes);
			config.qsim().setLinkDynamics(LinkDynamics.FIFO);
			config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);

			config.travelTimeCalculator().setAnalyzedModes("car,pav"); // still, not add taxi and pt. LLC(not sure)
			config.travelTimeCalculator().setSeparateModes(true);

			config.planCalcScore().getOrCreateModeParams("pav");// set default scoring params for pav
		}

		// ---------------------score settings---------------------------------------

		// specify the modes that could be chosen for the next iteration
		if (alldrt_on == true) {
			modesChanged = deleteElement(modesChanged, "car");

		}

		if (allpav_on == true) {
			modesChanged = deleteElement(modesChanged, "car");
			modesChanged = addElement(modesChanged, "pav");
		}

		if (pt_on == true) {
			modesChanged = addElement(modesChanged, "pt");
		}

		if (drt_on == true) {
			modesChanged = addElement(modesChanged, "drt");
		}

		if (taxi_on == true) {
			modesChanged = addElement(modesChanged, "taxi");
		}

		config.changeMode().setModes(modesChanged);
		// ---------------------simulation
		// loading---------------------------------------

		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();

		// car&pav availability settings
		if (availability_on) {
			config.changeMode().setIgnoreCarAvailability(false);
			/*
			 * int totalPersonSize = scenario.getPopulation().getPersons().size(); for
			 * (Person person : scenario.getPopulation().getPersons().values()) { if
			 * (Integer.valueOf(person.getId().toString()) < totalPersonSize / 2) {
			 * ObjectAttributes personAttributes =
			 * scenario.getPopulation().getPersonAttributes();
			 * personAttributes.putAttribute(person.getId().toString(), "carAvail",
			 * "never"); person.getCustomAttributes().put("carAvail", "never");
			 * person.getAttributes().putAttribute("carAvail", "never"); } }
			 */
		}

		// notice that do not modify config anymore after load the scenario! LLC
		Scenario scenario = ScenarioUtils.loadScenario(config);

		List<String> activityTypes = AccessibilityUtils.collectAllFacilityOptionTypes(scenario);

		if (pav_on) {
			addExtraModesToNetwork(scenario, "pav");
		}
		/*
		if (pt_on) {
			addExtraModesToNetwork(scenario, "pt");
		}
		*/
		if (drt_on) {
			addExtraModesToNetwork(scenario, "drt");
		}

		if (taxi_on) {
			addExtraModesToNetwork(scenario, "taxi");
		}
		//flowcapfactorで設定済み（過去の遺物）


		//adjustRoadCapacity(scenario, 50);// actually i just noticed that this method does the same thing with
											// flowCapacityFactor LLC

		// create new transport mode and/or set performance parameters for it
		VehicleType car = scenario.getVehicles().getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(100 / 3.6);
		car.setPcuEquivalents(1.0);
		/*
		if(allpav_on2) {
			car.setFlowEfficiencyFactor(1.0);
		}
		*/
		scenario.getVehicles().addVehicleType(car);
		/*
		if (pt_on) {
			VehicleType pt = new VehicleTypeImpl(Id.create("pt", VehicleType.class));
			pt.setMaximumVelocity(80 / 3.6);
			pt.setPcuEquivalents(10.0);
			scenario.getVehicles().addVehicleType(pt);
		}
		*/
		if (drt_on) {
			VehicleType drt = new VehicleTypeImpl(Id.create("drt", VehicleType.class));
			drt.setMaximumVelocity(100 / 3.6);
			drt.setPcuEquivalents(1.0);
			scenario.getVehicles().addVehicleType(drt);
		}

		if (pav_on) {
			VehicleType pav = new VehicleTypeImpl(Id.create("pav", VehicleType.class));
			pav.setMaximumVelocity(100 / 3.6);
			pav.setPcuEquivalents(1.0);
			pav.setFlowEfficiencyFactor(1.0);//1.2台分で普通自動車１台分のFlowEfficiencyしか消費しないという意味、上に等しいはず
			scenario.getVehicles().addVehicleType(pav);
		}

		if (alldrt_on==true) {
			// Random random2 = new Random();
			for (Person person : scenario.getPopulation().getPersons().values()) {
				// if (random2.nextDouble() > 0.2)
				// continue;

				Plan plan = person.getSelectedPlan();
				List<PlanElement> pes = plan.getPlanElements();

				for (PlanElement pe : pes) {
					if (pe instanceof Leg && ((Leg) pe).getMode().equals("car")) {
						((Leg) pe).setRoute(null);
						((Leg) pe).setMode("drt");
					}
				}
			}
		}


		if (drt_on==false) {
			// Random random2 = new Random();
			for (Person person : scenario.getPopulation().getPersons().values()) {
				// if (random2.nextDouble() > 0.2)
				// continue;

				Plan plan = person.getSelectedPlan();
				List<PlanElement> pes = plan.getPlanElements();

				for (PlanElement pe : pes) {
					if (pe instanceof Leg && ((Leg) pe).getMode().equals("drt")) {
						((Leg) pe).setRoute(null);
						((Leg) pe).setMode("car");
					}
				}
			}
		}

		if (taxi_on) {
			// VehicleType taxi
			// =scenario.getVehicles().getFactory().createVehicleType(Id.create("taxi",VehicleType.class));
			VehicleType taxi = new VehicleTypeImpl(Id.create("taxi", VehicleType.class));
			taxi.setMaximumVelocity(100 / 3.6);
			taxi.setPcuEquivalents(1.0);
			scenario.getVehicles().addVehicleType(taxi);

			// assign a certain percentage of agents with car mode to taxi mode
			Random random = new Random();
			for (Person person : scenario.getPopulation().getPersons().values()) {
				if (random.nextDouble() > 0.2)
					continue;

				Plan plan = person.getSelectedPlan();
				List<PlanElement> pes = plan.getPlanElements();

				for (PlanElement pe : pes) {
					if (pe instanceof Leg && ((Leg) pe).getMode().equals("car"))
						;

					((Leg) pe).setRoute(null);
					((Leg) pe).setMode("taxi");

				}
			}
		}
		/*
		 * これをオンにすると2割の人の全トリップがdrtになる // drt if (drt_on) { Random random2 = new
		 * Random(); for (Person person :
		 * scenario.getPopulation().getPersons().values()) { if (random2.nextDouble() >
		 * 0.2) continue;
		 *
		 * Plan plan = person.getSelectedPlan(); List<PlanElement> pes =
		 * plan.getPlanElements();
		 *
		 * for (PlanElement pe : pes) { if (pe instanceof Leg) { ((Leg)
		 * pe).setRoute(null); ((Leg) pe).setMode("drt"); } } } }
		 */

		// allpav
		if (allpav_on) {
			// Random random2 = new Random();
			for (Person person : scenario.getPopulation().getPersons().values()) {
				// if (random2.nextDouble() > 0.2)
				// continue;

				Plan plan = person.getSelectedPlan();
				List<PlanElement> pes = plan.getPlanElements();

				for (PlanElement pe : pes) {
					if (pe instanceof Leg && ((Leg) pe).getMode().equals("car")) {
						((Leg) pe).setRoute(null);
						((Leg) pe).setMode("pav");
					}
				}
			}
		}

		//drt
		/*
		Random random3 = new Random(); for (Person person :
		scenario.getPopulation().getPersons().values()) { if (random3.nextDouble() >
		1.0 ) continue;

		Plan plan = person.getSelectedPlan(); List<PlanElement> pes =
		plan.getPlanElements();

		 for (PlanElement pe : pes) { if (pe instanceof Leg) { ((Leg)
		 pe).setRoute(null); ((Leg) pe).setMode("drt"); } } }
		*/

		//pt
		/*
		Random random3 = new Random(); for (Person person :
		scenario.getPopulation().getPersons().values()) { if (random3.nextDouble() >
		0.5 ) continue;

		Plan plan = person.getSelectedPlan(); List<PlanElement> pes =
		plan.getPlanElements();

		 for (PlanElement pe : pes) { if (pe instanceof Leg) { ((Leg)
		 pe).setRoute(null); ((Leg) pe).setMode("pt"); } } }
		 */
		for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
            // divide population into two equal sub-populations LLC
            //if (random2.nextDouble() > 0.5 ) {
                scenario.getPopulation().getPersonAttributes().putAttribute(
                		personId.toString(), SUBPOP_ATTRIB_NAME, SUBPOP1_NAME);

          // } else {
            //    scenario.getPopulation().getPersonAttributes().putAttribute(
               // 		personId.toString(), SUBPOP_ATTRIB_NAME, SUBPOP2_NAME);
                /*
                Map<Id<Person>, ? extends Person> personMap = scenario.getPopulation().getPersons();
                Plan plan = personMap.get(personId).getSelectedPlan();
				List<PlanElement> pes = plan.getPlanElements();

				for (PlanElement pe : pes) {
					if (pe instanceof Leg && ((Leg) pe).getMode().equals("car")) {
						((Leg) pe).setRoute(null);
						((Leg) pe).setMode("pav");
					}
				}*/
            }
        //}

		scenario.getConfig().plans().setSubpopulationAttributeName(SUBPOP_ATTRIB_NAME);
		// clear previous strategies
		scenario.getConfig().strategy().clearStrategySettings();

		// check addOverridingModule @Override tripRouterProvider LLC
		// add innovative modules for SUBPOP1
		{
			StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings();
			modeChoiceStrategySettings
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString());
			modeChoiceStrategySettings.setSubpopulation(SUBPOP1_NAME);
			modeChoiceStrategySettings.setWeight(0.3);
			scenario.getConfig().strategy().addStrategySettings(modeChoiceStrategySettings);

			StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
			changeExpBetaStrategySettings
					.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation(SUBPOP1_NAME);
			changeExpBetaStrategySettings.setWeight(0.7);
			scenario.getConfig().strategy().addStrategySettings(changeExpBetaStrategySettings);
		}


		if (pav_on) {
			/*
			 * int totalPersonSize = scenario.getPopulation().getPersons().size();
		        for (Id<Person> p : scenario.getPopulation().getPersons().keySet()) {
		            int personIdInteger = Integer.valueOf(p.toString());
		            // divide population into two equal sub-populations LLC
		            if ( personIdInteger < totalPersonSize/2  ) {
		                scenario.getPopulation().getPersonAttributes().putAttribute(
		                		p.toString(), SUBPOP_ATTRIB_NAME, SUBPOP1_NAME);
		            } else {
		                scenario.getPopulation().getPersonAttributes().putAttribute(
		                		p.toString(), SUBPOP_ATTRIB_NAME, SUBPOP2_NAME);
		            }
		        }

				*/

			 // allocate a certain percentage of agents with PAV mode
			Random random2 = new Random();
			for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
	            // divide population into two equal sub-populations LLC
	            if (random2.nextDouble() > 0.5 ) {
	                scenario.getPopulation().getPersonAttributes().putAttribute(
	                		personId.toString(), SUBPOP_ATTRIB_NAME, SUBPOP1_NAME);

	            } else {
	                scenario.getPopulation().getPersonAttributes().putAttribute(
	                		personId.toString(), SUBPOP_ATTRIB_NAME, SUBPOP2_NAME);

	                Map<Id<Person>, ? extends Person> personMap = scenario.getPopulation().getPersons();
	                Plan plan = personMap.get(personId).getSelectedPlan();
					List<PlanElement> pes = plan.getPlanElements();

					for (PlanElement pe : pes) {
						if (pe instanceof Leg && ((Leg) pe).getMode().equals("car")) {
							((Leg) pe).setRoute(null);
							((Leg) pe).setMode("pav");
						}
					}
	            }
	        }

			scenario.getConfig().plans().setSubpopulationAttributeName(SUBPOP_ATTRIB_NAME);
			// clear previous strategies
			scenario.getConfig().strategy().clearStrategySettings();

			// check addOverridingModule @Override tripRouterProvider LLC
			// add innovative modules for SUBPOP1
			{
				StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings();
				modeChoiceStrategySettings
						.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString());
				modeChoiceStrategySettings.setSubpopulation(SUBPOP1_NAME);
				modeChoiceStrategySettings.setWeight(0.3);
				scenario.getConfig().strategy().addStrategySettings(modeChoiceStrategySettings);

				StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
				changeExpBetaStrategySettings
						.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
				changeExpBetaStrategySettings.setSubpopulation(SUBPOP1_NAME);
				changeExpBetaStrategySettings.setWeight(0.7);
				scenario.getConfig().strategy().addStrategySettings(changeExpBetaStrategySettings);
			}

			// add innovative modules for SUBPOP2
			{
				StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings();
				modeChoiceStrategySettings.setStrategyName(
						DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString().concat(SUBPOP2_NAME));
				modeChoiceStrategySettings.setSubpopulation(SUBPOP2_NAME);
				modeChoiceStrategySettings.setWeight(0.3);
				scenario.getConfig().strategy().addStrategySettings(modeChoiceStrategySettings);

				StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
				changeExpBetaStrategySettings
						.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
				changeExpBetaStrategySettings.setSubpopulation(SUBPOP2_NAME);
				changeExpBetaStrategySettings.setWeight(0.7);
				scenario.getConfig().strategy().addStrategySettings(changeExpBetaStrategySettings);
			}
		}

		Controler controler = new Controler(scenario);

		if (taxi_on) {
			controler.addOverridingModule(new TaxiOutputModule());

			controler.addOverridingModule(new TaxiModule());
		}

		// drt
		if (drt_on) {
			controler.addOverridingModule(new DvrpModule(DrtControlerCreator.createModuleForQSimPlugin(),
					DrtOptimizer.class, DefaultUnplannedRequestInserter.class));
			controler.addOverridingModule(new DrtModule());
			controler.addOverridingModule(new DrtAnalysisModule());
		}

		if (pt_on) {
			controler.addOverridingModule(new VariableAccessTransitRouterModule());
		}

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});

		if (pav_on) {

			if (pt_on) {
				modesChanged2 = addElement(modesChanged2, "pt");
			}


			if (drt_on) {
				modesChanged2 = addElement(modesChanged2, "drt");
			}

			if (taxi_on) {
				modesChanged2 = addElement(modesChanged2, "taxi");
			}
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding(
							DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name().concat(SUBPOP2_NAME))
									.toProvider(new javax.inject.Provider<PlanStrategy>() {
										final String[] availableModes = modesChanged2;

										@Inject
										Scenario sc;

										@Override
										public PlanStrategy get() {
											final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(
													new RandomPlanSelector<>());
											builder.addStrategyModule(
													new TripsToLegsModule(tripRouterProvider, sc.getConfig().global()));
											builder.addStrategyModule(
													new ChangeLegMode(sc.getConfig().global().getNumberOfThreads(),
															availableModes, true));
											builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
											return builder.build();
										}
									});
				}
			});
		}
		if (accessibility_on) {
			Log.warn("found the following activity types: " + activityTypes);

			//for (final String actType : activityTypes) { // add an overriding module per activity type:
				final AccessibilityModule accessibilityModule = new AccessibilityModule();
				accessibilityModule.setConsideredActivityType("leisure_shop");
				// accessibilityModule.setPushing2Geoserver(true);
				controler.addOverridingModule(accessibilityModule);
			//}
		}
		if (otfvis_on) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}

	public static void addExtraModesToNetwork(Scenario scenario, String... args) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> allowedModes = new HashSet<>();
			if (link.getAllowedModes().contains("pt")) continue;
				for (String s : args) {
					allowedModes.add(s);
				allowedModes.addAll(link.getAllowedModes());
				link.setAllowedModes(allowedModes);
			}
		}
	}

	public static void adjustRoadCapacity(Scenario scenario, int fold) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setCapacity(link.getCapacity() / fold);
		}
	}

	public static String[] addElement(String[] inputArray, String toBeAdded) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < inputArray.length; i++) {
			list.add(inputArray[i]);
		}
		list.add(toBeAdded);
		String[] newArray = list.toArray(new String[1]);

		return newArray;
	}

	public static String[] deleteElement(String[] inputArray, String toBedeleted) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < inputArray.length; i++) {
			list.add(inputArray[i]);
		}
		list.remove(toBedeleted);
		String[] newArray = list.toArray(new String[1]);

		return newArray;
	}
}
