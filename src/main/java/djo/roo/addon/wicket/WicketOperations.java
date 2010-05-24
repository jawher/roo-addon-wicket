package djo.roo.addon.wicket;

/**
 * Interface of commands that are available via the Roo shell.
 *
 * @author Jawher Moussa
 * @since 1.1.0-M1
 */
public interface WicketOperations {

	String FILTER_NAME = "Wicket Application Filter";

	boolean isSetupWicketAvailable();

	void setupWicket();
}