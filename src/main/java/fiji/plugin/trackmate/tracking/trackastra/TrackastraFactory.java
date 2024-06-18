package fiji.plugin.trackmate.tracking.trackastra;

import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.tracking.SpotGlobalTrackerFactory;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;

@Plugin( type = SpotTrackerFactory.class )
public class TrackastraFactory implements SpotGlobalTrackerFactory
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String KEY = "TRACKASTRA_TRACKER";

	/** The pretty name of the target detector. */
	public static final String NAME = "Trackastra tracker";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This tracker relies on Trackastra to track objects."
			+ "<p>"
			+ "The detector simply calls an external Trackastra installation. So for this "
			+ "to work, you must have a Trackastra installation running on your computer. "
			+ "Please follow the instructions from the Trackastra website: "
			+ "<u><a href=\"https://github.com/weigertlab/trackastra\">https://github.com/weigertlab/trackastra</a></u>"
			+ "<p>"
			+ "You will also need to specify the path to the <b>Python executable</b> that can run Trackastra. "
			+ "For instance if you used anaconda to install Trackastra, and that you have a "
			+ "Conda environment called 'trackastra', this path will be something along the line of "
			+ "'/opt/anaconda3/envs/trackastra/bin/python'."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the Trackastra paper: <a href=\"https://doi.org/10.48550/arXiv.2405.15700\">Benjamin Gallusser and Martin Weigert. "
			+ "Trackastra - Transformer-based cell tracking for live-cell microscopy. "
			+ "arXiv, 2024</a>"
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"https://imagej.net/plugins/trackmate/trackers/trackastra\">on the ImageJ Wiki</a>."
			+ "</html>";

	public static final ImageIcon ICON;
	static
	{
		final URL resource = GuiUtils.getResource( "images/Tracksastra-icon.png", TrackastraFactory.class );
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
	public TrackastraTracker create( final TrackMate trackmate, final Map< String, Object > settings )
	{
		final TrackastraCLI cli = new TrackastraCLI();
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		return new TrackastraTracker( cli, trackmate );
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		return new TrackastraConfigurationPanel();
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		errorMessage = null;
		final StringBuilder str = new StringBuilder();
		boolean ok = true;
		for ( final String key : settings.keySet() )
		{
			final Object obj = settings.get( key );
			if ( obj == null )
			{
				str.append( "Parameter " + key + " is not set.\n" );
				ok = false;
				continue;
			}
			ok = ok & writeAttribute( settings, element, key, obj.getClass(), str );
		}
		if ( !ok )
			errorMessage = str.toString();
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final Map< String, Object > def = getDefaultSettings();
		boolean ok = true;
		for ( final String key : def.keySet() )
		{
			final Class< ? extends Object > clazz = def.get( key ).getClass();
			ok = ok & IOUtils.readAttribute( element, key, clazz, settings, errorHolder );
		}
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
		final TrackastraCLI cli = new TrackastraCLI();
		final Map< String, Object > map = new HashMap<>();
		TrackMateSettingsBuilder.toTrackMateSettings( map, cli );
		return map;
	}

	@Override
	public boolean checkSettingsValidity( final Map< String, Object > settings )
	{
		errorMessage = null;
		final TrackastraCLI cli = new TrackastraCLI();
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		final String error = cli.check();
		if ( error != null )
		{
			errorMessage = error;
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
	public SpotTrackerFactory copy()
	{
		return new TrackastraFactory();
	}
}
