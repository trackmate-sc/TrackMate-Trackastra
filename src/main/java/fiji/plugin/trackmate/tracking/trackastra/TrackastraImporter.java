package fiji.plugin.trackmate.tracking.trackastra;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.Iterator;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.opencsv.CSVReader;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import gnu.trove.map.hash.TIntObjectHashMap;

public class TrackastraImporter
{
	public static void importEdges( final Path edges, final SpotCollection spots, final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph )
	{
		// Map of frame -> label -> spot.
		final TIntObjectHashMap< TIntObjectHashMap< Spot > > idMap = new TIntObjectHashMap<>();
		for ( final Spot spot : spots.iterable( false ) )
		{
			final int label = spot.getFeature( "MEDIAN_INTENSITY_CH1" ).intValue();
			final int frame = spot.getFeature( Spot.FRAME ).intValue();
			TIntObjectHashMap< Spot > map = idMap.get( frame );
			if ( map == null )
			{
				map = new TIntObjectHashMap<>();
				idMap.put( frame, map );
			}
			map.put( label, spot );
		}

		try (final CSVReader reader = new CSVReader( new FileReader( edges.toFile() ) ))
		{
			final Iterator< String[] > it = reader.readAll().iterator();
			// Skip header
			it.next();
			while ( it.hasNext() )
			{
				final String[] strs = it.next();
				final int sourceFrame = Integer.parseInt( strs[ 0 ] );
				final int sourceLabel = Integer.parseInt( strs[ 1 ] );
				final int targetFrame = Integer.parseInt( strs[ 2 ] );
				final int targetLabel = Integer.parseInt( strs[ 3 ] );
				final double weight = Double.parseDouble( strs[ 4 ] );

				final TIntObjectHashMap< Spot > mapSource = idMap.get( sourceFrame );
				if ( mapSource == null )
				{
					System.out.println( " - no spot in frame " + sourceFrame + ". Skipping." );
					continue;
				}
				final Spot source = mapSource.get( sourceLabel );
				if ( source == null )
				{
					System.out.println( " - no spot matching source label " + sourceLabel + ". Skipping." );
					continue;
				}

				final TIntObjectHashMap< Spot > mapTarget = idMap.get( targetFrame );
				if ( mapTarget == null )
				{
					System.out.println( " - no spot in frame " + targetFrame + ". Skipping." );
					continue;
				}
				final Spot target = mapTarget.get( targetLabel );
				if ( target == null )
				{
					System.out.println( " - no spot matching target label " + targetLabel + ". Skipping." );
					continue;
				}

				addEdge( source, target, weight, graph );
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

	private static void addEdge( final Spot source, final Spot target, final double weight, final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph )
	{
		if ( !graph.containsVertex( source ) )
			graph.addVertex( source );

		if ( !graph.containsVertex( target ) )
			graph.addVertex( target );

		final DefaultWeightedEdge edge = graph.addEdge( source, target );
		if ( edge != null )
			graph.setEdgeWeight( edge, weight );
	}
}
