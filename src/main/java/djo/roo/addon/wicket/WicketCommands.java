package djo.roo.addon.wicket;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;

/**
 * Sample of a command class. The command class is registered by the Roo shell following an
 * automatic classpath scan. You can provide simple user presentation-related logic in this
 * class. You can return any objects from each method, or use the logger directly if you'd
 * like to emit messages of different severity (and therefore different colours on 
 * non-Windows systems).
 * 
 * @since 1.1.0-M1
 */
@Component
@Service
public class WicketCommands implements CommandMarker {
	
	private static Logger logger = Logger.getLogger(WicketCommands.class.getName());

@Reference private WicketOperations wicketOperations;
	
	@CliAvailabilityIndicator("wicket setup")
	public boolean isInstallSecurityAvailable() {
		return wicketOperations.isSetupWicketAvailable();
	}
	
	@CliCommand(value="wicket setup", help="Install Apache wicket into your project")
	public void installSecurity() {
		wicketOperations.setupWicket();
	}
}