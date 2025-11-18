/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2024 - 2025 TrackMate developers.
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

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.util.SpotUtil;
import fiji.plugin.trackmate.util.TMUtils;
import gnu.trove.map.hash.TIntObjectHashMap;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class TrackastraImporter
{
	public static void importEdges(
			final Path edges,
			final SpotCollection spots,
			final ImagePlus masks,
			final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph,
			final Logger logger )
			throws FileNotFoundException, IOException, CsvException
	{
		// Map of frame -> label -> spot.
		@SuppressWarnings( "unchecked" )
		final ImgPlus< UnsignedShortType > maskImg = TMUtils.rawWraps( masks );
		final TIntObjectHashMap< TIntObjectHashMap< Spot > > idMap = new TIntObjectHashMap<>();
		for ( final Spot spot : spots.iterable( false ) )
		{
			final int frame = spot.getFeature( Spot.FRAME ).intValue();
			final ImgPlus< UnsignedShortType > imgTC = TMUtils.hyperSlice( maskImg, 0, frame );
			final IterableInterval< UnsignedShortType > it = SpotUtil.iterable( spot, imgTC );
			final int label = it.cursor().next().get();
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
					logger.log( " - no spot in frame " + sourceFrame + ". Skipping.\n", Color.ORANGE );
					continue;
				}
				final Spot source = mapSource.get( sourceLabel );
				if ( source == null )
				{
					logger.log( " - no spot matching source label " + sourceLabel + ". Skipping.\n", Color.ORANGE );
					continue;
				}

				final TIntObjectHashMap< Spot > mapTarget = idMap.get( targetFrame );
				if ( mapTarget == null )
				{
					logger.log( " - no spot in frame " + targetFrame + ". Skipping.\n", Color.ORANGE );
					continue;
				}
				final Spot target = mapTarget.get( targetLabel );
				if ( target == null )
				{
					logger.log( " - no spot matching target label " + targetLabel + ". Skipping.\n", Color.ORANGE );
					continue;
				}

				addEdge( source, target, weight, graph );
			}
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
