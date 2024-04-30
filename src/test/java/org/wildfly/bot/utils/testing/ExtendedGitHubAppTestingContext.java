package org.wildfly.bot.utils.testing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.internal.GitHubAppTestingContext;
import io.quarkiverse.githubapp.testing.internal.MockitoUtils;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAuthenticatedAppInstallation;
import org.kohsuke.github.GHEventInfo;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedSearchIterable;
import org.kohsuke.github.connector.GitHubConnectorResponse;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;
import org.wildfly.bot.utils.testing.reflection.ExposeGitHubAppTestingContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.wildfly.bot.utils.testing.reflection.ExposeGitHubAppTestingContext.setPayload;

public final class ExtendedGitHubAppTestingContext {

    private final GitHubAppTestingContext testingContext;

    private final GHApp ghApp;
    private final GHAppInstallation ghAppInstallation;

    private static final String REPO_PATH = "/repo/name";
    private static final String PAYLOAD = "payload";

    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final Map<String, Object> injected;

    static {
        objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }

    public ExtendedGitHubAppTestingContext() {
        this.testingContext = GitHubAppTestingContext.get();
        ghApp = MockitoUtils.doWithMockedClassClassLoader(GHApp.class, () -> Mockito.mock(GHApp.class));
        ghAppInstallation = MockitoUtils.doWithMockedClassClassLoader(GHAppInstallation.class,
                () -> Mockito.mock(GHAppInstallation.class));

        injected = new HashMap<>();
        injected.put(GitHubConnectorResponse.class.getName(), null);
        injected.put(GitHub.class.getName(), testingContext.mocks.applicationClient());
    }

    public GitHubAppTestingContext getTestingContext() {
        return this.testingContext;
    }

    void init() {
        reset();

        try {
            ExposeGitHubAppTestingContext.init(testingContext);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(
                    "Unable to call the original method from quarkus-github-app. Perhaps the declaration of method has changed?",
                    e);
        }
    }

    void initEventStubs(long installationId, String payload) throws IOException {
        try {
            ExposeGitHubAppTestingContext.initEventStubs(testingContext,
                    installationId);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(
                    "Unable to call the original method from quarkus-github-app. Perhaps the declaration of method has changed?",
                    e);
        }

        GitHub gitHub = testingContext.mocks.applicationClient();
        when(gitHub.getApp()).thenReturn(ghApp);

        // mock a single installation
        when(ghAppInstallation.getId()).thenReturn(installationId);
        PagedSearchIterable<GHAppInstallation> ghAppInstallations = GitHubAppMockito.mockPagedIterable(ghAppInstallation);
        when(ghApp.listInstallations()).thenReturn(ghAppInstallations);

        GitHub installation = testingContext.mocks.installationClient(installationId);
        GHAuthenticatedAppInstallation ghAuthenticatedAppInstallation = Mockito.mock(GHAuthenticatedAppInstallation.class);
        when(installation.getInstallation()).thenReturn(ghAuthenticatedAppInstallation);

        // for this installation mock all received repositories and their respective events
        Map<String, List<String>> repositories = collectRepos(payload);
        Collection<GHRepository> mockedRepositories = repositories.entrySet().stream().map(entry -> {
            GHRepository mockedRepo = this.testingContext.mocks.repository(entry.getKey());
            when(mockedRepo.getFullName()).thenReturn(entry.getKey());
            List<GHEventInfo> repoEvents = entry.getValue().stream().map(s -> {
                try {
                    JsonNode payloadJson = objectMapper.readTree(s);
                    JsonNode payloadNode = ((ObjectNode) payloadJson).remove(PAYLOAD);

                    GHEventInfo ghEventInfo = objectMapper.reader(new InjectableValues.Std(injected)).forType(GHEventInfo.class)
                            .readValue(payloadJson);

                    setPayload(ghEventInfo, payloadNode);

                    when(ghEventInfo.getRepository()).thenReturn(mockedRepo);
                    return ghEventInfo;
                } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(
                            "Unable to call the original method from quarkus-github-app. Perhaps the declaration of method has changed?",
                            e);
                }
            }).toList();
            PagedSearchIterable<GHEventInfo> mockedRepoEvents = mockPagedSearchIterable(repoEvents);
            try {
                when(mockedRepo.listEvents()).thenReturn(mockedRepoEvents);
            } catch (IOException e) {
                // This should not happen, as it's mocked invocation
                throw new RuntimeException(e);
            }
            return mockedRepo;
        }).toList();
        PagedSearchIterable<GHRepository> ghRepositories = mockPagedSearchIterable(mockedRepositories);
        when((ghAuthenticatedAppInstallation.listRepositories())).thenReturn(ghRepositories);
    }

    private static <T> PagedSearchIterable<T> mockPagedSearchIterable(Collection<T> collection) {
        PagedSearchIterable<T> iterableMock = mock(PagedSearchIterable.class,
                withSettings().stubOnly().strictness(Strictness.LENIENT).defaultAnswer(Answers.RETURNS_SELF));
        when(iterableMock.spliterator()).thenAnswer(ignored -> collection.spliterator());
        when(iterableMock.iterator()).thenAnswer(ignored -> {
            PagedIterator<GHRepository> iteratorMock = mock(PagedIterator.class,
                    withSettings().stubOnly().strictness(Strictness.LENIENT));
            Iterator<T> actualIterator = collection.iterator();
            when(iteratorMock.next()).thenAnswer(ignored2 -> actualIterator.next());
            when(iteratorMock.hasNext()).thenAnswer(ignored2 -> actualIterator.hasNext());
            return iteratorMock;
        });
        return iterableMock;
    }

    /**
     * Retrieves all repositories from the payload. For these repositories we will mock the events.
     *
     * @param payload
     * @return Map of repositories and List of events for the corresponding repository
     * @throws JsonProcessingException
     */
    private static Map<String, List<String>> collectRepos(String payload) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(payload);
        if (!jsonNode.isArray()) {
            throw new RuntimeException(
                    "Received payload is not an array of events, such as [\n{\n\"id\": ...\n}\n]\n but received %s"
                            .formatted(payload));
        }
        Map<String, List<String>> repoEventsMap = new HashMap<>();
        for (JsonNode event : jsonNode) {
            if (!event.at(REPO_PATH).isTextual()) {
                throw new RuntimeException("Received event does not contain repo attribute on path repo/name. Received: %s"
                        .formatted(event.toPrettyString()));
            }
            String repo = event.at(REPO_PATH).textValue();
            repoEventsMap.compute(repo, (k, v) -> {
                if (v == null) {
                    return new ArrayList<>(List.of(event.toPrettyString()));
                }
                v.add(event.toPrettyString());
                return v;
            });
        }

        return repoEventsMap;
    }

    void reset() {
        Mockito.reset(ghApp);
        Mockito.reset(ghAppInstallation);
    }
}
