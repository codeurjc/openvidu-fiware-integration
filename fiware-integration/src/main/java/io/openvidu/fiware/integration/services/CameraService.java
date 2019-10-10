package io.openvidu.fiware.integration.services;

import io.openvidu.fiware.integration.config.OpenViduConfig;
import io.openvidu.fiware.integration.daos.OpenViduConnector;
import io.openvidu.fiware.integration.daos.publisher.CameraPublisher;
import io.openvidu.fiware.integration.daos.reader.CameraReader;
import io.openvidu.fiware.integration.errors.OrionConnectorException;
import io.openvidu.fiware.integration.models.api.ApiCameraModel;
import io.openvidu.fiware.integration.models.api.requests.UpdateCameraRequest;
import io.openvidu.fiware.integration.utils.Consts;
import io.openvidu.fiware.integration.utils.Utils;
import io.openvidu.java.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CameraService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    OpenViduConfig openViduConfig;

    @Autowired
    OpenViduConnector openViduConnector;

    @Autowired
    CameraPublisher cameraPublisher;

    @Autowired
    CameraReader cameraReader;

    /**
     * Creates a new camera.
     */
    public ApiCameraModel createCamera(ApiCameraModel request) {
        String cameraUuid = request.getCameraUuid();
        if (cameraUuid == null) {
            // Auto-generate the uuid if it is not present.
            try {
                MessageDigest salt = MessageDigest.getInstance("SHA-256");
                salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
                cameraUuid = Utils.bytesToHex(salt.digest()).substring(0, 16);
            } catch (Exception e) {
                log.error("Error creating an uuid for the camera", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error creating an uuid for the camera");
            }
        }

        log.debug("Finding the session in Orion by uuid: " + cameraUuid);

        ApiCameraModel camera;
        try {
            camera = cameraReader.readObject(cameraUuid);
        } catch (OrionConnectorException e) {
            camera = new ApiCameraModel();
            camera.setCameraUuid(cameraUuid);
            camera.setCameraAddress(request.getCameraAddress());
            camera.setProtocol(request.getProtocol());
            camera.setCreationDate(Utils.getFormattedCurrentDate());
            camera.setDescription(request.getDescription());
            camera.setAddress(Utils.getPublicUrl());
        }

        if (camera.isActive()) {
            // Already created session with an active camera.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "There is already an active camera register by the uuid: " + cameraUuid);
        }

        // Build tokenOptions object with the serverData and the role
        TokenOptions tokenOptions = new TokenOptions.Builder().role(OpenViduRole.PUBLISHER).build();

        // Create the session if it does not exist.
        Session session = openViduConfig.getSessionFromCameraId(cameraUuid);
        if (session == null) {
            try {
                log.debug("Creating a new OpenVidu session");

                SessionProperties properties = new SessionProperties.Builder().customSessionId(cameraUuid).build();
                session = openViduConfig.getOpenVidu().createSession(properties);

                log.debug("Pushing to Orion: " + camera);

                cameraPublisher.publish(camera);
            } catch (Exception e) {
                log.error("Error creating a new OpenVidu session", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error creating a new OpenVidu session");
            }
        }

        // Get a publisher token.
        try {
            String token = session.generateToken(tokenOptions);
            camera.setOVToken(token);
        } catch (OpenViduJavaClientException e) {
            e.printStackTrace();
        } catch (OpenViduHttpException e) {
            log.error("Error creating a new publisher token", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating a new publisher token");
        }

        return camera;
    }

    /**
     * Gets a camera.
     */
    public ApiCameraModel getCamera(String cameraUuid) {
        ApiCameraModel camera = null;
        try {
            camera = cameraReader.readObject(cameraUuid);
        } catch (OrionConnectorException ignored) {
        }

        return camera;
    }

    /**
     * Creates a new camera.
     */
    public void deleteCamera(String cameraUuid) {
        log.debug("Removing camera from Orion with uuid: " + cameraUuid);

        cameraPublisher.delete(cameraUuid);

        // TODO send signal to unpublish the camera in the browser.

        Session session = openViduConfig.getSessionFromCameraId(cameraUuid);
        if (session != null) {
            try {
                session.close();
            } catch (OpenViduJavaClientException | OpenViduHttpException e) {
                log.error("Error removing session: " + session, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error removing session: " + session);
            }
        }
    }

    /**
     * Gets a new camera token to visualize its video.
     */
    public String getCameraToken(String cameraUuid) {
        // Get camera.
        ApiCameraModel camera;
        try {
            camera = cameraReader.readObject(cameraUuid);
        } catch (OrionConnectorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The camera uuid (" + cameraUuid + ") does not exist");
        }

        // Get session.
        Session session = openViduConfig.getSessionFromCameraId(cameraUuid);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The session for the camera uuid (" + cameraUuid + ") does not exist");
        }

        if (!camera.isActive()) {
            // The requested camera is inactive.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The camera is inactive. For uuid: " + cameraUuid);
        }

        // Build tokenOptions object with the serverData and the role
        TokenOptions tokenOptions = new TokenOptions.Builder().role(OpenViduRole.SUBSCRIBER).build();

        try {
            return session.generateToken(tokenOptions);
        } catch (Exception e) {
            log.error("Error obtaining a new subscriber token for: " + cameraUuid, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error obtaining a new subscriber token for: " + cameraUuid);
        }
    }

    /**
     * Updates the specified camera.
     */
    public void updateCamera(String cameraUuid, UpdateCameraRequest request) {
        // Get camera.
        ApiCameraModel camera;
        try {
            camera = cameraReader.readObject(cameraUuid);
            log.info("333333 {}", camera);
        } catch (OrionConnectorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The camera uuid (" + cameraUuid + ") does not exist");
        }

        if (request.getDescription() != null) {
            camera.setDescription(request.getDescription());
        }

        // Update OpenVidu if the filter has changed and is active but only if there are
        // events.
        if (camera.isActive() && request.getEvents() != null && !request.getEvents().isEmpty()) {
            String publisherStreamId = getPublisherStreamIdFromSession(cameraUuid);
            log.info("4444");

            if (!checkFiltersAreEquals(camera.getFilter(), request.getFilter())) {
                // Remove old events
                for (String ev : camera.getEvents()) {
                    openViduConnector.removeEventListener(cameraUuid, publisherStreamId, ev);
                }
                log.info("5555");

                if (request.getFilter() == null) {
                    // Remove the filter
                    openViduConnector.removeFilter(cameraUuid, publisherStreamId);
                } else {
                    try {
                        // Remove the filter
                        openViduConnector.removeFilter(cameraUuid, publisherStreamId);
                    } catch (Throwable e) {
                    }
                    openViduConnector.applyFilter(cameraUuid, publisherStreamId, request.getFilter());

                    // Add new events
                    for (String ev : request.getEvents()) {
                        openViduConnector.addEventListener(cameraUuid, publisherStreamId, ev);
                    }
                }
            } else if (!checkFilterEventsAreEquals(camera.getEvents(), request.getEvents())) {
                log.info("6666");

                // Remove old events
                for (String ev : camera.getEvents()) {
                    openViduConnector.removeEventListener(cameraUuid, publisherStreamId, ev);
                }

                // Add new events
                for (String ev : request.getEvents()) {
                    openViduConnector.addEventListener(cameraUuid, publisherStreamId, ev);
                }
            }
        }

        if (request.getFilter() == null) {
            camera.setFilter(null);
            camera.setEvents(new ArrayList<>());
        } else {
            camera.setFilter(request.getFilter());
            camera.setEvents(request.getEvents());
        }

        // (De)activate the session depending on the value
        if (request.getActive() != null) { // && request.getActive() != camera.isActive() - this condition is forced to
                                           // be true always.
            List<String> to = new ArrayList<>();
            to.add(getPublisherParticipantIdFromSession(cameraUuid));

            openViduConnector.sendSignal(cameraUuid, Consts.OpenViduSignal, request.getActive().toString(), to);
            camera.setActive(request.getActive());
        }

        log.debug("Updating camera in Orion: " + camera);

        cameraPublisher.update(camera);
    }

    /**
     * Gets all available cameras.
     */
    public List<ApiCameraModel> getAllCameras() {
        return cameraReader.readObjectList(Consts.OpenViduCameraOrionType);
    }

    private boolean checkFiltersAreEquals(String cameraFilter, String requestFilter) {
        if (cameraFilter == null) {
            return requestFilter == null;
        }

        return cameraFilter.equals(requestFilter);
    }

    private boolean checkFilterEventsAreEquals(List<String> cameraEvents, List<String> requestEvents) {
        if (cameraEvents.size() != requestEvents.size()) {
            return false;
        }

        for (int i = 0; i < cameraEvents.size(); i++) {
            String cameraEvent = cameraEvents.get(i);

            if (!requestEvents.contains(cameraEvent)) {
                return false;
            }
        }

        return true;
    }

    private String getPublisherStreamIdFromSession(String cameraUuid) {
        Connection connection = openViduConfig.getPublisherConnection(cameraUuid);

        if (connection == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot obtain the publisher for the camera uuid (" + cameraUuid + ")");
        }

        return connection.getPublishers().get(connection.getPublishers().size() - 1).getStreamId();
    }

    private String getPublisherParticipantIdFromSession(String cameraUuid) {
        Connection connection = openViduConfig.getPublisherConnection(cameraUuid);

        if (connection == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot obtain the publisher for the camera uuid (" + cameraUuid + ")");
        }

        return connection.getConnectionId();
    }
}
