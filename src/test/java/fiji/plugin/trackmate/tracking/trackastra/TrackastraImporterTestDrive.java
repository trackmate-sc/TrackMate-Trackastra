package fiji.plugin.trackmate.tracking.trackastra;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.opencsv.exceptions.CsvException;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.FolderOpener;

public class TrackastraImporterTestDrive
{

	public static void main( final String[] args )
	{
		ImageJ.main( args );

		final String trackmateFile = "samples/MAX_Merged.xml";
		final TmXmlReader reader = new TmXmlReader( new File( trackmateFile ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}
		final SpotCollection spots = reader.getModel().getSpots();

		final String maskFolder = "C:\\Users\\tinevez\\AppData\\Local\\Temp\\TrackMate-Trackastra-masks_4900017938790086225";
		final ImagePlus masks = FolderOpener.open( maskFolder, " filter=tif" );
		masks.setDimensions( 1, 1, masks.getNSlices() );

		final String edgeCSVfile = maskFolder + File.separator + "trackastra-edge-table.csv";

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );
		try
		{
			TrackastraImporter.importEdges( Paths.get( edgeCSVfile ), spots, masks, graph );
		}
		catch ( final FileNotFoundException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( final CsvException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
