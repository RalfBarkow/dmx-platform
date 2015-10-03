package de.deepamehta.core.osgi;

import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.impl.PluginImpl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Base class for all DeepaMehta plugins.
 * All DeepaMehta plugins are derived from this class, directly or indirectly.
 * ### FIXDOC: subclassing is not required if the plugin has no server-side part.
 */
public class PluginActivator implements BundleActivator, PluginContext {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected DeepaMehtaService dms;
    protected Bundle bundle;

    private BundleContext bundleContext;
    private PluginImpl plugin;
    private String pluginName;  // This bundle's name = POM project name, e.g. "DeepaMehta 4 Webclient"

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        try {
            // Note: logging "this" requires "pluginName" to be initialzed already
            this.bundleContext = context;
            this.bundle = context.getBundle();
            this.pluginName = (String) bundle.getHeaders().get("Bundle-Name");
            //
            logger.info("========== Starting " + this + " ==========");
            plugin = new PluginImpl(this);
            plugin.start();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while starting " + this + ":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
            // File Install would retry to start the bundle endlessly.
        }
    }

    @Override
    public void stop(BundleContext context) {
        try {
            if (plugin == null) {
                logger.info("Stopping " + this + " ABORTED -- it was not successfully started");
                return;
            }
            //
            logger.info("========== Stopping " + this + " ==========");
            plugin.stop();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while stopping " + this + ":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
        }
    }



    // ************************************
    // *** PluginContext Implementation ***
    // ************************************



    @Override
    public void init() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void serviceArrived(Object service) {
    }

    @Override
    public void serviceGone(Object service) {
    }

    // ---

    @Override
    public final String getPluginName() {
        return pluginName;
    }

    @Override
    public final BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public final void setCoreService(DeepaMehtaService dms) {
        this.dms = dms;
    }



    // ===

    @Override
    public String toString() {
        return "plugin \"" + pluginName + "\"";
    }



    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected final String getUri() {
        return plugin.getUri();
    }

    protected final InputStream getStaticResource(String name) {
        return plugin.getStaticResource(name);
    }

    // ---

    /**
     * Publishes a directory of the server's file system.
     *
     * @param   path            An absolute path to a directory.
     */
    protected final void publishFileSystem(String uriNamespace, String path) {
        plugin.publishFileSystem(uriNamespace, path);
    }
}
