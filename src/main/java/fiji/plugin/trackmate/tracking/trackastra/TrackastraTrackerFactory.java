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

import java.net.URL;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.tracking.SpotImageTrackerFactory;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.tracking.SpotTrackerFactoryGenericConfig;
import fiji.plugin.trackmate.util.cli.GenericConfigurationPanel;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import ij.ImagePlus;

@Plugin( type = SpotTrackerFactory.class )
public class TrackastraTrackerFactory implements SpotImageTrackerFactory, SpotTrackerFactoryGenericConfig< TrackastraCLI >
{

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
		final URL resource = GuiUtils.getResource( "images/Tracksastra-icon-64px.png", TrackastraTrackerFactory.class );
		ICON = new ImageIcon( resource );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return ICON;
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
	public String getUrl()
	{
		return DOC1_URL;
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model, final ImagePlus imp )
	{
		final TrackastraCLI config = getConfigurator( imp );
		return new GenericConfigurationPanel(
				config,
				getName(),
				getIcon(),
				getUrl() );
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		return SpotImageTrackerFactory.super.getTrackerConfigurationPanel( model );
	}

	@Override
	public TrackastraTracker create( final SpotCollection spots, final Map< String, Object > settings, final ImagePlus imp )
	{
		final TrackastraCLI cli = getConfigurator( imp );
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		return new TrackastraTracker( cli, spots, imp );
	}

	@Override
	public TrackastraCLI getConfigurator( final ImagePlus imp )
	{
		return new TrackastraCLI( imp.getNChannels() );
	}
}
