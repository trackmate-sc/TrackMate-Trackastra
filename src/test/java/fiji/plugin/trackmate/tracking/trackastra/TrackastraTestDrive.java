package fiji.plugin.trackmate.tracking.trackastra;

import fiji.plugin.trackmate.LoadTrackMatePlugIn;

public class TrackastraTestDrive
{
	public static void main( final String[] args )
	{

//		ImageJ.main( args );

//		final String img = "../TrackMate/samples/MAX_Merged.tif";
//		new TrackMatePlugIn().run( img );

		final String img = "samples/MAX_Merged.xml";
		new LoadTrackMatePlugIn().run( img );
	}
}
