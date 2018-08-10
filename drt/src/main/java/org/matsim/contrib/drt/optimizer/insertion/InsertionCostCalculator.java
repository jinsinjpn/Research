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

package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Stop;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.Insertion;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.Schedules;

/**
 * @author michalm
 */
public class InsertionCostCalculator {
	private static final double INFEASIBLE_SOLUTION_COST = Double.MAX_VALUE;//infeasible実行不可能な　　条件を満たさないため、膨大な数値を代入

	private final double stopDuration;
	private final double maxWaitTime;

	public InsertionCostCalculator(double stopDuration, double maxWaitTime) {
		this.stopDuration = stopDuration;
		this.maxWaitTime = maxWaitTime;
	}

	// the main goal - minimise bus operation time
	// ==> calculates how much longer the bus will operate after insertion
	//
	// the insertion is invalid if some maxTravel/Wait constraints are not fulfilled
	// ==> checks if all the constraints are satisfied for all passengers/requests ==> if not ==>
	// INFEASIBLE_SOLUTION_COST is returned
	public double calculate(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion, double currentTime) {
		double pickupDetourTimeLoss = calculatePickupDetourTimeLoss(drtRequest, vEntry, insertion);
		double pickupDetourTimeLoss1 = calculatePickupDetourTimeLoss1(drtRequest, vEntry, insertion);
		double dropoffDetourTimeLoss = calculateDropoffDetourTimeLoss(drtRequest, vEntry, insertion);
		double rideTime = calculateRideTime(drtRequest, vEntry, insertion);


		// this is what we want to minimise
		double totalTimeLoss = pickupDetourTimeLoss + dropoffDetourTimeLoss;
		double totalTimeLoss1 = pickupDetourTimeLoss1 + dropoffDetourTimeLoss;
		double totalTimeLoss2 = calculateTotal(drtRequest, vEntry, insertion, pickupDetourTimeLoss1,
				totalTimeLoss1, rideTime);//KAMIJO

		boolean constraintsSatisfied = areConstraintsSatisfied(drtRequest, vEntry, insertion, pickupDetourTimeLoss,
				totalTimeLoss, currentTime);
		return constraintsSatisfied ? totalTimeLoss2 : INFEASIBLE_SOLUTION_COST;//条件演算子//KAMIJO
	}

	private double calculateTotal(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion,
			double pickupDetourTimeLoss1, double totalTimeLoss1, double rideTime) {
		/*
		for (int s = 0; s < vEntry.stops.size(); s++) {
			Stop stop = vEntry.stops.get(s);
			System.out.println("s"+s);
			System.out.println("v"+vEntry.vehicle.getId());
			System.out.println("oc"+stop.occupancyChange);
			System.out.println("D"+stop.task.getDropoffRequests().size());
			System.out.println("P"+stop.task.getPickupRequests().size());
		}
		*/
		// this is what we cannot violate＝破る
		double pickupDetourTimeLoss2 = 0;
		for (int s = insertion.pickupIdx; s < insertion.dropoffIdx; s++) {//当人が乗り込んでから降りるまでの間に、行われるストップ
			Stop stop = vEntry.stops.get(s);


			//if(s>1) {
			//Stop stopbefore = vEntry.stops.get(s-1);
			// all stops after pickup are delayed by pickupDetourTimeLoss
			//if (stop.task.getDropoffRequests().isEmpty()==false){//ここは不安、確認必要
			//if (stop.task.getDropoffRequests().size()==1||stop.task.getDropoffRequests().size()==2){//ここは不安、確認必要
			//if (stop.occupancyChange==stopbefore.occupancyChange){//ここは不安、確認必要//前のストップのオキュチェンよりも大きければ降りたと判別するという
			//if (stop.occupancyChange==0){
				pickupDetourTimeLoss2 += stop.task.getDropoffRequests().size()*pickupDetourTimeLoss1;
			//}
			//}
		}
		double TimeLoss = 0;
		// this is what we cannot violate
		for (int s = insertion.dropoffIdx; s < vEntry.stops.size(); s++) {//主役が降りてから以降のストップ
			Stop stop = vEntry.stops.get(s);
			//if(s>1) {
			//Stop stopbefore = vEntry.stops.get(s-1);
			// all stops after dropoff are delayed by totalTimeLoss
			//if (stop.task.getDropoffRequests().isEmpty()==false){//
			//if (stop.task.getDropoffRequests().size()==1||stop.task.getDropoffRequests().size()==2){//ここは不安、確認必要
			//if (stop.occupancyChange==stopbefore.occupancyChange){//ここは不安、確認必要//前のストップのオキュチェンよりも大きければ降りたと判別するという
			//if (stop.occupancyChange==0){
				TimeLoss += stop.task.getDropoffRequests().size()*totalTimeLoss1;
			//}
			//}
		}
		//System.out.println("endend");
		//System.out.println(stopDuration);
		//System.out.println(insertion.pickupIdx);
		//System.out.println(insertion.dropoffIdx);
		//System.out.println(pickupDetourTimeLoss2);
		//System.out.println(TimeLoss);
		//System.out.println(rideTime);
		//System.out.println(totalTimeLoss1);
		return 0*totalTimeLoss1 + 1*(pickupDetourTimeLoss2 + TimeLoss + rideTime);//かっこの最後は主役の分//αが最初、次にβ
	}

	private double calculateRideTime(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion) {//全部KAMIJO作成、主役の乗る時間も考慮//こっちが修正したrequsetTOdropofftime
		double ridetime;
		//System.out.println(drtRequest.getPassenger().getId());
		//System.out.println(drtRequest.getPickupTask().getBeginTime());
		//System.out.println(drtRequest.getSubmissionTime());
		//double driveToPickupStartTime = (insertion.pickupIdx == 0) ? vEntry.start.time //最初の乗客だったら　車の動くスタートタイム：そうじゃなかったら　その１つ前のタスクの終了時間を主役を迎えにいく時間と定義する
		//		: vEntry.stops.get(insertion.pickupIdx - 1).task.getEndTime();

		if (insertion.pickupIdx != insertion.dropoffIdx) {
			double toDropoffTT = 0;

			if (insertion.dropoffIdx > 0
					&& drtRequest.getToLink() == vEntry.stops.get(insertion.dropoffIdx - 1).task.getLink()) {//１つ前と行き先が同じだったら
				toDropoffTT = 0; // no detour
			}
			else {
				toDropoffTT = insertion.pathToDropoff.path.travelTime + insertion.pathToDropoff.firstAndLastLinkTT;
			}



		ridetime = vEntry.stops.get(insertion.dropoffIdx - 1).task.getEndTime() - drtRequest.getSubmissionTime() + toDropoffTT;
		//ridetime += insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT + toDropoffTT;
		}
		else {
			boolean ongoingStopTask = insertion.pickupIdx == 0
					&& ((DrtTask)vEntry.vehicle.getSchedule().getCurrentTask()).getDrtTaskType() == DrtTaskType.STOP;
			double toPickupTT = 0.0;

			if ((ongoingStopTask && drtRequest.getFromLink() == vEntry.start.link) //初期状態（or今手ぶら）かつ、今いるリンクにDRTがいるパターン
					|| (insertion.pickupIdx > 0 //１つ前のピックアップの停車タスクとたまたま乗車位置が同じだった場合
							&& drtRequest.getFromLink() == vEntry.stops.get(insertion.pickupIdx - 1).task.getLink())) {
				toPickupTT = 0.0;
			}
			else {
				toPickupTT = insertion.pathToPickup.path.travelTime + insertion.pathToPickup.firstAndLastLinkTT;
			}

			if(insertion.pickupIdx > 0) {
				ridetime = insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT + toPickupTT + vEntry.stops.get(insertion.pickupIdx - 1).task.getEndTime() - drtRequest.getSubmissionTime();
				//System.out.println(drtRequest.getPassenger().getId());
				//System.out.println(drtRequest.getPickupTask().getBeginTime());
				//System.out.println(vEntry.stops.get(insertion.pickupIdx - 1).task.getEndTime());
				//System.out.println(drtRequest.getSubmissionTime());

			}
			else {
			ridetime = insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT + toPickupTT;
			}
		}
		//System.out.println(drtRequest.getPassenger().getId());
		//System.out.println(drtRequest.getPickupTask().getBeginTime());
		//System.out.println(drtRequest.getSubmissionTime());
		//System.out.println(ridetime);
		return ridetime;
	}


	/*
	private double calculateRideTime(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion) {//全部KAMIJO作成、主役の乗る時間も考慮//こっちが本当の意味のridetime
		double ridetime;
		if (insertion.pickupIdx != insertion.dropoffIdx) {
			double toDropoffTT = 0;

			if (insertion.dropoffIdx > 0
					&& drtRequest.getToLink() == vEntry.stops.get(insertion.dropoffIdx - 1).task.getLink()) {//１つ前と行き先が同じだったら
				toDropoffTT = 0; // no detour
			}
			else {
				toDropoffTT = insertion.pathToDropoff.path.travelTime + insertion.pathToDropoff.firstAndLastLinkTT;
			}

		ridetime = vEntry.stops.get(insertion.dropoffIdx - 1).task.getEndTime() - vEntry.stops.get(insertion.pickupIdx).task.getEndTime();
		ridetime += insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT + toDropoffTT;
		}
		else {
			ridetime = insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT;
		}
		return ridetime;
	}
	*/

	private double calculatePickupDetourTimeLoss1(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion) {//KAMIJO//空車で迎えに行く時の影響を省くためのメソッド
		// 'no detour' is also possible now for pickupIdx==0 if the currentTask is STOP
		boolean ongoingStopTask = insertion.pickupIdx == 0
				&& ((DrtTask)vEntry.vehicle.getSchedule().getCurrentTask()).getDrtTaskType() == DrtTaskType.STOP;

		if ((ongoingStopTask && drtRequest.getFromLink() == vEntry.start.link) //初期状態（or今手ぶら）かつ、今いるリンクにDRTがいるパターン
				|| (insertion.pickupIdx > 0 //１つ前のピックアップの停車タスクとたまたま乗車位置が同じだった場合
						&& drtRequest.getFromLink() == vEntry.stops.get(insertion.pickupIdx - 1).task.getLink())) {
			if (insertion.pickupIdx != insertion.dropoffIdx) {// PICKUP->DROPOFF　ピックアップした後に、すぐにその人をおろしに行くパターン ではない時
				return 0;// no detour
			}
			//以下ではピックアップした人をすぐに下ろすパターン、つまりその人の直線距離を計算している
			// no extra drive to pickup and stop (==> toPickupTT == 0 and stopDuration == 0)
			double fromPickupTT = insertion.pathFromPickup.path.travelTime
					+ insertion.pathFromPickup.firstAndLastLinkTT;
			double replacedDriveTT = calculateReplacedDriveDuration(vEntry, insertion.pickupIdx);
			return fromPickupTT - replacedDriveTT;
		}

		double toPickupTT = insertion.pathToPickup.path.travelTime + insertion.pathToPickup.firstAndLastLinkTT;
		double fromPickupTT = insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT;
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, insertion.pickupIdx);
		if (insertion.pickupIdx == vEntry.stops.size()) {//他のタスクがない場合//KAMIJO//こうすると、他のお客に関与しない場合の無人走行時間をカットできる
			return toPickupTT + stopDuration + fromPickupTT - replacedDriveTT;//重みを100分の1にしてほぼないとして扱うが順位だけはつけて適切なのが選ばれるようにする
		}//KAMIJO
		return toPickupTT + stopDuration + fromPickupTT - replacedDriveTT;
	}



	private double calculatePickupDetourTimeLoss(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion) {
		// 'no detour' is also possible now for pickupIdx==0 if the currentTask is STOP
		boolean ongoingStopTask = insertion.pickupIdx == 0
				&& ((DrtTask)vEntry.vehicle.getSchedule().getCurrentTask()).getDrtTaskType() == DrtTaskType.STOP;

		if ((ongoingStopTask && drtRequest.getFromLink() == vEntry.start.link) //初期状態（or今手ぶら）かつ、今いるリンクにDRTがいるパターン
				|| (insertion.pickupIdx > 0 //１つ前のピックアップの停車タスクとたまたま乗車位置が同じだった場合
						&& drtRequest.getFromLink() == vEntry.stops.get(insertion.pickupIdx - 1).task.getLink())) {
			if (insertion.pickupIdx != insertion.dropoffIdx) {// PICKUP->DROPOFF　ピックアップした後に、すぐにその人をおろしに行くパターン ではない時
				return 0;// no detour
			}
			//以下ではピックアップした人をすぐに下ろすパターン、つまりその人の直線距離を計算している
			// no extra drive to pickup and stop (==> toPickupTT == 0 and stopDuration == 0)
			double fromPickupTT = insertion.pathFromPickup.path.travelTime
					+ insertion.pathFromPickup.firstAndLastLinkTT;
			double replacedDriveTT = calculateReplacedDriveDuration(vEntry, insertion.pickupIdx);
			return fromPickupTT - replacedDriveTT;
		}

		double toPickupTT = insertion.pathToPickup.path.travelTime + insertion.pathToPickup.firstAndLastLinkTT;
		double fromPickupTT = insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT;
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, insertion.pickupIdx);
		return toPickupTT + stopDuration + fromPickupTT - replacedDriveTT;
	}

	private double calculateDropoffDetourTimeLoss(DrtRequest drtRequest, VehicleData.Entry vEntry,
			Insertion insertion) {
		if (insertion.dropoffIdx > 0
				&& drtRequest.getToLink() == vEntry.stops.get(insertion.dropoffIdx - 1).task.getLink()) {//１つ前と行き先が同じだったら
			return 0; // no detour
		}

		double toDropoffTT = insertion.dropoffIdx == insertion.pickupIdx ? // PICKUP->DROPOFF ?　　ピックアップしてすぐその人を下ろしたケース
				0 // PICKUP->DROPOFF taken into account as fromPickupTT　　pickupの時点で考慮されているのでここは0で良いということ
				: insertion.pathToDropoff.path.travelTime + insertion.pathToDropoff.firstAndLastLinkTT;
		double fromDropoffTT = insertion.dropoffIdx == vEntry.stops.size() ? // DROPOFF->STAY ?　　ついたら暇になるパターンは０
				0 //
				: insertion.pathFromDropoff.path.travelTime + insertion.pathFromDropoff.firstAndLastLinkTT;
		double replacedDriveTT = insertion.dropoffIdx == insertion.pickupIdx ? // PICKUP->DROPOFF ?
				0 // replacedDriveTT already taken into account in pickupDetourTimeLoss
				: calculateReplacedDriveDuration(vEntry, insertion.dropoffIdx);
		return toDropoffTT + stopDuration + fromDropoffTT - replacedDriveTT;
	}

	private double calculateReplacedDriveDuration(VehicleData.Entry vEntry, int insertionIdx) {

		if (insertionIdx == vEntry.stops.size()) {//その時点でその後に他のタスクがない場合
			return 0;// end of route - bus would wait there
		}

		double replacedDriveStartTime = (insertionIdx == 0) ? vEntry.start.time //最初の行動か否か、正だったらもともと0の位置にいたタスクの予定開始時刻-その時点の時刻、負だったら入るはずの位置のタスクの開始時刻-入るはずの場所の１つ前のタスクが終わった時刻（タスクとは乗せるor降ろすということ）
				: vEntry.stops.get(insertionIdx - 1).task.getEndTime();
		double replacedDriveEndTime = vEntry.stops.get(insertionIdx).task.getBeginTime();
		//System.out.println("rerere");
		//System.out.println(insertionIdx);
		//System.out.println(replacedDriveEndTime - replacedDriveStartTime);
		return replacedDriveEndTime - replacedDriveStartTime;
	}

	private boolean areConstraintsSatisfied(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion,
			double pickupDetourTimeLoss, double totalTimeLoss, double currentTime) {
		// this is what we cannot violate＝破る
		for (int s = insertion.pickupIdx; s < insertion.dropoffIdx; s++) {//当人が乗り込んでから降りるまでの間に、行われるストップ
			Stop stop = vEntry.stops.get(s);
			// all stops after pickup are delayed by pickupDetourTimeLoss
			if (stop.task.getBeginTime() + pickupDetourTimeLoss > stop.maxArrivalTime + maxWaitTime//現在の降りる予定時刻＋ロス時間＞降りる締め切り時間
					|| stop.task.getEndTime() + pickupDetourTimeLoss > stop.maxDepartureTime) {//現在の乗る予定時刻＋ロス時間＞乗る締め切り時間
				return false;
			}
		}

		// this is what we cannot violate
		for (int s = insertion.dropoffIdx; s < vEntry.stops.size(); s++) {//主役が降りてから以降のストップ
			Stop stop = vEntry.stops.get(s);
			// all stops after dropoff are delayed by totalTimeLoss
			if (stop.task.getBeginTime() + totalTimeLoss > stop.maxArrivalTime + maxWaitTime//configにある//kamijo
					|| stop.task.getEndTime() + totalTimeLoss > stop.maxDepartureTime) {
				return false;
			}
		}

		// reject solutions when maxWaitTime for the new request is violated
		double driveToPickupStartTime = (insertion.pickupIdx == 0) ? vEntry.start.time //最初の乗客だったら　車の動くスタートタイム：そうじゃなかったら　その１つ前のタスクの終了時間を主役を迎えにいく時間と定義する
				: vEntry.stops.get(insertion.pickupIdx - 1).task.getEndTime();

		double pickupEndTime = driveToPickupStartTime + insertion.pathToPickup.path.travelTime
				+ insertion.pathToPickup.firstAndLastLinkTT + stopDuration;//主役を乗せ終わる時間

		if (pickupEndTime > drtRequest.getEarliestStartTime() + maxWaitTime) {//主役がどれくらい待てるか（リクエストの出発希望時刻＋待てる時間の上限）//kamijo
			return false;
		}

		// reject solutions when latestArrivalTime for the new request is violated
		double dropoffStartTime = insertion.pickupIdx == insertion.dropoffIdx//主役を乗せてすぐ降ろす場合
				? pickupEndTime + insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT//主役が乗ってすぐ降りる
				: vEntry.stops.get(insertion.dropoffIdx - 1).task.getEndTime() + insertion.pathToDropoff.path.travelTime//主役が降りる１つ前のタスクの終了時間から計算
						+ insertion.pathToDropoff.firstAndLastLinkTT;

		if (dropoffStartTime > drtRequest.getLatestArrivalTime() + maxWaitTime) {//上のstop.maxArrivalTimeの導出と同じメソッド//kamijo
			return false;
		}

		// vehicle's time window cannot be violated
		DrtStayTask lastTask = (DrtStayTask)Schedules.getLastTask(vEntry.vehicle.getSchedule());
		double timeSlack = vEntry.vehicle.getServiceEndTime() - 600 - Math.max(lastTask.getBeginTime(), currentTime);//（入っていたラストタスクの終了時間,現在時間（主役がリクエストを出した時刻)）のうち大きい方と10分をサービス終了時刻から引く
		if (timeSlack < totalTimeLoss) {//要するに全てのタスクを終えるのがサービス終了時刻ギリギリになりそうだったらfalse//TODO ここのtotaltimelossで自分で加えたif文と矛盾が起きそう//まあとりあえずこれで問題なし//分離することで解決
			return false;
		}

		return true;// all constraints satisfied
	}
}
