package io.xstefank.wildfly.bot.format;

import io.xstefank.wildfly.bot.model.RegexDefinition;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.PagedIterable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEFAULT_COMMIT_MESSAGE;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEPENDABOT;

public class CommitMessagesCheck implements Check {

    private Pattern pattern;
    private String message;

    public CommitMessagesCheck(RegexDefinition description) {
        if (description.pattern == null) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }
        pattern = description.pattern;
        message = description.message != null ? description.message : DEFAULT_COMMIT_MESSAGE;
    }

    @Override
    public String check(GHPullRequest pullRequest) throws IOException {
        if (pullRequest.getUser().getLogin().equals(DEPENDABOT)) {
            // skip for dependabot for now
            return null;
        }
        PagedIterable<GHPullRequestCommitDetail> commits = pullRequest.listCommits();
        if (commits != null) {
            for (GHPullRequestCommitDetail commit : commits) {
                if (commit.getCommit() != null) {
                    String commitMessage =  commit.getCommit().getMessage();
                    if (commitMessage.isEmpty()) {
                        return commit.getSha() + ": Commit message is Empty";
                    }

                    Matcher matcher = pattern.matcher(commitMessage);

                    if (!matcher.matches()) {
                        return String.format("For commit: \"%s\" (%s) - %s" , commitMessage, commit.getSha(), this.message);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "commit";
    }
}
