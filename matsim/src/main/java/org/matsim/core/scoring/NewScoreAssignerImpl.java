package org.matsim.core.scoring;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.utils.geometry.CoordUtils;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;

class NewScoreAssignerImpl implements NewScoreAssigner {

	static private final Logger log = Logger.getLogger(NewScoreAssignerImpl.class);

	private Map<Plan,Integer> msaContributions = new HashMap<>() ;
	private Integer scoreMSAstartsAtIteration;
	private final double learningRate;
	private double scoreSum = 0.0;
	private long scoreCount = 0;
	//private final Network network;//KAMIJO

	@Inject
	NewScoreAssignerImpl(PlanCalcScoreConfigGroup planCalcScoreConfigGroup, ControlerConfigGroup controlerConfigGroup) {
		//this.network = QNetwork.getNetwork();//KAMIJO
		if (planCalcScoreConfigGroup.getFractionOfIterationsToStartScoreMSA()!=null ) {
			final int diff = controlerConfigGroup.getLastIteration() - controlerConfigGroup.getFirstIteration();
			this.scoreMSAstartsAtIteration = (int) (diff
					* planCalcScoreConfigGroup.getFractionOfIterationsToStartScoreMSA() + controlerConfigGroup.getFirstIteration());
		}
		learningRate = planCalcScoreConfigGroup.getLearningRate();
	}
	/*
	NewScoreAssignerImpl(PlanCalcScoreConfigGroup planCalcScoreConfigGroup, ControlerConfigGroup controlerConfigGroup) {
		this.network =null;
		if (planCalcScoreConfigGroup.getFractionOfIterationsToStartScoreMSA()!=null ) {
			final int diff = controlerConfigGroup.getLastIteration() - controlerConfigGroup.getFirstIteration();
			this.scoreMSAstartsAtIteration = (int) (diff
					* planCalcScoreConfigGroup.getFractionOfIterationsToStartScoreMSA() + controlerConfigGroup.getFirstIteration());
		}
		learningRate = planCalcScoreConfigGroup.getLearningRate();
	}
	*/


	public void assignNewScores(int iteration, ScoringFunctionsForPopulation scoringFunctionsForPopulation, Population population) {
		log.info("it: " + iteration + " msaStart: " + this.scoreMSAstartsAtIteration );

		int carcount = 0;
		double profit = 0.0;
		for (Person person : population.getPersons().values()) {
			ScoringFunction sf = scoringFunctionsForPopulation.getScoringFunctionForAgent(person.getId());
			double score = sf.getScore();
			//double score = sf.getScore()-1000;//ここを-1000するとoutputplanと次回転以降のプランが変わるはず
			Plan plan = person.getSelectedPlan();


			for(int s = 0; s < plan.getPlanElements().size(); s++) {//全部上条オリジナル//rideshare
				//System.out.println(s);
				//System.out.println(plan.getPlanElements().get(s));
				//System.out.println(plan.getPerson().getId().toString());


				if(plan.getPlanElements().get(s) instanceof Leg) {
					score -= 100;
				}
			}


			/*//carshare

			double carshare = 0.0424*0.050;

			for(int s = 0; s < plan.getPlanElements().size(); s++) {//全部上条オリジナル//rideshare
				//System.out.println(s);
				//System.out.println(plan.getPlanElements().get(s));
				//System.out.println(plan.getPerson().getId().toString());


				if(plan.getPlanElements().get(s) instanceof Leg) {//LEG型か否か
					Leg leg = (Leg)plan.getPlanElements().get(s);
					//if(leg.getMode()=="drt"&&(s+1)<plan.getPlanElements().size()&&plan.getPlanElements().get(s-1) instanceof Activity&&plan.getPlanElements().get(s+1) instanceof Activity) {
						if(leg.getMode()=="drt") {
						//System.out.println(s);
						//System.out.println(plan.getPlanElements().size());
						double drtcost;
						drtcost = leg.getRoute().getDistance()*carshare;
						//System.out.println(drtcost);

						score -= drtcost;
						profit += drtcost/0.0424;
						}
				}
			}
			*///carshare





			//rideshare
			double rideshare = 0.0424*0.040;

			for(int s = 0; s < plan.getPlanElements().size(); s++) {//全部上条オリジナル//rideshare
				//System.out.println(s);
				//System.out.println(plan.getPlanElements().get(s));
				//System.out.println(plan.getPerson().getId().toString());


				if(plan.getPlanElements().get(s) instanceof Leg) {//LEG型か否か
					Leg leg = (Leg)plan.getPlanElements().get(s);
					//if(leg.getMode()=="drt"&&(s+1)<plan.getPlanElements().size()&&plan.getPlanElements().get(s-1) instanceof Activity&&plan.getPlanElements().get(s+1) instanceof Activity) {
						if(leg.getMode()=="drt") {
						//System.out.println(s);
						//System.out.println(plan.getPlanElements().size());
						double drtcost;
						if(s+1==plan.getPlanElements().size()) {

							drtcost = leg.getRoute().getDistance()*rideshare/1.3;

							System.out.println("最後");
							System.out.println(leg.getRoute().getDistance());
							System.out.println(drtcost);
						}
						else {
							if(plan.getPlanElements().get(s-1) instanceof Leg||plan.getPlanElements().get(s+1) instanceof Leg) {
								drtcost = leg.getRoute().getDistance()*rideshare/1.3;

								System.out.println("act");
								System.out.println(leg.getRoute().getDistance());
								System.out.println(drtcost);

							}

							else {

						Activity beforeact = (Activity)plan.getPlanElements().get(s-1);
						Activity afteract = (Activity)plan.getPlanElements().get(s+1);

						if(beforeact.getType()=="pt interaction"||afteract.getType()=="pt interaction") {
							drtcost = leg.getRoute().getDistance()*rideshare/1.3;

							System.out.println("pt");
							System.out.println(leg.getRoute().getDistance());
							System.out.println(drtcost);

						}
						else {
						//System.out.println(beforeact);
						//System.out.println(plan.getPlanElements().get(s));
						//System.out.println(afteract);
						//System.out.println(plan.getPlanElements().get(s+2));
						//System.out.println(beforeact.getCoord());
						//System.out.println(afteract.getCoord());

						//beforecoord = PopulationReaderMatsimV6.getCoords(beforeact);
						//beforeact.getLinkId().
						//Network network = ExperiencedPlansServiceImpl.network;
						//Coord beforecoord = network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord();
						//Coord aftercoord = network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord();
						//if(beforeact.getCoord()==null&&afteract.getCoord()==null) {
						double dist = CoordUtils.calcEuclideanDistance(beforeact.getCoord(), afteract.getCoord());


						drtcost = dist*rideshare;//*0.007423*0.050;//ここの最後にdrtのmonetaryDistanceRateを入れるとgood

						//System.out.println(drtcost);

						//score -= drtcost;
						//}

						//System.out.println("score="+score);
						//break;
						}
							}
						}

						score -= drtcost;
						profit += drtcost/0.0424;
					}

				}
			}
			//rideshare

			/*

			for(int s = 0; s < plan.getPlanElements().size(); s++) {//全部上条オリジナル
				//System.out.println(s);
				//System.out.println(plan.getPlanElements().get(s));



				if(plan.getPlanElements().get(s) instanceof Leg) {//LEG型か否か
					Leg leg = (Leg)plan.getPlanElements().get(s);
					if(leg.getMode()=="drt") {
						double drtcost;
						Activity beforeact = (Activity)plan.getPlanElements().get(s-1);
						Activity afteract = (Activity)plan.getPlanElements().get(s+1);
						double dist = CoordUtils.calcEuclideanDistance(beforeact.getCoord(), afteract.getCoord());
						drtcost = dist*0.007423*100.050;//ここの最後にdrtのmonetaryDistanceRateを入れるとgood

						score -= drtcost;
						//System.out.println("score="+score);
						//break;
					}

				}
			}
			*/


			//重要
			for(int s = 0; s < plan.getPlanElements().size(); s++) {//全部上条オリジナル
				//System.out.println(s);
				//System.out.println(plan.getPlanElements().get(s));

				if(plan.getPlanElements().get(s) instanceof Leg) {//LEG型か否か
					Leg leg = (Leg)plan.getPlanElements().get(s);
					if(leg.getMode()=="car") {
						//score -= 500*0.0424;//ここをコメントアウトするだけ
						carcount += 1;

						//System.out.println("score="+score);
						break;
					}

				}
			}




			Double oldScore = plan.getScore();
			if (oldScore == null) {
				plan.setScore(score);
				if ( plan.getScore().isNaN() ) {
					log.warn("score is NaN; plan:" + plan.toString() );
				}
			} else {
				if ( this.scoreMSAstartsAtIteration == null || iteration < this.scoreMSAstartsAtIteration ) {
					final double newScore = this.learningRate * score + (1 - this.learningRate) * oldScore;
					if ( log.isTraceEnabled() ) {
						log.trace( " lrn: " + this.learningRate + " oldScore: " + oldScore + " simScore: " + score + " newScore: " + newScore );
					}
					plan.setScore(newScore);
					if ( plan.getScore().isNaN() ) {
						log.warn("score is NaN; plan:" + plan.toString()+" with lrn: " + this.learningRate + " oldScore: " + oldScore + " simScore: " + score + " newScore: " + newScore );
					}
				} else {
//					double alpha = 1./(this.iteration - this.scoreMSAstartsAtIteration + 1) ;
//					alpha *= scenario.getConfig().strategy().getMaxAgentPlanMemorySize() ; //(**)
//					if ( alpha>1 ) {
//						alpha = 1. ;
//					}

					Integer msaContribs = this.msaContributions.get(plan) ;
					if ( msaContribs==null ) {
						msaContribs = 0 ;
					}
					this.msaContributions.put(plan,msaContribs+1) ;
					double alpha = 1./(msaContribs+1) ;

					final double newScore = alpha * score + (1.-alpha) * oldScore;
					if ( log.isTraceEnabled() ) {
						log.trace( " alpha: " + alpha + " oldScore: " + oldScore + " simScore: " + score + " newScore: " + newScore );
					}
					plan.setScore( newScore ) ;
					if ( plan.getScore().isNaN() ) {
						log.warn("score is NaN; plan:" + plan.toString() );
					}
					/*
					// the above is some variant of MSA (method of successive
					// averages). It is not the same as MSA since
					// a plan is typically not scored in every iteration.
					// However, plans are called with rates, for example
					// only every 10th iteration. Yet, something like 1/(10x)
					// still diverges in the same way as 1/x
					// when integrated, so MSA should still converge to the
					// correct result. kai, oct'12
					// The above argument may be theoretically correct.  But something 9/10*old+1/10*new is too slow in practice.  Now
					// multiplying with number of plans (**) in hope that it is better.  (Where is the theory department?) kai, nov'13
					 * Looks to me like this is now truly MSA. kai, apr'15
					// yyyy this has never been tested with scenarios :-(  .  At least there is a test case.  kai, oct'12
					// (In the meantime, I have used it in certain of my own 1% runs, e.g. Ivory Coast.)
					 */
				}
			}

			this.scoreSum += score;
			this.scoreCount++;
		}
		log.info("carcount is "+carcount);
		log.info("profit is "+profit);
		System.out.println("carcount///////////////////////////////////////////////////////////////////");
		System.out.println(carcount);
		System.out.println("profit///////////////////////////////////////////////////////////////////");
		System.out.println(profit);
	}


}
