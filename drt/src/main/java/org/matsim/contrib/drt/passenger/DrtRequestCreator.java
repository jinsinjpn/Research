/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.drt.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class DrtRequestCreator implements PassengerRequestCreator {
	private final DrtConfigGroup drtCfg;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final EventsManager eventsManager;
	private final MobsimTimer timer;

	@Inject
	public DrtRequestCreator(DrtConfigGroup drtCfg, @Named(DvrpModule.DVRP_ROUTING) Network network,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim,
			@Named(DefaultDrtOptimizer.DRT_OPTIMIZER) TravelDisutility travelDisutility) {
		this.drtCfg = drtCfg;
		this.travelTime = travelTime;
		this.eventsManager = qSim.getEventsManager();
		this.timer = qSim.getSimTimer();

		router = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility, travelTime);
	}

	@Override
	public DrtRequest createRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double departureTime, double submissionTime) {//デフォルトではdepartureTime=submissionTime、そしてsubの方は今の所使われてる形跡なし
		double latestDepartureTime = departureTime + drtCfg.getMaxWaitTime();

		VrpPathWithTravelData unsharedRidePath = VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router,
				travelTime);

		double optimisticTravelTime = unsharedRidePath.getTravelTime();
		double maxTravelTime = drtCfg.getMaxTravelTimeAlpha() * optimisticTravelTime + drtCfg.getMaxTravelTimeBeta();
		double latestArrivalTime = departureTime + maxTravelTime;//大事//ここに待ち時間の分が足されていない

		double departureTime1 = departureTime; //ここを引き算してもリクエストを出す時刻は変わらない、しかし局所的に待ち時間に影響はする
		double submissionTime1 = submissionTime;//ここも、何も影響しない
		//System.out.println(departureTime);

		double unsharedDistance = VrpPaths.calcPathDistance(unsharedRidePath);

		eventsManager.processEvent(new DrtRequestSubmittedEvent(timer.getTimeOfDay(), id, passenger.getId(),
				fromLink.getId(), toLink.getId(), unsharedRidePath.getTravelTime(), unsharedDistance));

		return new DrtRequest(id, passenger, fromLink, toLink, departureTime1, latestDepartureTime, latestArrivalTime,
				submissionTime1, unsharedRidePath);
	}
}
