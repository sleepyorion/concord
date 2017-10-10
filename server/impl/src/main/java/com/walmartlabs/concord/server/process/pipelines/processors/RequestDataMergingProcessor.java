package com.walmartlabs.concord.server.process.pipelines.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.project.model.ProjectDefinition;
import com.walmartlabs.concord.project.model.ProjectDefinitionUtils;
import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.process.ProcessException;
import com.walmartlabs.concord.server.process.keys.AttachmentKey;
import com.walmartlabs.concord.server.process.logs.LogManager;
import com.walmartlabs.concord.server.project.ProjectDao;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Named
public class RequestDataMergingProcessor implements PayloadProcessor {

    public static final AttachmentKey REQUEST_ATTACHMENT_KEY = AttachmentKey.register("request");
    public static final List<String> DEFAULT_PROFILES = Collections.singletonList("default");

    private final ProjectDao projectDao;
    private final LogManager logManager;

    @Inject
    public RequestDataMergingProcessor(ProjectDao projectDao, LogManager logManager) {
        this.projectDao = projectDao;
        this.logManager = logManager;
    }

    @Override
    public Payload process(Chain chain, Payload payload) {
        // configuration from the user's request
        Map<String, Object> req = payload.getHeader(Payload.REQUEST_DATA_MAP, Collections.emptyMap());

        // project configuration
        Map<String, Object> projectCfg = getProjectCfg(payload);

        // _main.json file in the workspace
        Map<String, Object> workspaceCfg = getWorkspaceCfg(payload);

        // attached to the request JSON file
        Map<String, Object> attachedCfg = getAttachedCfg(payload);

        // determine the active profile names
        List<String> activeProfiles = getActiveProfiles(ImmutableList.of(req, attachedCfg, workspaceCfg, projectCfg));
        payload = payload.putHeader(Payload.ACTIVE_PROFILES, activeProfiles);

        // merged profile data
        Map<String, Object> profileCfg = getProfileCfg(payload, activeProfiles);

        // create the resulting configuration
        Map<String, Object> m = ConfigurationUtils.deepMerge(projectCfg, profileCfg, workspaceCfg, attachedCfg, req);

        // check if we need to set an entry point
        m = assertEntryPoint(payload.getInstanceId(), m);

        payload = payload.putHeader(Payload.REQUEST_DATA_MAP, m);

        return chain.process(payload);
    }

    private Map<String, Object> getProjectCfg(Payload payload) {
        UUID projectId = payload.getHeader(Payload.PROJECT_ID);
        if (projectId == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> m = projectDao.getConfiguration(projectId);
        return m != null ? m : Collections.emptyMap();
    }

    private Map<String, Object> getWorkspaceCfg(Payload payload) {
        Path workspace = payload.getHeader(Payload.WORKSPACE_DIR);
        Path src = workspace.resolve(Constants.Files.REQUEST_DATA_FILE_NAME);
        if (!Files.exists(src)) {
            return Collections.emptyMap();
        }

        try (InputStream in = Files.newInputStream(src)) {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(in, Map.class);
        } catch (IOException e) {
            throw new ProcessException(payload.getInstanceId(), "Invalid request data format", e, Status.BAD_REQUEST);
        }
    }

    private Map<String, Object> getProfileCfg(Payload payload, List<String> activeProfiles) {
        if (activeProfiles == null) {
            activeProfiles = Collections.emptyList();
        }

        ProjectDefinition pd = payload.getHeader(Payload.PROJECT_DEFINITION);
        if (pd == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> m = ProjectDefinitionUtils.getVariables(pd, activeProfiles);
        return m != null ? m : Collections.emptyMap();
    }

    private Map<String, Object> getAttachedCfg(Payload payload) {
        Path p = payload.getAttachment(REQUEST_ATTACHMENT_KEY);
        if (p == null) {
            return Collections.emptyMap();
        }

        try (InputStream in = Files.newInputStream(p)) {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(in, Map.class);
        } catch (IOException e) {
            throw new ProcessException(payload.getInstanceId(), "Invalid request data format", e, Status.BAD_REQUEST);
        }
    }

    private List<String> getActiveProfiles(List<Map<String, Object>> mm) {
        for (Map<String, Object> m : mm) {
            List<String> v = (List<String>) m.get(Constants.Request.ACTIVE_PROFILES_KEY);
            if (v != null) {
                return v;
            }
        }
        return DEFAULT_PROFILES;
    }

    private Map<String, Object> assertEntryPoint(UUID instanceId, Map<String, Object> m) {
        String s = (String) m.get(Constants.Request.ENTRY_POINT_KEY);
        if (s != null) {
            return m;
        }

        logManager.info(instanceId, "Using the default entry point");
        m.put(Constants.Request.ENTRY_POINT_KEY, Constants.Request.DEFAULT_ENTRY_POINT_NAME);
        return m;
    }
}
