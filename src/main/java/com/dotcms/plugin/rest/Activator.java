package com.dotcms.plugin.rest;

import org.osgi.framework.BundleContext;
import com.dotcms.rest.SupportDebuggerResource;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotcms.repackage.org.apache.logging.log4j.LogManager;
import com.dotcms.repackage.org.apache.logging.log4j.core.LoggerContext;
import com.dotmarketing.loggers.Log4jUtil;
import com.dotmarketing.util.Logger;

public class Activator extends GenericBundleActivator {

	Class clazz = SupportDebuggerResource.class;

    private LoggerContext pluginLoggerContext;

	public void start(BundleContext context) throws Exception {

        //Initializing log4j...
        LoggerContext dotcmsLoggerContext = Log4jUtil.getLoggerContext();
        //Initialing the log4j context of this plugin based on the dotCMS logger context
        pluginLoggerContext = (LoggerContext) LogManager.getContext(this.getClass().getClassLoader(),
                false,
                dotcmsLoggerContext,
                dotcmsLoggerContext.getConfigLocation());

		Logger.info(this.getClass(), "Adding new Restful Service:" + clazz.getSimpleName());
		RestServiceUtil.addResource(clazz);
	}

	public void stop(BundleContext context) throws Exception {

		Logger.info(this.getClass(), "Removing new Restful Service:" + clazz.getSimpleName());
		RestServiceUtil.removeResource(clazz);
        //Shutting down log4j in order to avoid memory leaks
        Log4jUtil.shutdown(pluginLoggerContext);
	}

}