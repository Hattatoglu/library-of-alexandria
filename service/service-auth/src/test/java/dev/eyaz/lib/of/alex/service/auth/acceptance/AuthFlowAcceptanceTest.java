package dev.eyaz.lib.of.alex.service.auth.acceptance;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME,
        value = "dev.eyaz.lib.of.alex.service.auth.acceptance.steps")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/acceptance-report.html")
public class AuthFlowAcceptanceTest {}
