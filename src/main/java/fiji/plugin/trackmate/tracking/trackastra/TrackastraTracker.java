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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.apache.commons.io.input.Tailer;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.opencsv.exceptions.CsvException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.action.LabelImgExporter;
import fiji.plugin.trackmate.action.LabelImgExporter.LabelIdPainting;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.cli.CLIUtils;
import fiji.plugin.trackmate.util.cli.CLIUtils.LoggerTailerListener;
import fiji.plugin.trackmate.util.cli.CommandBuilder;
import fiji.plugin.trackmate.visualization.GlasbeyLut;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import net.imagej.ImgPlus;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class TrackastraTracker implements SpotTracker, Benchmark
{

	final static String BASE_ERROR_MESSAGE = "[Trackastra] ";

	private static final String EDGE_CSV_FILENAME = "trackastra-edge-table.csv";

	private static final String TRACKASTRA_LOG_FILENAME = "trackastra-log.txt";

	private SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private Logger logger = Logger.VOID_LOGGER;

	private String errorMessage;

	private final TrackastraCLI cli;

	private long processingTime;

	private final SpotCollection spots;

	private final ImagePlus imp;

	public TrackastraTracker( final TrackastraCLI cli, final SpotCollection spots, final ImagePlus imp )
	{
		this.cli = cli;
		this.spots = spots;
		this.imp = imp;
	}

	@Override
	public SimpleWeightedGraph< Spot, DefaultWeightedEdge > getResult()
	{
		return graph;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		/*
		 * Check input now.
		 */

		// Check that the objects list itself isn't null
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is null.";
			return false;
		}

		// Check that the objects list contains inner collections.
		if ( spots.keySet().isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
			return false;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		/*
		 * 1. Export masks to tmp folder.
		 */

		final int[] dimensions = imp.getDimensions();
		final long[] dims = new long[] { dimensions[ 0 ], dimensions[ 1 ], dimensions[ 3 ], dimensions[ 4 ] };
		final double[] calibration = new double[] {
				imp.getCalibration().pixelWidth,
				imp.getCalibration().pixelHeight,
				imp.getCalibration().pixelDepth,
				imp.getCalibration().frameInterval
		};
		final boolean exportSpotsAsDots = false;
		final LabelIdPainting labelIdPainting = LabelIdPainting.LABEL_IS_INDEX_MOVIE_UNIQUE;
		final ImgPlus< UnsignedShortType > masks = LabelImgExporter.createLabelImg(
				spots, dims, calibration, exportSpotsAsDots, labelIdPainting,
				new UnsignedShortType(), logger );
		final ImagePlus maskImp = ImageJFunctions.wrap( masks, "masks" );
		maskImp.setDimensions( 1, imp.getNSlices(), imp.getNFrames() );
		maskImp.setLut( GlasbeyLut.toLUT() );
		maskImp.setDisplayRange( 0, 255 );
		maskImp.setOpenAsHyperStack( true );

		Path maskTmpFolder;
		try
		{
			maskTmpFolder = Files.createTempDirectory( "TrackMate-Trackastra-masks_" );
			CLIUtils.recursiveDeleteOnShutdownHook( maskTmpFolder );
			logger.setStatus( "Saving masks" );
			logger.log( "Saving masks to " + maskTmpFolder + "\n" );
			final boolean ok = writeStackList( maskImp, 1, maskTmpFolder.toString(), "-mask-t" );
			if ( !ok )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Problem saving masks.\n";
				return false;
			}
		}
		catch ( final IOException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Could not create temp folder to save masks:\n" + e.getMessage();
			return false;
		}

		/*
		 * 2. Export input image to tmp folder.
		 */

		final int c;
		if ( imp.getNChannels() == 1 )
		{
			c = 1;
		}
		else
		{
			// Get the right channel.
			c = cli.imageChannel().getValue();
		}

		Path imgTmpFolder;
		try
		{
			imgTmpFolder = Files.createTempDirectory( "TrackMate-Trackastra-imgs_" );
			CLIUtils.recursiveDeleteOnShutdownHook( imgTmpFolder );
			logger.setStatus( "Saving source image" );
			if ( c < 0 )
				logger.log( "Saving source image to " + imgTmpFolder + "\n" );
			else
				logger.log( "Saving channel " + c + " of the source image to " + imgTmpFolder + "\n" );
			final boolean ok = writeStackList( imp, c, imgTmpFolder.toString(), "-img-t" );
			if ( !ok )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Problem saving masks.\n";
				return false;
			}
		}
		catch ( final IOException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Could not create temp folder to save input image:\n" + e.getMessage();
			return false;
		}

		/*
		 * 3. Launch Trackastra
		 */

		cli.maskFolder().set( maskTmpFolder.toString() );
		cli.imageFolder().set( imgTmpFolder.toString() );
		final Path edgeCSVTablePath = maskTmpFolder.resolve( EDGE_CSV_FILENAME );
		cli.outputEdgeFile().set( edgeCSVTablePath.toString() );
		final String executableName = cli.getCommand();

		// Check validity of the CLI.
		final String error = cli.check();
		final boolean ok = error == null;
		if ( !ok )
		{
			errorMessage = BASE_ERROR_MESSAGE + error;
			return false;
		}

		// Redirect log to logger.
		final File logFile = maskTmpFolder.resolve( TRACKASTRA_LOG_FILENAME ).toFile();
		final Tailer tailer = Tailer.builder()
				.setFile( logFile )
				.setTailerListener( new LoggerTailerListener( logger ) )
				.setDelayDuration( Duration.ofMillis( 200 ) )
				.setTailFromEnd( true )
				.get();
		Process process;
		try
		{

			final List< String > cmd = CommandBuilder.build( cli );
			logger.setStatus( "Running " + executableName );
			logger.log( "Running " + executableName + " with args:\n" );
			cmd.forEach( t -> {
				if ( t.contains( File.separator ) )
					logger.log( t + ' ' );
				else
					logger.log( t + ' ', Logger.GREEN_COLOR.darker() );
			} );
			logger.log( "\n" );

			final ProcessBuilder pb = new ProcessBuilder( cmd );
			pb.redirectOutput( ProcessBuilder.Redirect.appendTo( logFile ) );
			pb.redirectError( ProcessBuilder.Redirect.appendTo( logFile ) );

			process = pb.start();
			process.waitFor();
		}
		catch ( final IOException e )
		{
			final String msg = e.getMessage();
			if ( msg.matches( ".+error=13.+" ) )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Problem running " + executableName + ":\n"
						+ "The executable does not have the file permission to run.\n";
			}
			else
			{
				errorMessage = BASE_ERROR_MESSAGE + "Problem running " + executableName + ":\n" + e.getMessage();
			}
			try
			{
				errorMessage = errorMessage + '\n' + new String( Files.readAllBytes( logFile.toPath() ) );
			}
			catch ( final IOException e1 )
			{}
			e.printStackTrace();
			return false;
		}
		catch ( final Exception e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Problem running " + executableName + ":\n" + e.getMessage();
			try
			{
				errorMessage = errorMessage + '\n' + new String( Files.readAllBytes( logFile.toPath() ) );
			}
			catch ( final IOException e1 )
			{}
			e.printStackTrace();
			return false;
		}
		finally
		{
			tailer.close();
			process = null;
		}

		/*
		 * 4. Read Trackastra results and pass it to the new graph.
		 */

		logger.setStatus( "Importing Trackastra results" );
		logger.log( "Importing Trackastra results file " + edgeCSVTablePath + "\n" );
		graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );
		try
		{
			TrackastraImporter.importEdges( edgeCSVTablePath, spots, maskImp, graph, logger );
		}
		catch ( final FileNotFoundException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Could not find Trackastra output file " + EDGE_CSV_FILENAME + "\n"
					+ "Trackastra did not execute properly?\n"
					+ e.getMessage();
			try
			{
				errorMessage = errorMessage + '\n' + new String( Files.readAllBytes( logFile.toPath() ) );
			}
			catch ( final IOException e1 )
			{}
			return false;
		}
		catch ( final IOException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Could not read Trackastra output file " + EDGE_CSV_FILENAME + "\n"
					+ e.getMessage();
			return false;
		}
		catch ( final CsvException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Issue with the Trackastra output file " + EDGE_CSV_FILENAME + "\n"
					+ e.getMessage();
			return false;
		}
		finally
		{
			logger.setProgress( 1d );
			logger.setStatus( "" );

			final long end = System.currentTimeMillis();
			processingTime = end - start;
		}
		return true;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public void setNumThreads()
	{}

	@Override
	public void setNumThreads( final int numThreads )
	{}

	@Override
	public int getNumThreads()
	{
		return 1;
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	public boolean writeStackList( final ImagePlus imp, final int c, final String folder, final String suffix )
	{
		final int nT = imp.getNFrames();
		final int nZ = imp.getNSlices();
		for ( int t = 1; t <= nT; t++ )
		{
			final String name = String.format( imp.getShortTitle() + suffix + "%04d", t );
			final ImagePlus dup = new Duplicator().run( imp, c, c, 1, nZ, t, t );
			dup.setTitle( name );
			final String path = folder + File.separator + name + ".tif";
			final boolean ok = IJ.saveAsTiff( dup, path );
			if (!ok)
			{
				logger.error( "Problem saving to " + path + '\n' );
				return false;
			}
		}
		return true;
	}
}
