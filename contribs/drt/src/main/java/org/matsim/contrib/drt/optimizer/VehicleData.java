/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;

/**
 * @author michalm
 */
public class VehicleData {
	public static class Entry {
		public final Vehicle vehicle;
		public final LinkTimePair start;
		public int startOccupancy;
		public final List<Stop> stops = new ArrayList<>();

		public Entry(Vehicle vehicle, LinkTimePair start) {
			this.vehicle = vehicle;
			this.start = start;
		}
	}

	public static class Stop {
		public final DrtStopTask task;
		public final double maxArrivalTime;// relating to max pass drive time (for dropoff requests) どれくらい乗ってられるか　double latestArrivalTime = departureTime + maxTravelTime;　の最短
		public final double maxDepartureTime;// relating to pass max wait time (for pickup requests)　どれくらい待ってられるか　double latestDepartureTime = departureTime + drtCfg.getMaxWaitTime(); の最短
		public final int occupancyChange;// diff in pickups and dropoffs
		public int outputOccupancy;

		public Stop(DrtStopTask task) {
			this.task = task;
			maxArrivalTime = calcMaxArrivalTime();
			maxDepartureTime = calcMaxDepartureTime();
			occupancyChange = task.getPickupRequests().size() - task.getDropoffRequests().size();
		}

		private double calcMaxArrivalTime() {
			double maxTime = Double.MAX_VALUE;
			for (DrtRequest r : task.getDropoffRequests()) {
				double reqMaxArrivalTime = r.getLatestArrivalTime();//double latestArrivalTime = departureTime + maxTravelTime;
				if (reqMaxArrivalTime < maxTime) {
					maxTime = reqMaxArrivalTime;
				}
			}
			return maxTime;
		}

		private double calcMaxDepartureTime() {
			double maxTime = Double.MAX_VALUE;
			for (DrtRequest r : task.getPickupRequests()) {
				double reqMaxDepartureTime = r.getLatestStartTime();//double latestDepartureTime = departureTime + drtCfg.getMaxWaitTime();　に等しい
				if (reqMaxDepartureTime < maxTime) {
					maxTime = reqMaxDepartureTime;//今、車両に入っているリクエストのうち直近のpickupのmax時間を探している
				}
			}
			return maxTime;
		}

	}

	private final List<Entry> entries = new ArrayList<>();
	private final double currentTime;

	public VehicleData(double currentTime, Iterable<? extends Vehicle> vehicles) {
		this.currentTime = currentTime;

		for (Vehicle v : vehicles) {
			Entry e = createVehicleData(v);
			if (e != null) {
				entries.add(e);
			}
		}
	}

	public void updateEntry(Entry vEntry) {
		int idx = entries.indexOf(vEntry);// TODO inefficient! ==> use map instead of list for storing entries...
		Entry e = createVehicleData(vEntry.vehicle);
		if (e != null) {
			entries.set(idx, e);
		} else {
			entries.remove(idx);
		}
	}

	private Entry createVehicleData(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		ScheduleStatus status = schedule.getStatus();
		if (currentTime <= vehicle.getServiceBeginTime()) {
			return null;
		}
		if (currentTime >= vehicle.getServiceEndTime() || status == ScheduleStatus.COMPLETED) {
			return null;
		}

		@SuppressWarnings("unchecked")
		List<DrtTask> tasks = (List<DrtTask>)schedule.getTasks();
		DrtTask currentTask = (DrtTask)schedule.getCurrentTask();

		LinkTimePair start;
		int nextTaskIdx;
		if (status == ScheduleStatus.STARTED) {
			switch (currentTask.getDrtTaskType()) {
				case DRIVE:
					DrtDriveTask driveTask = (DrtDriveTask)currentTask;
					start = ((OnlineDriveTaskTracker)driveTask.getTaskTracker()).getDiversionPoint();
					if (start == null) { // too late to divert a vehicle
						start = new LinkTimePair(driveTask.getPath().getToLink(), driveTask.getEndTime());
					}
					break;

				case STOP:
					DrtStopTask stopTask = (DrtStopTask)currentTask;
					start = new LinkTimePair(stopTask.getLink(), stopTask.getEndTime());
					break;

				case STAY:
					DrtStayTask stayTask = (DrtStayTask)currentTask;
					start = new LinkTimePair(stayTask.getLink(), currentTime);
					break;

				default:
					throw new RuntimeException();
			}

			nextTaskIdx = currentTask.getTaskIdx() + 1;
		} else { // PLANNED
			start = new LinkTimePair(vehicle.getStartLink(), vehicle.getServiceBeginTime());
			nextTaskIdx = 0;
		}

		Entry data = new Entry(vehicle, start);
		for (int i = nextTaskIdx; i < tasks.size(); i++) {
			DrtTask task = tasks.get(i);
			if (task.getDrtTaskType() == DrtTaskType.STOP) {
				Stop stop = new Stop((DrtStopTask)task);
				data.stops.add(stop);
			}
		}

		int outputOccupancy = 0;
		for (int i = data.stops.size() - 1; i >= 0; i--) {
			Stop s = data.stops.get(i);
			s.outputOccupancy = outputOccupancy;
			outputOccupancy -= s.occupancyChange;
		}
		data.startOccupancy = outputOccupancy;

		return data;
	}

	public int getSize() {
		return entries.size();
	}

	public Entry getEntry(int idx) {
		return entries.get(idx);
	}

	public List<Entry> getEntries() {
		return entries;
	}
}