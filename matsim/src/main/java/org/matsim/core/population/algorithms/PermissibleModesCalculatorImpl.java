/* *********************************************************************** *
 * project: org.matsim.*
 * PermissibleModesCalculatorImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.core.population.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;

public final class PermissibleModesCalculatorImpl implements PermissibleModesCalculator {
	private final List<String> availableModes;
	private final List<String> availableModesWithoutCar;
	private final List<String> availableModesWithoutBicyc;
	private final List<String> availableModesWithoutCarNorBicyc;
	private final boolean considerCarAvailability;


	public PermissibleModesCalculatorImpl(
			final String[] availableModes,
			final boolean considerCarAvailability) {
		this.availableModes = Arrays.asList(availableModes);

		//if ( this.availableModes.contains(TransportMode.car) && this.availableModes.contains(TransportMode.bike)) {
			final List<String> l1 = new ArrayList<String>( this.availableModes );
			while ( l1.remove( TransportMode.car ) ) {}
			while ( l1.remove( TransportMode.bike ) ) {}
			this.availableModesWithoutCarNorBicyc = Collections.unmodifiableList( l1 );
		//}
		//else{
			//this.availableModesWithoutCarNorBicyc = this.availableModes;
		//}

		//if(this.availableModes.contains(TransportMode.car) && !this.availableModes.contains(TransportMode.bike)) {
			final List<String> l2 = new ArrayList<String>( this.availableModes );
			while ( l2.remove( TransportMode.car ) ) {}
			this.availableModesWithoutCar = Collections.unmodifiableList( l2 );
		//}

		//else{
			//this.availableModesWithoutCar = this.availableModes;
		//}

		//if(!this.availableModes.contains(TransportMode.car) && this.availableModes.contains(TransportMode.bike)) {
			final List<String> l3 = new ArrayList<String>( this.availableModes );
			while ( l3.remove( TransportMode.bike ) ) {}
			this.availableModesWithoutBicyc = Collections.unmodifiableList( l3 );
		//}

		//else{
			//this.availableModesWithoutBicyc = this.availableModes;
		//}

		this.considerCarAvailability = considerCarAvailability;
	}

	@Override
	public Collection<String> getPermissibleModes(final Plan plan) {
		if (!considerCarAvailability) return availableModes;

		final Person person;
		try {
			person = plan.getPerson();
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException( "I need a PersonImpl to get car availability" );
		}




		final boolean carAvail = !"never".equals(PersonUtils.getCarAvail(person));
		final boolean bicycAvail = !"never".equals(PersonUtils.getBicycAvail(person));

	    if(carAvail && bicycAvail) {
	    	//System.out.println("a!!!");
	    	return availableModes;
	    }

	    if(!carAvail && bicycAvail) {
	    	//System.out.println("b!!!");
	    	return availableModesWithoutCar;
	    }

	    if(carAvail && !bicycAvail) {
	    	//System.out.println("c!!!");
	    	return availableModesWithoutBicyc;
	    }

	    if(!carAvail && !bicycAvail) {
	    	//System.out.println("d!!!");
	    	//System.out.println(availableModesWithoutCarNorBicyc);
	    	return availableModesWithoutCarNorBicyc;
	    }
	    else {
	    	//System.out.println("e!!!");
	    	return availableModes;
	    }
	}
}
