package fiji.plugin.trackmate.tracking.trackastra;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fiji.plugin.trackmate.util.cli.CLIConfigurator;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder.CliConfigPanel;
import fiji.plugin.trackmate.util.cli.CommonTrackMateArguments;
import ij.IJ;

public class TrackastraCLI extends CLIConfigurator
{

	public static final String KEY_TRACKASTRA_PYTHON_FILEPATH = "TRACKASTRA_PYTHON_FILEPATH";;

	public static final String DEFAULT_TRACKASTRA_PYTHON_FILEPATH = System.getProperty( "user.home" );

	public static final String KEY_TRACKASTRA_MODEL = "PRETRAINED_MODEL";

	public static final String DEFAULT_TRACKASTRA_MODEL = "general_2d";

	public static final String KEY_TRACKASTRA_CUSTOM_MODEL_FOLDER = "CUSTOM_MODEL_PATH";

	public static final String DEFAULT_TRACKASTRA_CUSTOM_MODEL_FOLDER = System.getProperty( "user.home" );

	public static final String KEY_USE_GPU = "USE_GPU";

	public static final Boolean DEFAULT_USE_GPU = Boolean.valueOf( true );

	public static final String KEY_TRACKASTRA_TRACKING_MODE = "TRACKING_MODE";

	public static final String DEFAULT_TRACKASTRA_TRACKING_MODE = "greedy";

	public static final String KEY_TRACKASTRA_INPUT_IMGS_FOLDER = "INPUT_IMGS_FOLDER";

	public static final String KEY_TRACKASTRA_INPUT_MASKS_FOLDER = "INPUT_MASKS_FOLDER";

	public static final String KEY_TRACKASTRA_OUTPUT_TABLE_PATH = "OUTPUT_EDGE_TABLE_PATH";

	public static final String TRACKSTRA_EXECUTABLE_NAME = "trackastra";

	private final ChoiceArgument modelPretrained;

	private final PathArgument customModelPath;

	private final SelectableArguments selectPretrainedOrCustom;

	private final ChoiceArgument trackingMode;

	private final Flag useGPU;

	private final PathArgument imageFolder;

	private final PathArgument maskFolder;

	private final PathArgument outputEdgeFile;

	private final IntArgument imageChannel;

	public TrackastraCLI( final int nChannels )
	{
		getExecutableArg()
				.name( "Trackastra env Python executable" )
				.help( "Path to Python executable in Trackastra env." )
				.key( KEY_TRACKASTRA_PYTHON_FILEPATH )
				.set( DEFAULT_TRACKASTRA_PYTHON_FILEPATH );

		// Add the -m trackastra to the command and window conda cmd
		setTranslator( getExecutableArg(), s -> {
			final String executablePath = ( String ) s;
			final String[] split = executablePath.replace( "\\", "/" ).split( "/" );
			// Activate conda env if it runs in Windows.
			final List< String > cmd = new ArrayList<>();
			if ( IJ.isWindows() )
			{
				final String envname = split[ split.length - 2 ];
				cmd.addAll( Arrays.asList( "cmd.exe", "/c", "conda", "activate", envname ) );
				cmd.add( "&" );
				cmd.add( TRACKSTRA_EXECUTABLE_NAME );
				cmd.add( "track" );
			}
			else
			{
				// Calling Cellpose from python.
				cmd.add( executablePath );
				cmd.add( "-m" );
				cmd.add( TRACKSTRA_EXECUTABLE_NAME );
			}
			return cmd;
		} );
		
		this.modelPretrained = addChoiceArgument()
				.name( "Model pretrained" )
				.help( "Name of pretrained Trackastra model." )
				.argument( "--model-pretrained" )
				.addChoice( "general_2d" )
				.addChoice( "ctc" )
				.defaultValue( DEFAULT_TRACKASTRA_MODEL )
				.key( KEY_TRACKASTRA_MODEL )
				.get();

		this.customModelPath = addPathArgument()
				.name( "Path to a custom model" )
				.argument( "--model-custom" )
				.help( "Local folder with custom model." )
				.defaultValue( DEFAULT_TRACKASTRA_CUSTOM_MODEL_FOLDER )
				.key( KEY_TRACKASTRA_CUSTOM_MODEL_FOLDER )
				.get();

		// State that we can set one or the other.
		this.selectPretrainedOrCustom = addSelectableArguments()
				.add( modelPretrained )
				.add( customModelPath );
		selectPretrainedOrCustom.select( modelPretrained );

		this.trackingMode = addChoiceArgument()
				.name( "Tracking mode" )
				.argument( "--mode" )
				.addChoice( "greedy_nodiv" )
				.addChoice( "greedy" )
				.addChoice( "ilp" )
				.defaultValue( 1 )
				.key( KEY_TRACKASTRA_TRACKING_MODE )
				.get();

		this.useGPU = addFlag()
				.name( "Use GPU" )
				.help( "If set, the GPU will be used for computation." )
				.argument( "--device" )
				.defaultValue( DEFAULT_USE_GPU )
				.key( KEY_USE_GPU )
				.get();
		setTranslator( useGPU, b -> Collections.singletonList( ( ( Boolean ) b ).booleanValue() ? "cuda" : "cpu" ) );

		this.imageFolder = addPathArgument()
				.name( "Input image folder path" )
				.help( "Directory with series of .tif files." )
				.argument( "--imgs" )
				.visible( false )
				.required( true )
				.key( KEY_TRACKASTRA_INPUT_IMGS_FOLDER )
				.get();

		this.maskFolder = addPathArgument()
				.name( "Input mask folder path" )
				.help( "Directory with series of .tif files." )
				.argument( "--masks" )
				.visible( false )
				.required( true )
				.key( KEY_TRACKASTRA_INPUT_MASKS_FOLDER )
				.get();

		this.outputEdgeFile = addPathArgument()
				.name( "Output edge table path" )
				.help( "Path to write the edge CSV table to." )
				.argument( "--output-edge-table" )
				.visible( false )
				.required( true )
				.key( KEY_TRACKASTRA_OUTPUT_TABLE_PATH )
				.get();

		this.imageChannel = addExtraArgument( CommonTrackMateArguments.targetChannel( nChannels ) );
	}

	public PathArgument customModelPath()
	{
		return customModelPath;
	}

	public ChoiceArgument modelPretrained()
	{
		return modelPretrained;
	}

	public PathArgument maskFolder()
	{
		return maskFolder;
	}

	public PathArgument imageFolder()
	{
		return imageFolder;
	}

	public PathArgument outputEdgeFile()
	{
		return outputEdgeFile;
	}

	public ChoiceArgument trackingMode()
	{
		return trackingMode;
	}

	/**
	 * Exposes the argument that configures in what channel of the source image
	 * are the objects we want to track. The channel value is 1-based.
	 * <p>
	 * This is important for a multi-channel image. In Trackastra, this channel
	 * is used to computes some object features used for tracking. This extra
	 * element is not used in the CLI.
	 * 
	 * @return the image channel argument.
	 */
	public IntArgument imageChannel()
	{
		return imageChannel;
	}

	public static CliConfigPanel build( final TrackastraCLI cli )
	{
		return CliGuiBuilder.build( cli );
	}
}
