package io.xstefank.wildlfy.bot.format;

import io.xstefank.wildlfy.bot.config.CommitsQuantity;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommitsQuantityCheck implements Check {
    private final Pattern VALUE_REGEX = Pattern.compile("^([1-9]|[1-9][0-9]|100)$");
    private final Pattern RANGE_REGEX = Pattern.compile("^([1-9]|[1-9][0-9]|100)-([1-9]|[1-9][0-9]|100)$");
    static final String DEFAULT_MESSAGE = "Number of commits is not within the allowed scope";

    private Integer parsedRangeBottomBoundary;
    private Integer parsedRangeUpperBoundary;
    private String message;

    public CommitsQuantityCheck(CommitsQuantity commitsQuantity) {
        if (commitsQuantity.quantity == null) {
            throw new IllegalArgumentException("Quantity was not set");
        }

        message = (commitsQuantity.message != null) ? commitsQuantity.message : DEFAULT_MESSAGE;
        Matcher valueMatcher = VALUE_REGEX.matcher(commitsQuantity.quantity);
        Matcher rangeMatcher = RANGE_REGEX.matcher(commitsQuantity.quantity);

        if (valueMatcher.matches()) {
            parsedRangeBottomBoundary = Integer.parseInt(commitsQuantity.quantity);
        }
        else if (rangeMatcher.matches()) {
            parsedRangeBottomBoundary = Integer.parseInt(rangeMatcher.group(1));
            parsedRangeUpperBoundary = Integer.parseInt(rangeMatcher.group(2));

            if (parsedRangeBottomBoundary >= parsedRangeUpperBoundary) {
                throw new IllegalArgumentException("First value of range should be lower, than second one.");
            }
        }
        else {
            throw new IllegalArgumentException("Unrecognized commit range syntax. Use specific number, or range in format 1-20. Allowed upper boundary is 100.");
        }
    }

    @Override
    public String check(GHPullRequest pullRequest) throws IOException {
        int numberOfCommits = pullRequest.getCommits();

        if (parsedRangeUpperBoundary == null) {
            return (numberOfCommits == parsedRangeBottomBoundary) ? null : message;
        } else {
            return (numberOfCommits >= parsedRangeBottomBoundary &&
                    numberOfCommits <= parsedRangeUpperBoundary) ? null : message;
        }
    }

    @Override
    public String getName() {
        return "commits-quantity";
    }
}
