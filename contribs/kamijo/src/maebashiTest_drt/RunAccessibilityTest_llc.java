package maebashiTest_drt;

/**
 * @author LLC
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

import org.jfree.util.Log;
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
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
//import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


public class RunAccessibilityTest_llc {

	private static final String ROOTDIR = "C:/Users/MATSIM/desktop/UT_MATSim/resources/Final/";
	private static final String OUTPUTDIR = ROOTDIR+"outputTTTTestGREATPARA/";
	private static final String CONFIGFILE = ROOTDIR+"config.xml";
	private static final String NETWORKFILE = ROOTDIR+"NetworkRedNew.xml";
	private static final String PLANSFILE = ROOTDIR+"Plans0510.xml";
	private static final String FACILITYFILE = ROOTDIR+"FacilityLeiShop.xml";

	private static final int ITERATIONTIMES = 50;

	private static final String TRANSITSCHEDULEFILE = ROOTDIR+"transitSchedule.xml";
	private static final String TRANSITVEHICLEFILE = ROOTDIR+"transitVehicles.xml";

    private static final String SUBPOP_ATTRIB_NAME = "subpopulation";
    private static final String SUBPOP1_NAME = "CARGROUP";
    private static final String SUBPOP2_NAME = "PAVGROUP";

    public static final int CELLSIZE = 2000;

    private static final boolean ACTIVATE_OTFVIS_OR_NOT = false;
    private static final boolean ACTIVATE_PT_OR_NOT = false;
    private static final boolean ACTIVATE_PAV_OR_NOT = false;
    private static final boolean ACTIVATE_AVAILABILITY_OR_NOT = true;
    private static final boolean ACTIVATE_ACCESSIBILITY_OR_NOT = false;

    static String[] modesChanged2 = {"pav","walk","bike","taxi"};  //pav group
    static String[] modesChanged = {"car","walk","bike", "taxi"};//car group
    static String[] chainBasedModes2= {"bike"};


	public static void main(String[] args) {

		RunAccessibilityTest_llc.createAndRunControler(CONFIGFILE, ACTIVATE_OTFVIS_OR_NOT, ACTIVATE_PT_OR_NOT,
				ACTIVATE_PAV_OR_NOT, ACTIVATE_AVAILABILITY_OR_NOT, ACTIVATE_ACCESSIBILITY_OR_NOT);

	}

	public static Controler createAndRunControler(String CONFIGFILE, boolean otfvis_on, boolean pt_on,
			boolean pav_on, boolean availability_on, boolean accessibility_on) {

		Config config = ConfigUtils.loadConfig(CONFIGFILE, new DvrpConfigGroup(), new TaxiConfigGroup(),
				new OTFVisConfigGroup(), new TaxiFareConfigGroup(), new AccessibilityConfigGroup());


		AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		//accConfig.setComputingAccessibilityForMode(Modes4Accessibility.pav, true);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		accConfig.setCellSizeCellBasedAccessibility(CELLSIZE);
		//final Set<Modes4Accessibility> computingModes = accConfig.getIsComputingMode();


		config.network().setInputFile(NETWORKFILE);
		config.plans().setInputFile(PLANSFILE);
		config.facilities().setInputFile(FACILITYFILE);
		config.controler().setOutputDirectory(OUTPUTDIR);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(ITERATIONTIMES-1);
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		config.controler().setMobsim("qsim");
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(108000);

		//config.qsim().setRemoveStuckVehicles(true);

		// a subtour is any sequence of trips which starts and ends at the same location
		//config.subtourModeChoice().setConsiderCarAvailability(true);
		//config.subtourModeChoice().setChainBasedModes("car, bike");
		//config.subtourModeChoice().setModes("car, bike, pav, walk, taxi");


// ----------------for public transit-------------------------------------

		if(pt_on) {

			VariableAccessConfigGroup vacfg = new VariableAccessConfigGroup();

				{
				VariableAccessModeConfigGroup taxi = new VariableAccessModeConfigGroup();
				taxi.setDistance(60000);
				taxi.setTeleported(false);
				taxi.setMode("taxi");
				vacfg.setAccessModeGroup(taxi);
				}

				{
				VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
				walk.setDistance(1000);
				walk.setTeleported(true);
				walk.setMode("walk");
				vacfg.setAccessModeGroup(walk);
				}
			config.addModule(vacfg);

			config.transit().setUseTransit(true);
			config.transit().setTransitScheduleFile(TRANSITSCHEDULEFILE);
			config.transit().setVehiclesFile(TRANSITVEHICLEFILE);
			config.transit().setTransitModes("pt");

			config.transitRouter().setSearchRadius(15000);
			config.transitRouter().setExtensionRadius(0);
		}

// ----------------for mixed traffic(in order to simulate two different modes, and notice that taxi is an exception)--------------
		if(pav_on) {

			Collection<String> mainModes = new ArrayList<>();
			mainModes.add("car");
			mainModes.add("pav");
			//do not and no need adding taxi, pt, walk, access_walk and egress_walk, since they have been injected from related module. LLC

			config.plansCalcRoute().setNetworkModes(mainModes);

			config.qsim().setMainModes(mainModes);
			//config.qsim().setLinkDynamics(LinkDynamics.FIFO);
			//config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);

			config.travelTimeCalculator().setAnalyzedModes("car,pav"); // still, do not add taxi and pt. LLC(not sure)
			config.travelTimeCalculator().setSeparateModes(true);

			//config.planCalcScore().getOrCreateModeParams("pav");// set default scoring params for pav
		}

// ---------------------score settings---------------------------------------

		// specify the modes that could be chosen for the next iteration

		if(pt_on) {
			modesChanged = addElement(modesChanged, "pt");
			config.subtourModeChoice().setModes(modesChanged);
		}

// ---------------------simulation loading---------------------------------------

		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();

		// notice that do not modify config anymore after load the scenario! LLC
		Scenario scenario = ScenarioUtils.loadScenario(config);

		List<String> activityTypes = AccessibilityUtils.collectAllFacilityOptionTypes(scenario);

		if(pav_on){
			addExtraModesToNetwork(scenario, "pav");
		}
		if(pt_on){
			addExtraModesToNetwork(scenario, "pt");
		}

		addExtraModesToNetwork(scenario, "taxi");

		//adjustRoadCapacity(scenario, 40); // actually i just noticed that this method does the same thing with flowCapacityFactor LLC

		// create new transport mode and/or set performance parameters for it
		VehicleType car =scenario.getVehicles().getFactory().createVehicleType(Id.create("car",VehicleType.class));
		car.setMaximumVelocity(80/3.6);
		car.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(car);

		VehicleType bike =scenario.getVehicles().getFactory().createVehicleType(Id.create("bike",VehicleType.class)); // ==bicycle not motorbike
		bike.setMaximumVelocity(20/3.6);
		bike.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bike);

		if(pt_on) {
			VehicleType pt = new VehicleTypeImpl(Id.create("pt", VehicleType.class));
			pt.setMaximumVelocity(80/3.6);
			pt.setPcuEquivalents(10.0);
			scenario.getVehicles().addVehicleType(pt);
		}

		if(pav_on) {
			VehicleType pav = new VehicleTypeImpl(Id.create("pav", VehicleType.class));
			pav.setMaximumVelocity(80/3.6);
			pav.setPcuEquivalents(0.8);
			pav.setFlowEfficiencyFactor(0.8);
			scenario.getVehicles().addVehicleType(pav);
		}

		//VehicleType taxi =scenario.getVehicles().getFactory().createVehicleType(Id.create("taxi",VehicleType.class));
		VehicleType taxi = new VehicleTypeImpl(Id.create("taxi", VehicleType.class));
		taxi.setMaximumVelocity(80/3.6);
		taxi.setPcuEquivalents(0.8);
		scenario.getVehicles().addVehicleType(taxi);

		//assign a certain percentage of agents with car mode to taxi mode
		Random random = new Random();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (random.nextDouble() > 0.20 ) continue;

			Plan plan = person.getSelectedPlan();
			List<PlanElement> pes = plan.getPlanElements();

			for (PlanElement pe : pes) {
				//if (pe instanceof Leg && ((Leg) pe).getMode().equals("car") ) {
				if (pe instanceof Leg ) {
					((Leg) pe).setRoute(null);
					((Leg) pe).setMode("taxi");

				}
			}
		}




		// car&pav availability settings
		if (availability_on) {

	/*
		int totalPersonSize = scenario.getPopulation().getPersons().size();
			for (Person person : scenario.getPopulation().getPersons().values()) {
				if(Integer.valueOf(person.getId().toString()) < totalPersonSize/2  ) {
				person.getAttributes().putAttribute("carAvail","never") ;
				}
			}
			*/
		}
		 // allocate a certain percentage of agents with PAV mode

		if (pav_on) {
			Random random2 = new Random();
		        for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
		            // divide population into two equal sub-populations LLC
		            if (random2.nextDouble() > 0.0) {
		                scenario.getPopulation().getPersonAttributes().putAttribute(
		                		personId.toString(), SUBPOP_ATTRIB_NAME, SUBPOP1_NAME);
		            } else {
		                scenario.getPopulation().getPersonAttributes().putAttribute(
		                		personId.toString(), SUBPOP_ATTRIB_NAME, SUBPOP2_NAME);

		                Map<Id<Person>, ? extends Person> personMap = scenario.getPopulation().getPersons();
		                Plan plan = personMap.get(personId).getSelectedPlan();
						List<PlanElement> pes = plan.getPlanElements();

						for (PlanElement pe : pes) {
							if (pe instanceof Leg) {
								((Leg) pe).setRoute(null);
								((Leg) pe).setMode("pav");
							}
						}
		            }
		        }

	        scenario.getConfig().plans().setSubpopulationAttributeName(SUBPOP_ATTRIB_NAME);
	        // clear previous strategies
	        scenario.getConfig().strategy().clearStrategySettings();

	        // check addOverridingModule    @Override  tripRouterProvider LLC
	        // add innovative modules for SUBPOP1
	        {
	            StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings() ;
	            modeChoiceStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.toString());
	            modeChoiceStrategySettings.setSubpopulation(SUBPOP1_NAME);
	            modeChoiceStrategySettings.setWeight(0.2);
	            scenario.getConfig().strategy().addStrategySettings(modeChoiceStrategySettings);

	            StrategyConfigGroup.StrategySettings timeMutatorStrategySettings = new StrategyConfigGroup.StrategySettings();
	            timeMutatorStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
	            timeMutatorStrategySettings.setSubpopulation(SUBPOP1_NAME);
	            timeMutatorStrategySettings.setWeight(0.1);
	            scenario.getConfig().strategy().addStrategySettings(timeMutatorStrategySettings);

	            StrategyConfigGroup.StrategySettings selectExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
	            selectExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectExpBeta.toString());
	            selectExpBetaStrategySettings.setSubpopulation(SUBPOP1_NAME);
	            selectExpBetaStrategySettings.setWeight(0.7);
	            scenario.getConfig().strategy().addStrategySettings(selectExpBetaStrategySettings);
	        }

	        // add innovative modules for SUBPOP2
	        {
	            StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings() ;
	            modeChoiceStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.toString().concat(SUBPOP2_NAME)); // a different name is must. Amit June'17
	            modeChoiceStrategySettings.setSubpopulation(SUBPOP2_NAME);
	            modeChoiceStrategySettings.setWeight(0.2);
	            scenario.getConfig().strategy().addStrategySettings(modeChoiceStrategySettings);

	            StrategyConfigGroup.StrategySettings TimeMutatorStrategySettings = new StrategyConfigGroup.StrategySettings();
	            TimeMutatorStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
	            TimeMutatorStrategySettings.setSubpopulation(SUBPOP2_NAME);
	            TimeMutatorStrategySettings.setWeight(0.1);
	            scenario.getConfig().strategy().addStrategySettings(TimeMutatorStrategySettings);

	            StrategyConfigGroup.StrategySettings selectExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
	            selectExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectExpBeta.toString());
	            selectExpBetaStrategySettings.setSubpopulation(SUBPOP2_NAME);
	            selectExpBetaStrategySettings.setWeight(0.7);
	            scenario.getConfig().strategy().addStrategySettings(selectExpBetaStrategySettings);
	        }
		}

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new TaxiOutputModule());

		controler.addOverridingModule(new TaxiModule());


		if(pt_on) {
			controler.addOverridingModule(new VariableAccessTransitRouterModule());
		}

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});

		if(pav_on) {

			if(pt_on) {
				modesChanged2 = addElement(modesChanged2, "pt");
			}

		 controler.addOverridingModule(new AbstractModule() {
	            @Override
	            public void install() {
	                final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
	                addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.name().concat(SUBPOP2_NAME)).toProvider(new javax.inject.Provider<PlanStrategy>() {
	                    final String[] availableModes = modesChanged2 ;

	                    @Inject
	                    Scenario sc;

	                    @Override
	                    public PlanStrategy get() {
	                        final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
	                        builder.addStrategyModule(new TripsToLegsModule(tripRouterProvider, sc.getConfig().global()));
	                        builder.addStrategyModule(new SubtourModeChoice(sc.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes2, true, tripRouterProvider));
	                        builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
	                        return builder.build();
	                    }
	                });
	            }
	        });
		}

		if(accessibility_on) {
			Log.warn( "found the following activity types: " + activityTypes );

			//List<String> leiShopWork = new ArrayList<String>();

				//	leiShopWork.add("leisure");
				//  leiShopWork.add("shop");
				//	leiShopWork.add("work");

			//for (final String actType : leiShopWork) { // add an overriding module per activity type:
				final AccessibilityModule accessibilityModule = new AccessibilityModule();
				accessibilityModule.setConsideredActivityType("leiShop");
				controler.addOverridingModule(accessibilityModule);
			//}
		}

		if (otfvis_on) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		controler.run();

		return controler;
	}




	public static void addExtraModesToNetwork(Scenario scenario, String... args) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> allowedModes = new HashSet<>();
			for (String s:args)
			allowedModes.add(s);
			allowedModes.addAll(link.getAllowedModes());
			link.setAllowedModes(allowedModes);
		}
	}

	public static void adjustRoadCapacity(Scenario scenario, int fold) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setCapacity(link.getCapacity()/fold);
		}
	}

	public static String[] addElement(String[] inputArray, String toBeAdded) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i <inputArray.length; i++) {
			list.add(inputArray[i]);
		}
		list.add(1, toBeAdded);
		String[] newArray = list.toArray(new String[1]);

		return newArray;
	}
}
