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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.opencsv.exceptions.CsvException;

import fiji.plugin.trackmate.Logger;
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
			TrackastraImporter.importEdges( Paths.get( edgeCSVfile ), spots, masks, graph, Logger.DEFAULT_LOGGER );
		}
		catch ( final FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		catch ( final CsvException e )
		{
			e.printStackTrace();
		}
	}
}
