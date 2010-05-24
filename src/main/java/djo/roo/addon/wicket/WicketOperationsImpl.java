package djo.roo.addon.wicket;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.project.Property;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of commands that are available via the Roo shell.
 * 
 * @author Ben Alex
 * @since 1.1.0M1
 */
@Component
@Service
public class WicketOperationsImpl implements WicketOperations {

	private static Logger logger = Logger.getLogger(WicketOperations.class
			.getName());

	@Reference
	private FileManager fileManager;
	@Reference
	private PathResolver pathResolver;
	@Reference
	private MetadataService metadataService;
	@Reference
	private ProjectOperations projectOperations;

	private ComponentContext context;

	protected void activate(ComponentContext context) {
		this.context = context;
	}

	public boolean isSetupWicketAvailable() {
		ProjectMetadata project = (ProjectMetadata) metadataService
				.get(ProjectMetadata.getProjectIdentifier());
		if (project == null) {
			return false;
		}
		// Do not permit installation if they have a gwt package already in
		// their project
		// String root = GwtPath.GWT_ROOT.canonicalFileSystemPath(project);
		// return !fileManager.exists(root);
		return true;
	}

	public void setupWicket() {
		copyWebXml();
		manageWebXml();
		updateConfiguration();

	}

	private void copyWebXml() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService
				.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata required");

		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP,
				"WEB-INF/web.xml"))) {
			// file exists, so nothing to do
			return;
		}

		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(),
				"web-template.xml");
		Document webXml;
		try {
			webXml = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		WebXmlUtils.setDisplayName(projectMetadata.getProjectName(), webXml,
				null);
		WebXmlUtils.setDescription("Roo generated "
				+ projectMetadata.getProjectName() + " application", webXml,
				null);

		writeToDiskIfNecessary(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP,
				"WEB-INF/web.xml"), webXml);

		fileManager.scan();
	}

	private void manageWebXml() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService
				.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata required");

		// Verify that the web.xml already exists
		String webXmlFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP,
				"WEB-INF/web.xml");
		Assert.isTrue(fileManager.exists(webXmlFile), "'" + webXmlFile
				+ "' does not exist");

		Document webXml;
		try {
			webXml = XmlUtils.getDocumentBuilder().parse(
					fileManager.getInputStream(webXmlFile));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		WebXmlUtils.addFilter(WicketOperations.FILTER_NAME,
				"org.apache.wicket.protocol.http.WicketFilter", "/*", webXml,
				null, new WebXmlUtils.WebXmlParam("configuration",
						"development"), new WebXmlUtils.WebXmlParam(
						"applicationClassName", projectMetadata
								.getTopLevelPackage()
								.getFullyQualifiedPackageName()
								+ "wicket"));

		writeToDiskIfNecessary(webXmlFile, webXml);
	}

	private void createWebApplicationContext() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService
				.get(ProjectMetadata.getProjectIdentifier());
		Assert.isTrue(projectMetadata != null, "Project metadata required");

		// Verify the middle tier application context already exists
		PathResolver pathResolver = projectMetadata.getPathResolver();
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(
				Path.SPRING_CONFIG_ROOT, "applicationContext.xml")),
				"Application context does not exist");

		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP,
				"WEB-INF/applicationContext.xml"))) {
			// this file already exists, nothing to do
			return;
		}

		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(),
				"webmvc-config.xml");
		Document webMvcConfig;
		try {
			webMvcConfig = XmlUtils.getDocumentBuilder().parse(
					templateInputStream);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		Element rootElement = (Element) webMvcConfig.getFirstChild();
		XmlUtils.findFirstElementByName("context:component-scan", rootElement)
				.setAttribute(
						"base-package",
						projectMetadata.getTopLevelPackage()
								.getFullyQualifiedPackageName());
		writeToDiskIfNecessary(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP,
				"WEB-INF/spring/webmvc-config.xml"), webMvcConfig);

		fileManager.scan();
	}

	private void updateConfiguration() {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Element> springDependencies = XmlUtils.findElements(
				"/configuration/wicket/dependencies/dependency", configuration);
		for (Element dependency : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}

		projectOperations.addProperty(new Property(XmlUtils.findFirstElement(
				"/configuration/wicket/properties/wicket.version",
				configuration)));

		projectOperations.updateProjectType(ProjectType.WAR);

		projectOperations.addBuildPlugin(new Plugin(XmlUtils.findFirstElement(
				"/configuration/wicket/build/plugin", configuration)));
	}

	/** return indicates if disk was changed (ie updated or created) */
	private boolean writeToDiskIfNecessary(String fileName, Document proposed) {

		// Build a string representation of the JSP
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		XmlUtils.writeXml(XmlUtils.createIndentingTransformer(),
				byteArrayOutputStream, proposed);
		String xmlContent = byteArrayOutputStream.toString();

		// If mutableFile becomes non-null, it means we need to use it to write
		// out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(fileName)) {
			// First verify if the file has even changed
			File f = new File(fileName);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {
			}

			if (!xmlContent.equals(existing)) {
				mutableFile = fileManager.updateFile(fileName);
			}

		} else {
			mutableFile = fileManager.createFile(fileName);
			Assert.notNull(mutableFile, "Could not create XML file '"
					+ fileName + "'");
		}

		try {
			if (mutableFile != null) {
				// We need to write the file out (it's a new file, or the
				// existing file has different contents)
				FileCopyUtils.copy(xmlContent, new OutputStreamWriter(
						mutableFile.getOutputStream()));
				// Return and indicate we wrote out the file
				return true;
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not output '"
					+ mutableFile.getCanonicalPath() + "'", ioe);
		}

		// A file existed, but it contained the same content, so we return false
		return false;
	}
}