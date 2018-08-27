package maebashiTest_drt;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.ConfigUtils;

import com.vividsolutions.jts.geom.Envelope;

public class RunQGisVisualize extends RunAccessibilityTest_drt {

	private static final String ROOTDIR = "/Users/jo/git/matsim/contribs/UT_MATSim/resources/Numata_0430_result/";
	private static final String OUTPUTDIR = ROOTDIR+"output_i200_1-1_0_1190/";
    private static final Envelope envelope = new Envelope(17191, 71569, 32524, 79840);
    static String scenarioCRS = "EPSG:2450";

	public static void main(String[] args) {

		final boolean includeDensityLayer = false;
		final Integer range = 9;
		final Double lowerBound = 0.;
		final Double upperBound = 3.5;
		final int populationThreshold = (int) (50./(1000./CELLSIZE * 1000./CELLSIZE));

		Set<Modes4Accessibility> computingModes = new HashSet();
		computingModes.add(Modes4Accessibility.freespeed);
		computingModes.add(Modes4Accessibility.car);

		String osName = System.getProperty("os.name");
		//for (String actType : activityTypes) {
			String activitySpecificDirectory = OUTPUTDIR + "leisure_shop" + "/";

			for (Modes4Accessibility mode : computingModes) {
				VisualizationUtils.createQGisOutputRuleBasedStandardColorRange("leisure_shop", mode.toString(), envelope, OUTPUTDIR, scenarioCRS,
						includeDensityLayer, lowerBound, upperBound, range, CELLSIZE, populationThreshold);
				VisualizationUtils.createSnapshot(activitySpecificDirectory, mode.toString(), osName);
		//	}
			}
	}
}
