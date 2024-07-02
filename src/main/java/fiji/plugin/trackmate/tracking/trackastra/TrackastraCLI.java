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


import fiji.plugin.trackmate.util.cli.CliGuiBuilder;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder.CliConfigPanel;
import fiji.plugin.trackmate.util.cli.CommonTrackMateArguments;
import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator;
import ij.IJ;

public class TrackastraCLI extends CondaCLIConfigurator
{

	public static final String KEY_TRACKASTRA_MODEL = "PRETRAINED_MODEL";

	public static final String DEFAULT_TRACKASTRA_MODEL = "general_2d";

	public static final String KEY_TRACKASTRA_CUSTOM_MODEL_FOLDER = "CUSTOM_MODEL_PATH";

	public static final String DEFAULT_TRACKASTRA_CUSTOM_MODEL_FOLDER = System.getProperty( "user.home" );

	public static final String KEY_DEVICE = "DEVICE";

	public static final String DEFAULT_DEVICE = "automatic";

	public static final String KEY_TRACKASTRA_TRACKING_MODE = "TRACKING_MODE";

	public static final String DEFAULT_TRACKASTRA_TRACKING_MODE = "greedy";

	public static final String KEY_TRACKASTRA_INPUT_IMGS_FOLDER = "INPUT_IMGS_FOLDER";

	public static final String KEY_TRACKASTRA_INPUT_MASKS_FOLDER = "INPUT_MASKS_FOLDER";

	public static final String KEY_TRACKASTRA_OUTPUT_TABLE_PATH = "OUTPUT_EDGE_TABLE_PATH";

	public static final String KEY_TRACKASTRA_PRETRAINED_OR_CUSTOM = "PRETRAINED_OR_CUSTOM";

	public static final String DEFAULT_TRACKASTRA_PRETRAINED_OR_CUSTOM = KEY_TRACKASTRA_MODEL;

	private final ChoiceArgument modelPretrained;

	private final PathArgument customModelPath;

	private final SelectableArguments selectPretrainedOrCustom;

	private final ChoiceArgument trackingMode;

	private final ChoiceArgument useDevice;

	private final PathArgument imageFolder;

	private final PathArgument maskFolder;

	private final PathArgument outputEdgeFile;

	private final IntArgument imageChannel;

	public TrackastraCLI( final int nChannels )
	{
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
				.add( customModelPath )
				.key( KEY_TRACKASTRA_PRETRAINED_OR_CUSTOM );

		this.trackingMode = addChoiceArgument()
				.name( "Tracking mode" )
				.help( "Mode for candidate graph pruning. For installing the ilp tracker, see " +
						" https://github.com/weigertlab/trackastra#installation." )
				.argument( "--mode" )
				.addChoice( "greedy_nodiv" )
				.addChoice( "greedy" )
				.addChoice( "ilp" )
				.defaultValue( 1 )
				.key( KEY_TRACKASTRA_TRACKING_MODE )
				.get();

		this.useDevice = addChoiceArgument()
				.name( "Use GPU" )
				.help( "Device to use. If not set, tries to use cuda/mps if available, otherwise "
						+ " falling back to cpu." )
				.key( KEY_DEVICE )
				.argument( "--device" )
				.addChoice( DEFAULT_DEVICE )
				.addChoice( "mps" )
				.addChoice( "cuda" )
				.addChoice( "cpu" )
				.defaultValue( DEFAULT_DEVICE )
				.get();

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

	@Override
	protected String getCommand()
	{
		return IJ.isWindows() ? "trackastra track" : "trackastra.cli track";
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

	public ChoiceArgument useDevice()
	{
		return useDevice;
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

	public SelectableArguments selectPretrainedOrCustom()
	{
		return selectPretrainedOrCustom;
	}

	public static CliConfigPanel build( final TrackastraCLI cli )
	{
		return CliGuiBuilder.build( cli );
	}
}
