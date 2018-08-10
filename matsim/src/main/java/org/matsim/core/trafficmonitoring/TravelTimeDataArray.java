/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeRoleArray.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.trafficmonitoring;

import org.jfree.util.Log;
import org.matsim.api.core.v01.network.Link;


/**
 * Implementation of {@link TravelTimeData} that stores the data per time bin
 * in simple arrays. Useful if not too many empty time bins (time bins with
 * no traffic on a link) exist, so no memory is wasted.
 *
 * @author mrieser
 */
public class TravelTimeDataArray implements TravelTimeData {
	//private int number;

	private final double[] timeSum;
	private final int[] timeCnt;
	private final double[] travelTimes;

	private final double[] timeSumall;//ここら辺はkamijoオリジナル
	private final int[] timeCntall;
	private final double[] travelTimesall;

	private final double[] timeSum1;//ここら辺はkamijoオリジナル
	private final int[] timeCnt1;
	private final double[] travelTimes1;
	private final double[] timeSum2;//ここら辺はkamijoオリジナル
	private final int[] timeCnt2;
	private final double[] travelTimes2;
	private final double[] timeSum3;//ここら辺はkamijoオリジナル
	private final int[] timeCnt3;
	private final double[] travelTimes3;
	private final double[] timeSum4;//ここら辺はkamijoオリジナル
	private final int[] timeCnt4;
	private final double[] travelTimes4;
	private final double[] timeSum5;//ここら辺はkamijoオリジナル
	private final int[] timeCnt5;
	private final double[] travelTimes5;

	private final Link link;


	public TravelTimeDataArray(final Link link, final int numSlots) {//最初の回転時のみここを用いる、全てのリンク毎に行われる


		this.timeSum = new double[numSlots];
		this.timeCnt = new int[numSlots];
		this.travelTimes = new double[numSlots];

		this.timeSumall = new double[numSlots];
		this.timeCntall = new int[numSlots];
		this.travelTimesall = new double[numSlots];

		this.timeSum1 = new double[numSlots];
		this.timeCnt1 = new int[numSlots];
		this.travelTimes1 = new double[numSlots];
		this.timeSum2 = new double[numSlots];
		this.timeCnt2 = new int[numSlots];
		this.travelTimes2 = new double[numSlots];
		this.timeSum3 = new double[numSlots];
		this.timeCnt3 = new int[numSlots];
		this.travelTimes3 = new double[numSlots];
		this.timeSum4 = new double[numSlots];
		this.timeCnt4 = new int[numSlots];
		this.travelTimes4 = new double[numSlots];
		this.timeSum5 = new double[numSlots];
		this.timeCnt5 = new int[numSlots];
		this.travelTimes5 = new double[numSlots];

		this.link = link;
		//System.out.println("sssssss");
		resetTravelTimes();
	}

	@Override
	public void resetTravelTimes() {//毎回転ごとにリセットしている
		//System.out.println("sssssss");
		//Log.info("sssssss");

		//this.number++;//これを使って動作を確かめた
		//System.out.println(this.number);
		for (int i = 0; i < this.timeSum.length; i++) {//ここの長さは１日割るtimebinか、１日/15分、30時間／15分
			//System.out.println(this.timeSum.length);
			//System.out.println(this.timeSum[i]);
			//System.out.println(this.timeCnt[i]);//int
			//System.out.println(this.travelTimes[i]);
			this.timeSumall[i] = this.timeSumall[i] + this.timeSum[i];
			this.timeCntall[i] = this.timeCntall[i] + this.timeCnt[i];

			this.timeSumall[i] = this.timeSumall[i] - this.timeSum5[i];//ここで5番目を追い出す
			this.timeCntall[i] = this.timeCntall[i] - this.timeCnt5[i];

			this.timeSum5[i] = this.timeSum4[i];
			this.timeCnt5[i] = this.timeCnt4[i];
			this.timeSum4[i] = this.timeSum3[i];
			this.timeCnt4[i] = this.timeCnt3[i];
			this.timeSum3[i] = this.timeSum2[i];
			this.timeCnt3[i] = this.timeCnt2[i];
			this.timeSum2[i] = this.timeSum[i];
			this.timeCnt2[i] = this.timeCnt[i];

			this.timeSum[i] = 0.0;
			this.timeCnt[i] = 0;
			this.travelTimes[i] = -1.0;

		}
	}

//	@Override
//	public void resetTravelTime( final int timeSlot ) {
//		this.timeSum[timeSlot] = 0.0;
//		this.timeCnt[timeSlot] = 0;
//		this.travelTimes[timeSlot] = -1.0;
//	}

	@Override
	public void setTravelTime( final int timeSlot, final double traveltime ) {
		this.timeSum[timeSlot] = traveltime ;
		this.timeCnt[timeSlot] = 1 ;
		this.travelTimes[timeSlot] = traveltime ; // since this is the only travel time, we do not need to trigger the cache consolidation.
		// if ever some other value is added, the cache is invalidated in addTravelTime. kai/theresa, may'15
	}

	@Override
	public void addTravelTime(final int timeSlot, final double traveltime) {
		double sum = this.timeSum[timeSlot];
		int cnt = this.timeCnt[timeSlot];
		sum += traveltime;
		cnt++;
		this.timeSum[timeSlot] = sum;
		this.timeCnt[timeSlot] = cnt;
		this.travelTimes[timeSlot] = -1.0; // initialize with negative value
	}

	@Override
	public double getTravelTime(final int timeSlot, final double now) {//ここは最後のlinkstatsにも使われる//よってここで数字を弄るのは厳禁
		//System.out.println("gggggg");
		//double ttime = this.travelTimes[timeSlot];
		//this.timeSumall[timeSlot] = this.timeSumall[timeSlot] + this.timeSum[timeSlot];//厳禁
		//this.timeCntall[timeSlot] = this.timeCntall[timeSlot] + this.timeCnt[timeSlot];
		if(this.timeCntall[timeSlot] + this.timeCnt[timeSlot]==0) {
			this.travelTimes[timeSlot] = this.link.getLength() / this.link.getFreespeed(now);//一度も通らなかったらここでリターンする//ここで0,0,時間が生まれる
			return this.travelTimes[timeSlot];
		}
		//if (ttime >= 0.0) {
		//	return this.timeSumall[timeSlot]/this.timeCntall[timeSlot];
		//	//return ttime; // negative values are invalid.//一台しか通らなかったらここでリターンする
		//}

		//int cnt = this.timeCnt[timeSlot];
		//if (cnt == 0) {
		//	//System.out.println("xxxxxxxx");
		//	this.travelTimes[timeSlot] = this.link.getLength() / this.link.getFreespeed(now);//一度も通らなかったらここでリターンする//ここで0,0,時間が生まれる
		//	return this.travelTimes[timeSlot];
		//}

		//double sum = this.timeSum[timeSlot];
		//this.travelTimes[timeSlot] = sum / cnt;//複数台通ったらこれをリターンする
		//System.out.println("gggggg");
		//System.out.println(this.travelTimes[timeSlot]);
		System.out.println(this.link.getId());
		System.out.println(this.link.getLength());
		System.out.println("gggggg");
		System.out.println((this.timeSumall[timeSlot] + this.timeSum[timeSlot])/(this.timeCntall[timeSlot] + this.timeCnt[timeSlot]));
		System.out.println(this.timeCntall[timeSlot] + this.timeCnt[timeSlot]);
		return (this.timeSumall[timeSlot] + this.timeSum[timeSlot])/(this.timeCntall[timeSlot] + this.timeCnt[timeSlot]);
	}


}
