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

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readStringAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.DEFAULT_DEVICE;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.DEFAULT_TRACKASTRA_CUSTOM_MODEL_FOLDER;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.DEFAULT_TRACKASTRA_MODEL;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.DEFAULT_TRACKASTRA_PRETRAINED_OR_CUSTOM;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.DEFAULT_TRACKASTRA_TRACKING_MODE;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.KEY_DEVICE;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.KEY_TRACKASTRA_CUSTOM_MODEL_FOLDER;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.KEY_TRACKASTRA_MODEL;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.KEY_TRACKASTRA_PRETRAINED_OR_CUSTOM;
import static fiji.plugin.trackmate.tracking.trackastra.TrackastraCLI.KEY_TRACKASTRA_TRACKING_MODE;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import static fiji.plugin.trackmate.util.cli.CondaCLIConfigurator.KEY_CONDA_ENV;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.tracking.SpotImageTrackerFactory;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import ij.ImagePlus;

@Plugin( type = SpotTrackerFactory.class )
public class TrackastraTrackerFactory implements SpotImageTrackerFactory
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String KEY = "TRACKASTRA_TRACKER";

	/** The pretty name of the target detector. */
	public static final String NAME = "Trackastra tracker";

	protected static final String DOC1_URL = "https://imagej.net/plugins/trackmate/trackers/trackmate-trackastra";

	public static final String SHORT_INFO_TEXT = "Trackastra links segmented cells by predicting associations "
			+ "with a transformer model that was trained on a diverse set of 2D and 3D microscopy videos.";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This tracker relies on Trackastra to track objects. "
			+ SHORT_INFO_TEXT
			+ "<p>"
			+ "This tracker module simply calls an external Trackastra installation. So for it "
			+ "to work, you must have a Trackastra installation running on your computer. "
			+ "Please follow the instructions from the Trackastra website: "
			+ "<u><a href=\"https://github.com/weigertlab/trackastra\">https://github.com/weigertlab/trackastra</a></u>"
			+ "In the next config panel, you will also need to specify the conda "
			+ "environment in which Trackastra is installed. "
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the Trackastra paper: <a href=\"https://doi.org/10.48550/arXiv.2405.15700\">Benjamin Gallusser and Martin Weigert. "
			+ "Trackastra - Transformer-based cell tracking for live-cell microscopy. "
			+ "arXiv, 2024</a>"
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"" + DOC1_URL + "\">on the ImageJ Wiki</a>."
			+ "</html>";

	public static final ImageIcon ICON;
	static
	{
		final URL resource = GuiUtils.getResource( "images/Tracksastra-icon.png", TrackastraTrackerFactory.class );
		ICON = new ImageIcon( resource );
	}

	private String errorMessage;;

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
		// return ICON;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public TrackastraTracker create( final SpotCollection spots, final Map< String, Object > settings, final ImagePlus imp )
	{
		final TrackastraCLI cli = new TrackastraCLI( imp.getNChannels() );
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		return new TrackastraTracker( cli, spots, imp );
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model, final ImagePlus imp )
	{
		return new TrackastraConfigurationPanel( imp.getNChannels() );
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & writeAttribute( settings, element, KEY_CONDA_ENV, String.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_TRACKASTRA_MODEL, String.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_TRACKASTRA_CUSTOM_MODEL_FOLDER, String.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_TRACKASTRA_TRACKING_MODE, String.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_DEVICE, String.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_TRACKASTRA_PRETRAINED_OR_CUSTOM, String.class, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readStringAttribute( element, settings, KEY_CONDA_ENV, errorHolder );
		ok = ok & readStringAttribute( element, settings, KEY_TRACKASTRA_MODEL, errorHolder );
		ok = ok & readStringAttribute( element, settings, KEY_TRACKASTRA_CUSTOM_MODEL_FOLDER, errorHolder );
		ok = ok & readStringAttribute( element, settings, KEY_TRACKASTRA_TRACKING_MODE, errorHolder );
		ok = ok & readStringAttribute( element, settings, KEY_DEVICE, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		ok = ok & readStringAttribute( element, settings, KEY_TRACKASTRA_PRETRAINED_OR_CUSTOM, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();
		return ok;
	}

	@Override
	public String toString( final Map< String, Object > settings )
	{
		if ( !checkSettingsValidity( settings ) )
			return errorMessage;
		return TMUtils.echoMap( settings, 0 );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > map = new HashMap<>();
		map.clear();
		map.put( KEY_CONDA_ENV, "" );
		map.put( KEY_TRACKASTRA_MODEL, DEFAULT_TRACKASTRA_MODEL );
		map.put( KEY_TRACKASTRA_CUSTOM_MODEL_FOLDER, DEFAULT_TRACKASTRA_CUSTOM_MODEL_FOLDER );
		map.put( KEY_TRACKASTRA_TRACKING_MODE, DEFAULT_TRACKASTRA_TRACKING_MODE );
		map.put( KEY_DEVICE, DEFAULT_DEVICE );
		map.put( KEY_TARGET_CHANNEL, Integer.valueOf( 1 ) );
		map.put( KEY_TRACKASTRA_PRETRAINED_OR_CUSTOM, DEFAULT_TRACKASTRA_PRETRAINED_OR_CUSTOM );
		return map;
	}

	@Override
	public boolean checkSettingsValidity( final Map< String, Object > settings )
	{
		if ( null == settings )
		{
			errorMessage = "Settings map is null.\n";
			return false;
		}

		boolean ok = true;
		final StringBuilder str = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_CONDA_ENV, String.class, str );
		ok = ok & checkParameter( settings, KEY_TRACKASTRA_MODEL, String.class, str );
		ok = ok & checkParameter( settings, KEY_TRACKASTRA_CUSTOM_MODEL_FOLDER, String.class, str );
		ok = ok & checkParameter( settings, KEY_TRACKASTRA_TRACKING_MODE, String.class, str );
		ok = ok & checkParameter( settings, KEY_TRACKASTRA_PRETRAINED_OR_CUSTOM, String.class, str );
		ok = ok & checkParameter( settings, KEY_DEVICE, String.class, str );
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, str );
		if ( !ok )
		{
			errorMessage = str.toString();
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public SpotImageTrackerFactory copy()
	{
		return new TrackastraTrackerFactory();
	}
}
