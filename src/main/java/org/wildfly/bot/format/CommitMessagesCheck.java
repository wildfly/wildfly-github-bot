package org.wildfly.bot.format;

import org.wildfly.bot.model.RegexDefinition;
import org.wildfly.bot.util.Patterns;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.PagedIterable;
import java.util.regex.Pattern;
import java.io.IOException;

import static org.wildfly.bot.model.RuntimeConstants.DEPENDABOT;

public class CommitMessagesCheck implements Check {

    private final Pattern pattern;
    private final String message;

    public CommitMessagesCheck(RegexDefinition description) {
        if (description.pattern == null) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }
        pattern = description.pattern;
        message = description.message;
    }

    @Override
    public String check(GHPullRequest pullRequest) throws IOException {
        if (pullRequest.getUser().getLogin().equals(DEPENDABOT)) {
            // Skip for Dependabot for now
            return null;
        }

        PagedIterable<GHPullRequestCommitDetail> commits = pullRequest.listCommits();
        if (commits != null) {
            boolean oneMatched = false;
            for (GHPullRequestCommitDetail commit : commits) {
                if (commit.getCommit() != null) {
                    String commitMessage = commit.getCommit().getMessage();
                    if (commitMessage.isEmpty()) {
                        return commit.getSha() + ": Commit message is Empty";
                    }

                    if (Patterns.matches(pattern, commitMessage)) {
                        oneMatched = true;
                        break;
                    }
                }
            }
            if (!oneMatched) {
                return String.format(this.message, pattern.pattern());
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return "commit";
    }
}
