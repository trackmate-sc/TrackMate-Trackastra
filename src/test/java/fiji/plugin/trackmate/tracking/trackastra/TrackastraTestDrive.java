/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.tracking.trackastra;

import fiji.plugin.trackmate.TrackMatePlugIn;

public class TrackastraTestDrive
{
	public static void main( final String[] args )
	{

//		ImageJ.main( args );

//		final String img = "../TrackMate/samples/MAX_Merged.tif";
		final String img = "samples/Fluo-C3DL-MDA231-training.tif";
		new TrackMatePlugIn().run( img );

//		final String img = "samples/MAX_Merged.xml";
//		new LoadTrackMatePlugIn().run( img );
	}
}
