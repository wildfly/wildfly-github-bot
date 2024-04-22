package org.wildfly.bot.utils.testing.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.tuples.Tuple2;
import org.kohsuke.github.GHEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @implNote {@code REPOSITORY}, {@code INSTALLATION} & {@code SENDER} attributes are
 * present in most events received by GitHub SSE. Thus, we generate them for easier testing.
 */
public abstract class GitHubEventPayload {
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String REPO = "repo";
    private static final String REPO_NAME = "name";
    private static final String PAYLOAD = "payload";
    private static final String REPOSITORY = "repository";
    private static final String INSTALLATION = "installation";
    private static final String SENDER = "sender";
    private static final String REPOSITORY_VALUE = """
            {
                  "id" : 41363498,
                  "node_id" : "MDEwOlJlcG9zaXRvcnk0MTM2MzQ5OA==",
                  "name" : "%2$s",
                  "full_name" : "%1$s/%2$s",
                  "private" : false,
                  "owner" : {
                    "login" : "%1$s",
                    "id" : 9353101,
                    "node_id" : "MDQ6VXNlcjkzNTMxMDE=",
                    "avatar_url" : "https://avatars.githubusercontent.com/u/9353101?v=4",
                    "gravatar_id" : "",
                    "url" : "https://api.github.com/users/%1$s",
                    "html_url" : "https://github.com/%1$s",
                    "followers_url" : "https://api.github.com/users/%1$s/followers",
                    "following_url" : "https://api.github.com/users/%1$s/following{/other_user}",
                    "gists_url" : "https://api.github.com/users/%1$s/gists{/gist_id}",
                    "starred_url" : "https://api.github.com/users/%1$s/starred{/owner}{/repo}",
                    "subscriptions_url" : "https://api.github.com/users/%1$s/subscriptions",
                    "organizations_url" : "https://api.github.com/users/%1$s/orgs",
                    "repos_url" : "https://api.github.com/users/%1$s/repos",
                    "events_url" : "https://api.github.com/users/%1$s/events{/privacy}",
                    "received_events_url" : "https://api.github.com/users/%1$s/received_events",
                    "type" : "User",
                    "site_admin" : false
                  },
                  "html_url" : "https://github.com/%1$s/%2$s",
                  "description" : "%2$s Application Server",
                  "fork" : true,
                  "url" : "https://api.github.com/repos/%1$s/%2$s",
                  "forks_url" : "https://api.github.com/repos/%1$s/%2$s/forks",
                  "keys_url" : "https://api.github.com/repos/%1$s/%2$s/keys{/key_id}",
                  "collaborators_url" : "https://api.github.com/repos/%1$s/%2$s/collaborators{/collaborator}",
                  "teams_url" : "https://api.github.com/repos/%1$s/%2$s/teams",
                  "hooks_url" : "https://api.github.com/repos/%1$s/%2$s/hooks",
                  "issue_events_url" : "https://api.github.com/repos/%1$s/%2$s/issues/events{/number}",
                  "events_url" : "https://api.github.com/repos/%1$s/%2$s/events",
                  "assignees_url" : "https://api.github.com/repos/%1$s/%2$s/assignees{/user}",
                  "branches_url" : "https://api.github.com/repos/%1$s/%2$s/branches{/branch}",
                  "tags_url" : "https://api.github.com/repos/%1$s/%2$s/tags",
                  "blobs_url" : "https://api.github.com/repos/%1$s/%2$s/git/blobs{/sha}",
                  "git_tags_url" : "https://api.github.com/repos/%1$s/%2$s/git/tags{/sha}",
                  "git_refs_url" : "https://api.github.com/repos/%1$s/%2$s/git/refs{/sha}",
                  "trees_url" : "https://api.github.com/repos/%1$s/%2$s/git/trees{/sha}",
                  "statuses_url" : "https://api.github.com/repos/%1$s/%2$s/statuses/{sha}",
                  "languages_url" : "https://api.github.com/repos/%1$s/%2$s/languages",
                  "stargazers_url" : "https://api.github.com/repos/%1$s/%2$s/stargazers",
                  "contributors_url" : "https://api.github.com/repos/%1$s/%2$s/contributors",
                  "subscribers_url" : "https://api.github.com/repos/%1$s/%2$s/subscribers",
                  "subscription_url" : "https://api.github.com/repos/%1$s/%2$s/subscription",
                  "commits_url" : "https://api.github.com/repos/%1$s/%2$s/commits{/sha}",
                  "git_commits_url" : "https://api.github.com/repos/%1$s/%2$s/git/commits{/sha}",
                  "comments_url" : "https://api.github.com/repos/%1$s/%2$s/comments{/number}",
                  "issue_comment_url" : "https://api.github.com/repos/%1$s/%2$s/issues/comments{/number}",
                  "contents_url" : "https://api.github.com/repos/%1$s/%2$s/contents/{+path}",
                  "compare_url" : "https://api.github.com/repos/%1$s/%2$s/compare/{base}...{head}",
                  "merges_url" : "https://api.github.com/repos/%1$s/%2$s/merges",
                  "archive_url" : "https://api.github.com/repos/%1$s/%2$s/{archive_format}{/ref}",
                  "downloads_url" : "https://api.github.com/repos/%1$s/%2$s/downloads",
                  "issues_url" : "https://api.github.com/repos/%1$s/%2$s/issues{/number}",
                  "pulls_url" : "https://api.github.com/repos/%1$s/%2$s/pulls{/number}",
                  "milestones_url" : "https://api.github.com/repos/%1$s/%2$s/milestones{/number}",
                  "notifications_url" : "https://api.github.com/repos/%1$s/%2$s/notifications{?since,all,participating}",
                  "labels_url" : "https://api.github.com/repos/%1$s/%2$s/labels{/name}",
                  "releases_url" : "https://api.github.com/repos/%1$s/%2$s/releases{/id}",
                  "deployments_url" : "https://api.github.com/repos/%1$s/%2$s/deployments",
                  "created_at" : "2015-08-25T13:00:10Z",
                  "updated_at" : "2022-02-10T12:39:03Z",
                  "pushed_at" : "2023-05-31T07:51:01Z",
                  "git_url" : "git://github.com/%1$s/%2$s.git",
                  "ssh_url" : "git@github.com:%1$s/%2$s.git",
                  "clone_url" : "https://github.com/%1$s/%2$s.git",
                  "svn_url" : "https://github.com/%1$s/%2$s",
                  "homepage" : "http://%2$s.org",
                  "size" : 259745,
                  "stargazers_count" : 0,
                  "watchers_count" : 0,
                  "language" : "Java",
                  "has_issues" : false,
                  "has_projects" : true,
                  "has_downloads" : true,
                  "has_wiki" : false,
                  "has_pages" : false,
                  "has_discussions" : false,
                  "forks_count" : 0,
                  "mirror_url" : null,
                  "archived" : false,
                  "disabled" : false,
                  "open_issues_count" : 1,
                  "license" : {
                    "key" : "lgpl-2.1",
                    "name" : "GNU Lesser General Public License v2.1",
                    "spdx_id" : "LGPL-2.1",
                    "url" : "https://api.github.com/licenses/lgpl-2.1",
                    "node_id" : "MDc6TGljZW5zZTEx"
                  },
                  "allow_forking" : true,
                  "is_template" : false,
                  "web_commit_signoff_required" : false,
                  "topics" : [ ],
                  "visibility" : "public",
                  "forks" : 0,
                  "open_issues" : 1,
                  "watchers" : 0,
                  "default_branch" : "main"
                }""";

    private static final String SENDER_VALUE = """
            {
                  "login" : "%1$s",
                  "id" : 9353101,
                  "node_id" : "MDQ6VXNlcjkzNTMxMDE=",
                  "avatar_url" : "https://avatars.githubusercontent.com/u/9353101?v=4",
                  "gravatar_id" : "",
                  "url" : "https://api.github.com/users/%1$s",
                  "html_url" : "https://github.com/%1$s",
                  "followers_url" : "https://api.github.com/users/%1$s/followers",
                  "following_url" : "https://api.github.com/users/%1$s/following{/other_user}",
                  "gists_url" : "https://api.github.com/users/%1$s/gists{/gist_id}",
                  "starred_url" : "https://api.github.com/users/%1$s/starred{/owner}{/repo}",
                  "subscriptions_url" : "https://api.github.com/users/%1$s/subscriptions",
                  "organizations_url" : "https://api.github.com/users/%1$s/orgs",
                  "repos_url" : "https://api.github.com/users/%1$s/repos",
                  "events_url" : "https://api.github.com/users/%1$s/events{/privacy}",
                  "received_events_url" : "https://api.github.com/users/%1$s/received_events",
                  "type" : "User",
                  "site_admin" : false
              }""";

    private static final String INSTALLATION_VALUE = """
            {
                  "id" : 22950279,
                  "node_id" : "MDIzOkludGVncmF0aW9uSW5zdGFsbGF0aW9uMjI5NTAyNzk="
                }""";
    private static final List<Tuple2<String, String>> autogeneratedAttributes = List.of(Tuple2.of(REPOSITORY, REPOSITORY_VALUE), Tuple2.of(SENDER, SENDER_VALUE), Tuple2.of(INSTALLATION, INSTALLATION_VALUE));

    private final String login;
    private final String repo;
    private final GHEvent event;
    private final long eventId;
    private final JsonNode json;
    private final JsonNode payload;


    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<GHEvent, String> eventToStringMap = Map.ofEntries(
            Map.entry(GHEvent.COMMIT_COMMENT, "CommitCommentEvent"),
            Map.entry(GHEvent.CREATE, "CreateEvent"),
            Map.entry(GHEvent.DELETE, "DeleteEvent"),
            Map.entry(GHEvent.FORK, "ForkEvent"),
            Map.entry(GHEvent.GOLLUM, "GollumEvent"),
            Map.entry(GHEvent.ISSUE_COMMENT, "IssueCommentEvent"),
            Map.entry(GHEvent.ISSUES, "IssuesEvent"),
            Map.entry(GHEvent.MEMBER, "MemberEvent"),
            Map.entry(GHEvent.PUBLIC, "PublicEvent"),
            Map.entry(GHEvent.PULL_REQUEST, "PullRequestEvent"),
            Map.entry(GHEvent.PULL_REQUEST_REVIEW, "PullRequestReviewEvent"),
            Map.entry(GHEvent.PULL_REQUEST_REVIEW_COMMENT, "PullRequestReviewCommentEvent"),
            Map.entry(GHEvent.PUSH, "PushEvent"),
            Map.entry(GHEvent.RELEASE, "ReleaseEvent"),
            Map.entry(GHEvent.WATCH, "WatchEvent"));

    public GitHubEventPayload(String fileName, String repository, GHEvent event, long eventId) throws IOException {
        if (!eventToStringMap.containsKey(event)) {
            throw new RuntimeException("[%s] event type is not allowed as GitHubEventPayload. Please see https://docs.github.com/en/rest/using-the-rest-api/github-event-types?apiVersion=2022-11-28");
        }

        String[] repoSplit = repository.split("/");
        if (repoSplit.length != 2) {
            throw new RuntimeException("Repository is expected in following format login/repo but received [%s]".formatted(repository));
        }

        this.login = repoSplit[0];
        this.repo = repoSplit[1];
        this.event = event;
        this.eventId = eventId;
        File jsonFile = new File("src/test/resources/" + fileName);
        if (!jsonFile.exists()) {
            throw new FileNotFoundException("File " + fileName + " cannot be found or does not exist.");
        }
        this.json = objectMapper.readTree(jsonFile);
        this.payload = json.get(PAYLOAD);
    }

    @Override
    public String toString() {
        ((ObjectNode) json).put(ID, eventId);
        ((ObjectNode) json).put(TYPE, eventToStringMap.get(this.event));
        ((ObjectNode) json.get(REPO)).put(REPO_NAME, this.repo);

        JsonNode payload = json.get(PAYLOAD);
        // this adds autogenerated attributes, such as "repository", "sender" and "installation"
        for (Tuple2<String, String> attribute : autogeneratedAttributes) {
            try {
                JsonNode value = objectMapper.readTree(attribute.getItem2().formatted(this.login, this.repo));
                // ensures putIfAbsent passes new value, put is deprecated
                ((ObjectNode) payload).remove(attribute.getItem1());
                ((ObjectNode) payload).putIfAbsent(attribute.getItem1(), value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return json.toPrettyString();
    }

    /**
     * For manipulating the template Json, where you receive only {@link JsonNode} with the payload attribute.
     * Attributes "id", "type" & "repo" and "repository", "sender" & "installation" will get generated by this class.
     */
    protected ObjectNode getPayload() {
        return (ObjectNode) payload;
    }

    protected ObjectNode getPayload(String path) {

        if (!payload.has(path)) {
            ((ObjectNode) payload).put(path, "");
        }
        return (ObjectNode) payload.get(path);
    }
}
