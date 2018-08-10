package org.matsim.core.scoring;


import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
//import org.matsim.core.population.io.PopulationReaderMatsimV6;
import org.matsim.api.core.v01.Coord;//KAMIJO


import java.util.HashMap;
import java.util.Map;



class ExperiencedPlansServiceImpl implements ExperiencedPlansService, EventsToLegs.LegHandler, EventsToActivities.ActivityHandler {

	private final static Logger log = Logger.getLogger(ExperiencedPlansServiceImpl.class);
	public final Network network;//KAMIJO

	@Inject private Config config;
	@Inject private Population population;
	@Inject(optional = true) private ScoringFunctionsForPopulation scoringFunctionsForPopulation;

	private final Map<Id<Person>, Plan> agentRecords = new HashMap<>();

	@Inject
	ExperiencedPlansServiceImpl(ControlerListenerManager controlerListenerManager, EventsToActivities eventsToActivities, EventsToLegs eventsToLegs, final Network network) {//kamijo
		this.network = network;//KAMIJO
		controlerListenerManager.addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				for (Person person : population.getPersons().values()) {
					agentRecords.put(person.getId(), PopulationUtils.createPlan());
				}
			}
		});
		eventsToActivities.addActivityHandler(this);
		eventsToLegs.addLegHandler(this);
	}

	@Override
	synchronized public void handleLeg(PersonExperiencedLeg o) {
		// Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
		// on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
		Id<Person> agentId = o.getAgentId();
		Leg leg = o.getLeg();
		Plan plan = agentRecords.get(agentId);
		if (plan != null) {
			plan.addLeg(leg);
		}
	}

	@Override
	synchronized public void handleActivity(PersonExperiencedActivity o) {
		// Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
		// on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
		Id<Person> agentId = o.getAgentId();
		Activity activity = o.getActivity();
		Plan plan = agentRecords.get(agentId);
		if (plan != null) {
			agentRecords.get(agentId).addActivity(activity);
		}
	}

	@Override
	public void writeExperiencedPlans(String iterationFilename) {
//		finishIteration(); // already called somewhere else in pgm flow.
		Population tmpPop = PopulationUtils.createPopulation(config);
		for (Map.Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
			Person person = PopulationUtils.getFactory().createPerson(entry.getKey());
			Plan plan = entry.getValue();
			person.addPlan(plan);
			tmpPop.addPerson(person);
		}
		new PopulationWriter(tmpPop, null).write(iterationFilename);
		// I removed the "V5" here in the assumption that it is better to move along with future format changes.  If this is
		// undesired, please change back but could you then please also add a comment why you prefer this.  Thanks.
		// kai, jan'16
	}
	@Override
	public final void finishIteration() {

		//int carcount = 0;
		// I separated this from "writeExperiencedPlans" so that it can be called separately even when nothing is written.  Can't say
		// if the design might be better served by an iteration ends listener.  kai, feb'17
		for (Map.Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
			Plan plan = entry.getValue();
			if (scoringFunctionsForPopulation != null) {


			double score = scoringFunctionsForPopulation.getScoringFunctionForAgent(entry.getKey()).getScore();//KAMIJO


			for(int s = 0; s < plan.getPlanElements().size(); s++) {//全部上条オリジナル//rideshare//experoencedの方はうまく作動していない、本当に実行したlegにしか-10がついていない
				//System.out.println(s);
				//System.out.println(plan.getPlanElements().get(s));
				//System.out.println(plan.getPerson().getId().toString());


				if(plan.getPlanElements().get(s) instanceof Leg) {
					score -= 10;
				}
			}


			double carshare = 0.009761*0.050*1.3;
			double rideshare = 0.009761*0.040;



			for(int s = 0; s < plan.getPlanElements().size(); s++) {//全部上条オリジナル
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
						//Activity beforeact = (Activity)plan.getPlanElements().get(s-1);
						//Activity afteract = (Activity)plan.getPlanElements().get(s+1);
						//System.out.println(beforeact);
						//System.out.println(beforeact.getCoord());
						//System.out.println(afteract.getCoord());
						//beforecoord = PopulationReaderMatsimV6.getCoords(beforeact);
						//beforeact.getLinkId().
						Coord beforecoord = this.network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord();
						Coord aftercoord = this.network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord();
						//if(beforeact.getCoord()==null&&afteract.getCoord()==null) {
						double dist = CoordUtils.calcEuclideanDistance(beforecoord, aftercoord);


						drtcost = dist*carshare;//0.007423*0.050;//ここの最後にdrtのmonetaryDistanceRateを入れるとgood

						//System.out.println(drtcost);

						score -= drtcost;
						//}

						//System.out.println("score="+score);
						//break;
					}

				}
			}





			//重要
				for(int s = 0; s < plan.getPlanElements().size(); s++) {//全部上条オリジナル
					//System.out.println(s);
					//System.out.println(plan.getPlanElements().get(s));

					if(plan.getPlanElements().get(s) instanceof Leg) {//LEG型か否か
						Leg leg = (Leg)plan.getPlanElements().get(s);
						if(leg.getMode()=="car") {
							score -= 500*0.009761;//ここをコメントアウトするだけ
							//carcount += 1;
							//System.out.println(carcount);
							//System.out.println("score="+score);
							break;
						}

					}
				}

				plan.setScore(score);



				//plan.setScore(scoringFunctionsForPopulation.getScoringFunctionForAgent(entry.getKey()).getScore());
				if (plan.getScore().isNaN()) {
					log.warn("score is NaN; plan:" + plan.toString());
				}
			}
		}
	}

	@Override
	public Map<Id<Person>, Plan> getExperiencedPlans() {
		return this.agentRecords;
	}

}
