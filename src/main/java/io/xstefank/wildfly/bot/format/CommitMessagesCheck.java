package io.xstefank.wildfly.bot.format;

import io.xstefank.wildfly.bot.model.RegexDefinition;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.PagedIterable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

public class CommitMessagesCheck implements Check {

    static final String DEFAULT_MESSAGE = "One of the commit messages has wrong format";

    private Pattern pattern;
    private String message;

    public CommitMessagesCheck(RegexDefinition description) {
        if (description.pattern == null) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }
        pattern = description.pattern;
        message = (description.message != null) ? description.message : DEFAULT_MESSAGE;
    }

    @Override
    public String check(GHPullRequest pullRequest) throws IOException {
        PagedIterable<GHPullRequestCommitDetail> commits = pullRequest.listCommits();
        for (GHPullRequestCommitDetail commit : commits) {

            String commitSha =  commit.getSha();
            if (commitSha.isEmpty()) {
                return commit.getSha() + ": Commit message is Empty";
            }

            Matcher matcher = pattern.matcher(commitSha);

            if (!matcher.matches()) {
                return String.format("For commit: \"%s\" %s" , commitSha, this.message);
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "commits-message";
    }
}
