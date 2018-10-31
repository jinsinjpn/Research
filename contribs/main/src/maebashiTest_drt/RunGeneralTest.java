package maebashiTest_drt;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.GridBasedAccessibilityShutdownListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.run.RunAccessibilityExample;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.opengis.geometry.Envelope;

public class RunGeneralTest {
	private static final String ROOTDIR = "C:/Users/MATSim/eclipse-workspace/UT_MATSim/resources/";
	private static final String OUTPUTDIR = ROOTDIR+"/output2";
	private static final String CONFIGFILE = ROOTDIR+"config2.xml";
	private static final String NETWORKFILE = ROOTDIR+"MaebashiNetwork.xml";;
	private static final String PLANSFILE = ROOTDIR+"MaebashiPlans.xml";
	private static final String FACILITYFILE = ROOTDIR+"MaebashiFacility.xml";
	
	private static final int ITERATIONTIMES = 1; 
	

    private static final boolean ACTIVATE_OTFVIS_OR_NOT = false; 
    
    private static final String accessibilityOutputDirectory = ROOTDIR+"/ibilities";
    private static final Double cellSize = 200.;
   // Envelope envelope = new Envelope(111,111,111,111);
    
	private static final Logger log = Logger.getLogger(RunGeneralTest.class);
    
    public static void main(String[] args) {
    	RunGeneralTest.createControler(CONFIGFILE, ACTIVATE_OTFVIS_OR_NOT).run();
    	
    }
    
    public static Controler createControler(String CONFIGFILE, boolean otfvis_on) {
    	
    	Config config = ConfigUtils.loadConfig(CONFIGFILE, new DvrpConfigGroup(), new TaxiConfigGroup(),
				new OTFVisConfigGroup(), new TaxiFareConfigGroup());
		
	
		config.network().setInputFile(NETWORKFILE);
		config.plans().setInputFile(PLANSFILE);
		config.facilities().setInputFile(FACILITYFILE);
		config.controler().setOutputDirectory(OUTPUTDIR);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(ITERATIONTIMES-1);
		config.controler().setWriteEventsInterval(1);
		config.controler().setMobsim("qsim");
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(86400);		
		
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();	
		
		/*final AccessibilityConfigGroup acg = new AccessibilityConfigGroup();
		acg.setCellSizeCellBasedAccessibility(1000);
		acg.setTimeOfDay(28800.0);
		config.addModule(acg); 
		*/
		
		AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ) ;
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
	
			Controler controler = new Controler(scenario);
			
				
				List<String> activityTypes = AccessibilityUtils.collectAllFacilityOptionTypes(scenario) ;
				log.warn( "found the following activity types: " + activityTypes );

				for (final String actType : activityTypes) { // add an overriding module per activity type:
					final AccessibilityModule module = new AccessibilityModule();
					module.setConsideredActivityType(actType);
					controler.addOverridingModule(module);
				}

			
	/*		ActivityFacilities opportunities = scenario.getActivityFacilities();
		
			final List<String> activityTypes = new ArrayList<String>();
			for (ActivityFacility fac : opportunities.getFacilities().values()) { 
				for (ActivityOption option : fac.getActivityOptions().values()) {
					if(!activityTypes.contains(option.getType())) {
						activityTypes.add(option.getType());
					}
			}
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					for (final String actType : activityTypes) {
						final ActivityFacilities opportunities = FacilitiesUtils.createActivityFacilities();
						for (ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
							for (ActivityOption option : fac.getActivityOptions().values()) {
								if(option.getType().equals(actType)) {
									opportunities.addActivityFacility(fac);
								}
							}
						}
						addControlerListenerBinding().toProvider(new Provider<ControlerListener>(){
							@Inject Map<String, TravelTime> travelTimes;
							@Inject Map<String, TravelDisUtilityFactory> travelDisutilityFactories;
							
							@Override
							public ControlerListener get() {
								Double cellSizeForCellBasedAccessibility = Double.parseDouble(scenario.getConfig().getModule("accessibility").getValue("cellSizeForCellBasedAccessibility"));
								Config config = scenario.getConfig();
								
								BoundingBox bb = BoundingBox.createBoundingBox(scenario.getNetwork());
								AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, scenario);
								accessibilityCalculator.setMeasuringPosints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSizeForCellBasedAccessibility));
							}
						}
					}
				}
			}
				
			GridBasedAccessibilityShutdownListenerV3 accessibilityListener = 
					new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, opportunities, null, config, scenario,
							travelTimes. travelDisutilityFactories, bb.getXMin, bb.getYMin, bb.getXMax, bb.getYMax, cellSizeForBasedAccessibility);
			
			controler.addControlerListener(new GridBasedAccessibilityShutdownListenerV3());
*/
			controler.addOverridingModule(new TaxiOutputModule());
				
			controler.addOverridingModule(new TaxiModule());
				
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
				}
			});
				
			if (otfvis_on) {
				controler.addOverridingModule(new OTFVisLiveModule());
			}
			controler.run();
				
			return controler;
	
    }
}
