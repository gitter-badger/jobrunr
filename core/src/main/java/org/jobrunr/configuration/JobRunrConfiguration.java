package org.jobrunr.configuration;

import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.JsonMapperException;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;

public class JobRunrConfiguration {

    private StorageProvider storageProvider;
    private JsonMapper jsonMapper;
    private JobMapper jobMapper;
    private BackgroundJobServer backgroundJobServer;
    private JobRunrDashboardWebServer dashboardWebServer;
    private JobActivator jobActivator;
    private List<JobFilter> jobFilters;

    JobRunrConfiguration() {
        this.jsonMapper = determineJsonMapper();
        this.jobMapper = new JobMapper(jsonMapper);
        this.jobFilters = new ArrayList<>();
    }

    public JobRunrConfiguration useStorageProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        storageProvider.setJobMapper(jobMapper);
        return this;
    }

    public JobRunrConfiguration withJobFilter(JobFilter... jobFilters) {
        this.jobFilters.addAll(Arrays.asList(jobFilters));
        return this;
    }

    public JobRunrConfiguration useDefaultBackgroundJobServer() {
        this.backgroundJobServer = new BackgroundJobServer(storageProvider, jobActivator);
        this.backgroundJobServer.setJobFilters(jobFilters);
        this.backgroundJobServer.start();
        return this;
    }

    public JobRunrConfiguration useBackgroundJobServer(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.backgroundJobServer.setJobFilters(jobFilters);
        return this;
    }

    public JobRunrConfiguration useDashboard() {
        this.dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper);
        return this;
    }

    public JobRunrConfiguration useJobActivator(JobActivator jobActivator) {
        this.jobActivator = jobActivator;
        return this;
    }

    public JobScheduler initialize() {
        final JobScheduler jobScheduler = new JobScheduler(storageProvider, jobFilters);
        BackgroundJob.setJobScheduler(jobScheduler);
        return jobScheduler;
    }

    private static JsonMapper determineJsonMapper() {
        if (classExists("com.fasterxml.jackson.databind.ObjectMapper")) {
            return new JacksonJsonMapper();
        } else if (classExists("com.google.gson.Gson")) {
            return new GsonJsonMapper();
        } else {
            throw new JsonMapperException("No JsonMapper class is found. Make sure you have Jackson as Json library available");
        }
    }
}
