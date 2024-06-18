package fiji.plugin.trackmate.tracking.trackastra;

import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

public class TrackastraFactory implements SpotTrackerFactory
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String KEY = "CELLPOSE_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose detector";

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

	public static final ImageIcon ICON = new ImageIcon( GuiUtils.getResource( "images/Trackastra-icon.png", TrackastraFactory.class ) );

	private String errorMessage;;

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
	public SpotTracker create( final SpotCollection spots, final Map< String, Object > settings )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString( final Map< String, Object > sm )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkSettingsValidity( final Map< String, Object > settings )
	{
		// TODO Auto-generated method stub
		return false;
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
