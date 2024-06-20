package fiji.plugin.trackmate.tracking.trackastra;

import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.TRACKSTRA_EXECUTABLE_NAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
import fiji.plugin.trackmate.util.cli.CommandBuilder;
import fiji.plugin.trackmate.visualization.GlasbeyLut;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.plugin.StackWriter;
import net.imagej.ImgPlus;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class TrackastraTracker implements SpotTracker, Benchmark
{

	final static String BASE_ERROR_MESSAGE = "[Trackastra] ";

	private static final String EDGE_CSV_FILENAME = "trackastra-edge-table.csv";

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
		maskImp.setDimensions( 1, 1, imp.getNFrames() );
		maskImp.setLut( GlasbeyLut.toLUT() );
		maskImp.setDisplayRange( 0, 255 );
		maskImp.setOpenAsHyperStack( true );

		Path maskTmpFolder;
		try
		{
			maskTmpFolder = Files.createTempDirectory( "TrackMate-Trackastra-masks_" );
			CLIUtils.recursiveDeleteOnShutdownHook( maskTmpFolder );
			final String saveOptions = "";
			logger.log( "Saving masks to " + maskTmpFolder + "\n" );
			StackWriter.save( maskImp, maskTmpFolder.toString() + File.separator, saveOptions );
		}
		catch ( final IOException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Could not create temp folder to save masks:\n" + e.getMessage();
			return false;
		}

		/*
		 * 2. Export input image to tmp folder.
		 */

		final int nChannels = imp.getNChannels();
		final ImagePlus out;
		final int c;
		if ( nChannels == 1 )
		{
			c = -1;
			out = imp;
		}
		else
		{
			// Get the right channel.
			c = cli.imageChannel().get();
			final int nZ = imp.getNSlices();
			final int nT = imp.getNFrames();
			out = new Duplicator().run( imp, c, c, 1, nZ, 1, nT );
		}

		Path imgTmpFolder;
		try
		{
			imgTmpFolder = Files.createTempDirectory( "TrackMate-Trackastra-imgs_" );
			CLIUtils.recursiveDeleteOnShutdownHook( imgTmpFolder );
			final String saveOptions = "";
			if ( c < 0 )
				logger.log( "Saving source image to " + imgTmpFolder + "\n" );
			else
				logger.log( "Saving channel " + c + " of the source image to " + imgTmpFolder + "\n" );
			StackWriter.save( out, imgTmpFolder.toString() + File.separator, saveOptions );
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
		final String executableName = Paths.get( cli.getExecutableArg().getValue() ).getFileName().toString();

		// Check validity of the CLI.
		final String error = cli.check();
		final boolean ok = error == null;
		if ( !ok )
		{
			errorMessage = BASE_ERROR_MESSAGE + error;
			return false;
		}

		Process process;
		try
		{
			final List< String > cmd = CommandBuilder.build( cli );
			logger.setStatus( "Running " + TRACKSTRA_EXECUTABLE_NAME );
			logger.log( "Running " + TRACKSTRA_EXECUTABLE_NAME + " with args:\n" );
			logger.log( String.join( " ", cmd ) );
			logger.log( "\n" );
			final ProcessBuilder pb = new ProcessBuilder( cmd );
			pb.redirectOutput( ProcessBuilder.Redirect.INHERIT );
			pb.redirectError( ProcessBuilder.Redirect.INHERIT );

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
			e.printStackTrace();
			return false;
		}
		catch ( final Exception e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Problem running " + executableName + ":\n" + e.getMessage();
			e.printStackTrace();
			return false;
		}
		finally
		{
			process = null;
		}

		/*
		 * 4. Read Trackastra results and pass it to the new graph.
		 */

		logger.log( "Importing Trackastra results file " + edgeCSVTablePath + ".\n" );
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
}
